package com.wooyeon.table.domain.matching.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 테이블의 한 자리 = 참가자 한 명 */
@Getter
@Entity
@Table(name = "table_seats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TableSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id")
    private DiningTable table;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlacementReason placement;

    private int relocationCount;

    /** 원하는 상대 성향을 다른 멤버 중 누군가 충족하는지 */
    private boolean preferenceMet;

    TableSeat(DiningTable table, TableMember member, Long userId) {
        this.table = table;
        this.applicationId = member.candidateId();
        this.userId = userId;
        this.placement = member.placement();
        this.relocationCount = member.relocationCount();
        this.preferenceMet = member.preferenceMet();
    }
}
