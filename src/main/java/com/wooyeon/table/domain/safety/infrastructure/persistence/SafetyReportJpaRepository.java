package com.wooyeon.table.domain.safety.infrastructure.persistence;

import com.wooyeon.table.domain.safety.domain.model.ReportStatus;
import com.wooyeon.table.domain.safety.domain.model.SafetyReport;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyReportJpaRepository extends JpaRepository<SafetyReport, Long> {

    List<SafetyReport> findAllByStatusOrderByUrgentDescIdAsc(ReportStatus status);

    List<SafetyReport> findAllByOrderByIdDesc();

    List<SafetyReport> findAllByReporterUserIdInAndReportedUserIdIn(Collection<Long> reporters,
                                                                   Collection<Long> reported);
}
