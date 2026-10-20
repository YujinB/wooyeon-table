package com.wooyeon.table.domain.notification.domain.repository;

import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    boolean existsByDedupKey(String dedupKey);

    Optional<Notification> findById(Long id);

    /** 앱 알림함: 예약 시각이 지난 알림 (푸시 성공 여부와 무관, 최근 것부터) */
    List<Notification> findVisibleByUserId(Long userId, LocalDateTime now);

    /** 지금 보내야 하는 알림 (예약 시각·재시도 시각이 지난 것) */
    List<Notification> findDue(LocalDateTime now);

    List<Notification> findAllByStatus(NotificationStatus status);

    /** 최근 발송·예약 내역 (최신부터, 운영자 조회) */
    List<Notification> findRecent();

    List<Notification> findAllByApplicationId(Long applicationId);
}
