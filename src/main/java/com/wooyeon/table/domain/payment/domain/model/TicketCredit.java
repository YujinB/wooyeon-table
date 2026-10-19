package com.wooyeon.table.domain.payment.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 매칭 실패 후 보관한 1회권 (STA-05). 다음 1회권 신청 때 결제 없이 한 번 쓴다 */
@Getter
@Entity
@Table(name = "ticket_credits", indexes = @Index(name = "idx_ticket_credits_user", columnList = "userId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketCredit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 보관한 원래 신청. 한 신청에서 보관권은 하나만 (중복 보관 방지) */
    @Column(nullable = false, unique = true)
    private Long sourceApplicationId;

    @Column(nullable = false)
    private int amount;

    /** 이 보관권으로 결제한 신청 */
    private Long usedByApplicationId;

    private TicketCredit(Long userId, Long sourceApplicationId, int amount) {
        this.userId = userId;
        this.sourceApplicationId = sourceApplicationId;
        this.amount = amount;
    }

    public static TicketCredit keep(Long userId, Long sourceApplicationId, int amount) {
        return new TicketCredit(userId, sourceApplicationId, amount);
    }

    public boolean isUsed() {
        return usedByApplicationId != null;
    }

    public void use(Long applicationId) {
        if (isUsed()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "이미 쓴 보관 이용권: " + id);
        }
        this.usedByApplicationId = applicationId;
    }
}
