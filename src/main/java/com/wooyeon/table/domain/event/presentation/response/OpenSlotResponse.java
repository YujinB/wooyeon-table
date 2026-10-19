package com.wooyeon.table.domain.event.presentation.response;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * 참가자 회차 목록 한 줄 (APP-01). 회차는 날짜 단위라 4개 지역 모두 신청할 수 있다.
 * refundClosesAt 이후에는 환불이 제한된다 (PAY-02)
 */
public record OpenSlotResponse(Long slotId, LocalDate eventDate, LocalTime startTime, LocalDateTime applyDeadline,
                               LocalDateTime refundClosesAt, List<String> regions) {

    private static final List<String> REGIONS = Arrays.stream(Region.values()).map(Region::getLabel).toList();

    public static OpenSlotResponse from(EventSlotInfo slot) {
        return new OpenSlotResponse(slot.id(), slot.eventDate(), slot.startTime(), slot.matchAt(),
                EventSlot.refundClosesAt(slot.eventDate()), REGIONS);
    }
}
