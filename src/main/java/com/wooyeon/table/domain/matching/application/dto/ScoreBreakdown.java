package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;

/** 테이블 선호 점수 항목별 내역 (0~100, 소수 첫째 자리) */
public record ScoreBreakdown(double total, double genderBalance, double mbti, double interest, double preference,
                             double novelty) {

    public static ScoreBreakdown from(DiningTable table) {
        return new ScoreBreakdown(round(table.getTotalScore()), round(table.getGenderScore()),
                round(table.getMbtiScore()), round(table.getInterestScore()), round(table.getPreferenceScore()),
                round(table.getNoveltyScore()));
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
