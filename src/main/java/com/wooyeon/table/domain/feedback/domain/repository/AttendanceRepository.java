package com.wooyeon.table.domain.feedback.domain.repository;

import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository {

    Attendance save(Attendance attendance);

    Optional<Attendance> findById(Long id);

    Optional<Attendance> findByApplicationId(Long applicationId);

    List<Attendance> findAllBySlotId(Long slotId);

    List<Attendance> findAllByTableId(Long tableId);

    List<Attendance> findAllByStatus(AttendanceStatus status);

    /** 사용자들의 참석 이력 (다음 매칭 반영·이용 제한용) */
    List<Attendance> findAllByUserIdIn(Collection<Long> userIds);
}
