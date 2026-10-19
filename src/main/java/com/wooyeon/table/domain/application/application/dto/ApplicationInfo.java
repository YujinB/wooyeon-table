package com.wooyeon.table.domain.application.application.dto;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.ApplicationStatus;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.util.Set;

public record ApplicationInfo(Long id, Long userId, TicketType ticketType, Set<Region> regions,
                              Set<Long> preferredSlotIds, ApplicationStatus applicationStatus,
                              PaymentStatus paymentStatus, MatchStatus matchStatus, Long confirmedSlotId) {

    public static ApplicationInfo from(Application application) {
        return new ApplicationInfo(application.getId(), application.getUserId(), application.getTicketType(),
                Set.copyOf(application.getRegions()), Set.copyOf(application.getPreferredSlotIds()),
                application.getApplicationStatus(), application.getPaymentStatus(), application.getMatchStatus(),
                application.getConfirmedSlotId());
    }
}
