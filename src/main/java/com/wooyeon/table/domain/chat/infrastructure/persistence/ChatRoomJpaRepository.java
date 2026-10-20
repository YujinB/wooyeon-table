package com.wooyeon.table.domain.chat.infrastructure.persistence;

import com.wooyeon.table.domain.chat.domain.model.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByTableId(Long tableId);

    List<ChatRoom> findAllBySlotIdOrderByIdAsc(Long slotId);

    @Query("select distinct r from ChatRoom r join r.members m where m.userId = :userId order by r.eventDate desc")
    List<ChatRoom> findAllByMemberUserId(Long userId);
}
