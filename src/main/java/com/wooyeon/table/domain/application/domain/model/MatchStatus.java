package com.wooyeon.table.domain.application.domain.model;

/** 매칭 상태 (PRD 12절 match_status) */
public enum MatchStatus {
    WAITING,
    MATCHING,
    REALLOCATING,
    CONFIRMED,
    /** 매칭 실패 후 대체 일정·이용권 보관·환불 선택 대기 */
    ALTERNATIVE_OFFERED,
    TRANSFERRED,
    NO_MATCH,
    EXCEPTION
}
