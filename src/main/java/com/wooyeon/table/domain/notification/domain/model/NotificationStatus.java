package com.wooyeon.table.domain.notification.domain.model;

public enum NotificationStatus {
    SCHEDULED,
    SENT,
    /** 발송 실패, 다음 재시도 대기 */
    RETRYING,
    /** 재시도까지 모두 실패 → 운영자 예외 (INF-08) */
    GAVE_UP
}
