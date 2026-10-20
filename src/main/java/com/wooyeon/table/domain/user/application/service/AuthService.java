package com.wooyeon.table.domain.user.application.service;

import com.wooyeon.table.domain.user.application.dto.LoginResult;
import com.wooyeon.table.domain.user.domain.model.User;
import com.wooyeon.table.domain.user.domain.repository.UserRepository;
import com.wooyeon.table.global.auth.AuthTokenProvider;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 테스트 계정 로그인 (ACC-01) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenProvider tokenProvider;

    public LoginResult login(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId)
                .filter(User::canLogin)
                .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
        return new LoginResult(tokenProvider.issue(user.getId(), user.getRole()), user.getId(), user.getName(),
                user.getRole());
    }
}
