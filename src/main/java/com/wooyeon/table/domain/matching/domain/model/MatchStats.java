package com.wooyeon.table.domain.matching.domain.model;

/**
 * 실행 통계 (MatchRun 기록용).
 *
 * @param initialTables     그리디로 처음 만든 테이블 수
 * @param reallocations     남은 사람을 기존 테이블 빈자리에 넣은 횟수
 * @param displacements     남은 사람을 넣으려고 기존 멤버를 다른 테이블로 옮긴 횟수
 * @param moves             개선 단계 이동 횟수
 * @param swaps             개선 단계 교환 횟수
 * @param dissolvedTables   최소 점수 미달로 해제한 테이블 수
 * @param improvementRounds 개선 단계 반복 횟수
 */
public record MatchStats(
        int candidateCount,
        int initialTables,
        int reallocations,
        int displacements,
        int moves,
        int swaps,
        int dissolvedTables,
        int improvementRounds
) {
}
