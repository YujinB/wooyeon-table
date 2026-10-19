package com.wooyeon.table.domain.user.presentation.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** 두 항목 모두 필수 동의 */
public record AgreeConsentRequest(@NotNull @AssertTrue Boolean privacy, @NotNull @AssertTrue Boolean policy) {
}
