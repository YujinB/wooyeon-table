package com.wooyeon.table.domain.matching.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회차별 자동 매칭 실행 기록 (PRD 13절 MatchRun, 16절 운영 추적).
 * 회차당 하나만 존재해서(slotId unique) 같은 회차를 다시 실행해도 결과가 중복 생성되지 않는다.
 */
@Getter
@Entity
@Table(name = "match_runs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchRun extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long slotId;

    @Column(nullable = false, length = 30)
    private String algorithmVersion;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime finishedAt;

    private int candidateCount;
    private int assignedCount;
    private int tableCount;
    /** 다음 희망 날짜로 넘어간 인원 */
    private int carriedOverCount;
    /** 대체 안내(ALTERNATIVE_OFFERED)로 바뀐 인원 */
    private int alternativeOfferedCount;

    private int initialTables;
    private int reallocations;
    private int displacements;
    private int moves;
    private int swaps;
    private int dissolvedTables;
    private int improvementRounds;

    /** 적용 기준 (최소 점수·가중치) */
    @Column(nullable = false, length = 200)
    private String policySnapshot;

    private MatchRun(Long slotId, String algorithmVersion, LocalDateTime startedAt, LocalDateTime finishedAt,
                     String policySnapshot, MatchResult result, int carriedOverCount, int alternativeOfferedCount) {
        MatchStats stats = result.stats();
        this.slotId = slotId;
        this.algorithmVersion = algorithmVersion;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.policySnapshot = policySnapshot;
        this.candidateCount = stats.candidateCount();
        this.assignedCount = result.assignedCount();
        this.tableCount = result.tables().size();
        this.carriedOverCount = carriedOverCount;
        this.alternativeOfferedCount = alternativeOfferedCount;
        this.initialTables = stats.initialTables();
        this.reallocations = stats.reallocations();
        this.displacements = stats.displacements();
        this.moves = stats.moves();
        this.swaps = stats.swaps();
        this.dissolvedTables = stats.dissolvedTables();
        this.improvementRounds = stats.improvementRounds();
    }

    public static MatchRun completed(Long slotId, String algorithmVersion, LocalDateTime startedAt,
                                     LocalDateTime finishedAt, MatchingPolicy policy, MatchResult result,
                                     int carriedOverCount, int alternativeOfferedCount) {
        return new MatchRun(slotId, algorithmVersion, startedAt, finishedAt, snapshot(policy), result,
                carriedOverCount, alternativeOfferedCount);
    }

    private static String snapshot(MatchingPolicy policy) {
        ScoreWeights w = policy.weights();
        return "size=%d~%d, ageGap<=%d, minScore=%.0f, weights(gender=%.0f, mbti=%.0f, interest=%.0f, preference=%.0f, novelty=%.0f), improvement=%s"
                .formatted(policy.minTableSize(), policy.maxTableSize(), policy.maxAgeGap(), policy.minScore(),
                        w.genderBalance(), w.mbti(), w.interest(), w.preference(), w.novelty(),
                        policy.improvementEnabled());
    }
}
