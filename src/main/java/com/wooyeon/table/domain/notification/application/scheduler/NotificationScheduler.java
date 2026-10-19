package com.wooyeon.table.domain.notification.application.scheduler;

import com.wooyeon.table.domain.notification.application.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 예약 알림(리마인드)과 실패 알림 재시도를 주기적으로 보낸다. 기본 켜짐 (NOTIFICATION_DISPATCHER_ENABLED) */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.dispatcher.enabled", havingValue = "true")
public class NotificationScheduler {

    private final NotificationDispatcher notificationDispatcher;

    @Scheduled(fixedDelayString = "${notification.dispatcher.interval-ms:30000}")
    public void dispatch() {
        try {
            notificationDispatcher.dispatchDue();
        } catch (RuntimeException e) {
            log.error("알림 발송 주기 실패", e);
        }
    }
}
