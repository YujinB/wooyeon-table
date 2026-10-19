package com.wooyeon.table.domain.notification.application.service;

import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import com.wooyeon.table.domain.notification.domain.repository.NotificationRepository;
import com.wooyeon.table.domain.notification.domain.service.PushGateway;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시나리오별 알림 생성·발송 (INF-01, PRD 14절). 다른 도메인은 이 서비스만 부른다.
 * 지금 보낼 알림은 바로 한 번 보내 보고, 실패하거나 예약된 알림은 발송기(NotificationDispatcher)가 처리한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushGateway pushGateway;
    private final Clock clock;

    /** 지금 보내는 알림. dedupKey가 같은 알림이 이미 있으면 만들지 않는다 */
    public void notifyNow(Long userId, Long applicationId, NotificationType type, Map<String, String> values,
                          String dedupKey) {
        schedule(userId, applicationId, type, values, LocalDateTime.now(clock), dedupKey);
    }

    public void schedule(Long userId, Long applicationId, NotificationType type, Map<String, String> values,
                         LocalDateTime at, String dedupKey) {
        if (notificationRepository.existsByDedupKey(dedupKey)) {
            return;
        }
        Notification notification = notificationRepository.save(
                Notification.schedule(userId, applicationId, type, type.render(values), at, dedupKey));
        LocalDateTime now = LocalDateTime.now(clock);
        if (notification.isDue(now)) {
            deliver(notification, now);
        }
    }

    /** 푸시로 보낸다 (앱 알림함에는 예약 시각부터 이미 보인다). 푸시 실패는 재시도 대상 */
    public void deliver(Notification notification, LocalDateTime now) {
        try {
            pushGateway.send(notification.getUserId(), notification.getTitle(), notification.getBody());
            notification.markSent(now);
        } catch (RuntimeException e) {
            notification.markFailed(e.getMessage(), now);
            log.warn("알림 {} 발송 실패 ({}회): {}", notification.getId(), notification.getAttempts(), e.getMessage());
        }
    }
}
