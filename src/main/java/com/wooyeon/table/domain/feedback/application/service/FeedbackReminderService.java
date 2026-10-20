package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.dto.SlotDeletedEvent;
import com.wooyeon.table.domain.event.application.dto.SlotOpenedEvent;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.feedback.application.dto.MyHistory;
import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import com.wooyeon.table.domain.feedback.domain.repository.FeedbackRepository;
import com.wooyeon.table.domain.notification.application.service.NotificationService;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재방문을 돕는 알림 두 가지 (PRD 14·15절)
 * <ul>
 *   <li>후기 미응답 리마인드: 모임 다음 날(시작 24시간 뒤)부터, 참석했는데 후기를 안 쓴 사람에게 한 번만. 다음 신청은 막지 않는다</li>
 *   <li>다음 모집 시작: 운영자가 새 회차를 열면 참석 이력이 있는 사람에게 선호 지역과 함께 추천</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackReminderService {

    /** 이보다 오래된 모임은 리마인드하지 않음 (지난 데이터에 한꺼번에 보내지 않게) */
    private static final int REMIND_WITHIN_DAYS = 7;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN);
    private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("M/d(E) HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final EventSlotQueryService eventSlotQueryService;
    private final NotificationService notificationService;
    private final HistoryService historyService;
    private final Clock clock;

    /** @return 이번에 리마인드를 보낸 수 */
    public int remindMissingFeedback() {
        LocalDateTime now = LocalDateTime.now(clock);
        int sent = 0;
        for (EventSlotInfo slot : eventSlotQueryService.findAllSlots()) {
            LocalDateTime remindFrom = slot.startsAt().plusHours(24);
            if (slot.status() != EventSlotStatus.COMPLETED || now.isBefore(remindFrom)
                    || now.isAfter(remindFrom.plusDays(REMIND_WITHIN_DAYS))) {
                continue;
            }
            for (Attendance attendance : attendanceRepository.findAllBySlotId(slot.id())) {
                if (attendance.getStatus() != AttendanceStatus.ATTENDED
                        || feedbackRepository.findByApplicationId(attendance.getApplicationId()).isPresent()) {
                    continue;
                }
                notificationService.notifyNow(attendance.getUserId(), attendance.getApplicationId(),
                        NotificationType.FEEDBACK_REMINDER, Map.of("date", slot.eventDate().format(DATE)),
                        "FEEDBACK_REMINDER:" + attendance.getApplicationId());
                sent++;
            }
        }
        return sent;
    }

    @EventListener
    public void onSlotOpened(SlotOpenedEvent event) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(event.slotId());
        long notified = attendanceRepository.findAllByStatus(AttendanceStatus.ATTENDED).stream()
                .map(Attendance::getUserId)
                .distinct()
                .filter(userId -> recommend(userId, slot))
                .count();
        log.info("새 회차 {} 추천 알림 {}명", slot.eventDate(), notified);
    }

    /** 잘못 만든 회차를 지우면 그 회차 모집 알림도 알림함에서 뺀다 */
    @EventListener
    public void onSlotDeleted(SlotDeletedEvent event) {
        int withdrawn = notificationService.withdraw(slotOpenedKeyPrefix(event.slotId()));
        log.info("삭제한 회차 {} 모집 알림 {}건 회수", event.slotId(), withdrawn);
    }

    private static String slotOpenedKeyPrefix(Long slotId) {
        return "NEXT_SLOT_OPENED:" + slotId + ":";
    }

    /** 지역·이유는 참가 이력 추천과 같은 기준(후기 점수가 높았던 지역, 없으면 많이 간 지역) */
    private boolean recommend(Long userId, EventSlotInfo slot) {
        MyHistory history = historyService.history(userId);
        return history.recommendations().stream()
                .findFirst()
                .map(basis -> {
                    notificationService.notifyNow(userId, null, NotificationType.NEXT_SLOT_OPENED, Map.of(
                                    "date", slot.eventDate().format(DATE),
                                    "time", slot.startTime().format(TIME),
                                    "region", basis.region() == null ? "가까운 지역" : basis.region(),
                                    "reason", basis.reason() + ".",
                                    "deadline", slot.matchAt().format(DEADLINE)),
                            slotOpenedKeyPrefix(slot.id()) + userId);
                    return true;
                })
                .orElse(false);
    }
}
