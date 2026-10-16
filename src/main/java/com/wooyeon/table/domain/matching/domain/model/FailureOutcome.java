package com.wooyeon.table.domain.matching.domain.model;

/** 이번 회차에서 테이블에 들지 못한 신청의 처리 (PRD 7.4) */
public enum FailureOutcome {
    /** 본인이 고른 다음 날짜 회차의 후보로 넘어감 (매칭 대기 유지) */
    CARRIED_OVER,
    /** 남은 희망 날짜가 없어 대체 일정·보관·환불 선택 대기 */
    ALTERNATIVE_OFFERED
}
