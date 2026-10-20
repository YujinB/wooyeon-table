package com.wooyeon.table.domain.operation.application.dto;

import com.wooyeon.table.domain.notification.application.dto.NotificationFailure;
import com.wooyeon.table.domain.payment.application.dto.PaymentRecordView;
import com.wooyeon.table.domain.safety.application.dto.ReportView;

/** 운영자 조회 화면의 한 줄: 각 도메인 결과에 사용자 이름을 붙인다 */
public final class AdminRows {

    public record PaymentRow(String name, PaymentRecordView payment) {
    }

    public record NotificationRow(String name, NotificationFailure notification) {
    }

    public record ReportRow(String reporterName, String reportedName, ReportView report) {
    }

    private AdminRows() {
    }
}
