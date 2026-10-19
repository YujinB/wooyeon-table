package com.wooyeon.table.domain.notification.application.dto;

import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationStatus;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import java.time.LocalDateTime;

/** 운영자용 알림 발송 상태 */
public record NotificationFailure(Long notificationId, Long userId, Long applicationId, NotificationType type,
                                  String title, NotificationStatus status, int attempts, String lastError,
                                  LocalDateTime scheduledAt, LocalDateTime sentAt) {

    public static NotificationFailure from(Notification notification) {
        return new NotificationFailure(notification.getId(), notification.getUserId(), notification.getApplicationId(),
                notification.getType(), notification.getTitle(), notification.getStatus(), notification.getAttempts(),
                notification.getLastError(), notification.getScheduledAt(), notification.getSentAt());
    }
}
