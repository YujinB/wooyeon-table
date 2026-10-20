package com.wooyeon.table.domain.safety.application.dto;

import com.wooyeon.table.domain.safety.domain.model.ReportReason;
import com.wooyeon.table.domain.safety.domain.model.ReportStatus;
import com.wooyeon.table.domain.safety.domain.model.SafetyReport;
import com.wooyeon.table.domain.safety.domain.model.SanctionLevel;
import java.time.LocalDateTime;

/** 운영자용 신고 (신고자·피신고자 포함, 참가자에게는 내보내지 않음) */
public record ReportView(Long reportId, Long reporterUserId, Long reportedUserId, Long tableId, ReportReason reason,
                         String reasonLabel, String detail, String evidenceUrl, boolean urgent, ReportStatus status,
                         SanctionLevel action, String operatorMemo, LocalDateTime reportedAt,
                         LocalDateTime resolvedAt) {

    public static ReportView from(SafetyReport report) {
        return new ReportView(report.getId(), report.getReporterUserId(), report.getReportedUserId(),
                report.getTableId(), report.getReason(), report.getReason().getLabel(), report.getDetail(),
                report.getEvidenceUrl(), report.isUrgent(), report.getStatus(), report.getAction(),
                report.getOperatorMemo(), report.getCreatedAt(), report.getResolvedAt());
    }
}
