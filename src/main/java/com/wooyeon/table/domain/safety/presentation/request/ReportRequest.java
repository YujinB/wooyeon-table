package com.wooyeon.table.domain.safety.presentation.request;

import com.wooyeon.table.domain.safety.application.dto.ReportCommand;
import com.wooyeon.table.domain.safety.domain.model.ReportReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** block을 비우면 차단도 함께 한다 */
public record ReportRequest(@NotNull @Min(1) Integer seatNo, @NotNull ReportReason reason,
                            @Size(max = 1000) String detail, @Size(max = 500) String evidenceUrl, Boolean block) {

    public ReportCommand toCommand() {
        return new ReportCommand(seatNo, reason, detail == null || detail.isBlank() ? null : detail.strip(),
                evidenceUrl == null || evidenceUrl.isBlank() ? null : evidenceUrl.strip(), block == null || block);
    }
}
