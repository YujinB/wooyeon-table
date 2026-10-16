package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.MatchFailure;
import com.wooyeon.table.domain.matching.domain.repository.MatchFailureRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MatchFailureRepositoryImpl implements MatchFailureRepository {

    private final MatchFailureJpaRepository jpaRepository;

    @Override
    public List<MatchFailure> saveAll(List<MatchFailure> failures) {
        return jpaRepository.saveAll(failures);
    }

    @Override
    public List<MatchFailure> findAllByMatchRunId(Long matchRunId) {
        return jpaRepository.findAllByMatchRunIdOrderByApplicationId(matchRunId);
    }

    @Override
    public Optional<MatchFailure> findLatestByApplicationId(Long applicationId) {
        return jpaRepository.findFirstByApplicationIdOrderByIdDesc(applicationId);
    }
}
