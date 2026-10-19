package com.wooyeon.table.domain.notification.infrastructure.persistence;

import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    boolean existsByDedupKey(String dedupKey);

    List<Notification> findAllByUserIdAndScheduledAtLessThanEqualOrderByScheduledAtDescIdDesc(Long userId,
                                                                                             LocalDateTime now);

    List<Notification> findAllByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
            Collection<NotificationStatus> statuses, LocalDateTime now);

    List<Notification> findAllByStatusOrderByIdAsc(NotificationStatus status);

    List<Notification> findAllByApplicationIdOrderByIdAsc(Long applicationId);
}
