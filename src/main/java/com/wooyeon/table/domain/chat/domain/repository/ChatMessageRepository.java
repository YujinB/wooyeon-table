package com.wooyeon.table.domain.chat.domain.repository;

import com.wooyeon.table.domain.chat.domain.model.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage message);

    /** 시스템 메시지 중복 게시 확인 */
    boolean existsByDedupKey(String dedupKey);

    /** 지금 보이는 메시지 (게시 시각 순) */
    List<ChatMessage> findVisible(Long roomId, LocalDateTime now);

    List<ChatMessage> findAllByRoomId(Long roomId);
}
