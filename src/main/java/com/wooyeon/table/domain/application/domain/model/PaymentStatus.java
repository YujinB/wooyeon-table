package com.wooyeon.table.domain.application.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 결제 상태 (PRD 12절 payment_status). 해커톤에서는 모의 결제로 상태만 전환한다 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("결제 대기"),
    PAID("결제 완료"),
    FAILED("결제 실패"),
    /** 매칭 불가로 환불 선택 가능 */
    REFUND_AVAILABLE("환불 가능"),
    REFUND_REQUESTED("환불 요청"),
    REFUND_PROCESSING("환불 처리 중"),
    REFUNDED("환불 완료"),
    REFUND_FAILED("환불 실패");

    private final String label;
}
