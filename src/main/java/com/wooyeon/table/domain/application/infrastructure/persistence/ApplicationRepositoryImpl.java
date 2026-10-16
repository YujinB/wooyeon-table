package com.wooyeon.table.domain.application.infrastructure.persistence;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.ApplicationStatus;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.application.domain.repository.ApplicationRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ApplicationRepositoryImpl implements ApplicationRepository {

    private final ApplicationJpaRepository jpaRepository;

    @Override
    public Application save(Application application) {
        return jpaRepository.save(application);
    }

    @Override
    public Optional<Application> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Application> findAllByIdIn(Collection<Long> ids) {
        return jpaRepository.findAllByIdIn(ids);
    }

    @Override
    public List<Application> findMatchCandidates(Long slotId) {
        return jpaRepository.findBySlotAndStatuses(slotId, ApplicationStatus.SUBMITTED, PaymentStatus.PAID,
                MatchStatus.WAITING);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }
}
