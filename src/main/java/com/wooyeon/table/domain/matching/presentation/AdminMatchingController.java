package com.wooyeon.table.domain.matching.presentation;

import com.wooyeon.table.domain.matching.application.dto.MatchRunSummary;
import com.wooyeon.table.domain.matching.application.dto.SlotMatchingResult;
import com.wooyeon.table.domain.matching.application.dto.TableDetail;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminMatchingController {

    private final MatchingService matchingService;
    private final MatchingQueryService matchingQueryService;

    /** 데모용 "지금 마감 실행". 이미 실행한 회차면 기존 결과를 돌려준다 */
    @PostMapping("/slots/{slotId}/match-run")
    public ApiResponse<MatchRunSummary> runNow(@PathVariable Long slotId) {
        return ApiResponse.ok(matchingService.runForSlot(slotId));
    }

    /** 회차 매칭 결과: 실행 요약·테이블 목록·미배정자 사유와 처리 결과 */
    @GetMapping("/slots/{slotId}/matching")
    public ApiResponse<SlotMatchingResult> slotResult(@PathVariable Long slotId) {
        return ApiResponse.ok(matchingQueryService.getSlotResult(slotId));
    }

    /** 테이블 상세: 구성·점수 항목별 내역·좌석별 배치 사유 */
    @GetMapping("/tables/{tableId}")
    public ApiResponse<TableDetail> tableDetail(@PathVariable Long tableId) {
        return ApiResponse.ok(matchingQueryService.getTableDetail(tableId));
    }
}
