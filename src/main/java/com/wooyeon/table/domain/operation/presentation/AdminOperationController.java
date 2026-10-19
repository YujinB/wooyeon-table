package com.wooyeon.table.domain.operation.presentation;

import com.wooyeon.table.domain.operation.application.dto.AdminRows.NotificationRow;
import com.wooyeon.table.domain.operation.application.dto.AdminRows.PaymentRow;
import com.wooyeon.table.domain.operation.application.dto.AdminRows.ReportRow;
import com.wooyeon.table.domain.operation.application.dto.Dashboard;
import com.wooyeon.table.domain.operation.application.dto.ExceptionItem;
import com.wooyeon.table.domain.operation.application.dto.SlotApplicant;
import com.wooyeon.table.domain.operation.application.service.OperationQueryService;
import com.wooyeon.table.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminOperationController {

    private final OperationQueryService operationQueryService;

    /** 회차별 신청·결제·매칭 현황 + 예외 건수 */
    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard() {
        return ApiResponse.ok(operationQueryService.dashboard());
    }

    @GetMapping("/slots/{slotId}/applicants")
    public ApiResponse<List<SlotApplicant>> applicants(@PathVariable Long slotId) {
        return ApiResponse.ok(operationQueryService.applicants(slotId));
    }

    /** 결제·환불 내역 (PRD 11절 결제·환불) */
    @GetMapping("/payments")
    public ApiResponse<List<PaymentRow>> payments() {
        return ApiResponse.ok(operationQueryService.payments());
    }

    /** 자동 메시지 발송 이력 (PRD 11절 자동 메시지 관리, 16절 발송 성공·실패·재시도 기록) */
    @GetMapping("/notifications")
    public ApiResponse<List<NotificationRow>> notifications() {
        return ApiResponse.ok(operationQueryService.notifications());
    }

    /** 신고 전체 이력 (PRD 11절 피드백·안전) */
    @GetMapping("/report-history")
    public ApiResponse<List<ReportRow>> reportHistory() {
        return ApiResponse.ok(operationQueryService.reportHistory());
    }

    /** 예외 처리함: 신고·식당 배정·환불·알림 발송·노쇼 후보 중 미해결 건 (MAT-13) */
    @GetMapping("/exceptions")
    public ApiResponse<List<ExceptionItem>> exceptions() {
        return ApiResponse.ok(operationQueryService.exceptions());
    }
}
