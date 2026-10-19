package com.wooyeon.table.domain.feedback.domain.model;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사람별 선호 (FDB-04). 같은 테이블 멤버에 대해 "다시 만나고 싶음/싫음"을 비공개로 남긴다.
 * 상대에게는 절대 보이지 않고, 다음 매칭에만 쓰인다 (싫음 = 같은 테이블 금지, W4-2)
 */
@Getter
@Entity
@Table(name = "person_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uk_person_preferences",
                columnNames = {"fromUserId", "toUserId", "tableId"}),
        indexes = @Index(name = "idx_person_preferences_to", columnList = "toUserId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonPreference extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromUserId;

    @Column(nullable = false)
    private Long toUserId;

    @Column(nullable = false)
    private Long tableId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreferenceType type;

    private PersonPreference(Long fromUserId, Long toUserId, Long tableId, PreferenceType type) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.tableId = tableId;
        this.type = type;
    }

    public static PersonPreference of(Long fromUserId, Long toUserId, Long tableId, PreferenceType type) {
        return new PersonPreference(fromUserId, toUserId, tableId, type);
    }
}
