package com.wooyeon.table.domain.user.application.dto;

import com.wooyeon.table.domain.user.domain.model.UserRole;

public record LoginResult(String token, Long userId, String name, UserRole role) {
}
