package com.wooyeon.table.domain.chat.application.dto;

import com.wooyeon.table.domain.chat.domain.model.ChatMessageKind;
import java.time.LocalDateTime;
import java.util.List;

/** 운영자용 단체방 (생성 안 됐으면 created=false) */
public record AdminChatRoomView(boolean created, Long roomId, String title, LocalDateTime createdAt,
                                List<Member> members, List<Message> messages) {

    public record Member(String alias, Long userId, Long applicationId, String intro) {
    }

    /** visible=false면 예약된 공지 (리마인드 등) */
    public record Message(ChatMessageKind kind, String senderAlias, String content, LocalDateTime visibleAt,
                          boolean visible) {
    }

    public static AdminChatRoomView notCreated() {
        return new AdminChatRoomView(false, null, null, null, List.of(), List.of());
    }
}
