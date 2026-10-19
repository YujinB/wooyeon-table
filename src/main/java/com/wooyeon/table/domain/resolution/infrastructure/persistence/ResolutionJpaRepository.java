package com.wooyeon.table.domain.resolution.infrastructure.persistence;

import com.wooyeon.table.domain.resolution.domain.model.Resolution;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolutionJpaRepository extends JpaRepository<Resolution, Long> {

    Optional<Resolution> findByApplicationId(Long applicationId);
}
