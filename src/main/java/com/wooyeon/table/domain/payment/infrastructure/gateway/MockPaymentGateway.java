package com.wooyeon.table.domain.payment.infrastructure.gateway;

import com.wooyeon.table.domain.payment.domain.service.PaymentGateway;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 모의 PG: 요청하면 항상 성공하고, 데모용으로 실패를 흉내 낼 수 있다 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResult charge(long applicationId, int amount, boolean simulateFailure) {
        if (simulateFailure) {
            return GatewayResult.fail("카드 승인 거절 (모의)");
        }
        return GatewayResult.ok("MOCK-PAY-" + UUID.randomUUID());
    }

    @Override
    public GatewayResult refund(String transactionId, int amount, boolean simulateFailure) {
        if (simulateFailure) {
            return GatewayResult.fail("원결제 취소 실패 (모의)");
        }
        return GatewayResult.ok("MOCK-REFUND-" + UUID.randomUUID());
    }
}
