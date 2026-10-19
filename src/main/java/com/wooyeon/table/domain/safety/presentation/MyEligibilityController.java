package com.wooyeon.table.domain.safety.presentation;

import com.wooyeon.table.domain.safety.application.dto.Eligibility;
import com.wooyeon.table.domain.safety.application.service.EligibilityService;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyEligibilityController {

    private final EligibilityService eligibilityService;

    /** 지금 신청할 수 있는지 (참가 제한이면 사유·해제일) */
    @GetMapping("/api/app/me/eligibility")
    public ApiResponse<Eligibility> eligibility(@LoginUser AuthUser user) {
        return ApiResponse.ok(eligibilityService.check(user.userId()));
    }
}
