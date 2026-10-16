package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.MatchRun;
import com.wooyeon.table.domain.matching.domain.repository.MatchRunRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MatchRunRepositoryImpl implements MatchRunRepository {

    private final MatchRunJpaRepository jpaRepository;

    @Override
    public MatchRun save(MatchRun matchRun) {
        return jpaRepository.save(matchRun);
    }

    @Override
    public Optional<MatchRun> findBySlotId(Long slotId) {
        return jpaRepository.findBySlotId(slotId);
    }
}
