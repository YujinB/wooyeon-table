package com.wooyeon.table.domain.chat.application.service;

import com.wooyeon.table.domain.matching.application.event.TablesConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 테이블 확정 → 단체방 생성 (INF-04). 공지에 식당 정보가 들어가므로 식당 배정(Order 0) 다음에 실행한다
 */
@Component
@RequiredArgsConstructor
public class TableConfirmationListener {

    public static final int ORDER = 10;

    private final ChatRoomService chatRoomService;

    @EventListener
    @Order(ORDER)
    public void onTablesConfirmed(TablesConfirmedEvent event) {
        chatRoomService.openForSlot(event.slotId());
    }
}
