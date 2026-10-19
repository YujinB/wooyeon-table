package com.wooyeon.table.domain.payment.application.dto;

import com.wooyeon.table.domain.payment.domain.model.PaymentRecord;
import com.wooyeon.table.domain.payment.domain.model.PaymentRecordType;
import java.time.LocalDateTime;

/** 운영자용 결제·환불 한 줄 */
public record PaymentRecordView(Long recordId, Long applicationId, Long userId, PaymentRecordType type,
                                String typeLabel, boolean success, int amount, String failureReason,
                                LocalDateTime at) {

    public static PaymentRecordView from(PaymentRecord record) {
        return new PaymentRecordView(record.getId(), record.getApplicationId(), record.getUserId(), record.getType(),
                record.getType().getLabel(), record.isSuccess(), record.getAmount(), record.getFailureReason(),
                record.getCreatedAt());
    }
}
