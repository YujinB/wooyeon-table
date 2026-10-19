package com.wooyeon.table.domain.matching.domain.model;

/**
 * 선호 조건 가중치. 합이 100이 아니어도 점수 계산 때 100점 만점으로 환산한다 (MAT-15 대비 설정값).
 */
public record ScoreWeights(double genderBalance, double mbti, double interest, double preference, double novelty) {

    public static final ScoreWeights DEFAULT = new ScoreWeights(30, 25, 20, 15, 10);

    public ScoreWeights {
        if (genderBalance < 0 || mbti < 0 || interest < 0 || preference < 0 || novelty < 0) {
            throw new IllegalArgumentException("가중치는 0 이상이어야 합니다");
        }
        if (genderBalance + mbti + interest + preference + novelty <= 0) {
            throw new IllegalArgumentException("가중치 합은 0보다 커야 합니다");
        }
    }

    public double sum() {
        return genderBalance + mbti + interest + preference + novelty;
    }
}
