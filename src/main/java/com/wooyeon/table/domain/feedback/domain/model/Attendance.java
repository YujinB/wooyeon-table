package com.wooyeon.table.domain.feedback.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
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
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 확정 참가자의 참석 기록 (PRD 13절 Attendance). 신청당 하나 */
@Getter
@Entity
@Table(name = "attendances", indexes = {
        @Index(name = "idx_attendances_slot", columnList = "slotId"),
        @Index(name = "idx_attendances_user", columnList = "userId")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long tableId;

    @Column(nullable = false)
    private Long slotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    private LocalDateTime checkedInAt;

    private LocalDateTime canceledAt;

    /** 운영자가 노쇼 확정·참석 정정한 시각 */
    private LocalDateTime reviewedAt;

    private Attendance(Long applicationId, Long userId, Long tableId, Long slotId) {
        this.applicationId = applicationId;
        this.userId = userId;
        this.tableId = tableId;
        this.slotId = slotId;
        this.status = AttendanceStatus.SCHEDULED;
    }

    public static Attendance scheduled(Long applicationId, Long userId, Long tableId, Long slotId) {
        return new Attendance(applicationId, userId, tableId, slotId);
    }

    /** 앱 체크인 → 참석 완료 (FDB-01). 이미 참석이면 그대로 */
    public void checkIn(LocalDateTime now) {
        if (status == AttendanceStatus.ATTENDED) {
            return;
        }
        require(Set.of(AttendanceStatus.SCHEDULED), "체크인");
        this.status = AttendanceStatus.ATTENDED;
        this.checkedInAt = now;
    }

    /** 확정 후 취소. 환불 마감 전이면 사전 취소, 이후(당일 포함)면 제한 시점 이후 취소 */
    public void cancel(boolean beforeRefundClose, LocalDateTime now) {
        require(Set.of(AttendanceStatus.SCHEDULED), "취소");
        this.status = beforeRefundClose ? AttendanceStatus.CANCELED_EARLY : AttendanceStatus.CANCELED_LATE;
        this.canceledAt = now;
    }

    /** 행사 종료까지 체크인 안 함 → 노쇼 후보 */
    public boolean markNoShowCandidateIfAbsent() {
        if (status != AttendanceStatus.SCHEDULED) {
            return false;
        }
        this.status = AttendanceStatus.NO_SHOW_CANDIDATE;
        return true;
    }

    /** 운영자 확인: 노쇼 확정 또는 참석으로 정정 (체크인을 잊은 경우) */
    public void review(boolean noShow, LocalDateTime now) {
        require(Set.of(AttendanceStatus.NO_SHOW_CANDIDATE, AttendanceStatus.NO_SHOW, AttendanceStatus.ATTENDED),
                "운영자 확인");
        this.status = noShow ? AttendanceStatus.NO_SHOW : AttendanceStatus.ATTENDED;
        this.reviewedAt = now;
    }

    private void require(Set<AttendanceStatus> allowed, String action) {
        if (!allowed.contains(status)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    action + "할 수 없는 참석 상태: " + status.getLabel());
        }
    }
}
