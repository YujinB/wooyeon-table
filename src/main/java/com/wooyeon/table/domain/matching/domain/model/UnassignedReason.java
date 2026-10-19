package com.wooyeon.table.domain.matching.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 이번 실행에서 테이블에 들지 못한 사유 (MAT-12) */
@Getter
@RequiredArgsConstructor
public enum UnassignedReason {
    NOT_ENOUGH_CANDIDATES("같은 날짜·지역 신청자가 테이블 최소 인원보다 적음"),
    AGE_GAP("나이 차 조건을 만족하는 신청자가 부족함"),
    EXCLUDED_RELATION("차단·신고·비선호 관계를 빼면 신청자가 부족함"),
    BELOW_MIN_SCORE("만들 수 있는 조합이 최소 매칭 점수에 못 미침"),
    NO_FEASIBLE_TABLE("조건에 맞는 테이블에 빈자리가 없음");

    private final String description;
}
