package com.wooyeon.table.domain.safety.infrastructure.persistence;

import com.wooyeon.table.domain.safety.domain.model.ReportStatus;
import com.wooyeon.table.domain.safety.domain.model.SafetyReport;
import com.wooyeon.table.domain.safety.domain.repository.SafetyReportRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SafetyReportRepositoryImpl implements SafetyReportRepository {

    private final SafetyReportJpaRepository jpaRepository;

    @Override
    public SafetyReport save(SafetyReport report) {
        return jpaRepository.save(report);
    }

    @Override
    public Optional<SafetyReport> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<SafetyReport> findAllByStatus(ReportStatus status) {
        return jpaRepository.findAllByStatusOrderByUrgentDescIdAsc(status);
    }

    @Override
    public List<SafetyReport> findAll() {
        return jpaRepository.findAllByOrderByIdDesc();
    }

    @Override
    public List<SafetyReport> findAmong(Collection<Long> userIds) {
        return userIds.isEmpty() ? List.of()
                : jpaRepository.findAllByReporterUserIdInAndReportedUserIdIn(userIds, userIds);
    }
}
