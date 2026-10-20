package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.MatchRun;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRunJpaRepository extends JpaRepository<MatchRun, Long> {

    Optional<MatchRun> findBySlotId(Long slotId);
}
