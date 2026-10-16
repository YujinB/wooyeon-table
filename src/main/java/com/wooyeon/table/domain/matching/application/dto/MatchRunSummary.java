package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.matching.domain.model.MatchRun;
import java.time.LocalDateTime;

/**
 * @param alreadyExecuted 이미 실행된 회차라 기존 결과를 돌려줬는지 (멱등 실행)
 */
public record MatchRunSummary(
        Long matchRunId,
        Long slotId,
        String algorithmVersion,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        int candidateCount,
        int assignedCount,
        int tableCount,
        int carriedOverCount,
        int alternativeOfferedCount,
        int reallocations,
        int dissolvedTables,
        boolean alreadyExecuted
) {

    public static MatchRunSummary of(MatchRun run, boolean alreadyExecuted) {
        return new MatchRunSummary(run.getId(), run.getSlotId(), run.getAlgorithmVersion(), run.getStartedAt(),
                run.getFinishedAt(), run.getCandidateCount(), run.getAssignedCount(), run.getTableCount(),
                run.getCarriedOverCount(), run.getAlternativeOfferedCount(),
                run.getReallocations() + run.getDisplacements(), run.getDissolvedTables(), alreadyExecuted);
    }
}
