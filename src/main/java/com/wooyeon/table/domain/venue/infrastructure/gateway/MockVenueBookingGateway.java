package com.wooyeon.table.domain.venue.infrastructure.gateway;

import com.wooyeon.table.domain.venue.domain.model.Venue;
import com.wooyeon.table.domain.venue.domain.service.VenueBookingGateway;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 모의 식당 예약: 자동 예약을 받는 식당은 성공, 아니면 거절 (같은 지역 다음 식당으로 재탐색) */
@Component
public class MockVenueBookingGateway implements VenueBookingGateway {

    @Override
    public BookingResult book(Venue venue, LocalDateTime at, int partySize, String reservationName) {
        if (!venue.isAutoBookable()) {
            return BookingResult.fail(venue.getName() + ": 자동 예약 불가 (전화 예약만)");
        }
        return BookingResult.ok("MOCK-VENUE-" + UUID.randomUUID());
    }
}
