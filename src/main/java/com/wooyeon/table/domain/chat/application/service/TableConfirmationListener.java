package com.wooyeon.table.domain.chat.application.service;

import com.wooyeon.table.domain.chat.domain.model.ChatRoom;
import com.wooyeon.table.domain.matching.application.event.TablesConfirmedEvent;
import com.wooyeon.table.domain.venue.application.event.VenueAssignedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 테이블 확정 → 단체방 생성 (INF-04) + 시스템 공지 (INF-05). 공지에 식당 정보가 들어가므로 식당 배정(Order 0) 다음에 실행한다
 */
@Component
@RequiredArgsConstructor
public class TableConfirmationListener {

    public static final int ORDER = 10;

    private final ChatRoomService chatRoomService;
    private final ChatNoticeService chatNoticeService;

    /** 운영자 재시도로 식당이 늦게 배정되면 시간·장소 공지를 올린다 */
    @EventListener
    public void onVenueAssigned(VenueAssignedEvent event) {
        chatNoticeService.postPlace(event.tableId());
    }

    @EventListener
    @Order(ORDER)
    public void onTablesConfirmed(TablesConfirmedEvent event) {
        for (ChatRoom room : chatRoomService.openForSlot(event.slotId())) {
            chatNoticeService.postConfirmationNotices(room);
            chatNoticeService.notifyConfirmed(room);
            chatNoticeService.scheduleReminder(room);
        }
    }
}
