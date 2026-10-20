package com.wooyeon.table.domain.notification.infrastructure.persistence;

import com.wooyeon.table.domain.notification.domain.model.Notification;
import com.wooyeon.table.domain.notification.domain.model.NotificationStatus;
import com.wooyeon.table.domain.notification.domain.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private static final List<NotificationStatus> PENDING = List.of(NotificationStatus.SCHEDULED,
            NotificationStatus.RETRYING);

    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public boolean existsByDedupKey(String dedupKey) {
        return jpaRepository.existsByDedupKey(dedupKey);
    }

    @Override
    public int deleteByDedupKeyPrefix(String prefix) {
        return jpaRepository.deleteByDedupKeyStartingWith(prefix);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Notification> findVisibleByUserId(Long userId, LocalDateTime now) {
        return jpaRepository.findAllByUserIdAndScheduledAtLessThanEqualOrderByScheduledAtDescIdDesc(userId, now);
    }

    @Override
    public List<Notification> findDue(LocalDateTime now) {
        return jpaRepository.findAllByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(PENDING, now);
    }

    @Override
    public List<Notification> findAllByStatus(NotificationStatus status) {
        return jpaRepository.findAllByStatusOrderByIdAsc(status);
    }

    @Override
    public List<Notification> findRecent() {
        return jpaRepository.findTop500ByOrderByIdDesc();
    }

    @Override
    public List<Notification> findAllByApplicationId(Long applicationId) {
        return jpaRepository.findAllByApplicationIdOrderByIdAsc(applicationId);
    }
}
