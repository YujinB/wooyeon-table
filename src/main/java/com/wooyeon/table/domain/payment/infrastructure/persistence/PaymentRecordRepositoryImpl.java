package com.wooyeon.table.domain.payment.infrastructure.persistence;

import com.wooyeon.table.domain.payment.domain.model.PaymentRecord;
import com.wooyeon.table.domain.payment.domain.repository.PaymentRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRecordRepositoryImpl implements PaymentRecordRepository {

    private final PaymentRecordJpaRepository jpaRepository;

    @Override
    public PaymentRecord save(PaymentRecord record) {
        return jpaRepository.save(record);
    }

    @Override
    public List<PaymentRecord> findAllByApplicationId(Long applicationId) {
        return jpaRepository.findAllByApplicationIdOrderByIdAsc(applicationId);
    }

    @Override
    public List<PaymentRecord> findRecent() {
        return jpaRepository.findTop500ByOrderByIdDesc();
    }

    @Override
    public long countSucceededByTransactionId(String transactionId) {
        return jpaRepository.countByTransactionIdAndSuccessTrue(transactionId);
    }
}
