package com.wooyeon.table.domain.payment.presentation.request;

/** simulateFailure: 데모에서 카드 승인 거절을 흉내 낸다 */
public record PayRequest(boolean simulateFailure) {
}
