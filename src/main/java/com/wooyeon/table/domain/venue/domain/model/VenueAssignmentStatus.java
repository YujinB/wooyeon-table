package com.wooyeon.table.domain.venue.domain.model;

public enum VenueAssignmentStatus {
    ASSIGNED,
    /** 같은 지역 식당을 모두 시도해도 배정 실패 → 운영자 예외 */
    FAILED
}
