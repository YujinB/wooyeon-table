package com.wooyeon.table.domain.matching.domain.model;

import java.util.List;

public record MatchResult(List<MatchedTable> tables, List<UnassignedCandidate> unassigned, MatchStats stats) {

    public int assignedCount() {
        return tables.stream().mapToInt(MatchedTable::size).sum();
    }

    public double averageScore() {
        return tables.stream().mapToDouble(t -> t.score().total()).average().orElse(0);
    }

    public double lowestScore() {
        return tables.stream().mapToDouble(t -> t.score().total()).min().orElse(0);
    }
}
