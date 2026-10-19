package com.wooyeon.table.domain.chat.infrastructure.persistence;

import com.wooyeon.table.domain.chat.domain.model.ChatRoom;
import com.wooyeon.table.domain.chat.domain.repository.ChatRoomRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepository {

    private final ChatRoomJpaRepository jpaRepository;

    @Override
    public ChatRoom save(ChatRoom room) {
        return jpaRepository.saveAndFlush(room);
    }

    @Override
    public Optional<ChatRoom> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<ChatRoom> findByTableId(Long tableId) {
        return jpaRepository.findByTableId(tableId);
    }

    @Override
    public List<ChatRoom> findAllBySlotId(Long slotId) {
        return jpaRepository.findAllBySlotIdOrderByIdAsc(slotId);
    }

    @Override
    public List<ChatRoom> findAllByMemberUserId(Long userId) {
        return jpaRepository.findAllByMemberUserId(userId);
    }
}
