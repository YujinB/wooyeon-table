package com.wooyeon.table.domain.chat.application.service;

import com.wooyeon.table.domain.chat.application.dto.ChatRoomView;
import com.wooyeon.table.domain.chat.application.dto.ChatRoomView.Member;
import com.wooyeon.table.domain.chat.application.dto.ChatRoomView.Message;
import com.wooyeon.table.domain.chat.application.dto.ChatRoomView.Summary;
import com.wooyeon.table.domain.chat.domain.model.ChatMember;
import com.wooyeon.table.domain.chat.domain.model.ChatMessage;
import com.wooyeon.table.domain.chat.domain.model.ChatRoom;
import com.wooyeon.table.domain.chat.domain.repository.ChatMessageRepository;
import com.wooyeon.table.domain.chat.domain.repository.ChatRoomRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참가자 단체방 조회·대화 (INF-04·06). 멤버만 들어올 수 있고, 다른 멤버는 별칭과 제한된 소개로만 보인다.
 * 매칭 확정 전에는 단체방 자체가 없어 다른 참가자 정보가 공개되지 않는다
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Summary> myRooms(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return chatRoomRepository.findAllByMemberUserId(userId).stream()
                .map(room -> {
                    List<ChatMessage> visible = chatMessageRepository.findVisible(room.getId(), now);
                    ChatMessage last = visible.isEmpty() ? null : visible.get(visible.size() - 1);
                    return new Summary(room.getId(), room.getTitle(), room.getEventDate(), room.getMembers().size(),
                            last == null ? null : last.getContent(), last == null ? null : last.getVisibleAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomView view(Long userId, Long roomId) {
        ChatRoom room = memberRoom(userId, roomId);
        ChatMember me = room.memberOf(userId).orElseThrow();
        List<Member> members = room.getMembers().stream()
                .map(member -> new Member(member.getAlias(), member.getIntro(), member.getUserId().equals(userId)))
                .toList();
        List<Message> messages = chatMessageRepository.findVisible(roomId, LocalDateTime.now(clock)).stream()
                .map(message -> new Message(message.getId(), message.getKind(), message.isSystem(),
                        message.getSenderAlias(), message.getContent(), message.getVisibleAt(),
                        userId.equals(message.getSenderUserId())))
                .toList();
        return new ChatRoomView(room.getId(), room.getTitle(), room.getEventDate(), me.getAlias(), members, messages);
    }

    public void send(Long userId, Long roomId, String content) {
        ChatRoom room = memberRoom(userId, roomId);
        ChatMember me = room.memberOf(userId).orElseThrow();
        chatMessageRepository.save(ChatMessage.fromMember(roomId, me, content.strip(), LocalDateTime.now(clock)));
    }

    /** 멤버가 아니면 방이 없는 것처럼 응답한다 */
    private ChatRoom memberRoom(Long userId, Long roomId) {
        return chatRoomRepository.findById(roomId)
                .filter(room -> room.hasMember(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND, String.valueOf(roomId)));
    }
}
