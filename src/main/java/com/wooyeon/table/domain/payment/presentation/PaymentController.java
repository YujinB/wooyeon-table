package com.wooyeon.table.domain.payment.presentation;

import com.wooyeon.table.domain.payment.application.dto.PaymentResult;
import com.wooyeon.table.domain.payment.application.service.PaymentService;
import com.wooyeon.table.domain.payment.presentation.request.PayRequest;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/applications/{applicationId}")
public class PaymentController {

    private final PaymentService paymentService;

    /** 모의 결제. 실패해도 200으로 결과(success=false, paymentStatus=FAILED)를 돌려주고 다시 결제할 수 있다 */
    @PostMapping("/payment")
    public ApiResponse<PaymentResult> pay(@LoginUser AuthUser user, @PathVariable Long applicationId,
                                          @RequestBody(required = false) PayRequest request) {
        boolean simulateFailure = request != null && request.simulateFailure();
        return ApiResponse.ok(paymentService.pay(user.userId(), applicationId, simulateFailure));
    }
}
