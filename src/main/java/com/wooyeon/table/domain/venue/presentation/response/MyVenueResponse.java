package com.wooyeon.table.domain.venue.presentation.response;

import com.wooyeon.table.domain.venue.application.dto.VenueAssignmentView;
import com.wooyeon.table.domain.venue.application.dto.VenueInfo;
import java.time.LocalDateTime;

/**
 * 참가자용 식당 안내 (INF-03): 식당명·주소·지도 링크·집합 시간·예약명·가격 안내.
 * 아직 배정 전이거나 운영자가 처리 중이면 assigned=false와 안내 문구만
 */
public record MyVenueResponse(boolean assigned, String message, String name, String address, String mapUrl,
                              LocalDateTime gatherAt, String reservationName, String priceGuide) {

    public static MyVenueResponse of(VenueAssignmentView assignment, LocalDateTime gatherAt) {
        VenueInfo venue = assignment.venue();
        return new MyVenueResponse(true, "예약명으로 말씀하시면 자리로 안내해 드려요", venue.name(), venue.address(),
                venue.mapUrl(), gatherAt, assignment.reservationName(), venue.priceGuide());
    }

    public static MyVenueResponse pending() {
        return new MyVenueResponse(false, "식당을 배정하고 있어요. 정해지면 바로 알려드릴게요", null, null, null, null,
                null, null);
    }
}
