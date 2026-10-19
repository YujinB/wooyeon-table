package com.wooyeon.table.domain.matching.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 이번 회차에서 테이블에 들지 못한 신청의 처리 (PRD 7.4) */
@Getter
@RequiredArgsConstructor
public enum FailureOutcome {
    /** 본인이 고른 다음 날짜 회차의 후보로 넘어감 (매칭 대기 유지) */
    CARRIED_OVER("선택한 다음 날짜 회차 매칭에 자동으로 다시 참여"),
    /** 남은 희망 날짜가 없어 대체 일정·보관·환불 선택 대기 */
    ALTERNATIVE_OFFERED("선택한 날짜가 모두 끝나 대체 일정 이동·이용권 보관·환불 중 선택 대기");

    private final String description;
}
