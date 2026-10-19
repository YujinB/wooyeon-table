package com.wooyeon.table.domain.resolution.application.dto;

import com.wooyeon.table.domain.matching.application.dto.MyMatchResult.MyFailure;
import com.wooyeon.table.domain.resolution.domain.model.ResolutionChoice;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 매칭 실패 안내 (STA-04): 실패 사유, 옮겨 갈 수 있는 회차와 지역별 대기 인원, 고를 수 있는 해결 방식
 */
public record Alternatives(Long applicationId, MyFailure failure, List<AlternativeSlot> slots,
                           List<ChoiceOption> choices) {

    /** recommendedRegions: 대기 인원이 많은 지역부터 (테이블이 만들어질 가능성이 높은 순) */
    public record AlternativeSlot(Long slotId, LocalDate eventDate, LocalDateTime eventStartsAt,
                                  LocalDateTime applyDeadline, List<RegionDemand> recommendedRegions) {
    }

    /** mine = 원래 고른 지역 */
    public record RegionDemand(String code, String label, long waitingCount, boolean mine) {
    }

    public record ChoiceOption(ResolutionChoice choice, String label, String description, boolean available,
                               String unavailableReason) {
    }
}
