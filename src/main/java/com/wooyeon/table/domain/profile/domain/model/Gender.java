package com.wooyeon.table.domain.profile.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE("남"),
    FEMALE("여");

    private final String label;

    public static Gender fromLabel(String raw) {
        String value = raw == null ? "" : raw.trim();
        for (Gender gender : values()) {
            if (gender.label.equals(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("알 수 없는 성별: " + raw);
    }
}
