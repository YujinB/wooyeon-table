package com.wooyeon.table.domain.payment.domain.repository;

import com.wooyeon.table.domain.payment.domain.model.PaymentRecord;
import java.util.List;

public interface PaymentRecordRepository {

    PaymentRecord save(PaymentRecord record);

    /** 신청의 결제·환불 내역 (오래된 것부터) */
    List<PaymentRecord> findAllByApplicationId(Long applicationId);
}
