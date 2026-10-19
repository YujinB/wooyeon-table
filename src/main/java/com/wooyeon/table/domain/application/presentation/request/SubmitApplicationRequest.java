package com.wooyeon.table.domain.application.presentation.request;

import com.wooyeon.table.domain.application.application.dto.SubmitApplicationCommand;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.profile.domain.model.Region;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/** 참가 가능한 날짜(회차)와 희망 지역은 복수 선택 (APP-02) */
public record SubmitApplicationRequest(@NotNull TicketType ticketType,
                                       @NotEmpty Set<Region> regions,
                                       @NotEmpty Set<@NotNull Long> slotIds) {

    public SubmitApplicationCommand toCommand() {
        return new SubmitApplicationCommand(ticketType, regions, slotIds);
    }
}
