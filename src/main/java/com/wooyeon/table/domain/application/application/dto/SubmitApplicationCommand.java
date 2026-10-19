package com.wooyeon.table.domain.application.application.dto;

import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.util.Set;

public record SubmitApplicationCommand(TicketType ticketType, Set<Region> regions, Set<Long> slotIds) {
}
