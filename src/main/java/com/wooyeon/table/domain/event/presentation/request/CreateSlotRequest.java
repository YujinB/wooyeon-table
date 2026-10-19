package com.wooyeon.table.domain.event.presentation.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** startTime·matchAt을 비우면 19:30 시작, 전날 0시 마감 */
public record CreateSlotRequest(@NotNull LocalDate eventDate, LocalTime startTime, LocalDateTime matchAt) {
}
