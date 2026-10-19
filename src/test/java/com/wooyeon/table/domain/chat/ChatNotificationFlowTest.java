package com.wooyeon.table.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.chat.application.dto.AdminChatRoomView;
import com.wooyeon.table.domain.chat.application.service.AdminChatQueryService;
import com.wooyeon.table.domain.chat.application.service.ChatRoomService;
import com.wooyeon.table.domain.chat.domain.model.ChatMessageKind;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.domain.notification.application.service.NotificationDispatcher;
import com.wooyeon.table.domain.notification.application.service.NotificationService;
import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationStatus;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import com.wooyeon.table.domain.notification.domain.repository.NotificationRepository;
import com.wooyeon.table.domain.notification.infrastructure.push.MockPushGateway;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 확정 → 단체방·공지·알림·리마인드 예약, 공개 범위, 발송 재시도·운영자 예외 (INF-01, INF-04~08) */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class ChatNotificationFlowTest {

    private static final LocalDate DATE = LocalDate.of(2031, 5, 1);

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private MatchingQueryService matchingQueryService;
    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private AdminChatQueryService adminChatQueryService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationDispatcher notificationDispatcher;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MockPushGateway mockPushGateway;
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
    void 확정되면_단체방이_한_번만_생기고_공지_4종이_올라가며_리마인드는_전날_12시로_예약된다() {
        Long slot = eventSlotCommandService.findOrCreate(DATE);
        apply(5, Region.HONGDAE_HAPJEONG, slot);

        matchingService.runForSlot(slot);

        Long tableId = matchingQueryService.findConfirmedTables(slot).get(0).tableId();
        AdminChatRoomView room = adminChatQueryService.findByTable(tableId);
        assertThat(room.created()).isTrue();
        assertThat(room.members()).hasSize(5);
        assertThat(room.messages()).filteredOn(AdminChatRoomView.Message::visible)
                .extracting(AdminChatRoomView.Message::kind)
                .containsExactly(ChatMessageKind.WELCOME, ChatMessageKind.PLACE, ChatMessageKind.CURATION,
                        ChatMessageKind.POLICY);
        assertThat(room.messages()).filteredOn(message -> message.kind() == ChatMessageKind.PLACE).singleElement()
                .satisfies(place -> assertThat(place.content()).contains("19:30 집합", "[가상]", "예약명: 우연한식탁 0501-"));
        assertThat(room.messages()).filteredOn(message -> message.kind() == ChatMessageKind.REMINDER).singleElement()
                .satisfies(reminder -> {
                    assertThat(reminder.visible()).isFalse();
                    assertThat(reminder.visibleAt()).isEqualTo(DATE.minusDays(1).atTime(12, 0));
                    assertThat(reminder.content()).contains("예약명", "노쇼");
                });

        assertThat(chatRoomService.openForSlot(slot)).as("다시 실행해도 단체방은 그대로").isEmpty();
        assertThat(adminChatQueryService.findByTable(tableId).messages()).hasSize(room.messages().size());

        Long member = room.members().get(0).applicationId();
        assertThat(notificationRepository.findAllByApplicationId(member))
                .extracting(Notification::getType, Notification::getStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(NotificationType.MATCH_CONFIRMED, NotificationStatus.SENT),
                        org.assertj.core.groups.Tuple.tuple(NotificationType.EVENT_REMINDER,
                                NotificationStatus.SCHEDULED));
    }

    @Test
    void 단체방은_멤버만_보고_다른_참가자는_별칭과_제한된_소개로만_보인다() throws Exception {
        Long slot = eventSlotCommandService.findOrCreate(DATE);
        List<long[]> members = apply(5, Region.JAMSIL, slot);
        matchingService.runForSlot(slot);
        String me = token(members.get(0)[0]);
        long roomId = adminChatQueryService.findByTable(
                matchingQueryService.findConfirmedTables(slot).get(0).tableId()).roomId();

        String body = mockMvc.perform(get("/api/app/chat-rooms/" + roomId).header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members.length()").value(5))
                .andExpect(jsonPath("$.data.members[0].alias").value("참가자 1"))
                .andExpect(jsonPath("$.data.members[0].intro").value(org.hamcrest.Matchers.containsString("ENFP")))
                .andExpect(jsonPath("$.data.messages[0].system").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("채팅테스트", "userId", "phone", "1996");

        mockMvc.perform(post("/api/app/chat-rooms/" + roomId + "/messages").header(HttpHeaders.AUTHORIZATION, me)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"목요일에 봬요!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[-1:].content").value("목요일에 봬요!"))
                .andExpect(jsonPath("$.data.messages[-1:].mine").value(true));

        long[] outsider = apply(1, Region.JAMSIL, eventSlotCommandService.findOrCreate(DATE.plusDays(7))).get(0);
        mockMvc.perform(get("/api/app/chat-rooms/" + roomId).header(HttpHeaders.AUTHORIZATION, token(outsider[0])))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/app/chat-rooms").header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(jsonPath("$.data[0].memberCount").value(5))
                .andExpect(jsonPath("$.data[0].lastMessage").value("목요일에 봬요!"));
    }

    @Test
    void 테이블_맞춤_대화_주제와_아이스브레이킹은_멤버가_보고_모임_시작_시각에_단체방에_올라간다() throws Exception {
        Long slot = eventSlotCommandService.findOrCreate(DATE);
        List<long[]> members = apply(4, Region.GANGNAM_SINNONHYEON, slot);
        matchingService.runForSlot(slot);
        Long tableId = matchingQueryService.findConfirmedTables(slot).get(0).tableId();
        AdminChatRoomView room = adminChatQueryService.findByTable(tableId);

        mockMvc.perform(get("/api/app/chat-rooms/" + room.roomId() + "/content")
                        .header(HttpHeaders.AUTHORIZATION, token(members.get(0)[0])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topics.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.topics[0].basis").value("모두 '맛집·카페 탐방'을(를) 좋아해요"))
                .andExpect(jsonPath("$.data.icebreakers[0].name").value("두 개의 진실, 하나의 거짓말"))
                .andExpect(jsonPath("$.data.icebreakers[1].name").value("MBTI 맞혀 보기"));

        assertThat(room.messages()).filteredOn(message -> message.kind() == ChatMessageKind.CONTENT).singleElement()
                .satisfies(content -> {
                    assertThat(content.visible()).isFalse();
                    assertThat(content.visibleAt()).isEqualTo(DATE.atTime(19, 30));
                    assertThat(content.content()).contains("[오늘의 대화 주제]", "1. ", "[아이스브레이킹]");
                });
    }

    @Test
    void 알림함에는_보낼_시각이_된_알림만_보이고_읽음_처리된다() throws Exception {
        Long slot = eventSlotCommandService.findOrCreate(DATE);
        List<long[]> members = apply(5, Region.EULJIRO_JONGNO, slot);
        matchingService.runForSlot(slot);
        String me = token(members.get(0)[0]);

        notifications(me)
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].type").value("MATCH_CONFIRMED"))
                .andExpect(jsonPath("$.data.items[0].body").value(org.hamcrest.Matchers.containsString("을지로/종로")));
        mockMvc.perform(post("/api/app/notifications/read-all").header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(status().isOk());
        notifications(me).andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void 푸시가_실패하면_재시도하고_3번_실패하면_운영자_예외_다시_보내면_성공() {
        long userId = apply(1, Region.JAMSIL, eventSlotCommandService.findOrCreate(DATE)).get(0)[0];
        mockPushGateway.failFor(userId, true);
        try {
            notificationService.notifyNow(userId, null, NotificationType.REFUND_COMPLETED, Map.of("amount", "9,900"),
                    "retry-test:" + userId);
            Notification notification = notificationRepository.findVisibleByUserId(userId, LocalDateTime.now())
                    .get(0);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.RETRYING);
            assertThat(notification.getAttempts()).isEqualTo(1);
            assertThat(notification.getNextAttemptAt()).isAfter(LocalDateTime.now());

            makeDue(notification);
            notificationDispatcher.dispatchDue();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.RETRYING);
            makeDue(notification);
            notificationDispatcher.dispatchDue();
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.GAVE_UP);
            assertThat(notification.getAttempts()).isEqualTo(Notification.MAX_ATTEMPTS);
            assertThat(notificationDispatcher.findGaveUp()).contains(notification);

            mockPushGateway.failFor(userId, false);
            notificationDispatcher.resend(notification.getId());
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(notificationDispatcher.findGaveUp()).doesNotContain(notification);
        } finally {
            mockPushGateway.failFor(userId, false);
        }
    }

    private static void makeDue(Notification notification) {
        ReflectionTestUtils.setField(notification, "nextAttemptAt", LocalDateTime.now().minusSeconds(1));
    }

    private ResultActions notifications(String token) throws Exception {
        return mockMvc.perform(get("/api/app/notifications").header(HttpHeaders.AUTHORIZATION, token));
    }

    private String token(long userId) {
        return "Bearer " + authTokenProvider.issue(userId, UserRole.PARTICIPANT);
    }

    /** 서로 잘 맞는 신청자 count명. [userId, applicationId] */
    private List<long[]> apply(int count, Region region, Long slotId) {
        return IntStream.range(0, count).mapToObj(i -> {
            int n = sequence.incrementAndGet();
            Long userId = userCommandService.createParticipantIfAbsent("chat-test-" + n, "채팅테스트" + n);
            profileCommandService.createIfAbsent(Profile.builder()
                    .userId(userId)
                    .birthYear(1996)
                    .gender(n % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                    .mbti(Mbti.of(n % 2 == 0 ? "INFJ" : "ENFP"))
                    .interests(Set.of(Interest.FOOD_CAFE, Interest.TRAVEL))
                    .traits(Set.of(Trait.LIGHT_HUMOR, Trait.GOOD_LISTENER))
                    .preferredTraits(Set.of(PreferredTrait.HUMOROUS))
                    .mealBudgets(Set.of(MealBudget.FROM_20K_TO_30K))
                    .build());
            Long applicationId = applicationJpaRepository.save(Application.paidSubmission(userId, TicketType.SINGLE,
                    Set.of(region), List.of(slotId))).getId();
            return new long[]{userId, applicationId};
        }).toList();
    }
}
