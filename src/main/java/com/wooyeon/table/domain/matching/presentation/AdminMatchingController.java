package com.wooyeon.table.domain.matching.presentation;

import com.wooyeon.table.domain.matching.application.dto.MatchRunSummary;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/slots")
public class AdminMatchingController {

    private final MatchingService matchingService;

    /** 데모용 "지금 마감 실행". 이미 실행한 회차면 기존 결과를 돌려준다 */
    @PostMapping("/{slotId}/match-run")
    public ApiResponse<MatchRunSummary> runNow(@PathVariable Long slotId) {
        return ApiResponse.ok(matchingService.runForSlot(slotId));
    }
}
