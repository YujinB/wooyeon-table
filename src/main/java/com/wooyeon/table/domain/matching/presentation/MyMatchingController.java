package com.wooyeon.table.domain.matching.presentation;

import com.wooyeon.table.domain.matching.application.dto.MyMatchResult;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참가자 앱 매칭 결과. 로그인(#4) 전이라 신청 id로 조회하고, 로그인 후 본인 신청만 보이도록 바꾼다 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/applications")
public class MyMatchingController {

    private final MatchingQueryService matchingQueryService;

    @GetMapping("/{applicationId}/match")
    public ApiResponse<MyMatchResult> myResult(@PathVariable Long applicationId) {
        return ApiResponse.ok(matchingQueryService.getMyResult(applicationId));
    }
}
