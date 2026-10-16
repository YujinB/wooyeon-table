package com.wooyeon.table.domain.application.domain.model;

import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.global.common.BaseTimeEntity;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참가 신청. 참가 가능한 회차(날짜)와 희망 지역을 복수로 고른다.
 * 신청·결제·매칭 상태는 PRD 12절대로 각각 분리해 저장한다.
 */
@Getter
@Entity
@Table(name = "applications", indexes = @Index(name = "idx_applications_user", columnList = "userId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketType ticketType;

    @ElementCollection
    @CollectionTable(name = "application_regions", joinColumns = @JoinColumn(name = "application_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 30)
    private Set<Region> regions = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "application_slots", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "slot_id")
    private Set<Long> preferredSlotIds = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus applicationStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MatchStatus matchStatus;

    /** 확정된 회차 */
    private Long confirmedSlotId;

    private Application(Long userId, TicketType ticketType, Collection<Region> regions,
                        Collection<Long> preferredSlotIds, PaymentStatus paymentStatus) {
        if (regions.isEmpty() || preferredSlotIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "희망 지역과 참가 가능 날짜를 하나 이상 골라야 합니다");
        }
        this.userId = userId;
        this.ticketType = ticketType;
        this.regions = new HashSet<>(regions);
        this.preferredSlotIds = new HashSet<>(preferredSlotIds);
        this.applicationStatus = ApplicationStatus.SUBMITTED;
        this.paymentStatus = paymentStatus;
        this.matchStatus = MatchStatus.WAITING;
    }

    /** 기존 신청자 데이터 적재용: 신청 완료·모의 결제 완료 상태로 만든다 */
    public static Application paidSubmission(Long userId, TicketType ticketType, Collection<Region> regions,
                                             Collection<Long> preferredSlotIds) {
        return new Application(userId, ticketType, regions, preferredSlotIds, PaymentStatus.PAID);
    }

    public void confirmMatch(Long slotId) {
        requireWaiting();
        if (!preferredSlotIds.contains(slotId)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "신청하지 않은 회차에 확정할 수 없습니다");
        }
        this.matchStatus = MatchStatus.CONFIRMED;
        this.confirmedSlotId = slotId;
    }

    /** 고른 날짜 모두에서 매칭 실패 → 대체 일정·보관·환불 선택 대기 (MAT-11) */
    public void offerAlternative() {
        requireWaiting();
        this.matchStatus = MatchStatus.ALTERNATIVE_OFFERED;
        if (paymentStatus == PaymentStatus.PAID) {
            this.paymentStatus = PaymentStatus.REFUND_AVAILABLE;
        }
    }

    private void requireWaiting() {
        if (matchStatus != MatchStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "매칭 대기 상태가 아닌 신청: " + id + " (" + matchStatus + ")");
        }
    }
}
