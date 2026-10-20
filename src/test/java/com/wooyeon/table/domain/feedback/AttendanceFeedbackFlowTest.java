package com.wooyeon.table.domain.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.chat.application.dto.TableMember;
import com.wooyeon.table.domain.chat.application.service.ChatMemberQueryService;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.application.service.SlotDeletionService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.application.service.EventCloseService;
import com.wooyeon.table.domain.feedback.domain.model.PersonPreference;
import com.wooyeon.table.domain.feedback.domain.model.PreferenceType;
import com.wooyeon.table.domain.feedback.domain.repository.PersonPreferenceRepository;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import com.wooyeon.table.domain.notification.domain.repository.NotificationRepository;
import com.wooyeon.table.domain.profile.application.service.ProfileCommandService;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.domain.user.domain.model.UserRole;
import com.wooyeon.table.global.auth.AuthTokenProvider;
import com.wooyeon.table.support.IntegrationTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import com.wooyeon.table.domain.feedback.application.service.FeedbackReminderService;
import com.wooyeon.table.domain.feedback.application.service.HistoryService;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import com.wooyeon.table.domain.feedback.domain.repository.FeedbackRepository;
import com.wooyeon.table.domain.notification.application.service.NotificationService;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 확정 → 참석 예정 → 체크인·취소 → 행사 종료(노쇼 후보·피드백 요청) → 운영자 확인 → 후기·사람별 선호 (FDB-01~04) */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class AttendanceFeedbackFlowTest {

    private static final LocalDate DATE = LocalDate.of(2031, 9, 4);

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private MatchingQueryService matchingQueryService;
    @Autowired
    private ChatMemberQueryService chatMemberQueryService;
    @Autowired
    private EventCloseService eventCloseService;
    @Autowired
    private PersonPreferenceRepository personPreferenceRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private SlotDeletionService slotDeletionService;
    @Autowired
    private EventSlotQueryService eventSlotQueryService;
    @Autowired
    private UserCommandService userCommandService;
    @Autowired
    private ProfileCommandService profileCommandService;
    @Autowired
    private ApplicationJpaRepository applicationJpaRepository;
    @Autowired
    private AuthTokenProvider authTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    private final AtomicInteger sequence = new AtomicInteger();
    private Long slot;
    private Long tableId;
    /** 좌석 순 멤버 */
    private List<TableMember> members;

    @BeforeEach
    void 다섯_명_테이블_확정() {
        slot = eventSlotCommandService.findOrCreate(DATE);
        IntStream.range(0, 5).forEach(i -> apply(slot));
        matchingService.runForSlot(slot);
        tableId = matchingQueryService.findConfirmedTables(slot).get(0).tableId();
        members = chatMemberQueryService.membersOfTable(tableId);
    }

    @Test
    void 체크인하면_참석_취소하면_사전_취소_종료_처리하면_나머지는_노쇼_후보이고_운영자가_확정_정정한다() throws Exception {
        attendance(0, "").andExpect(jsonPath("$.data.status").value("SCHEDULED"));
        attendance(0, "/check-in").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ATTENDED"));
        attendance(1, "/cancel").andExpect(jsonPath("$.data.status").value("CANCELED_EARLY"));
        attendance(1, "/check-in").andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/slots/" + slot + "/close").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants").value(5))
                .andExpect(jsonPath("$.data.attended").value(1))
                .andExpect(jsonPath("$.data.noShowCandidates").value(3))
                .andExpect(jsonPath("$.data.canceled").value(1));

        List<AttendanceView> candidates = eventCloseService.noShowCandidates().stream()
                .filter(candidate -> candidate.slotId().equals(slot)).toList();
        assertThat(candidates).hasSize(3);
        assertThat(eventCloseService.review(candidates.get(0).attendanceId(), true).statusLabel()).isEqualTo("노쇼");
        assertThat(eventCloseService.review(candidates.get(1).attendanceId(), false).statusLabel())
                .isEqualTo("참석 완료");

        assertThat(members).allSatisfy(member -> assertThat(
                notificationRepository.findAllByApplicationId(member.applicationId()).stream()
                        .map(Notification::getType).anyMatch(NotificationType.FEEDBACK_REQUESTED::equals))
                .isEqualTo(!member.equals(members.get(1))));
        attendance(2, "/check-in").andExpect(status().isConflict());
    }

    @Test
    void 종료_후_참석자는_후기와_사람별_선호를_한_번_남기고_선호는_비공개() throws Exception {
        feedback(0).andExpect(jsonPath("$.data.submittable").value(false))
                .andExpect(jsonPath("$.data.unavailableReason").value("모임이 끝난 뒤에 후기를 남길 수 있어요"));
        attendance(0, "/check-in");
        attendance(1, "/cancel");
        eventCloseService.close(slot);

        String form = feedback(0)
                .andExpect(jsonPath("$.data.submittable").value(true))
                .andExpect(jsonPath("$.data.others.length()").value(4))
                .andExpect(jsonPath("$.data.others[0].alias").value("참가자 2"))
                .andReturn().getResponse().getContentAsString();
        assertThat(form).doesNotContain("userId", "후기테스트");

        submit(0, Map.of("wantAgainSeats", List.of(2), "avoidSeats", List.of(1)))
                .andExpect(status().isBadRequest());
        submit(0, Map.of("wantAgainSeats", List.of(2, 3), "avoidSeats", List.of(3)))
                .andExpect(status().isBadRequest());
        submit(0, Map.of("wantAgainSeats", List.of(2, 3), "avoidSeats", List.of(4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitted").value(true))
                .andExpect(jsonPath("$.data.mine.tableMood").value(5))
                .andExpect(jsonPath("$.data.mine.recommendScore").value(9))
                .andExpect(jsonPath("$.data.mine.avoidSeats[0]").value(4));
        submit(0, Map.of()).andExpect(status().isConflict());
        submit(1, Map.of()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("사전 취소")));

        List<Long> userIds = members.stream().map(TableMember::userId).toList();
        assertThat(personPreferenceRepository.findAmong(userIds, PreferenceType.AVOID)).singleElement()
                .satisfies(avoid -> {
                    assertThat(avoid.getFromUserId()).isEqualTo(members.get(0).userId());
                    assertThat(avoid.getToUserId()).isEqualTo(members.get(3).userId());
                });
        assertThat(personPreferenceRepository.findAmong(userIds, PreferenceType.WANT_AGAIN))
                .extracting(PersonPreference::getToUserId)
                .containsExactlyInAnyOrder(members.get(1).userId(), members.get(2).userId());
        feedback(3).andExpect(jsonPath("$.data.mine").doesNotExist());

        // 운영자 요약: 평균·순추천지수·한마디만, 사람별 선호는 담지 않음
        String summary = mockMvc.perform(get("/api/admin/slots/" + slot + "/feedback")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responses").value(1))
                .andExpect(jsonPath("$.data.averages.tableMood").value(5.0))
                .andExpect(jsonPath("$.data.nps").value(100))
                .andExpect(jsonPath("$.data.tables.length()").value(1))
                .andExpect(jsonPath("$.data.comments[0].comment").value("즐거웠어요"))
                .andReturn().getResponse().getContentAsString();
        assertThat(summary).doesNotContain("avoidSeats", "wantAgainSeats");
    }

    @Test
    void 행사_시작_4시간이_지난_매칭_완료_회차만_자동_종료_대상이다() {
        LocalDateTime startsAt = DATE.atTime(19, 30);
        assertThat(eventSlotQueryService.findSlotIdsDueForClosing(startsAt.plusHours(3), 4)).doesNotContain(slot);
        assertThat(eventSlotQueryService.findSlotIdsDueForClosing(startsAt.plusHours(4), 4)).contains(slot);

        eventCloseService.close(slot);
        assertThat(eventSlotQueryService.findSlotIdsDueForClosing(startsAt.plusHours(5), 4)).doesNotContain(slot);
    }

    @Test
    void 다음_날_후기를_안_쓴_참석자에게만_한_번_리마인드하고_새_회차가_열리면_추천_알림을_보낸다() throws Exception {
        attendance(0, "/check-in");
        attendance(1, "/check-in");
        eventCloseService.close(slot);
        submit(1, Map.of());

        // 모임 다음 날 20시: 참석했는데 후기를 안 쓴 좌석 1만, 두 번 돌려도 한 번
        Clock nextDay = Clock.fixed(DATE.plusDays(1).atTime(20, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        FeedbackReminderService reminder = new FeedbackReminderService(attendanceRepository, feedbackRepository,
                eventSlotQueryService, notificationService, historyService, nextDay);
        reminder.remindMissingFeedback();
        reminder.remindMissingFeedback();
        assertThat(notificationRepository.existsByDedupKey("FEEDBACK_REMINDER:" + members.get(0).applicationId()))
                .isTrue();
        assertThat(notificationRepository.existsByDedupKey("FEEDBACK_REMINDER:" + members.get(1).applicationId()))
                .as("후기를 쓴 사람").isFalse();
        assertThat(notificationRepository.existsByDedupKey("FEEDBACK_REMINDER:" + members.get(2).applicationId()))
                .as("노쇼 후보").isFalse();

        // 운영자가 새 회차를 열면 참석 이력이 있는 사람에게 추천 알림
        Long opened = eventSlotCommandService.create(LocalDate.now().plusDays(60), null, null);
        assertThat(notificationRepository.existsByDedupKey("NEXT_SLOT_OPENED:" + opened + ":" + members.get(0).userId()))
                .isTrue();
        assertThat(notificationRepository.existsByDedupKey("NEXT_SLOT_OPENED:" + opened + ":" + members.get(2).userId()))
                .as("참석 이력 없음").isFalse();

        // 잘못 만든 회차를 지우면 보낸 모집 알림도 거둬들인다
        slotDeletionService.delete(opened);
        assertThat(notificationRepository.existsByDedupKey("NEXT_SLOT_OPENED:" + opened + ":" + members.get(0).userId()))
                .isFalse();
    }

    private ResultActions attendance(int seat, String action) throws Exception {
        String url = "/api/app/applications/" + members.get(seat).applicationId() + "/attendance" + action;
        return mockMvc.perform((action.isEmpty() ? get(url) : post(url))
                .header(HttpHeaders.AUTHORIZATION, token(members.get(seat).userId())));
    }

    private ResultActions feedback(int seat) throws Exception {
        return mockMvc.perform(get("/api/app/applications/" + members.get(seat).applicationId() + "/feedback")
                .header(HttpHeaders.AUTHORIZATION, token(members.get(seat).userId())));
    }

    private ResultActions submit(int seat, Map<String, Object> preferences) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("tableMood", 5, "conversation", 4, "venue", 4,
                "rejoinIntent", 5, "recommendScore", 9, "comment", "즐거웠어요"));
        body.putAll(preferences);
        return mockMvc.perform(post("/api/app/applications/" + members.get(seat).applicationId() + "/feedback")
                .header(HttpHeaders.AUTHORIZATION, token(members.get(seat).userId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String token(Long userId) {
        return "Bearer " + authTokenProvider.issue(userId, UserRole.PARTICIPANT);
    }

    private String admin() {
        return "Bearer " + authTokenProvider.issue(-1L, UserRole.ADMIN);
    }

    private void apply(Long slotId) {
        int n = sequence.incrementAndGet();
        Long userId = userCommandService.createParticipantIfAbsent("feedback-test-" + n, "후기테스트" + n);
        profileCommandService.createIfAbsent(Profile.builder()
                .userId(userId)
                .birthYear(1995)
                .gender(n % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                .mbti(Mbti.of("ENFP"))
                .interests(Set.of(Interest.FOOD_CAFE, Interest.TRAVEL))
                .traits(Set.of(Trait.LIGHT_HUMOR))
                .preferredTraits(Set.of(PreferredTrait.HUMOROUS))
                .mealBudgets(Set.of(MealBudget.FROM_20K_TO_30K))
                .build());
        applicationJpaRepository.save(Application.paidSubmission(userId, TicketType.SINGLE,
                Set.of(Region.HONGDAE_HAPJEONG), List.of(slotId)));
    }
}
