package com.wooyeon.table.domain.venue.application.service;

import com.wooyeon.table.domain.matching.application.event.TablesConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 테이블 확정과 동시에 식당을 배정한다 (INF-02, PRD 18절 11단계). 단체방 공지보다 먼저 (Order 0) */
@Component
@RequiredArgsConstructor
public class TablesConfirmedListener {

    private final VenueAssignmentService venueAssignmentService;

    @EventListener
    @Order(0)
    public void onTablesConfirmed(TablesConfirmedEvent event) {
        venueAssignmentService.assignSlot(event.slotId());
    }
}
