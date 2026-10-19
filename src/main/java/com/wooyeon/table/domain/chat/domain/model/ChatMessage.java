package com.wooyeon.table.domain.chat.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 단체방 메시지. 시스템 메시지는 visibleAt부터 보인다 (리마인드처럼 미리 만들어 두는 공지).
 * 시스템 메시지는 dedupKey(방+종류) 유일 제약으로 한 번만 게시된다
 */
@Getter
@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_chat_messages_room", columnList = "roomId, visibleAt"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageKind kind;

    /** 멤버 메시지면 보낸 멤버의 별칭, 시스템이면 null */
    @Column(length = 20)
    private String senderAlias;

    private Long senderUserId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime visibleAt;

    @Column(unique = true, length = 80)
    private String dedupKey;

    private ChatMessage(Long roomId, ChatMessageKind kind, String senderAlias, Long senderUserId, String content,
                        LocalDateTime visibleAt, String dedupKey) {
        this.roomId = roomId;
        this.kind = kind;
        this.senderAlias = senderAlias;
        this.senderUserId = senderUserId;
        this.content = content;
        this.visibleAt = visibleAt;
        this.dedupKey = dedupKey;
    }

    public static ChatMessage system(Long roomId, ChatMessageKind kind, String content, LocalDateTime visibleAt) {
        return new ChatMessage(roomId, kind, null, null, content, visibleAt, roomId + ":" + kind);
    }

    public static ChatMessage fromMember(Long roomId, ChatMember member, String content, LocalDateTime at) {
        return new ChatMessage(roomId, ChatMessageKind.CHAT, member.getAlias(), member.getUserId(), content, at,
                null);
    }

    public boolean isSystem() {
        return kind != ChatMessageKind.CHAT;
    }
}
