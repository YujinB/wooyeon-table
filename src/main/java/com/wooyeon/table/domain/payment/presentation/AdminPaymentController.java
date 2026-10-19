package com.wooyeon.table.domain.payment.presentation;

import com.wooyeon.table.domain.payment.application.dto.PaymentResult;
import com.wooyeon.table.domain.payment.application.service.PaymentService;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/applications/{applicationId}")
public class AdminPaymentController {

    private final PaymentService paymentService;

    /** 환불 실패 건 다시 환불 (예외 처리함) */
    @PostMapping("/refund/retry")
    public ApiResponse<PaymentResult> retryRefund(@PathVariable Long applicationId) {
        return ApiResponse.ok(paymentService.refund(applicationId, false));
    }
}
