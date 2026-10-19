package com.wooyeon.table.domain.safety.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BlockRequest(@NotNull @Min(1) Integer seatNo) {
}
