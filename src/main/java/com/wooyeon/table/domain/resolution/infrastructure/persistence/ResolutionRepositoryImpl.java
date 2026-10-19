package com.wooyeon.table.domain.resolution.infrastructure.persistence;

import com.wooyeon.table.domain.resolution.domain.model.Resolution;
import com.wooyeon.table.domain.resolution.domain.repository.ResolutionRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResolutionRepositoryImpl implements ResolutionRepository {

    private final ResolutionJpaRepository jpaRepository;

    @Override
    public Resolution save(Resolution resolution) {
        return jpaRepository.saveAndFlush(resolution);
    }

    @Override
    public Optional<Resolution> findByApplicationId(Long applicationId) {
        return jpaRepository.findByApplicationId(applicationId);
    }
}
