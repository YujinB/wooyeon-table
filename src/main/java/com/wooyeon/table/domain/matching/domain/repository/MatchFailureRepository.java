package com.wooyeon.table.domain.matching.domain.repository;

import com.wooyeon.table.domain.matching.domain.model.MatchFailure;
import java.util.List;
import java.util.Optional;

public interface MatchFailureRepository {

    List<MatchFailure> saveAll(List<MatchFailure> failures);

    List<MatchFailure> findAllByMatchRunId(Long matchRunId);

    /** 이 신청의 가장 최근 실패 기록 (이월됐다가 다시 실패하면 여러 건) */
    Optional<MatchFailure> findLatestByApplicationId(Long applicationId);
}
