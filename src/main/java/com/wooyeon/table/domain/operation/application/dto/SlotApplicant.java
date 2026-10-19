package com.wooyeon.table.domain.operation.application.dto;

import com.wooyeon.table.domain.application.domain.model.ParticipantStatus;
import java.time.LocalDateTime;
import java.util.List;

/** 운영자용 회차 신청자 한 줄 */
public record SlotApplicant(Long applicationId, Long userId, String name, String ticketLabel, List<String> regions,
                            ParticipantStatus status, String statusLabel, String paymentStatusLabel,
                            boolean confirmedHere, LocalDateTime submittedAt) {
}
