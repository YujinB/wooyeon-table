package com.wooyeon.table.domain.safety.presentation;

import com.wooyeon.table.domain.safety.application.dto.ReportView;
import com.wooyeon.table.domain.safety.application.service.SanctionService;
import com.wooyeon.table.domain.safety.domain.model.UserSanction;
import com.wooyeon.table.domain.safety.presentation.request.ReportActionRequest;
import com.wooyeon.table.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminSafetyController {

    private final SanctionService sanctionService;

    /** 신고 목록 (open=true면 검토 대기만, 긴급 먼저) */
    @GetMapping("/reports")
    public ApiResponse<List<ReportView>> reports(@RequestParam(defaultValue = "true") boolean open) {
        return ApiResponse.ok(sanctionService.reports(open));
    }

    /** 경고·참가 제한(days)·영구 제한·종결(action 비움) (SAF-02) */
    @PostMapping("/reports/{reportId}/action")
    public ApiResponse<ReportView> act(@PathVariable Long reportId, @RequestBody ReportActionRequest request) {
        return ApiResponse.ok(sanctionService.act(reportId, request.action(), request.days(), request.memo()));
    }

    @GetMapping("/users/{userId}/sanctions")
    public ApiResponse<List<UserSanction>> sanctions(@PathVariable Long userId) {
        return ApiResponse.ok(sanctionService.sanctionsOf(userId));
    }
}
