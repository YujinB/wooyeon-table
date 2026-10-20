package com.wooyeon.table.domain.venue.domain.service;

import com.wooyeon.table.domain.venue.domain.model.Venue;
import java.time.LocalDateTime;

/** 식당 예약 포트. 해커톤에서는 모의 구현 (실제 파트너 예약 API가 생기면 구현만 바꾼다) */
public interface VenueBookingGateway {

    BookingResult book(Venue venue, LocalDateTime at, int partySize, String reservationName);

    record BookingResult(boolean success, String reference, String failureReason) {

        public static BookingResult ok(String reference) {
            return new BookingResult(true, reference, null);
        }

        public static BookingResult fail(String reason) {
            return new BookingResult(false, null, reason);
        }
    }
}
