package com.wooyeon.table.domain.matching.domain.model;

public record UnassignedCandidate(long candidateId, UnassignedReason reason, int relocationCount) {
}
