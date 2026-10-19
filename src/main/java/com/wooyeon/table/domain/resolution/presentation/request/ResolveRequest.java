package com.wooyeon.table.domain.resolution.presentation.request;

import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.resolution.domain.model.ResolutionChoice;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/** 일정 이동이면 slotId 필수, regions를 비우면 원래 고른 지역 그대로 */
public record ResolveRequest(@NotNull ResolutionChoice choice, Long slotId, Set<Region> regions) {
}
