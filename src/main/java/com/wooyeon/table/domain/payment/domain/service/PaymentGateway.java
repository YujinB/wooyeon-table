package com.wooyeon.table.domain.payment.domain.service;

/** 결제대행(PG) 포트. 해커톤에서는 모의 구현을 쓰고, 실결제 연동(PAY-04) 때 구현만 바꾼다 */
public interface PaymentGateway {

    GatewayResult charge(long applicationId, int amount, boolean simulateFailure);

    /** 원결제수단으로 환불 (PAY-03) */
    GatewayResult refund(String transactionId, int amount, boolean simulateFailure);

    record GatewayResult(boolean success, String transactionId, String failureReason) {

        public static GatewayResult ok(String transactionId) {
            return new GatewayResult(true, transactionId, null);
        }

        public static GatewayResult fail(String reason) {
            return new GatewayResult(false, null, reason);
        }
    }
}
