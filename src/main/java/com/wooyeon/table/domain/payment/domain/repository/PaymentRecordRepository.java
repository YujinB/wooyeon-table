package com.wooyeon.table.domain.payment.domain.repository;

import com.wooyeon.table.domain.payment.domain.model.PaymentRecord;
import java.util.List;

public interface PaymentRecordRepository {

    PaymentRecord save(PaymentRecord record);

    /** 신청의 결제·환불 내역 (오래된 것부터) */
    List<PaymentRecord> findAllByApplicationId(Long applicationId);

    /** 최근 결제·환불 내역 (최신부터, 운영자 조회) */
    List<PaymentRecord> findRecent();

    /** 거래 번호가 같은 성공 기록 수 (멤버십으로 0원 처리한 신청 수 세기) */
    long countSucceededByTransactionId(String transactionId);
}
