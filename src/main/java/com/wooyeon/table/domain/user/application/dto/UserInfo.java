package com.wooyeon.table.domain.user.application.dto;

import com.wooyeon.table.domain.user.domain.model.User;
import com.wooyeon.table.domain.user.domain.model.UserRole;

public record UserInfo(Long id, String loginId, String name, String phone, UserRole role) {

    public static UserInfo from(User user) {
        return new UserInfo(user.getId(), user.getLoginId(), user.getName(), user.getPhone(), user.getRole());
    }
}
