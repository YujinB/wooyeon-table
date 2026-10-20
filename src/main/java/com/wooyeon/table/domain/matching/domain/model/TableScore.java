package com.wooyeon.table.domain.matching.domain.model;

/**
 * 테이블 선호 점수(0~100)와 항목별 내역. 항목 값은 가중치를 반영한 점수라 더하면 total이 된다.
 */
public record TableScore(double total, double genderBalance, double mbti, double interest, double preference,
                         double novelty) {
}
