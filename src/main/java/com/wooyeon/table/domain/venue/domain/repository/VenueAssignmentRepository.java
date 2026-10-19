package com.wooyeon.table.domain.venue.domain.repository;

import com.wooyeon.table.domain.venue.domain.model.VenueAssignment;
import com.wooyeon.table.domain.venue.domain.model.VenueAssignmentStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VenueAssignmentRepository {

    VenueAssignment save(VenueAssignment assignment);

    Optional<VenueAssignment> findByTableId(Long tableId);

    List<VenueAssignment> findAllBySlotId(Long slotId);

    List<VenueAssignment> findAllByStatus(VenueAssignmentStatus status);

    /** 그 회차에 식당별로 이미 배정된 테이블 수 */
    Map<Long, Long> countAssignedByVenue(Long slotId);
}
