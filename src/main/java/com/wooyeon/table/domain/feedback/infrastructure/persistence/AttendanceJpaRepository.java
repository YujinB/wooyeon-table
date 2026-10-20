package com.wooyeon.table.domain.feedback.infrastructure.persistence;

import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceJpaRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByApplicationId(Long applicationId);

    List<Attendance> findAllBySlotIdOrderByTableIdAscIdAsc(Long slotId);

    List<Attendance> findAllByTableIdOrderByIdAsc(Long tableId);

    List<Attendance> findAllByStatusOrderByIdAsc(AttendanceStatus status);

    List<Attendance> findAllByUserIdIn(Collection<Long> userIds);
}
