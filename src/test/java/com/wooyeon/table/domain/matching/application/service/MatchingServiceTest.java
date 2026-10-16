package com.wooyeon.table.domain.matching.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.matching.application.dto.MatchRunSummary;
import com.wooyeon.table.domain.matching.application.scheduler.MatchingScheduler;
import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.matching.domain.model.FailureOutcome;
import com.wooyeon.table.domain.matching.domain.model.MatchFailure;
import com.wooyeon.table.domain.matching.domain.model.UnassignedReason;
import com.wooyeon.table.domain.matching.domain.repository.DiningTableRepository;
import com.wooyeon.table.domain.matching.domain.repository.MatchFailureRepository;
import com.wooyeon.table.domain.profile.application.service.ProfileCommandService;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.support.IntegrationTest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class MatchingServiceTest {

    private static final LocalDate FIRST = LocalDate.of(2030, 1, 3);
    private static final LocalDate SECOND = LocalDate.of(2030, 1, 10);

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private EventSlotQueryService eventSlotQueryService;
    @Autowired
    private UserCommandService userCommandService;
    @Autowired
    private ProfileCommandService profileCommandService;
    @Autowired
    private ApplicationJpaRepository applicationJpaRepository;
    @Autowired
    private DiningTableRepository diningTableRepository;
    @Autowired
    private MatchFailureRepository matchFailureRepository;

    private final AtomicInteger sequence = new AtomicInteger();

    @Test
    void 결제_완료자를_매칭해_확정하고_인원이_모자란_사람은_다음_날짜로_넘기거나_대체_안내한다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        Long second = eventSlotCommandService.findOrCreate(SECOND);
        List<Long> hongdae = apply(5, Region.HONGDAE_HAPJEONG, first);
        List<Long> jamsilBothDates = apply(2, Region.JAMSIL, first, second);
        List<Long> jamsilFirstOnly = apply(1, Region.JAMSIL, first);

        MatchRunSummary summary = matchingService.runForSlot(first);

        assertThat(summary.alreadyExecuted()).isFalse();
        assertThat(summary.candidateCount()).isEqualTo(8);
        assertThat(summary.assignedCount()).isEqualTo(5);
        assertThat(summary.carriedOverCount()).isEqualTo(2);
        assertThat(summary.alternativeOfferedCount()).isEqualTo(1);

        assertThat(applications(hongdae)).allSatisfy(a -> {
            assertThat(a.getMatchStatus()).isEqualTo(MatchStatus.CONFIRMED);
            assertThat(a.getConfirmedSlotId()).isEqualTo(first);
        });
        assertThat(applications(jamsilBothDates)).allSatisfy(a -> {
            assertThat(a.getMatchStatus()).isEqualTo(MatchStatus.WAITING);
            assertThat(a.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        });
        assertThat(applications(jamsilFirstOnly)).singleElement().satisfies(a -> {
            assertThat(a.getMatchStatus()).isEqualTo(MatchStatus.ALTERNATIVE_OFFERED);
            assertThat(a.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_AVAILABLE);
        });

        List<DiningTable> tables = diningTableRepository.findAllBySlotId(first);
        assertThat(tables).singleElement().satisfies(table -> {
            assertThat(table.getRegion()).isEqualTo(Region.HONGDAE_HAPJEONG);
            assertThat(table.size()).isEqualTo(5);
            assertThat(table.getTotalScore()).isGreaterThanOrEqualTo(50);
        });
        List<MatchFailure> failures = matchFailureRepository.findAllByMatchRunId(summary.matchRunId());
        assertThat(failures).allSatisfy(f -> assertThat(f.getReason()).isEqualTo(UnassignedReason.NOT_ENOUGH_CANDIDATES));
        assertThat(failures).filteredOn(f -> f.getOutcome() == FailureOutcome.CARRIED_OVER)
                .extracting(MatchFailure::getNextSlotId).containsOnly(second);
        assertThat(eventSlotQueryService.getSlot(first).status()).isEqualTo(EventSlotStatus.MATCHED);
    }

    @Test
    void 이월된_사람은_다음_회차에서_새_신청자와_함께_매칭된다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        Long second = eventSlotCommandService.findOrCreate(SECOND);
        List<Long> carried = apply(2, Region.JAMSIL, first, second);
        List<Long> secondOnly = apply(2, Region.JAMSIL, second);

        matchingService.runForSlot(first);
        MatchRunSummary summary = matchingService.runForSlot(second);

        assertThat(summary.assignedCount()).isEqualTo(4);
        assertThat(applications(carried)).extracting(Application::getConfirmedSlotId).containsOnly(second);
        assertThat(applications(secondOnly)).extracting(Application::getMatchStatus)
                .containsOnly(MatchStatus.CONFIRMED);
    }

    @Test
    void 같은_회차를_다시_실행하면_기존_결과를_돌려주고_테이블을_중복_생성하지_않는다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        apply(6, Region.GANGNAM_SINNONHYEON, first);

        MatchRunSummary once = matchingService.runForSlot(first);
        MatchRunSummary twice = matchingService.runForSlot(first);

        assertThat(twice.alreadyExecuted()).isTrue();
        assertThat(twice.matchRunId()).isEqualTo(once.matchRunId());
        assertThat(diningTableRepository.findAllBySlotId(first)).hasSize(1);
    }

    @Test
    void 스케줄러는_매칭_시각이_지난_회차만_실행한다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        Long second = eventSlotCommandService.findOrCreate(SECOND);
        apply(4, Region.EULJIRO_JONGNO, first);
        apply(4, Region.EULJIRO_JONGNO, second);
        // 첫 회차 매칭 시각(2일 전 12:00)과 둘째 회차 매칭 시각 사이
        Clock betweenDeadlines = Clock.fixed(FIRST.atTime(13, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));

        new MatchingScheduler(eventSlotQueryService, matchingService, betweenDeadlines).runDueSlots();

        assertThat(eventSlotQueryService.getSlot(first).status()).isEqualTo(EventSlotStatus.MATCHED);
        assertThat(eventSlotQueryService.getSlot(second).status()).isEqualTo(EventSlotStatus.RECRUITING);
    }

    /** 서로 잘 맞는 신청자 count명 (성별 번갈아, 같은 관심사) */
    private List<Long> apply(int count, Region region, Long... slotIds) {
        return IntStream.range(0, count).mapToObj(i -> {
            int n = sequence.incrementAndGet();
            Long userId = userCommandService.createParticipantIfAbsent("matching-test-" + n, "테스트" + n);
            profileCommandService.createIfAbsent(Profile.builder()
                    .userId(userId)
                    .birthYear(2000)
                    .gender(n % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                    .mbti(Mbti.of(n % 2 == 0 ? "ENFP" : "INFJ"))
                    .interests(Set.of(Interest.FOOD_CAFE, Interest.TRAVEL))
                    .traits(Set.of(Trait.LIGHT_HUMOR, Trait.GOOD_LISTENER))
                    .preferredTraits(Set.of(PreferredTrait.HUMOROUS))
                    .build());
            return applicationJpaRepository.save(
                    Application.paidSubmission(userId, TicketType.SINGLE, Set.of(region), List.of(slotIds))).getId();
        }).toList();
    }

    private List<Application> applications(List<Long> ids) {
        return applicationJpaRepository.findAllByIdIn(ids);
    }
}
