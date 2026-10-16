package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.profile.domain.model.Region;

/** 회차 결과 화면의 테이블 한 줄 */
public record TableSummary(Long tableId, int tableNo, Region region, String regionLabel, int size, int minAge,
                           int maxAge, int maleCount, int femaleCount, double totalScore) {

    public static TableSummary from(DiningTable table) {
        return new TableSummary(table.getId(), table.getTableNo(), table.getRegion(), table.getRegion().getLabel(),
                table.size(), table.getMinAge(), table.getMaxAge(), table.getMaleCount(), table.getFemaleCount(),
                ScoreBreakdown.from(table).total());
    }
}
