package com.wooyeon.table.domain.safety.domain.repository;

import com.wooyeon.table.domain.safety.domain.model.ReportStatus;
import com.wooyeon.table.domain.safety.domain.model.SafetyReport;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SafetyReportRepository {

    SafetyReport save(SafetyReport report);

    Optional<SafetyReport> findById(Long id);

    /** 긴급 신고 먼저, 오래된 것부터 */
    List<SafetyReport> findAllByStatus(ReportStatus status);

    List<SafetyReport> findAll();

    /** 두 사람 모두 이 집합에 속한 신고 (매칭 제외 관계) */
    List<SafetyReport> findAmong(Collection<Long> userIds);
}
