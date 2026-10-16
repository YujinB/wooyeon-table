package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.MatchFailure;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchFailureJpaRepository extends JpaRepository<MatchFailure, Long> {

    List<MatchFailure> findAllByMatchRunIdOrderByApplicationId(Long matchRunId);

    Optional<MatchFailure> findFirstByApplicationIdOrderByIdDesc(Long applicationId);
}
