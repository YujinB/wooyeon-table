package com.wooyeon.table.domain.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.wooyeon.table.domain.application.application.service.ApplicationCommandService;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.demo.application.DemoSeedService;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.feedback.application.dto.MyHistory;
import com.wooyeon.table.domain.feedback.application.service.FeedbackService;
import com.wooyeon.table.domain.feedback.application.service.FeedbackSummaryService;
import com.wooyeon.table.domain.feedback.application.service.HistoryService;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.domain.operation.application.dto.Dashboard;
import com.wooyeon.table.domain.operation.application.dto.Dashboard.SlotOverview;
import com.wooyeon.table.domain.operation.application.dto.ExceptionItem;
import com.wooyeon.table.domain.operation.application.dto.ExceptionItem.ExceptionType;
import com.wooyeon.table.domain.operation.application.service.OperationQueryService;
import com.wooyeon.table.domain.profile.application.service.ProfileQueryService;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.user.application.DemoAccountInitializer;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.support.IntegrationTest;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** 시연용 시드: 지난 모임은 끝까지 진행돼 있고, 다가오는 회차에는 대기 참가자가 있어 체험 계정이 바로 테이블에 들어간다 */
@IntegrationTest
@Transactional
class DemoSeedServiceTest {

    private static final List<String> DEMO = DemoAccountInitializer.PARTICIPANT_LOGIN_IDS;

    @Autowired
    private DemoSeedService demoSeedService;
    @Autowired
    private OperationQueryService operationQueryService;
    @Autowired
    private FeedbackSummaryService feedbackSummaryService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private ProfileQueryService profileQueryService;
    @Autowired
    private UserCommandService userCommandService;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private ApplicationCommandService applicationCommandService;
    @Autowired
    private ApplicationQueryService applicationQueryService;
    @Autowired
    private MatchingService matchingService;
    @Autowired
    private Clock clock;

    @Test
    void 지난_모임은_종료까지_진행되고_다가오는_4회차에는_대기_참가자가_있다() {
        assertThat(demoSeedService.seedIfEmpty(DEMO)).isTrue();
        assertThat(demoSeedService.seedIfEmpty(DEMO)).isFalse();

        LocalDate today = LocalDate.now(clock);
        LocalDate lastThursday = today.with(TemporalAdjusters.previous(DayOfWeek.THURSDAY));
        Long pastSlot = eventSlotCommandService.findOrCreate(lastThursday);
        Dashboard dashboard = operationQueryService.dashboard();

        // 지난 목요일: 홍대 6명(체험참가자2 포함)·강남 5명 → 테이블 2개, 행사 종료
        SlotOverview past = slot(dashboard, pastSlot);
        assertThat(past.status()).isEqualTo(EventSlotStatus.COMPLETED);
        assertThat(past.confirmed()).isEqualTo(11);
        assertThat(past.tables()).isEqualTo(2);

        // 다가오는 목요일 4회차: 마감 전이고 회차마다 지역별 5명씩 20명 매칭 대기
        List<SlotOverview> upcoming = dashboard.slots().stream()
                .filter(slot -> slot.status() == EventSlotStatus.RECRUITING)
                .filter(slot -> slot.matchAt().isAfter(LocalDateTime.now(clock)))
                .toList();
        assertThat(upcoming).hasSize(4).allSatisfy(slot -> {
            assertThat(slot.eventDate().getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
            assertThat(slot.waiting()).isEqualTo(20);
        });

        // 예외 처리함: 노쇼 후보 1명, 신고 1건 (종료 후라 긴급 아님)
        List<ExceptionItem> exceptions = operationQueryService.exceptions().stream()
                .filter(item -> pastSlot.equals(item.slotId()) || item.type() == ExceptionType.REPORT)
                .toList();
        assertThat(exceptions).extracting(ExceptionItem::type)
                .containsExactlyInAnyOrder(ExceptionType.NO_SHOW, ExceptionType.REPORT);
        assertThat(exceptions).noneMatch(ExceptionItem::urgent);

        // 후기: 체험참가자2를 뺀 참석자 9명 중 6명
        assertThat(feedbackSummaryService.summarize(pastSlot).responses()).isEqualTo(6);

        // 체험참가자2: 참석 완료, 후기는 직접 쓸 수 있게 남아 있음
        Long demo2 = userCommandService.createParticipantIfAbsent(DEMO.get(1), DEMO.get(1));
        MyHistory history = historyService.history(demo2);
        assertThat(history.history()).singleElement().satisfies(entry -> {
            assertThat(entry.attendance()).isEqualTo(AttendanceStatus.ATTENDED);
            assertThat(entry.feedbackSubmitted()).isFalse();
            assertThat(feedbackService.form(demo2, entry.applicationId()).submittable()).isTrue();
        });

        // 결제 완료로 넣은 신청에는 모의 결제 기록이 있음 (운영자 결제·환불 화면)
        assertThat(operationQueryService.payments()).hasSizeGreaterThanOrEqualTo(90)
                .allSatisfy(row -> assertThat(row.payment().success()).isTrue());

        // 체험참가자1~3은 바로 신청 가능, 4~5는 프로필부터
        assertThat(DEMO).extracting(loginId -> profileQueryService.isCompleteForApplication(
                        userCommandService.createParticipantIfAbsent(loginId, loginId)))
                .containsExactly(true, true, true, false, false);
    }

    @Test
    void 체험_계정이_다가오는_회차에_신청하고_마감을_실행하면_테이블에_들어간다() {
        demoSeedService.seedIfEmpty(DEMO);
        LocalDate next = operationQueryService.dashboard().slots().stream()
                .filter(slot -> slot.status() == EventSlotStatus.RECRUITING)
                .filter(slot -> slot.matchAt().isAfter(LocalDateTime.now(clock)))
                .map(SlotOverview::eventDate)
                .min(LocalDate::compareTo)
                .orElseThrow();
        Long slotId = eventSlotCommandService.findOrCreate(next);
        Long demo1 = userCommandService.createParticipantIfAbsent(DEMO.get(0), DEMO.get(0));
        Long applicationId = applicationCommandService.save(Application.paidSubmission(demo1, TicketType.SINGLE,
                Set.of(Region.HONGDAE_HAPJEONG), List.of(slotId)));

        matchingService.runForSlot(slotId);

        assertThat(applicationQueryService.getApplication(applicationId).matchStatus())
                .isEqualTo(MatchStatus.CONFIRMED);
        assertThat(slot(operationQueryService.dashboard(), slotId).tables()).isEqualTo(4);
    }

    private static SlotOverview slot(Dashboard dashboard, Long slotId) {
        return dashboard.slots().stream().filter(slot -> slot.slotId().equals(slotId)).findFirst().orElseThrow();
    }
}
