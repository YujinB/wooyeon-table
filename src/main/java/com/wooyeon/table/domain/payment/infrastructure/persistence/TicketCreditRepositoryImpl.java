package com.wooyeon.table.domain.payment.infrastructure.persistence;

import com.wooyeon.table.domain.payment.domain.model.TicketCredit;
import com.wooyeon.table.domain.payment.domain.repository.TicketCreditRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TicketCreditRepositoryImpl implements TicketCreditRepository {

    private final TicketCreditJpaRepository jpaRepository;

    @Override
    public TicketCredit save(TicketCredit credit) {
        return jpaRepository.saveAndFlush(credit);
    }

    @Override
    public Optional<TicketCredit> findFirstUnused(Long userId) {
        return jpaRepository.findFirstByUserIdAndUsedByApplicationIdIsNullOrderByIdAsc(userId);
    }

    @Override
    public List<TicketCredit> findAllUnused(Long userId) {
        return jpaRepository.findAllByUserIdAndUsedByApplicationIdIsNullOrderByIdAsc(userId);
    }
}
