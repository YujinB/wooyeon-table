package com.wooyeon.table.domain.application.presentation.response;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.domain.model.ApplicationStatus;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 내 신청 한 건. 신청·결제·매칭 상태를 따로 보여준다 (PRD 12절, PAY-01) */
public record MyApplicationResponse(Long applicationId, TicketType ticketType, String ticketLabel, int price,
                                    List<String> regions, List<LocalDate> eventDates, LocalDate confirmedEventDate,
                                    ApplicationStatus applicationStatus, PaymentStatus paymentStatus,
                                    MatchStatus matchStatus, LocalDateTime submittedAt) {

    public static MyApplicationResponse of(ApplicationInfo application, Map<Long, EventSlotInfo> slots) {
        List<LocalDate> dates = application.preferredSlotIds().stream()
                .map(slots::get)
                .map(EventSlotInfo::eventDate)
                .sorted()
                .toList();
        LocalDate confirmed = application.confirmedSlotId() == null
                ? null : slots.get(application.confirmedSlotId()).eventDate();
        List<String> regions = application.regions().stream()
                .sorted(Comparator.naturalOrder())
                .map(Region::getLabel)
                .toList();
        return new MyApplicationResponse(application.id(), application.ticketType(),
                application.ticketType().getLabel(), application.ticketType().getPrice(), regions, dates, confirmed,
                application.applicationStatus(), application.paymentStatus(), application.matchStatus(),
                application.submittedAt());
    }
}
