package com.wooyeon.table.domain.safety.application.dto;

import com.wooyeon.table.domain.safety.domain.model.ReportReason;

/** seatNo = 단체방 좌석 번호(참가자 N), block = 신고하면서 차단도 할지 */
public record ReportCommand(int seatNo, ReportReason reason, String detail, String evidenceUrl, boolean block) {
}
