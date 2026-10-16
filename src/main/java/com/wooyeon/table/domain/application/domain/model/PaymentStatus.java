package com.wooyeon.table.domain.application.domain.model;

/** 결제 상태 (PRD 12절 payment_status). 해커톤에서는 모의 결제로 상태만 전환한다 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    /** 매칭 불가로 환불 선택 가능 */
    REFUND_AVAILABLE,
    REFUND_REQUESTED,
    REFUND_PROCESSING,
    REFUNDED,
    REFUND_FAILED
}
