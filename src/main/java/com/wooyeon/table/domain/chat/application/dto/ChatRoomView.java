package com.wooyeon.table.domain.chat.application.dto;

import com.wooyeon.table.domain.chat.domain.model.ChatMessageKind;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 참가자가 보는 단체방 (INF-06). 멤버는 별칭·제한된 소개만, 메시지는 지금 보이는 것만
 */
public record ChatRoomView(Long roomId, String title, LocalDate eventDate, String myAlias, List<Member> members,
                           List<Message> messages) {

    /** seatNo: 신고·차단할 때 상대를 가리키는 번호 (실명·계정 대신) */
    public record Member(int seatNo, String alias, String intro, boolean me) {
    }

    public record Message(Long messageId, ChatMessageKind kind, boolean system, String senderAlias, String content,
                          LocalDateTime at, boolean mine) {
    }

    public record Summary(Long roomId, String title, LocalDate eventDate, int memberCount, String lastMessage,
                          LocalDateTime lastMessageAt) {
    }
}
