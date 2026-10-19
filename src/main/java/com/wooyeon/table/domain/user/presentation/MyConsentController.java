package com.wooyeon.table.domain.user.presentation;

import com.wooyeon.table.domain.user.application.dto.MyConsent;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.domain.user.application.service.UserQueryService;
import com.wooyeon.table.domain.user.presentation.request.AgreeConsentRequest;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 개인정보 수집·이용과 운영 정책 동의 (ACC-05). 동의해야 신청할 수 있다 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/me/consent")
public class MyConsentController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    @GetMapping
    public ApiResponse<MyConsent> consent(@LoginUser AuthUser user) {
        return ApiResponse.ok(userQueryService.myConsent(user.userId()));
    }

    @PostMapping
    public ApiResponse<MyConsent> agree(@LoginUser AuthUser user, @Valid @RequestBody AgreeConsentRequest request) {
        userCommandService.agree(user.userId());
        return ApiResponse.ok(userQueryService.myConsent(user.userId()));
    }
}
