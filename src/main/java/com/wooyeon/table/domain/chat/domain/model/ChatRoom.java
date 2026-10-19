package com.wooyeon.table.domain.chat.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테이블 단체방 (INF-04). 테이블당 하나(유일 제약)라 매칭을 다시 돌려도 중복 단체방이 생기지 않는다 (PRD 16절).
 * 모임이 끝날 때까지 나갈 수 없다 (PRD 15절)
 */
@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long tableId;

    @Column(nullable = false)
    private Long slotId;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, length = 50)
    private String title;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seatNo ASC")
    private List<ChatMember> members = new ArrayList<>();

    private ChatRoom(Long tableId, Long slotId, LocalDate eventDate, String title) {
        this.tableId = tableId;
        this.slotId = slotId;
        this.eventDate = eventDate;
        this.title = title;
    }

    public static ChatRoom open(Long tableId, Long slotId, LocalDate eventDate, String title) {
        return new ChatRoom(tableId, slotId, eventDate, title);
    }

    /** 참가자 전원 자동 초대. 별칭·소개만 저장하고 실명·연락처는 두지 않는다 (INF-06) */
    public void invite(Long userId, Long applicationId, String alias, String intro) {
        members.add(new ChatMember(this, members.size() + 1, userId, applicationId, alias, intro));
    }

    public Optional<ChatMember> memberOf(Long userId) {
        return members.stream().filter(member -> member.getUserId().equals(userId)).findFirst();
    }

    public boolean hasMember(Long userId) {
        return memberOf(userId).isPresent();
    }
}
