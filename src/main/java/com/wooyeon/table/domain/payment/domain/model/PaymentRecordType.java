package com.wooyeon.table.domain.payment.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentRecordType {
    CHARGE("결제"),
    /** 유효한 멤버십으로 신청 (0원) */
    MEMBERSHIP_USE("멤버십 사용"),
    REFUND("환불");

    private final String label;
}
