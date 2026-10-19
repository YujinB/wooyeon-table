package com.wooyeon.table.domain.venue.infrastructure.persistence;

import com.wooyeon.table.domain.venue.domain.model.VenueAssignment;
import com.wooyeon.table.domain.venue.domain.model.VenueAssignmentStatus;
import com.wooyeon.table.domain.venue.domain.repository.VenueAssignmentRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VenueAssignmentRepositoryImpl implements VenueAssignmentRepository {

    private final VenueAssignmentJpaRepository jpaRepository;

    @Override
    public VenueAssignment save(VenueAssignment assignment) {
        return jpaRepository.saveAndFlush(assignment);
    }

    @Override
    public Optional<VenueAssignment> findByTableId(Long tableId) {
        return jpaRepository.findByTableId(tableId);
    }

    @Override
    public List<VenueAssignment> findAllBySlotId(Long slotId) {
        return jpaRepository.findAllBySlotIdOrderByTableIdAsc(slotId);
    }

    @Override
    public List<VenueAssignment> findAllByStatus(VenueAssignmentStatus status) {
        return jpaRepository.findAllByStatusOrderByIdAsc(status);
    }

    @Override
    public Map<Long, Long> countAssignedByVenue(Long slotId) {
        return jpaRepository.findAllBySlotIdAndStatus(slotId, VenueAssignmentStatus.ASSIGNED).stream()
                .collect(Collectors.groupingBy(VenueAssignment::getVenueId, Collectors.counting()));
    }
}
