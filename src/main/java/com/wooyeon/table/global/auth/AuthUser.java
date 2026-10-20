package com.wooyeon.table.global.auth;

import com.wooyeon.table.domain.user.domain.model.UserRole;

/** 토큰에서 꺼낸 로그인 사용자 */
public record AuthUser(Long userId, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
