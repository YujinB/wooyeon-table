package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.matching.domain.model.FailureOutcome;
import com.wooyeon.table.domain.matching.domain.model.UnassignedReason;
import java.time.LocalDate;

/** 운영자용 미배정 상세 — 사유와 처리 결과 (MAT-11·12) */
public record FailureDetail(Long applicationId, Long userId, String name, UnassignedReason reason,
                            String reasonDescription, int relocationCount, FailureOutcome outcome,
                            String outcomeDescription, Long nextSlotId, LocalDate nextEventDate) {
}
