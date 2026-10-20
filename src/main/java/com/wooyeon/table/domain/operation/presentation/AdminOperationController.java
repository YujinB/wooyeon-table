package com.wooyeon.table.domain.operation.presentation;

import com.wooyeon.table.domain.operation.application.dto.AdminRows.NotificationRow;
import com.wooyeon.table.domain.operation.application.dto.AdminRows.PaymentRow;
import com.wooyeon.table.domain.operation.application.dto.AdminRows.ReportRow;
import com.wooyeon.table.domain.operation.application.dto.Dashboard;
import com.wooyeon.table.domain.operation.application.dto.ExceptionItem;
import com.wooyeon.table.domain.operation.application.dto.SlotApplicant;
import com.wooyeon.table.domain.operation.application.service.CsvExportService;
import com.wooyeon.table.domain.operation.application.service.CsvExportService.CsvFile;
import com.wooyeon.table.domain.operation.application.service.OperationQueryService;
import com.wooyeon.table.global.audit.StatusChange;
import com.wooyeon.table.global.response.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminOperationController {

    private final OperationQueryService operationQueryService;
    private final CsvExportService csvExportService;

    /** 회차별 신청·결제·매칭 현황 + 예외 건수 */
    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard() {
        return ApiResponse.ok(operationQueryService.dashboard());
    }

    @GetMapping("/slots/{slotId}/applicants")
    public ApiResponse<List<SlotApplicant>> applicants(@PathVariable Long slotId) {
        return ApiResponse.ok(operationQueryService.applicants(slotId));
    }

    /** 회차 신청자 CSV (PRD 16절) */
    @GetMapping("/slots/{slotId}/applicants.csv")
    public ResponseEntity<byte[]> applicantsCsv(@PathVariable Long slotId) {
        return csv(csvExportService.applicants(slotId));
    }

    /** 회차 테이블 결과 CSV: 좌석 한 줄씩 (PRD 16절) */
    @GetMapping("/slots/{slotId}/tables.csv")
    public ResponseEntity<byte[]> tablesCsv(@PathVariable Long slotId) {
        return csv(csvExportService.tables(slotId));
    }

    private static ResponseEntity<byte[]> csv(CsvFile file) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename()).build().toString())
                .body(file.content());
    }

    /** 신청의 상태 변경 이력: 언제·누가·이전 값 → 새 값 (PRD 16절) */
    @GetMapping("/applications/{applicationId}/history")
    public ApiResponse<List<StatusChange>> statusHistory(@PathVariable Long applicationId) {
        return ApiResponse.ok(operationQueryService.statusHistory(applicationId));
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
