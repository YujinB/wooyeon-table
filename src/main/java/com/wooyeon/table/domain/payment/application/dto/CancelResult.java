package com.wooyeon.table.domain.payment.application.dto;

import com.wooyeon.table.domain.application.domain.model.ApplicationStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;

/** 신청 취소 결과. 환불이 없었으면 refund는 null */
public record CancelResult(Long applicationId, ApplicationStatus applicationStatus, PaymentStatus paymentStatus,
                           PaymentResult refund) {
}
