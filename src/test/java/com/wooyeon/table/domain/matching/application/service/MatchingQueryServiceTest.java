package com.wooyeon.table.domain.matching.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.matching.application.dto.FailureDetail;
import com.wooyeon.table.domain.matching.application.dto.MyMatchResult;
import com.wooyeon.table.domain.matching.application.dto.ScoreBreakdown;
import com.wooyeon.table.domain.matching.application.dto.SlotMatchingResult;
import com.wooyeon.table.domain.matching.application.dto.TableDetail;
import com.wooyeon.table.domain.matching.domain.model.FailureOutcome;
import com.wooyeon.table.domain.matching.domain.model.UnassignedReason;
import com.wooyeon.table.domain.profile.application.service.ProfileCommandService;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class MatchingQueryServiceTest {

    private static final LocalDate FIRST = LocalDate.of(2030, 1, 3);
    private static final LocalDate SECOND = LocalDate.of(2030, 1, 10);

    @Autowired
    private MatchingQueryService matchingQueryService;
    @Autowired
    private MatchingService matchingService;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private UserCommandService userCommandService;
    @Autowired
    private ProfileCommandService profileCommandService;
    @Autowired
    private ApplicationJpaRepository applicationJpaRepository;

    private final AtomicInteger sequence = new AtomicInteger();

    @Test
    void 운영자는_회차_결과에서_테이블과_미배정자의_사유_처리_결과를_본다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        Long second = eventSlotCommandService.findOrCreate(SECOND);
        apply(5, Region.HONGDAE_HAPJEONG, first);
        List<Long> carried = apply(2, Region.JAMSIL, first, second);
        List<Long> alternative = apply(1, Region.JAMSIL, first);
        matchingService.runForSlot(first);

        SlotMatchingResult result = matchingQueryService.getSlotResult(first);

        assertThat(result.eventDate()).isEqualTo(FIRST);
        assertThat(result.run().assignedCount()).isEqualTo(5);
        assertThat(result.appliedPolicy()).contains("minScore=50");
        assertThat(result.tables()).singleElement().satisfies(table -> {
            assertThat(table.regionLabel()).isEqualTo("홍대/합정");
            assertThat(table.size()).isEqualTo(5);
        });
        assertThat(result.failures()).hasSize(3)
                .allSatisfy(f -> assertThat(f.reason()).isEqualTo(UnassignedReason.NOT_ENOUGH_CANDIDATES));
        assertThat(result.failures()).filteredOn(f -> carried.contains(f.applicationId()))
                .allSatisfy(f -> {
                    assertThat(f.outcome()).isEqualTo(FailureOutcome.CARRIED_OVER);
                    assertThat(f.nextEventDate()).isEqualTo(SECOND);
                    assertThat(f.name()).startsWith("테스트");
                });
        assertThat(result.failures()).filteredOn(f -> alternative.contains(f.applicationId()))
                .extracting(FailureDetail::outcome).containsExactly(FailureOutcome.ALTERNATIVE_OFFERED);
    }

    @Test
    void 운영자는_테이블_상세에서_점수_항목별_내역과_좌석별_프로필_배치_사유를_본다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        apply(5, Region.HONGDAE_HAPJEONG, first);
        matchingService.runForSlot(first);
        Long tableId = matchingQueryService.getSlotResult(first).tables().get(0).tableId();

        TableDetail detail = matchingQueryService.getTableDetail(tableId);

        ScoreBreakdown score = detail.score();
        assertThat(score.total()).isGreaterThanOrEqualTo(50);
        assertThat(score.genderBalance() + score.mbti() + score.interest() + score.preference() + score.novelty())
                .isCloseTo(score.total(), within(0.3));
        assertThat(detail.minAge()).isEqualTo(30);
        assertThat(detail.mbtiComposition()).hasSize(5);
        assertThat(detail.commonInterests()).containsExactlyInAnyOrder("맛집·카페 탐방", "여행");
        assertThat(detail.seats()).hasSize(5).allSatisfy(seat -> {
            assertThat(seat.name()).startsWith("테스트");
            assertThat(seat.age()).isEqualTo(30);
            assertThat(seat.interests()).contains("맛집·카페 탐방");
            assertThat(seat.placementDescription()).isNotBlank();
            assertThat(seat.preferenceMet()).isTrue();
        });
    }

    @Test
    void 확정된_참가자는_다른_사람_정보_없이_왜_이_테이블인지_본다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        List<Long> hongdae = apply(5, Region.HONGDAE_HAPJEONG, first);
        matchingService.runForSlot(first);

        MyMatchResult mine = matchingQueryService.getMyResult(hongdae.get(0));

        assertThat(mine.matchStatus()).isEqualTo(MatchStatus.CONFIRMED);
        assertThat(mine.failure()).isNull();
        assertThat(mine.table().eventDate()).isEqualTo(FIRST);
        assertThat(mine.table().size()).isEqualTo(5);
        assertThat(mine.table().reasons())
                .anySatisfy(reason -> assertThat(reason).contains("홍대/합정", "5명", "30~30세"))
                .anySatisfy(reason -> assertThat(reason).contains("성비"))
                .anySatisfy(reason -> assertThat(reason).contains("맛집·카페 탐방", "여행"))
                .anySatisfy(reason -> assertThat(reason).contains("편하고 유머 있는 사람"));
    }

    @Test
    void 테이블에_들지_못한_참가자는_사유와_다음_단계를_본다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        Long second = eventSlotCommandService.findOrCreate(SECOND);
        apply(5, Region.HONGDAE_HAPJEONG, first);
        Long carried = apply(2, Region.JAMSIL, first, second).get(0);
        Long alternative = apply(1, Region.JAMSIL, first).get(0);
        matchingService.runForSlot(first);

        MyMatchResult carriedResult = matchingQueryService.getMyResult(carried);
        MyMatchResult alternativeResult = matchingQueryService.getMyResult(alternative);

        assertThat(carriedResult.matchStatus()).isEqualTo(MatchStatus.WAITING);
        assertThat(carriedResult.table()).isNull();
        assertThat(carriedResult.message()).contains("다음 날짜");
        assertThat(carriedResult.failure().outcome()).isEqualTo(FailureOutcome.CARRIED_OVER);
        assertThat(carriedResult.failure().nextEventDate()).isEqualTo(SECOND);
        assertThat(carriedResult.failure().reason())
                .isEqualTo(UnassignedReason.NOT_ENOUGH_CANDIDATES.getDescription());

        assertThat(alternativeResult.matchStatus()).isEqualTo(MatchStatus.ALTERNATIVE_OFFERED);
        assertThat(alternativeResult.message()).contains("환불");
        assertThat(alternativeResult.failure().outcome()).isEqualTo(FailureOutcome.ALTERNATIVE_OFFERED);
        assertThat(alternativeResult.failure().nextEventDate()).isNull();
    }

    @Test
    void 매칭_전이면_대기_상태와_빈_결과를_돌려준다() {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        Long waiting = apply(1, Region.JAMSIL, first).get(0);

        SlotMatchingResult result = matchingQueryService.getSlotResult(first);
        MyMatchResult mine = matchingQueryService.getMyResult(waiting);

        assertThat(result.run()).isNull();
        assertThat(result.tables()).isEmpty();
        assertThat(result.failures()).isEmpty();
        assertThat(mine.matchStatus()).isEqualTo(MatchStatus.WAITING);
        assertThat(mine.message()).contains("매칭 대기");
        assertThat(mine.table()).isNull();
        assertThat(mine.failure()).isNull();
    }

    @Test
    void 없는_테이블이나_신청을_조회하면_예외() {
        assertThatThrownBy(() -> matchingQueryService.getTableDetail(-1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> matchingQueryService.getMyResult(-1L)).isInstanceOf(BusinessException.class);
    }

    /** 서로 잘 맞는 신청자 count명 (성별 번갈아, 같은 관심사, 서로 원하는 성향 충족) */
    private List<Long> apply(int count, Region region, Long... slotIds) {
        return IntStream.range(0, count).mapToObj(i -> {
            int n = sequence.incrementAndGet();
            Long userId = userCommandService.createParticipantIfAbsent("query-test-" + n, "테스트" + n);
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
}
