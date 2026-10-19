package com.wooyeon.table.domain.safety.presentation;

import com.wooyeon.table.domain.safety.application.dto.ReportReceipt;
import com.wooyeon.table.domain.safety.application.service.SafetyService;
import com.wooyeon.table.domain.safety.presentation.request.BlockRequest;
import com.wooyeon.table.domain.safety.presentation.request.ReportRequest;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/chat-rooms/{roomId}")
public class SafetyController {

    private final SafetyService safetyService;

    /** 같은 테이블 참가자 신고 (기본으로 차단 포함). 내용은 상대에게 공개되지 않음 (SAF-01) */
    @PostMapping("/reports")
    public ApiResponse<ReportReceipt> report(@LoginUser AuthUser user, @PathVariable Long roomId,
                                             @Valid @RequestBody ReportRequest request) {
        return ApiResponse.ok(safetyService.report(user.userId(), roomId, request.toCommand()));
    }

    /** 신고 없이 차단만 (다음 매칭부터 같은 테이블 금지) */
    @PostMapping("/blocks")
    public ApiResponse<Void> block(@LoginUser AuthUser user, @PathVariable Long roomId,
                                   @Valid @RequestBody BlockRequest request) {
        safetyService.block(user.userId(), roomId, request.seatNo());
        return ApiResponse.ok(null);
    }
}
