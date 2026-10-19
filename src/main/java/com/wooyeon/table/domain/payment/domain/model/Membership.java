package com.wooyeon.table.domain.payment.domain.model;

import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.global.common.BaseTimeEntity;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버십 이용 기간 (주최 측 Q&A: 기간 안 횟수 무제한, 매칭 실패 시 실패 횟수만큼 기한 연장).
 * 멤버십 이용권으로 결제하면 결제일부터 기간이 시작된다
 */
@Getter
@Entity
@Table(name = "memberships", indexes = @Index(name = "idx_memberships_user", columnList = "userId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketType ticketType;

    /** 이 멤버십을 산 신청 */
    @Column(nullable = false)
    private Long sourceApplicationId;

    @Column(nullable = false)
    private LocalDate startsOn;

    /** 이 날짜까지 열리는 회차에 쓸 수 있다 */
    @Column(nullable = false)
    private LocalDate expiresOn;

    /** 매칭 실패로 늘어난 횟수 (W2-3) */
    @Column(nullable = false)
    private int extendedCount;

    @Column(nullable = false)
    private boolean canceled;

    private Membership(Long userId, TicketType ticketType, Long sourceApplicationId, LocalDate startsOn) {
        if (!ticketType.isMembership()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "멤버십 이용권이 아님: " + ticketType);
        }
        this.userId = userId;
        this.ticketType = ticketType;
        this.sourceApplicationId = sourceApplicationId;
        this.startsOn = startsOn;
        this.expiresOn = startsOn.plusMonths(ticketType.getMonths());
    }

    public static Membership start(Long userId, TicketType ticketType, Long sourceApplicationId, LocalDate today) {
        return new Membership(userId, ticketType, sourceApplicationId, today);
    }

    public boolean covers(LocalDate eventDate) {
        return !canceled && !eventDate.isBefore(startsOn) && !eventDate.isAfter(expiresOn);
    }

    /** 매칭 실패 1회당 늘리는 기간 (주최 측 Q&A "실패 횟수만큼 기한 연장" → 1회 = 목요일 모임 한 번 = 7일) */
    public static final int EXTENSION_DAYS_PER_FAILURE = 7;

    public void extendForFailure() {
        this.expiresOn = expiresOn.plusDays(EXTENSION_DAYS_PER_FAILURE);
        this.extendedCount++;
    }

    /** 멤버십을 산 신청을 취소·환불하면 멤버십도 끝난다 */
    public void cancel() {
        this.canceled = true;
    }
}
