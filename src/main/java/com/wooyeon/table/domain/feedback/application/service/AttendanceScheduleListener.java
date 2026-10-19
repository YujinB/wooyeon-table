package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.matching.application.event.TablesConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 테이블 확정 → 멤버 참석 예정 기록 (식당·단체방 다음) */
@Component
@RequiredArgsConstructor
public class AttendanceScheduleListener {

    private final AttendanceService attendanceService;

    @EventListener
    @Order(20)
    public void onTablesConfirmed(TablesConfirmedEvent event) {
        attendanceService.scheduleForSlot(event.slotId());
    }
}
