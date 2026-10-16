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

    public boolean isRecruiting() {
        return status == EventSlotStatus.RECRUITING;
    }
}
