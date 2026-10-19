package com.wooyeon.table.domain.safety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.chat.application.dto.TableMember;
import com.wooyeon.table.domain.chat.application.service.AdminChatQueryService;
import com.wooyeon.table.domain.chat.application.service.ChatMemberQueryService;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.feedback.application.dto.FeedbackCommand;
import com.wooyeon.table.domain.feedback.application.service.EventCloseService;
import com.wooyeon.table.domain.feedback.application.service.FeedbackService;
import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import com.wooyeon.table.domain.matching.application.dto.ConfirmedTable;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.domain.profile.application.service.ProfileCommandService;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import com.wooyeon.table.domain.safety.application.dto.Eligibility;
import com.wooyeon.table.domain.safety.application.service.EligibilityService;
import com.wooyeon.table.domain.safety.domain.model.SanctionLevel;
import com.wooyeon.table.domain.safety.domain.repository.UserSanctionRepository;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.domain.user.domain.model.UserRole;
import com.wooyeon.table.global.auth.AuthTokenProvider;
import com.wooyeon.table.support.IntegrationTest;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 신고·차단·조치, 노쇼 반복 제한, 다음 매칭에 싫음·신고·이전 만남 반영, 참가 이력·추천 (SAF-01·02, MAT-05·06, RET-01·02) */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class SafetyNextMatchFlowTest {

    private static final LocalDate FIRST = LocalDate.of(2031, 11, 6);
    private static final LocalDate SECOND = LocalDate.of(2031, 11, 13);

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private MatchingQueryService matchingQueryService;
    @Autowired
    private ChatMemberQueryService chatMemberQueryService;
    @Autowired
    private AdminChatQueryService adminChatQueryService;
    @Autowired
    private EventCloseService eventCloseService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private EligibilityService eligibilityService;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private UserSanctionRepository userSanctionRepository;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private UserCommandService userCommandService;
    @Autowired
    private ProfileCommandService profileCommandService;
    @Autowired
    private ApplicationJpaRepository applicationJpaRepository;
    @Autowired
    private AuthTokenProvider authTokenProvider;
    @Autowired
    private MockMvc mockMvc;

    private final AtomicInteger sequence = new AtomicInteger();

    @Test
    void 싫음_선호와_신고가_있으면_다음_매칭에서_같은_테이블에_앉지_않고_이전_만남은_점수에_반영된다() throws Exception {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        List<Long> regulars = IntStream.range(0, 5).mapToObj(i -> newUser()).toList();
        regulars.forEach(user -> apply(user, first));
        matchingService.runForSlot(first);
        Long firstTable = matchingQueryService.findConfirmedTables(first).get(0).tableId();
        List<TableMember> seats = chatMemberQueryService.membersOfTable(firstTable);
        eventCloseService.close(first);

        // 좌석 1이 좌석 2를 "다시 만나고 싶지 않음", 좌석 3이 좌석 4를 신고
        feedbackService.submit(seats.get(0).userId(), seats.get(0).applicationId(),
                new FeedbackCommand(4, 4, 4, 4, 8, null, Set.of(), Set.of(2)));
        mockMvc.perform(post("/api/app/chat-rooms/" + adminChatQueryService.findByTable(firstTable).roomId()
                                + "/reports").header(HttpHeaders.AUTHORIZATION, token(seats.get(2).userId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatNo\":4,\"reason\":\"OFFENSIVE_LANGUAGE\",\"detail\":\"반말과 비하\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked").value(true))
                .andExpect(jsonPath("$.data.urgent").value(false));

        Long second = eventSlotCommandService.findOrCreate(SECOND);
        regulars.forEach(user -> apply(user, second));
        IntStream.range(0, 4).forEach(i -> apply(newUser(), second));
        matchingService.runForSlot(second);

        List<ConfirmedTable> tables = matchingQueryService.findConfirmedTables(second);
        assertThat(tables).isNotEmpty();
        for (ConfirmedTable table : tables) {
            Set<Long> users = new HashSet<>(table.userIds());
            assertThat(users.containsAll(Set.of(seats.get(0).userId(), seats.get(1).userId())))
                    .as("싫음 선호 쌍은 같은 테이블 금지").isFalse();
            assertThat(users.containsAll(Set.of(seats.get(2).userId(), seats.get(3).userId())))
                    .as("신고 쌍은 같은 테이블 금지").isFalse();
            long metPairs = pairsAmong(users, new HashSet<>(regulars));
            assertThat(matchingQueryService.getTableDetail(table.tableId()).previouslyMetPairs())
                    .as("지난 모임에서 함께 앉았던 쌍 수가 '새로운 사람' 점수 계산에 반영").isEqualTo((int) metPairs);
        }
    }

    @Test
    void 진행_전_테이블_신고는_긴급이고_운영자가_참가_제한하면_신청이_막힌다() throws Exception {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        IntStream.range(0, 5).forEach(i -> apply(newUser(), first));
        matchingService.runForSlot(first);
        Long tableId = matchingQueryService.findConfirmedTables(first).get(0).tableId();
        List<TableMember> seats = chatMemberQueryService.membersOfTable(tableId);
        long roomId = adminChatQueryService.findByTable(tableId).roomId();

        mockMvc.perform(post("/api/app/chat-rooms/" + roomId + "/reports")
                        .header(HttpHeaders.AUTHORIZATION, token(seats.get(0).userId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatNo\":2,\"reason\":\"UNWANTED_CONTACT\",\"block\":false}"))
                .andExpect(jsonPath("$.data.urgent").value(true))
                .andExpect(jsonPath("$.data.blocked").value(false));
        mockMvc.perform(post("/api/app/chat-rooms/" + roomId + "/reports")
                        .header(HttpHeaders.AUTHORIZATION, token(seats.get(0).userId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatNo\":1,\"reason\":\"OTHER\"}"))
                .andExpect(status().isBadRequest());

        String reports = mockMvc.perform(get("/api/admin/reports").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(jsonPath("$.data[0].urgent").value(true))
                .andExpect(jsonPath("$.data[0].reasonLabel").value("원치 않는 연락·만남 강요"))
                .andReturn().getResponse().getContentAsString();
        long reportId = Long.parseLong(reports.replaceAll("(?s).*?\"reportId\":(\\d+).*", "$1"));
        mockMvc.perform(post("/api/admin/reports/" + reportId + "/action").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"RESTRICTED\",\"days\":14,\"memo\":\"연락처 반복 요구\"}"))
                .andExpect(jsonPath("$.data.status").value("ACTIONED"));

        String reported = token(seats.get(1).userId());
        mockMvc.perform(get("/api/app/me/eligibility").header(HttpHeaders.AUTHORIZATION, reported))
                .andExpect(jsonPath("$.data.eligible").value(false))
                .andExpect(jsonPath("$.data.level").value("RESTRICTED"));
        Long open = eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(30));
        mockMvc.perform(post("/api/app/applications").header(HttpHeaders.AUTHORIZATION, reported)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketType\":\"SINGLE\",\"regions\":[\"JAMSIL\"],\"slotIds\":[" + open + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PARTICIPATION_RESTRICTED"));
    }

    @Test
    void 노쇼가_2번이면_30일_자동_참가_제한이_한_번만_기록된다() {
        Long user = newUser();
        IntStream.range(0, 2).forEach(i -> {
            Attendance attendance = Attendance.scheduled(9_000_000L + sequence.incrementAndGet(), user, -1L, -1L);
            attendance.markNoShowCandidateIfAbsent();
            attendance.review(true, java.time.LocalDateTime.now());
            attendanceRepository.save(attendance);
        });

        Eligibility eligibility = eligibilityService.check(user);
        eligibilityService.check(user);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.level()).isEqualTo(SanctionLevel.RESTRICTED);
        assertThat(eligibility.reason()).isEqualTo("노쇼 2회 · 제한 시점 이후 취소 0회");
        assertThat(eligibility.until().toLocalDate())
                .isEqualTo(LocalDate.now().plusDays(EligibilityService.AUTO_RESTRICT_DAYS));
        assertThat(userSanctionRepository.findAllByUserId(user)).hasSize(1);
    }

    @Test
    void 참가_이력과_후기_점수가_높았던_지역으로_다음_일정을_추천한다() throws Exception {
        Long first = eventSlotCommandService.findOrCreate(FIRST);
        IntStream.range(0, 5).forEach(i -> apply(newUser(), first));
        matchingService.runForSlot(first);
        TableMember me = chatMemberQueryService.membersOfTable(
                matchingQueryService.findConfirmedTables(first).get(0).tableId()).get(0);
        eventCloseService.close(first);
        feedbackService.submit(me.userId(), me.applicationId(),
                new FeedbackCommand(5, 5, 4, 5, 10, null, Set.of(), Set.of()));
        Long open = eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(500));

        mockMvc.perform(get("/api/app/me/history").header(HttpHeaders.AUTHORIZATION, token(me.userId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.history[0].eventDate").value(FIRST.toString()))
                .andExpect(jsonPath("$.data.history[0].region").value("홍대/합정"))
                .andExpect(jsonPath("$.data.history[0].attendanceLabel").value("노쇼 후보"))
                .andExpect(jsonPath("$.data.history[0].feedbackSubmitted").value(true))
                .andExpect(jsonPath("$.data.recommendations[?(@.slotId == " + open + ")].region").value("홍대/합정"))
                .andExpect(jsonPath("$.data.recommendations[0].reason")
                        .value("지난 홍대/합정 모임 분위기를 평균 5.0점으로 평가하셨어요"));
    }

    private static long pairsAmong(Set<Long> tableUsers, Set<Long> previousTable) {
        long inBoth = tableUsers.stream().filter(previousTable::contains).count();
        return inBoth * (inBoth - 1) / 2;
    }

    private String token(Long userId) {
        return "Bearer " + authTokenProvider.issue(userId, UserRole.PARTICIPANT);
    }

    private String admin() {
        return "Bearer " + authTokenProvider.issue(-1L, UserRole.ADMIN);
    }

    private Long newUser() {
        int n = sequence.incrementAndGet();
        Long userId = userCommandService.createParticipantIfAbsent("safety-test-" + n, "안전테스트" + n);
        profileCommandService.createIfAbsent(Profile.builder()
                .userId(userId)
                .birthYear(1994)
                .gender(n % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                .mbti(Mbti.of(n % 2 == 0 ? "INFJ" : "ENFP"))
                .interests(Set.of(Interest.FOOD_CAFE, Interest.TRAVEL))
                .traits(Set.of(Trait.LIGHT_HUMOR, Trait.GOOD_LISTENER))
                .preferredTraits(Set.of(PreferredTrait.HUMOROUS))
                .mealBudgets(Set.of(MealBudget.FROM_20K_TO_30K))
                .build());
        return userId;
    }

    private void apply(Long userId, Long slotId) {
        applicationJpaRepository.save(Application.paidSubmission(userId, TicketType.SINGLE,
                Set.of(Region.HONGDAE_HAPJEONG), List.of(slotId)));
    }
}
