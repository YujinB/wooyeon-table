package com.wooyeon.table.domain.venue.domain.model;

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
 * 테이블별 식당 배정 결과 (INF-02). 테이블당 하나(유일 제약)라 배정을 여러 번 돌려도 예약이 중복되지 않는다.
 * FAILED는 같은 지역 식당을 모두 시도해도 배정하지 못한 운영자 예외
 */
@Getter
@Entity
@Table(name = "venue_assignments", indexes = @Index(name = "idx_venue_assignments_slot", columnList = "slotId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueAssignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long tableId;

    @Column(nullable = false)
    private Long slotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VenueAssignmentStatus status;

    private Long venueId;

    /** 식당에 전달한 예약명 */
    @Column(length = 50)
    private String reservationName;

    /** 모의 예약 번호 */
    @Column(length = 60)
    private String bookingReference;

    /** 이번 배정에서 시도한 식당 수 (재탐색 횟수 확인용) */
    @Column(nullable = false)
    private int attemptedVenues;

    @Column(length = 300)
    private String failureReason;

    private LocalDateTime decidedAt;

    private VenueAssignment(Long tableId, Long slotId) {
        this.tableId = tableId;
        this.slotId = slotId;
    }

    public static VenueAssignment pending(Long tableId, Long slotId) {
        return new VenueAssignment(tableId, slotId);
    }

    public void assign(Long venueId, String reservationName, String bookingReference, int attemptedVenues,
                       LocalDateTime at) {
        this.status = VenueAssignmentStatus.ASSIGNED;
        this.venueId = venueId;
        this.reservationName = reservationName;
        this.bookingReference = bookingReference;
        this.attemptedVenues = attemptedVenues;
        this.failureReason = null;
        this.decidedAt = at;
    }

    public void fail(String reason, int attemptedVenues, LocalDateTime at) {
        this.status = VenueAssignmentStatus.FAILED;
        this.venueId = null;
        this.reservationName = null;
        this.bookingReference = null;
        this.attemptedVenues = attemptedVenues;
        this.failureReason = reason;
        this.decidedAt = at;
    }

    public boolean isAssigned() {
        return status == VenueAssignmentStatus.ASSIGNED;
    }
}
