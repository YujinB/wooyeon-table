package com.wooyeon.table.domain.safety.domain.model;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 조치 기록 (SAF-02): 경고·참가 제한(기한)·영구 제한. 신고 처리로 생기거나
 * 노쇼·당일 취소 반복으로 자동으로 생긴다 (reportId 없음)
 */
@Getter
@Entity
@Table(name = "user_sanctions", indexes = @Index(name = "idx_user_sanctions_user", columnList = "userId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSanction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SanctionLevel level;

    @Column(nullable = false, length = 300)
    private String reason;

    private Long reportId;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    /** 참가 제한 종료 시각. 경고는 null, 영구 제한도 null */
    private LocalDateTime endsAt;

    private UserSanction(Long userId, SanctionLevel level, String reason, Long reportId, LocalDateTime startsAt,
                         LocalDateTime endsAt) {
        this.userId = userId;
        this.level = level;
        this.reason = reason;
        this.reportId = reportId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public static UserSanction warning(Long userId, String reason, Long reportId, LocalDateTime now) {
        return new UserSanction(userId, SanctionLevel.WARNING, reason, reportId, now, null);
    }

    public static UserSanction restricted(Long userId, String reason, Long reportId, LocalDateTime now, int days) {
        return new UserSanction(userId, SanctionLevel.RESTRICTED, reason, reportId, now, now.plusDays(days));
    }

    public static UserSanction banned(Long userId, String reason, Long reportId, LocalDateTime now) {
        return new UserSanction(userId, SanctionLevel.BANNED, reason, reportId, now, null);
    }

    /** 지금 신청을 막는 조치인지 */
    public boolean blocksParticipation(LocalDateTime now) {
        return level == SanctionLevel.BANNED || (level == SanctionLevel.RESTRICTED && now.isBefore(endsAt));
    }
}
