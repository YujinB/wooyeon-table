package com.wooyeon.table.domain.payment.domain.model;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제·환불 내역 한 줄 (PAY-01). 신청의 결제 상태는 Application이 갖고, 여기에는 시도한 거래를 그대로 쌓는다.
 * 운영자 결제·환불 내역과 환불 실패 예외 목록의 근거가 된다
 */
@Getter
@Entity
@Table(name = "payment_records", indexes = @Index(name = "idx_payment_records_application",
        columnList = "applicationId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentRecordType type;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false)
    private int amount;

    /** 모의 결제 거래 번호 (실패면 null) */
    @Column(length = 50)
    private String transactionId;

    @Column(length = 200)
    private String failureReason;

    private PaymentRecord(Long applicationId, Long userId, PaymentRecordType type, boolean success, int amount,
                          String transactionId, String failureReason) {
        this.applicationId = applicationId;
        this.userId = userId;
        this.type = type;
        this.success = success;
        this.amount = amount;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
    }

    public static PaymentRecord succeeded(Long applicationId, Long userId, PaymentRecordType type, int amount,
                                          String transactionId) {
        return new PaymentRecord(applicationId, userId, type, true, amount, transactionId, null);
    }

    public static PaymentRecord failed(Long applicationId, Long userId, PaymentRecordType type, int amount,
                                       String failureReason) {
        return new PaymentRecord(applicationId, userId, type, false, amount, null, failureReason);
    }

    /** 실제로 돈이 나간 결제 (환불 대상) */
    public boolean isRefundableCharge() {
        return type == PaymentRecordType.CHARGE && success && amount > 0;
    }
}
