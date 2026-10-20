package com.wooyeon.table.domain.feedback.infrastructure.persistence;

import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AttendanceRepositoryImpl implements AttendanceRepository {

    private final AttendanceJpaRepository jpaRepository;

    @Override
    public Attendance save(Attendance attendance) {
        return jpaRepository.save(attendance);
    }

    @Override
    public Optional<Attendance> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Attendance> findByApplicationId(Long applicationId) {
        return jpaRepository.findByApplicationId(applicationId);
    }

    @Override
    public List<Attendance> findAllBySlotId(Long slotId) {
        return jpaRepository.findAllBySlotIdOrderByTableIdAscIdAsc(slotId);
    }

    @Override
    public List<Attendance> findAllByTableId(Long tableId) {
        return jpaRepository.findAllByTableIdOrderByIdAsc(tableId);
    }

    @Override
    public List<Attendance> findAllByStatus(AttendanceStatus status) {
        return jpaRepository.findAllByStatusOrderByIdAsc(status);
    }

    @Override
    public List<Attendance> findAllByUserIdIn(Collection<Long> userIds) {
        return userIds.isEmpty() ? List.of() : jpaRepository.findAllByUserIdIn(userIds);
    }
}
