package com.wooyeon.table.domain.venue.presentation;

import com.wooyeon.table.domain.venue.application.dto.SlotVenueAssignmentResult;
import com.wooyeon.table.domain.venue.application.dto.VenueAssignmentView;
import com.wooyeon.table.domain.venue.application.dto.VenueInfo;
import com.wooyeon.table.domain.venue.application.service.VenueAssignmentService;
import com.wooyeon.table.domain.venue.application.service.VenueQueryService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import com.wooyeon.table.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminVenueController {

    private final VenueAssignmentService venueAssignmentService;
    private final VenueQueryService venueQueryService;

    /** 파트너 식당 재고 */
    @GetMapping("/venues")
    public ApiResponse<List<VenueInfo>> venues() {
        return ApiResponse.ok(venueQueryService.findVenues());
    }

    /** 회차 테이블별 식당 배정 현황 */
    @GetMapping("/slots/{slotId}/venues")
    public ApiResponse<List<VenueAssignmentView>> slotAssignments(@PathVariable Long slotId) {
        return ApiResponse.ok(venueQueryService.findBySlot(slotId));
    }

    /** 아직 배정되지 않은 테이블에 식당 배정 (매칭 직후 자동 실행되며, 기존 데이터 보정·데모용) */
    @PostMapping("/slots/{slotId}/venues/assign")
    public ApiResponse<SlotVenueAssignmentResult> assignSlot(@PathVariable Long slotId) {
        return ApiResponse.ok(venueAssignmentService.assignSlot(slotId));
    }

    /** 테이블 식당 배정 상태 (테이블 상세 화면용) */
    @GetMapping("/tables/{tableId}/venue")
    public ApiResponse<VenueAssignmentView> tableVenue(@PathVariable Long tableId) {
        return ApiResponse.ok(venueQueryService.findByTable(tableId).orElse(null));
    }

    /** 식당 배정 실패 예외 목록 */
    @GetMapping("/venue-exceptions")
    public ApiResponse<List<VenueAssignmentView>> failures() {
        return ApiResponse.ok(venueQueryService.findFailures());
    }

    /** 실패한 테이블 다시 배정 */
    @PostMapping("/tables/{tableId}/venue/retry")
    public ApiResponse<VenueAssignmentView> retry(@PathVariable Long tableId) {
        venueAssignmentService.retryTable(tableId);
        return ApiResponse.ok(venueQueryService.findByTable(tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DINING_TABLE_NOT_FOUND, String.valueOf(tableId))));
    }
}
