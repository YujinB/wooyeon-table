package com.wooyeon.table.domain.payment.infrastructure.persistence;

import com.wooyeon.table.domain.payment.domain.model.TicketCredit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCreditJpaRepository extends JpaRepository<TicketCredit, Long> {

    Optional<TicketCredit> findFirstByUserIdAndUsedByApplicationIdIsNullOrderByIdAsc(Long userId);
}
