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
    @Column(nullable = false, length = 20)
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

    /** 참가자 신청 제출 (APP-03): 신청 완료·결제 대기. 결제는 W2-2 모의 결제에서 */
    public static Application submit(Long userId, TicketType ticketType, Collection<Region> regions,
                                     Collection<Long> preferredSlotIds) {
        return new Application(userId, ticketType, regions, preferredSlotIds, PaymentStatus.PENDING);
    }

    public boolean isActive() {
        return applicationStatus != ApplicationStatus.CANCELED;
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

    /**
     * 매칭 실패 후 추천 일정으로 이동 (STA-06): 결제는 그대로 두고 새 회차의 매칭 대기로 바꾼다.
     * 대체 안내 때 환불 가능으로 바뀐 결제 상태는 다시 결제 완료로 돌린다 (새 결제 없음)
     */
    public void moveTo(Long slotId, Collection<Region> newRegions) {
        requireAlternativeOffered();
        this.preferredSlotIds.clear();
        this.preferredSlotIds.add(slotId);
        if (newRegions != null && !newRegions.isEmpty()) {
            this.regions.clear();
            this.regions.addAll(newRegions);
        }
        this.matchStatus = MatchStatus.WAITING;
        restorePaid();
    }

    /** 이용권 보관 (STA-05): 이 신청은 매칭 없이 끝나고 이용권은 다음 신청에 쓴다 */
    public void keepTicket() {
        requireAlternativeOffered();
        this.matchStatus = MatchStatus.NO_MATCH;
        restorePaid();
    }

    /** 환불 선택 (PAY-03): 매칭 없이 끝내고, 환불 상태 전환은 결제 쪽이 이어서 한다 */
    public void closeForRefund() {
        requireAlternativeOffered();
        this.matchStatus = MatchStatus.NO_MATCH;
    }

    private void restorePaid() {
        if (paymentStatus == PaymentStatus.REFUND_AVAILABLE) {
            this.paymentStatus = PaymentStatus.PAID;
        }
    }

    private void requireAlternativeOffered() {
        if (matchStatus != MatchStatus.ALTERNATIVE_OFFERED) {
            throw new BusinessException(ErrorCode.RESOLUTION_NOT_AVAILABLE, id + " (" + matchStatus + ")");
        }
    }

    /** 모의 결제 성공 (PAY-01). 결제 대기·실패 상태에서만 */
    public void markPaid() {
        requirePayable();
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void markPaymentFailed() {
        requirePayable();
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public boolean isPayable() {
        return applicationStatus == ApplicationStatus.SUBMITTED
                && (paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.FAILED);
    }

    /** 매칭 확정 전 신청 취소. 결제했다면 환불은 호출하는 쪽이 이어서 처리한다 */
    public void cancel() {
        if (applicationStatus != ApplicationStatus.SUBMITTED || matchStatus != MatchStatus.WAITING) {
            throw new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED,
                    "매칭 대기 중인 신청만 취소할 수 있습니다 (" + applicationStatus + "/" + matchStatus + ")");
        }
        this.applicationStatus = ApplicationStatus.CANCELED;
    }

    /** 환불 요청 → 처리 중 → 완료/실패 (PAY-03 모의 상태 전환) */
    public void requestRefund() {
        transitPayment(Set.of(PaymentStatus.PAID, PaymentStatus.REFUND_AVAILABLE, PaymentStatus.REFUND_FAILED),
                PaymentStatus.REFUND_REQUESTED);
    }

    public void startRefund() {
        transitPayment(Set.of(PaymentStatus.REFUND_REQUESTED), PaymentStatus.REFUND_PROCESSING);
    }

    public void completeRefund() {
        transitPayment(Set.of(PaymentStatus.REFUND_PROCESSING), PaymentStatus.REFUNDED);
    }

    public void failRefund() {
        transitPayment(Set.of(PaymentStatus.REFUND_PROCESSING), PaymentStatus.REFUND_FAILED);
    }

    private void transitPayment(Set<PaymentStatus> from, PaymentStatus to) {
        if (!from.contains(paymentStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "결제 상태 " + paymentStatus + " → " + to + " 불가 (신청 " + id + ")");
        }
        this.paymentStatus = to;
    }

    private void requirePayable() {
        if (!isPayable()) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED,
                    "결제할 수 없는 상태 (" + applicationStatus + "/" + paymentStatus + ")");
        }
    }

    private void requireWaiting() {
        if (matchStatus != MatchStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "매칭 대기 상태가 아닌 신청: " + id + " (" + matchStatus + ")");
        }
    }
}
