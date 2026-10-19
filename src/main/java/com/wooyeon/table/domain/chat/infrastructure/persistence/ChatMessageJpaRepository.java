package com.wooyeon.table.domain.chat.infrastructure.persistence;

import com.wooyeon.table.domain.chat.domain.model.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {

    boolean existsByDedupKey(String dedupKey);

    List<ChatMessage> findAllByRoomIdAndVisibleAtLessThanEqualOrderByVisibleAtAscIdAsc(Long roomId,
                                                                                      LocalDateTime now);

    List<ChatMessage> findAllByRoomIdOrderByVisibleAtAscIdAsc(Long roomId);
}
