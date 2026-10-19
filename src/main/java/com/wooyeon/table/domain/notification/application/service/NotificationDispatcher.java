package com.wooyeon.table.domain.notification.application.service;

import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationStatus;
import com.wooyeon.table.domain.notification.domain.repository.NotificationRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약·재시도 알림 발송 (INF-07·08). 예약 시각이 된 알림과 재시도 시각이 된 실패 알림을 보낸다.
 * 3번 실패하면 GAVE_UP으로 운영자 예외 목록에 올라가고, 운영자가 다시 보낼 수 있다
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    /** @return 이번에 시도한 알림 수 */
    public DispatchResult dispatchDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Notification> due = notificationRepository.findDue(now);
        due.forEach(notification -> notificationService.deliver(notification, now));
        long sent = due.stream().filter(notification -> notification.getStatus() == NotificationStatus.SENT).count();
        long gaveUp = due.stream().filter(notification -> notification.getStatus() == NotificationStatus.GAVE_UP)
                .count();
        if (!due.isEmpty()) {
            log.info("알림 발송: 시도 {} · 성공 {} · 포기 {}", due.size(), sent, gaveUp);
        }
        return new DispatchResult(due.size(), sent, gaveUp);
    }

    /** 반복 실패한 알림 (운영자 예외) */
    @Transactional(readOnly = true)
    public List<Notification> findGaveUp() {
        return notificationRepository.findAllByStatus(NotificationStatus.GAVE_UP);
    }

    /** 운영자 다시 보내기: 바로 한 번 시도 */
    public Notification resend(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "알림을 찾을 수 없습니다"));
        if (notification.getStatus() != NotificationStatus.GAVE_UP) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "발송 포기 상태인 알림만 다시 보냅니다");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        notification.requeue(now);
        notificationService.deliver(notification, now);
        return notification;
    }

    public record DispatchResult(int attempted, long sent, long gaveUp) {
    }
}
