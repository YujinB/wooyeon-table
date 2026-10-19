package com.wooyeon.table.domain.venue.application.service;

import com.wooyeon.table.domain.venue.application.dto.VenueAssignmentView;
import com.wooyeon.table.domain.venue.application.dto.VenueInfo;
import com.wooyeon.table.domain.venue.domain.model.VenueAssignment;
import com.wooyeon.table.domain.venue.domain.model.VenueAssignmentStatus;
import com.wooyeon.table.domain.venue.domain.repository.VenueAssignmentRepository;
import com.wooyeon.table.domain.venue.domain.repository.VenueRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueQueryService {

    private final VenueRepository venueRepository;
    private final VenueAssignmentRepository venueAssignmentRepository;

    /** 식당 재고 목록 (운영자) */
    public List<VenueInfo> findVenues() {
        return venueRepository.findAll().stream().map(VenueInfo::from).toList();
    }

    public Optional<VenueAssignmentView> findByTable(Long tableId) {
        return venueAssignmentRepository.findByTableId(tableId).map(this::view);
    }

    public List<VenueAssignmentView> findBySlot(Long slotId) {
        return views(venueAssignmentRepository.findAllBySlotId(slotId));
    }

    /** 자동 배정에 실패해 운영자가 처리해야 하는 테이블 (INF-02 예외) */
    public List<VenueAssignmentView> findFailures() {
        return views(venueAssignmentRepository.findAllByStatus(VenueAssignmentStatus.FAILED));
    }

    private List<VenueAssignmentView> views(List<VenueAssignment> assignments) {
        Map<Long, VenueInfo> venues = venueRepository.findAllByIdIn(assignments.stream()
                        .map(VenueAssignment::getVenueId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())).stream()
                .map(VenueInfo::from)
                .collect(Collectors.toMap(VenueInfo::venueId, Function.identity()));
        return assignments.stream()
                .map(assignment -> VenueAssignmentView.of(assignment, venues.get(assignment.getVenueId())))
                .toList();
    }

    private VenueAssignmentView view(VenueAssignment assignment) {
        VenueInfo venue = assignment.getVenueId() == null
                ? null
                : venueRepository.findById(assignment.getVenueId()).map(VenueInfo::from).orElse(null);
        return VenueAssignmentView.of(assignment, venue);
    }
}
