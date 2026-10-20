package com.wooyeon.table.domain.matching.domain.repository;

import com.wooyeon.table.domain.matching.domain.model.MatchRun;
import java.util.Optional;

public interface MatchRunRepository {

    MatchRun save(MatchRun matchRun);

    Optional<MatchRun> findBySlotId(Long slotId);
}
