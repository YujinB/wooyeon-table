package com.wooyeon.table.domain.safety.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 차단 (SAF-01). 한쪽만 차단해도 다음 매칭부터 같은 테이블에 앉지 않는다. 상대에게 알리지 않는다 */
@Getter
@Entity
@Table(name = "user_blocks", uniqueConstraints = @UniqueConstraint(name = "uk_user_blocks",
        columnNames = {"blockerUserId", "blockedUserId"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long blockerUserId;

    @Column(nullable = false)
    private Long blockedUserId;

    private UserBlock(Long blockerUserId, Long blockedUserId) {
        this.blockerUserId = blockerUserId;
        this.blockedUserId = blockedUserId;
    }

    public static UserBlock of(Long blockerUserId, Long blockedUserId) {
        return new UserBlock(blockerUserId, blockedUserId);
    }
}
