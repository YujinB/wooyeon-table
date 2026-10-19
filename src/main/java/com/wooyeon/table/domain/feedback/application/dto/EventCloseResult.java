package com.wooyeon.table.domain.feedback.application.dto;

public record EventCloseResult(Long slotId, int participants, int attended, int noShowCandidates, int canceled) {
}
