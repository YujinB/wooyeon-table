package com.wooyeon.table.domain.matching.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 한 회차 실행에서 테이블에 들지 못한 신청과 그 처리 결과 (MAT-11·12) */
@Getter
@Entity
@Table(name = "match_failures", indexes = @Index(name = "idx_match_failures_application", columnList = "applicationId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchFailure extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long matchRunId;

    @Column(nullable = false)
    private Long slotId;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UnassignedReason reason;

    private int relocationCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FailureOutcome outcome;

    /** 이월된 다음 회차 (이월일 때만) */
    private Long nextSlotId;

    private MatchFailure(Long matchRunId, Long slotId, Long applicationId, Long userId, UnassignedCandidate unassigned,
                         FailureOutcome outcome, Long nextSlotId) {
        this.matchRunId = matchRunId;
        this.slotId = slotId;
        this.applicationId = applicationId;
        this.userId = userId;
        this.reason = unassigned.reason();
        this.relocationCount = unassigned.relocationCount();
        this.outcome = outcome;
        this.nextSlotId = nextSlotId;
    }

    public static MatchFailure carriedOver(Long matchRunId, Long slotId, Long userId, UnassignedCandidate unassigned,
                                           Long nextSlotId) {
        return new MatchFailure(matchRunId, slotId, unassigned.candidateId(), userId, unassigned,
                FailureOutcome.CARRIED_OVER, nextSlotId);
    }

    public static MatchFailure alternativeOffered(Long matchRunId, Long slotId, Long userId,
                                                  UnassignedCandidate unassigned) {
        return new MatchFailure(matchRunId, slotId, unassigned.candidateId(), userId, unassigned,
                FailureOutcome.ALTERNATIVE_OFFERED, null);
    }
}
