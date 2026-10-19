package com.wooyeon.table.domain.safety.application.dto;

/** 신고 접수 결과. 신고 상대에게는 아무것도 알리지 않는다 */
public record ReportReceipt(Long reportId, boolean blocked, boolean urgent) {
}
