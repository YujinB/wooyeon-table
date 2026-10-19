package com.wooyeon.table.domain.venue.infrastructure.persistence;

import com.wooyeon.table.domain.venue.domain.model.VenueAssignment;
import com.wooyeon.table.domain.venue.domain.model.VenueAssignmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueAssignmentJpaRepository extends JpaRepository<VenueAssignment, Long> {

    Optional<VenueAssignment> findByTableId(Long tableId);

    List<VenueAssignment> findAllBySlotIdOrderByTableIdAsc(Long slotId);

    List<VenueAssignment> findAllByStatusOrderByIdAsc(VenueAssignmentStatus status);

    List<VenueAssignment> findAllBySlotIdAndStatus(Long slotId, VenueAssignmentStatus status);
}
