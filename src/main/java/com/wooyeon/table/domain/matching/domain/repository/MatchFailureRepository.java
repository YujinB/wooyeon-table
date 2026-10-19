package com.wooyeon.table.domain.matching.domain.repository;

import com.wooyeon.table.domain.matching.domain.model.MatchFailure;
import java.util.List;

public interface MatchFailureRepository {

    List<MatchFailure> saveAll(List<MatchFailure> failures);

    List<MatchFailure> findAllByMatchRunId(Long matchRunId);
}
