package com.wooyeon.table.domain.venue.application.service;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.dto.ConfirmedTable;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.profile.application.service.ProfileQueryService;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.venue.application.dto.SlotVenueAssignmentResult;
import com.wooyeon.table.domain.venue.application.event.VenueAssignedEvent;
import com.wooyeon.table.domain.venue.domain.model.Venue;
import com.wooyeon.table.domain.venue.domain.model.VenueAssignment;
import com.wooyeon.table.domain.venue.domain.repository.VenueAssignmentRepository;
import com.wooyeon.table.domain.venue.domain.repository.VenueRepository;
import com.wooyeon.table.domain.venue.domain.service.VenueBookingGateway;
import com.wooyeon.table.domain.venue.domain.service.VenueBookingGateway.BookingResult;
import com.wooyeon.table.domain.venue.domain.service.VenueSelector;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 확정 테이블에 파트너 식당 자동 배정 (INF-02). 같은 지역 후보를 순위대로 예약 시도하고(재탐색),
 * 모두 실패한 테이블만 FAILED로 남겨 운영자 예외로 넘긴다. 이미 배정된 테이블은 건너뛴다 (멱등)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VenueAssignmentService {

    private static final DateTimeFormatter RESERVATION_DATE = DateTimeFormatter.ofPattern("MMdd");

    private final MatchingQueryService matchingQueryService;
    private final EventSlotQueryService eventSlotQueryService;
    private final ProfileQueryService profileQueryService;
    private final VenueRepository venueRepository;
    private final VenueAssignmentRepository venueAssignmentRepository;
    private final VenueBookingGateway venueBookingGateway;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 회차의 확정 테이블 중 아직 배정되지 않은 테이블에 식당을 배정한다 (테이블 번호 순) */
    public SlotVenueAssignmentResult assignSlot(Long slotId) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(slotId);
        int assigned = 0;
        List<Long> failedTableIds = new ArrayList<>();
        for (ConfirmedTable table : matchingQueryService.findConfirmedTables(slotId)) {
            VenueAssignment result = assign(table, slot);
            if (result.isAssigned()) {
                assigned++;
            } else {
                failedTableIds.add(table.tableId());
            }
        }
        log.info("회차 {} 식당 배정: 배정 {} · 실패 {}", slot.eventDate(), assigned, failedTableIds.size());
        return new SlotVenueAssignmentResult(slotId, assigned, failedTableIds);
    }

    /** 운영자 재시도 (식당 재고를 늘리거나 다시 연 뒤) */
    public VenueAssignment retryTable(Long tableId) {
        ConfirmedTable table = matchingQueryService.getConfirmedTable(tableId);
        VenueAssignment result = assign(table, eventSlotQueryService.getSlot(table.slotId()));
        if (result.isAssigned()) {
            eventPublisher.publishEvent(new VenueAssignedEvent(tableId));
        }
        return result;
    }

    private VenueAssignment assign(ConfirmedTable table, EventSlotInfo slot) {
        VenueAssignment assignment = venueAssignmentRepository.findByTableId(table.tableId())
                .orElseGet(() -> VenueAssignment.pending(table.tableId(), table.slotId()));
        if (assignment.isAssigned()) {
            return assignment;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Collection<Set<MealBudget>> budgets = profileQueryService.findMealBudgets(table.userIds()).values();
        Map<Long, Long> used = venueAssignmentRepository.countAssignedByVenue(table.slotId());
        List<Venue> candidates = VenueSelector.rank(venueRepository.findActiveByRegion(table.region()), used,
                budgets);
        String reservationName = "우연한식탁 " + slot.eventDate().format(RESERVATION_DATE) + "-" + table.tableNo();

        List<String> rejections = new ArrayList<>();
        for (Venue venue : candidates) {
            BookingResult booking = venueBookingGateway.book(venue, slot.startsAt(), table.size(), reservationName);
            if (booking.success()) {
                assignment.assign(venue.getId(), reservationName, booking.reference(), rejections.size() + 1, now);
                return venueAssignmentRepository.save(assignment);
            }
            rejections.add(booking.failureReason());
        }
        String reason = candidates.isEmpty()
                ? table.region().getLabel() + "에 이 회차 남은 자리가 있는 파트너 식당이 없습니다"
                : "같은 지역 식당 " + candidates.size() + "곳 모두 예약 실패: " + String.join(" / ", rejections);
        assignment.fail(reason, candidates.size(), now);
        log.warn("테이블 {} 식당 배정 실패 → 운영자 예외: {}", table.tableId(), reason);
        return venueAssignmentRepository.save(assignment);
    }
}
