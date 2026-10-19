package com.wooyeon.table.domain.notification.application.dto;

import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import java.time.LocalDateTime;

public record NotificationItem(Long notificationId, NotificationType type, String title, String body,
                               Long applicationId, LocalDateTime at, boolean read) {

    public static NotificationItem from(Notification notification) {
        return new NotificationItem(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getApplicationId(), notification.getScheduledAt(),
                notification.getReadAt() != null);
    }
}
