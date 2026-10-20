package com.wooyeon.table.domain.safety.presentation.request;

import com.wooyeon.table.domain.safety.domain.model.SanctionLevel;

/** action 비우면 조치 없이 종결, RESTRICTED면 days 필수 */
public record ReportActionRequest(SanctionLevel action, Integer days, String memo) {
}
