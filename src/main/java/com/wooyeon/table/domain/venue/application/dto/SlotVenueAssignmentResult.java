package com.wooyeon.table.domain.venue.application.dto;

import java.util.List;

/** assignedTables = 지금 배정돼 있는 테이블 수(이전에 배정된 것 포함), failedTableIds = 운영자 예외 */
public record SlotVenueAssignmentResult(Long slotId, int assignedTables, List<Long> failedTableIds) {
}
