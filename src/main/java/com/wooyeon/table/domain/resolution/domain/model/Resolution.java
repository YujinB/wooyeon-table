package com.wooyeon.table.domain.resolution.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매칭 실패 신청에 대해 참가자가 고른 해결 방식 기록. 신청당 하나만 (유일 제약) —
 * 버튼을 두 번 눌러도 이동·보관·환불이 두 번 처리되지 않는다 (PRD 16절 중복 환불 금지)
 */
@Getter
@Entity
@Table(name = "resolutions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resolution extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResolutionChoice choice;

    /** 일정 이동이면 옮겨 간 회차 */
    private Long targetSlotId;

    private Resolution(Long applicationId, Long userId, ResolutionChoice choice, Long targetSlotId) {
        this.applicationId = applicationId;
        this.userId = userId;
        this.choice = choice;
        this.targetSlotId = targetSlotId;
    }

    public static Resolution of(Long applicationId, Long userId, ResolutionChoice choice, Long targetSlotId) {
        return new Resolution(applicationId, userId, choice, targetSlotId);
    }
}
