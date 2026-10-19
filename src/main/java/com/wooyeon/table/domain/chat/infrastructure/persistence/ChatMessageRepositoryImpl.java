package com.wooyeon.table.domain.chat.infrastructure.persistence;

import com.wooyeon.table.domain.chat.domain.model.ChatMessage;
import com.wooyeon.table.domain.chat.domain.repository.ChatMessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final ChatMessageJpaRepository jpaRepository;

    @Override
    public ChatMessage save(ChatMessage message) {
        return jpaRepository.save(message);
    }

    @Override
    public boolean existsByDedupKey(String dedupKey) {
        return jpaRepository.existsByDedupKey(dedupKey);
    }

    @Override
    public List<ChatMessage> findVisible(Long roomId, LocalDateTime now) {
        return jpaRepository.findAllByRoomIdAndVisibleAtLessThanEqualOrderByVisibleAtAscIdAsc(roomId, now);
    }

    @Override
    public List<ChatMessage> findAllByRoomId(Long roomId) {
        return jpaRepository.findAllByRoomIdOrderByVisibleAtAscIdAsc(roomId);
    }
}
