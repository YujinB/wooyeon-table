package com.wooyeon.table.domain.event.domain.model;

public enum EventSlotStatus {
    /** 신청 받는 중 (아직 매칭 전) */
    RECRUITING,
    /** 자동 매칭 실행 완료 */
    MATCHED
}
