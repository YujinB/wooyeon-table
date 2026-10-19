package com.wooyeon.table.domain.event.application.dto;

import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record EventSlotInfo(Long id, LocalDate eventDate, LocalTime startTime, LocalDateTime matchAt,
                            EventSlotStatus status) {

    public static EventSlotInfo from(EventSlot slot) {
        return new EventSlotInfo(slot.getId(), slot.getEventDate(), slot.getStartTime(), slot.getMatchAt(),
                slot.getStatus());
    }

    /** 이 시각 이후 취소하면 환불되지 않는다 (PAY-02) */
    public LocalDateTime refundClosesAt() {
        return EventSlot.refundClosesAt(eventDate);
    }

    public boolean isOpenForApplication(LocalDateTime now) {
        return isRecruiting() && now.isBefore(matchAt);
    }

    public boolean isRecruiting() {
        return status == EventSlotStatus.RECRUITING;
    }
}
