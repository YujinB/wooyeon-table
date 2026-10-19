package com.wooyeon.table.domain.safety.domain.model;

public enum ReportStatus {
    /** 접수, 운영자 검토 대기 */
    RECEIVED,
    /** 조치함 (경고·참가 제한·영구 제한) */
    ACTIONED,
    /** 조치 없이 종결 */
    DISMISSED
}
