package com.wooyeon.table.domain.safety.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 운영자 조치 단계 (SAF-02) */
@Getter
@RequiredArgsConstructor
public enum SanctionLevel {
    WARNING("경고"),
    RESTRICTED("참가 제한"),
    BANNED("영구 제한");

    private final String label;
}
