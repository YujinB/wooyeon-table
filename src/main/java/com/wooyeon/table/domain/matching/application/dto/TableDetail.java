package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.matching.domain.model.DiningTableStatus;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** 운영자용 테이블 상세 — PRD 7.2 표시 항목 + 좌석별 배치 사유 (MAT-12) */
public record TableDetail(
        Long tableId,
        Long slotId,
        LocalDate eventDate,
        LocalTime startTime,
        int tableNo,
        Region region,
        String regionLabel,
        DiningTableStatus status,
        LocalDateTime confirmedAt,
        int size,
        int minAge,
        int maxAge,
        int maleCount,
        int femaleCount,
        List<String> mbtiComposition,
        List<String> commonInterests,
        int previouslyMetPairs,
        ScoreBreakdown score,
        List<SeatDetail> seats
) {
}
