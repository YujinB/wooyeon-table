package com.wooyeon.table.domain.matching.domain.model;

/**
 * 매칭 실행 설정. 필수 조건(인원·나이 차)과 선호 점수 기준을 함께 담는다.
 *
 * @param improvementEnabled   그리디 이후 테이블 간 이동·교환으로 총점을 개선할지 (벤치마크 비교용)
 * @param maxImprovementRounds 개선 단계 반복 상한
 */
public record MatchingPolicy(
        int minTableSize,
        int maxTableSize,
        int maxAgeGap,
        ScoreWeights weights,
        double minScore,
        boolean improvementEnabled,
        int maxImprovementRounds
) {

    public MatchingPolicy {
        if (minTableSize < 2 || maxTableSize < minTableSize) {
            throw new IllegalArgumentException("테이블 인원 범위가 올바르지 않습니다");
        }
        if (maxAgeGap < 0 || minScore < 0 || minScore > 100 || maxImprovementRounds < 0) {
            throw new IllegalArgumentException("매칭 설정값이 올바르지 않습니다");
        }
        if (weights == null) {
            throw new IllegalArgumentException("가중치가 없습니다");
        }
    }

    public static MatchingPolicy defaults() {
        return new MatchingPolicy(4, 6, 8, ScoreWeights.DEFAULT, 50, true, 100);
    }

    public MatchingPolicy withImprovement(boolean enabled) {
        return new MatchingPolicy(minTableSize, maxTableSize, maxAgeGap, weights, minScore, enabled,
                maxImprovementRounds);
    }

    public MatchingPolicy withMinScore(double score) {
        return new MatchingPolicy(minTableSize, maxTableSize, maxAgeGap, weights, score, improvementEnabled,
                maxImprovementRounds);
    }
}
