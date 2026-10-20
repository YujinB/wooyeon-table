package com.wooyeon.table.domain.matching.presentation;

import com.wooyeon.table.domain.matching.application.dto.MyMatchResult;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참가자 앱 매칭 결과. 본인 신청만 조회할 수 있다 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/applications")
public class MyMatchingController {

    private final MatchingQueryService matchingQueryService;

    @GetMapping("/{applicationId}/match")
    public ApiResponse<MyMatchResult> myResult(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(matchingQueryService.getMyResult(user.userId(), applicationId));
    }
}
