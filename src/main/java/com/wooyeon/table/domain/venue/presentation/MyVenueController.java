package com.wooyeon.table.domain.venue.presentation;

import com.wooyeon.table.domain.matching.application.dto.MyMatchResult.MyTable;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.venue.application.dto.VenueAssignmentView;
import com.wooyeon.table.domain.venue.application.service.VenueQueryService;
import com.wooyeon.table.domain.venue.presentation.response.MyVenueResponse;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/applications/{applicationId}")
public class MyVenueController {

    private final MatchingQueryService matchingQueryService;
    private final VenueQueryService venueQueryService;

    /** 확정된 본인 신청의 식당 안내 (INF-03). 확정 전이면 409 */
    @GetMapping("/venue")
    public ApiResponse<MyVenueResponse> myVenue(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        MyTable table = matchingQueryService.getMyResult(user.userId(), applicationId).table();
        if (table == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "매칭이 확정된 신청만 식당을 볼 수 있습니다");
        }
        return ApiResponse.ok(venueQueryService.findByTable(table.tableId())
                .filter(VenueAssignmentView::isAssigned)
                .map(assignment -> MyVenueResponse.of(assignment, table.eventDate().atTime(table.startTime())))
                .orElseGet(MyVenueResponse::pending));
    }
}
