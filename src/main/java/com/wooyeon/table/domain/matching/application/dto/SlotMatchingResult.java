package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 운영자용 회차 매칭 결과
 *
 * @param run           실행 요약. 아직 실행 전이면 null
 * @param appliedPolicy 실행 때 적용한 기준 (인원·나이 차·최소 점수·가중치)
 */
public record SlotMatchingResult(Long slotId, LocalDate eventDate, LocalDateTime matchAt, EventSlotStatus slotStatus,
                                 MatchRunSummary run, String appliedPolicy, List<TableSummary> tables,
                                 List<FailureDetail> failures) {
}
