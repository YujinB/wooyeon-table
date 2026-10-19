package com.wooyeon.table.domain.payment.presentation;

import com.wooyeon.table.domain.payment.application.dto.CancelResult;
import com.wooyeon.table.domain.payment.application.service.CancellationService;
import com.wooyeon.table.domain.payment.domain.model.RefundPolicy;
import com.wooyeon.table.domain.payment.presentation.response.RefundPolicyResponse;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app")
public class CancellationController {

    private final CancellationService cancellationService;

    /** 매칭 확정 전 취소. 결제했으면 바로 자동 환불 */
    @PostMapping("/applications/{applicationId}/cancel")
    public ApiResponse<CancelResult> cancel(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(cancellationService.cancel(user.userId(), applicationId));
    }

    /** 결제 전·신청 완료 화면의 환불 규정 (PAY-02) */
    @GetMapping("/policies/refund")
    public ApiResponse<RefundPolicyResponse> refundPolicy() {
        return ApiResponse.ok(new RefundPolicyResponse(RefundPolicy.SUMMARY, RefundPolicy.RULES));
    }
}
