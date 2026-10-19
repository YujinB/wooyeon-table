package com.wooyeon.table.domain.chat.domain.repository;

import com.wooyeon.table.domain.chat.domain.model.ChatRoom;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository {

    ChatRoom save(ChatRoom room);

    Optional<ChatRoom> findById(Long id);

    Optional<ChatRoom> findByTableId(Long tableId);

    List<ChatRoom> findAllBySlotId(Long slotId);

    /** 이 사용자가 멤버인 단체방 (최근 날짜부터) */
    List<ChatRoom> findAllByMemberUserId(Long userId);
}
