package com.wooyeon.table.domain.application.application.dto;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.util.Set;

/** 매칭 후보가 될 신청 */
public record MatchCandidateApplication(Long applicationId, Long userId, Set<Region> regions,
                                        Set<Long> preferredSlotIds) {

    public static MatchCandidateApplication from(Application application) {
        return new MatchCandidateApplication(application.getId(), application.getUserId(),
                Set.copyOf(application.getRegions()), Set.copyOf(application.getPreferredSlotIds()));
    }
}
