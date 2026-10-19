package com.wooyeon.table.domain.chat.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 단체방 멤버. 다른 참가자에게는 별칭과 제한된 소개만 보인다 (INF-06) */
@Getter
@Entity
@Table(name = "chat_members", uniqueConstraints = @UniqueConstraint(name = "uk_chat_members_room_user",
        columnNames = {"room_id", "userId"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @Column(nullable = false)
    private int seatNo;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long applicationId;

    /** 예: "참가자 3" */
    @Column(nullable = false, length = 20)
    private String alias;

    /** 예: "20대 후반 · ENFP · 맛집·카페 탐방, 여행 · 가볍고 유머 있는 대화를 좋아함" */
    @Column(nullable = false, length = 300)
    private String intro;

    ChatMember(ChatRoom room, int seatNo, Long userId, Long applicationId, String alias, String intro) {
        this.room = room;
        this.seatNo = seatNo;
        this.userId = userId;
        this.applicationId = applicationId;
        this.alias = alias;
        this.intro = intro;
    }
}
