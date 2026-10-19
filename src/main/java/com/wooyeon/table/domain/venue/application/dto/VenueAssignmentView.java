package com.wooyeon.table.domain.venue.application.dto;

import com.wooyeon.table.domain.venue.domain.model.VenueAssignment;
import com.wooyeon.table.domain.venue.domain.model.VenueAssignmentStatus;
import java.time.LocalDateTime;

/** 운영자용 테이블 식당 배정 상태. venue는 배정됐을 때만 */
public record VenueAssignmentView(Long tableId, Long slotId, VenueAssignmentStatus status, VenueInfo venue,
                                  String reservationName, String bookingReference, int attemptedVenues,
                                  String failureReason, LocalDateTime decidedAt) {

    public static VenueAssignmentView of(VenueAssignment assignment, VenueInfo venue) {
        return new VenueAssignmentView(assignment.getTableId(), assignment.getSlotId(), assignment.getStatus(),
                venue, assignment.getReservationName(), assignment.getBookingReference(),
                assignment.getAttemptedVenues(), assignment.getFailureReason(), assignment.getDecidedAt());
    }
}
