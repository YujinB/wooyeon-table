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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 안전 신고 (PRD 13절 SafetyReport, SAF-01). 신고 내용·신고자는 피신고자에게 공개되지 않는다.
 * urgent = 아직 끝나지 않은 같은 테이블에 함께 있어 즉시 분리가 필요한 신고 (운영자 예외 최우선)
 */
@Getter
@Entity
@Table(name = "safety_reports", indexes = @Index(name = "idx_safety_reports_reported", columnList = "reportedUserId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SafetyReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reporterUserId;

    @Column(nullable = false)
    private Long reportedUserId;

    @Column(nullable = false)
    private Long tableId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(length = 1000)
    private String detail;

    /** 증빙 (캡처 링크 등, 선택) */
    @Column(length = 500)
    private String evidenceUrl;

    @Column(nullable = false)
    private boolean urgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SanctionLevel action;

    @Column(length = 500)
    private String operatorMemo;

    private LocalDateTime resolvedAt;

    @Builder
    private SafetyReport(Long reporterUserId, Long reportedUserId, Long tableId, ReportReason reason, String detail,
                         String evidenceUrl, boolean urgent) {
        this.reporterUserId = reporterUserId;
        this.reportedUserId = reportedUserId;
        this.tableId = tableId;
        this.reason = reason;
        this.detail = detail;
        this.evidenceUrl = evidenceUrl;
        this.urgent = urgent;
        this.status = ReportStatus.RECEIVED;
    }

    /** 운영자 조치 (SAF-02). action이 null이면 조치 없이 종결(기각) */
    public void resolve(SanctionLevel action, String memo, LocalDateTime now) {
        this.status = action == null ? ReportStatus.DISMISSED : ReportStatus.ACTIONED;
        this.action = action;
        this.operatorMemo = memo;
        this.resolvedAt = now;
    }
}
