package com.wooyeon.table.domain.notification.application.service;

import com.wooyeon.table.domain.notification.application.dto.MyNotifications;
import com.wooyeon.table.domain.notification.application.dto.NotificationFailure;
import com.wooyeon.table.domain.notification.application.dto.NotificationItem;
import com.wooyeon.table.domain.notification.domain.model.NotificationStatus;
import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.repository.NotificationRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 앱 내 알림함 (INF-01) */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MyNotifications findMine(Long userId) {
        List<NotificationItem> items = notificationRepository.findVisibleByUserId(userId, LocalDateTime.now(clock))
                .stream()
                .map(NotificationItem::from)
                .toList();
        return new MyNotifications(items.stream().filter(item -> !item.read()).count(), items);
    }

    /** 재시도까지 모두 실패한 알림 (운영자 예외 처리함) */
    @Transactional(readOnly = true)
    public List<NotificationFailure> findGaveUp() {
        return notificationRepository.findAllByStatus(NotificationStatus.GAVE_UP).stream()
                .map(NotificationFailure::from)
                .toList();
    }

    /** 자동 메시지 발송 이력 (최근 500건) */
    @Transactional(readOnly = true)
    public List<NotificationFailure> findRecent() {
        return notificationRepository.findRecent().stream().map(NotificationFailure::from).toList();
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(found -> found.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "알림을 찾을 수 없습니다"));
        notification.markRead(LocalDateTime.now(clock));
    }

    public void markAllRead(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        notificationRepository.findVisibleByUserId(userId, now).forEach(notification -> notification.markRead(now));
    }
}
