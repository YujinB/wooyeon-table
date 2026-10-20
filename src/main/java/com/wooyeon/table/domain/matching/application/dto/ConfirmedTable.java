package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.matching.domain.model.TableSeat;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.util.List;

/** 확정 테이블 요약. 다른 도메인(식당·단체방·안내)이 확정 후 처리에 쓴다 */
public record ConfirmedTable(Long tableId, Long slotId, int tableNo, Region region, int size, List<Long> userIds,
                             List<Long> applicationIds) {

    public static ConfirmedTable from(DiningTable table) {
        return new ConfirmedTable(table.getId(), table.getSlotId(), table.getTableNo(), table.getRegion(),
                table.size(), table.getSeats().stream().map(TableSeat::getUserId).toList(),
                table.getSeats().stream().map(TableSeat::getApplicationId).toList());
    }
}
