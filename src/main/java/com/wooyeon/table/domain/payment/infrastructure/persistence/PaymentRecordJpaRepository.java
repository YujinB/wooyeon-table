package com.wooyeon.table.domain.payment.infrastructure.persistence;

import com.wooyeon.table.domain.payment.domain.model.PaymentRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordJpaRepository extends JpaRepository<PaymentRecord, Long> {

    List<PaymentRecord> findAllByApplicationIdOrderByIdAsc(Long applicationId);

    List<PaymentRecord> findTop500ByOrderByIdDesc();
}
