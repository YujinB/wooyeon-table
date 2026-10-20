package com.wooyeon.table.domain.notification.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 (PRD 13절 Notification: 수신자·템플릿·채널·예약 시각·발송 결과).
 * 예약 시각이 되면 앱 내 알림함에 보이고(DB라 실패 없음), 같은 내용을 앱 푸시(모의)로 보낸다. 상태는 푸시 발송 결과다. dedupKey로 같은 알림은 한 번만 만든다.
 * 발송 실패는 간격을 늘려 재시도하고, MAX_ATTEMPTS번 실패하면 GAVE_UP → 운영자 예외 (INF-08)
 */
@Getter
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user", columnList = "userId"),
        @Index(name = "idx_notifications_due", columnList = "status, nextAttemptAt")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    /** 이 시각부터 보낸다 (리마인드처럼 미리 예약하는 알림) */
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false)
    private int attempts;

    /** 다음 발송(재시도) 시각 */
    private LocalDateTime nextAttemptAt;

    @Column(length = 300)
    private String lastError;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @Column(nullable = false, unique = true, length = 100)
    private String dedupKey;

    private Notification(Long userId, Long applicationId, NotificationType type, String title, String body,
                         LocalDateTime scheduledAt, String dedupKey) {
        this.userId = userId;
        this.applicationId = applicationId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.scheduledAt = scheduledAt;
        this.status = NotificationStatus.SCHEDULED;
        this.nextAttemptAt = scheduledAt;
        this.dedupKey = dedupKey;
    }

    public static Notification schedule(Long userId, Long applicationId, NotificationType type, String body,
                                        LocalDateTime scheduledAt, String dedupKey) {
        return new Notification(userId, applicationId, type, type.getTitle(), body, scheduledAt, dedupKey);
    }

    public boolean isDue(LocalDateTime now) {
        return (status == NotificationStatus.SCHEDULED || status == NotificationStatus.RETRYING)
                && !nextAttemptAt.isAfter(now);
    }

    public void markSent(LocalDateTime now) {
        this.attempts++;
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    /** 실패: 1분 → 5분 뒤 재시도, MAX_ATTEMPTS번째 실패면 포기(운영자 예외) */
    public void markFailed(String error, LocalDateTime now) {
        this.attempts++;
        this.lastError = error;
        if (attempts >= MAX_ATTEMPTS) {
            this.status = NotificationStatus.GAVE_UP;
            this.nextAttemptAt = null;
            return;
        }
        this.status = NotificationStatus.RETRYING;
        this.nextAttemptAt = now.plus(attempts == 1 ? Duration.ofMinutes(1) : Duration.ofMinutes(5));
    }

    /** 운영자가 다시 보내기 */
    public void requeue(LocalDateTime now) {
        this.status = NotificationStatus.RETRYING;
        this.nextAttemptAt = now;
    }

    public void markRead(LocalDateTime now) {
        if (readAt == null) {
            this.readAt = now;
        }
    }
}
