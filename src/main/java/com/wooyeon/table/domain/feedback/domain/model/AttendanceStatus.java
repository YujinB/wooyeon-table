package com.wooyeon.table.domain.feedback.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 참석 상태 (PRD 12절 attendance_status). NO_SHOW_CANDIDATE는 운영자가 후보만 확인하도록(FDB-01) 더한 값 */
@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    SCHEDULED("참석 예정"),
    ATTENDED("참석 완료"),
    CANCELED_EARLY("사전 취소"),
    CANCELED_LATE("당일·제한 시점 이후 취소"),
    /** 행사 종료까지 체크인하지 않음 → 운영자 확인 대기 */
    NO_SHOW_CANDIDATE("노쇼 후보"),
    NO_SHOW("노쇼");

    private final String label;

    public boolean isCanceled() {
        return this == CANCELED_EARLY || this == CANCELED_LATE;
    }
}
