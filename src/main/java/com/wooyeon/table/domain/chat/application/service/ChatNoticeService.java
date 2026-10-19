package com.wooyeon.table.domain.chat.application.service;

import com.wooyeon.table.domain.chat.domain.model.ChatMember;
import com.wooyeon.table.domain.chat.domain.model.ChatMessage;
import com.wooyeon.table.domain.chat.domain.model.ChatMessageKind;
import com.wooyeon.table.domain.chat.domain.model.ChatRoom;
import com.wooyeon.table.domain.chat.domain.repository.ChatMessageRepository;
import com.wooyeon.table.domain.chat.domain.repository.ChatRoomRepository;
import com.wooyeon.table.domain.chat.domain.service.MemberIntro;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.dto.TableDetail;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.notification.application.service.NotificationService;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import com.wooyeon.table.domain.payment.domain.model.RefundPolicy;
import com.wooyeon.table.domain.venue.application.dto.VenueAssignmentView;
import com.wooyeon.table.domain.venue.application.service.VenueQueryService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 단체방 시스템 공지 (INF-05): 환영, 시간·장소, 테이블 조합 큐레이션, 취소·노쇼 정책.
 * 종류별로 방마다 한 번만 게시한다. 식당이 아직 없으면 시간·장소는 배정되는 순간 게시한다
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatNoticeService {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN);
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MatchingQueryService matchingQueryService;
    private final EventSlotQueryService eventSlotQueryService;
    private final VenueQueryService venueQueryService;
    private final NotificationService notificationService;
    private final Clock clock;

    public void postConfirmationNotices(ChatRoom room) {
        LocalDateTime now = LocalDateTime.now(clock);
        TableDetail table = matchingQueryService.getTableDetail(room.getTableId());
        Optional<VenueAssignmentView> venue = assignedVenue(room.getTableId());

        post(room, ChatMessageKind.WELCOME, welcome(room, table, venue.isPresent()), now);
        venue.ifPresent(assigned -> post(room, ChatMessageKind.PLACE, place(room, assigned), now));
        post(room, ChatMessageKind.CURATION, curation(table), now);
        post(room, ChatMessageKind.POLICY, policy(), now);
    }

    /** 매칭 확정 알림: 날짜·지역·식당·집합 시간·앱 단체방 안내 (INF-01, PRD 14절) */
    public void notifyConfirmed(ChatRoom room) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(room.getSlotId());
        TableDetail table = matchingQueryService.getTableDetail(room.getTableId());
        String venue = assignedVenue(room.getTableId())
                .map(assigned -> assigned.venue().name())
                .orElse("배정 중 (정해지면 단체방에 안내)");
        for (ChatMember member : room.getMembers()) {
            notificationService.notifyNow(member.getUserId(), member.getApplicationId(),
                    NotificationType.MATCH_CONFIRMED, Map.of(
                            "date", slot.eventDate().format(DATE),
                            "time", slot.startTime().format(TIME),
                            "region", table.regionLabel(),
                            "venue", venue),
                    "MATCH_CONFIRMED:" + member.getApplicationId());
        }
    }

    /** 식당이 늦게(운영자 재시도로) 배정된 테이블에 시간·장소 공지를 올린다 */
    public void postPlace(Long tableId) {
        chatRoomRepository.findByTableId(tableId).ifPresent(room -> assignedVenue(tableId)
                .ifPresent(venue -> post(room, ChatMessageKind.PLACE, place(room, venue), LocalDateTime.now(clock))));
    }

    /** 종류별 1회. visibleAt이 미래면 그때부터 보인다 */
    void post(ChatRoom room, ChatMessageKind kind, String content, LocalDateTime visibleAt) {
        ChatMessage message = ChatMessage.system(room.getId(), kind, content, visibleAt);
        if (!chatMessageRepository.existsByDedupKey(message.getDedupKey())) {
            chatMessageRepository.save(message);
        }
    }

    Optional<VenueAssignmentView> assignedVenue(Long tableId) {
        return venueQueryService.findByTable(tableId).filter(VenueAssignmentView::isAssigned);
    }

    String place(ChatRoom room, VenueAssignmentView assignment) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(room.getSlotId());
        return String.join("\n",
                "[시간·장소]",
                slot.eventDate().format(DATE) + " " + slot.startTime().format(TIME) + " 집합",
                "식당: " + assignment.venue().name(),
                "주소: " + assignment.venue().address(),
                "지도: " + assignment.venue().mapUrl(),
                "예약명: " + assignment.reservationName(),
                "가격: " + assignment.venue().priceGuide());
    }

    private static String welcome(ChatRoom room, TableDetail table, boolean venueReady) {
        List<String> lines = new ArrayList<>();
        lines.add(room.getTitle() + " 단체방이 열렸어요. " + table.size() + "명이 함께해요.");
        lines.add("서로 별칭(참가자 1~" + table.size() + ")으로 보이고, 연락처와 실명은 공개되지 않아요.");
        if (!venueReady) {
            lines.add("식당은 정해지는 대로 이 방에 안내할게요.");
        }
        return String.join("\n", lines);
    }

    private static String curation(TableDetail table) {
        List<String> lines = new ArrayList<>();
        lines.add("[이번 테이블은 이렇게 모였어요]");
        String ages = sameBand(table) ? MemberIntro.ageBand(table.minAge())
                : MemberIntro.ageBand(table.minAge()) + "~" + MemberIntro.ageBand(table.maxAge());
        lines.add(table.size() + "명 · " + ages + " · 남 " + table.maleCount() + " · 여 " + table.femaleCount());
        if (!table.commonInterests().isEmpty()) {
            lines.add("모두 좋아하는 것: " + String.join(", ", table.commonInterests()));
        }
        if (!table.mbtiComposition().isEmpty()) {
            lines.add("MBTI: " + String.join(", ", table.mbtiComposition()));
        }
        lines.add("나이대가 가깝고 대화 성향과 만나고 싶은 사람 성향이 서로 맞도록 자동으로 구성했어요.");
        return String.join("\n", lines);
    }

    private static boolean sameBand(TableDetail table) {
        return MemberIntro.ageBand(table.minAge()).equals(MemberIntro.ageBand(table.maxAge()));
    }

    private static String policy() {
        return String.join("\n",
                "[취소·노쇼 안내]",
                RefundPolicy.RULES.get(1),
                RefundPolicy.RULES.get(2),
                RefundPolicy.RULES.get(3));
    }
}
