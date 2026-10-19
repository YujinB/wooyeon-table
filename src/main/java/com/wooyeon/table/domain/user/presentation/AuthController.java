package com.wooyeon.table.domain.user.presentation;

import com.wooyeon.table.domain.user.application.dto.LoginResult;
import com.wooyeon.table.domain.user.application.service.AuthService;
import com.wooyeon.table.domain.user.presentation.request.LoginRequest;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /** 로그인 → Authorization: Bearer {token} 으로 /api/app/**, /api/admin/** 호출 */
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.loginId(), request.password()));
    }
}
