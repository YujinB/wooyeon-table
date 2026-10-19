package com.wooyeon.table.domain.safety.application.dto;

import com.wooyeon.table.domain.safety.domain.model.SanctionLevel;
import java.time.LocalDateTime;

/** 신청 가능 여부. 막혔으면 조치 단계·사유·해제 시각(영구면 null) */
public record Eligibility(boolean eligible, SanctionLevel level, String reason, LocalDateTime until) {

    public String message() {
        if (eligible) {
            return "신청할 수 있어요";
        }
        String period = until == null ? "영구" : until.toLocalDate() + "까지";
        return level.getLabel() + " (" + period + "): " + reason;
    }
}
