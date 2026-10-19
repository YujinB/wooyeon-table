package com.wooyeon.table.domain.notification.application.dto;

import java.util.List;

public record MyNotifications(long unreadCount, List<NotificationItem> items) {
}
