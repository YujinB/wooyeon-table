package com.wooyeon.table.domain.payment.application.dto;

import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.payment.domain.model.PaymentRecordType;
import java.time.LocalDate;

/** 결제·환불 시도 결과. membershipExpiresOn은 멤버십을 사거나 쓴 경우에만 */
public record PaymentResult(Long applicationId, boolean success, PaymentRecordType type, int amount,
                            PaymentStatus paymentStatus, String failureReason, LocalDate membershipExpiresOn) {
}
