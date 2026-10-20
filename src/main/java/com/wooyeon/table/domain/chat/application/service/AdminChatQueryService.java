package com.wooyeon.table.domain.chat.application.service;

import com.wooyeon.table.domain.chat.application.dto.AdminChatRoomView;
import com.wooyeon.table.domain.chat.domain.repository.ChatMessageRepository;
import com.wooyeon.table.domain.chat.domain.repository.ChatRoomRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminChatQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final Clock clock;

    public AdminChatRoomView findByTable(Long tableId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return chatRoomRepository.findByTableId(tableId)
                .map(room -> new AdminChatRoomView(true, room.getId(), room.getTitle(), room.getCreatedAt(),
                        room.getMembers().stream()
                                .map(member -> new AdminChatRoomView.Member(member.getAlias(), member.getUserId(),
                                        member.getApplicationId(), member.getIntro()))
                                .toList(),
                        chatMessageRepository.findAllByRoomId(room.getId()).stream()
                                .map(message -> new AdminChatRoomView.Message(message.getKind(),
                                        message.getSenderAlias(), message.getContent(), message.getVisibleAt(),
                                        !message.getVisibleAt().isAfter(now)))
                                .toList()))
                .orElseGet(AdminChatRoomView::notCreated);
    }
}
