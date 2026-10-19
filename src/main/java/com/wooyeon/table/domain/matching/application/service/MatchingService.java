package com.wooyeon.table.domain.matching.application.service;

import com.wooyeon.table.domain.application.application.dto.MatchCandidateApplication;
import com.wooyeon.table.domain.application.application.service.ApplicationCommandService;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.dto.MatchRunSummary;
import com.wooyeon.table.domain.matching.application.event.MatchFailedEvent;
import com.wooyeon.table.domain.matching.domain.model.Candidate;
import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.matching.domain.model.MatchFailure;
import com.wooyeon.table.domain.matching.domain.model.MatchResult;
import com.wooyeon.table.domain.matching.domain.model.MatchRun;
import com.wooyeon.table.domain.matching.domain.model.MatchedTable;
import com.wooyeon.table.domain.matching.domain.model.MatchingPolicy;
import com.wooyeon.table.domain.matching.domain.model.PairRelations;
import com.wooyeon.table.domain.matching.domain.model.TableMember;
import com.wooyeon.table.domain.matching.domain.model.UnassignedCandidate;
import com.wooyeon.table.domain.matching.domain.repository.DiningTableRepository;
import com.wooyeon.table.domain.matching.domain.repository.MatchFailureRepository;
import com.wooyeon.table.domain.matching.domain.repository.MatchRunRepository;
import com.wooyeon.table.domain.matching.domain.service.MatchingEngine;
import com.wooyeon.table.domain.profile.application.dto.MatchingProfile;
import com.wooyeon.table.domain.profile.application.service.ProfileQueryService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회차 하나의 자동 매칭 실행 (PRD 7.1).
 * 후보 조회 → 엔진 실행 → 테이블 확정 저장 → 신청 상태 전환 → 미배정자 이월 또는 대체 안내까지 한 트랜잭션에서 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    public static final String ALGORITHM_VERSION = "greedy-improve-split-v2";

    private final MatchingEngine matchingEngine;
    private final MatchingPolicy matchingPolicy;
    private final MatchRunRepository matchRunRepository;
    private final DiningTableRepository diningTableRepository;
    private final MatchFailureRepository matchFailureRepository;
    private final EventSlotCommandService eventSlotCommandService;
    private final EventSlotQueryService eventSlotQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final ApplicationCommandService applicationCommandService;
    private final ProfileQueryService profileQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 회차 매칭을 실행한다. 이미 실행한 회차면 다시 돌리지 않고 기존 결과를 돌려준다 (멱등).
     * 회차 행을 잠그고 시작하므로 스케줄러와 "지금 마감 실행"이 겹쳐도 한 번만 실행된다.
     */
    @Transactional
    public MatchRunSummary runForSlot(Long slotId) {
        EventSlotInfo slot = eventSlotCommandService.lockForMatching(slotId);
        Optional<MatchRun> existing = matchRunRepository.findBySlotId(slotId);
        if (existing.isPresent()) {
            return MatchRunSummary.of(existing.get(), true);
        }

        LocalDateTime startedAt = LocalDateTime.now(clock);
        List<MatchCandidateApplication> applications = applicationQueryService.findMatchCandidates(slotId);
        Map<Long, MatchingProfile> profiles = profileQueryService.findMatchingProfiles(
                applications.stream().map(MatchCandidateApplication::userId).toList());
        Map<Long, MatchCandidateApplication> applicationById = new HashMap<>();
        List<Candidate> candidates = new ArrayList<>();
        for (MatchCandidateApplication application : applications) {
            MatchingProfile profile = profiles.get(application.userId());
            if (profile == null) {
                // 데이터 누락은 예외 처리함(MAT-13, #12)으로 보낼 대상. 지금은 후보에서만 뺀다
                log.warn("프로필이 없어 매칭 후보에서 제외: application={}", application.applicationId());
                continue;
            }
            applicationById.put(application.applicationId(), application);
            candidates.add(toCandidate(application, profile, slot));
        }

        // 차단·신고·이전 만남은 피드백 반영(#11)에서 연결한다
        MatchResult result = matchingEngine.match(candidates, PairRelations.none());
        LocalDateTime finishedAt = LocalDateTime.now(clock);

        List<UnassignedCandidate> carriedOver = new ArrayList<>();
        Map<Long, Long> nextSlotByApplication = new HashMap<>();
        List<UnassignedCandidate> alternativeOffered = new ArrayList<>();
        Map<Long, EventSlotInfo> laterSlots = laterRecruitingSlots(slot, applicationById.values());
        for (UnassignedCandidate unassigned : result.unassigned()) {
            Optional<EventSlotInfo> next = nextSlot(applicationById.get(unassigned.candidateId()), laterSlots);
            if (next.isPresent()) {
                carriedOver.add(unassigned);
                nextSlotByApplication.put(unassigned.candidateId(), next.get().id());
            } else {
                alternativeOffered.add(unassigned);
            }
        }

        MatchRun run = matchRunRepository.save(MatchRun.completed(slotId, ALGORITHM_VERSION, startedAt, finishedAt,
                matchingPolicy, result, carriedOver.size(), alternativeOffered.size()));
        saveTables(run, slotId, result, applicationById, finishedAt);
        saveFailures(run, slotId, applicationById, carriedOver, nextSlotByApplication, alternativeOffered);

        applicationCommandService.confirmMatch(confirmedApplicationIds(result), slotId);
        applicationCommandService.offerAlternative(
                alternativeOffered.stream().map(UnassignedCandidate::candidateId).toList());
        if (!alternativeOffered.isEmpty()) {
            eventPublisher.publishEvent(new MatchFailedEvent(slot.eventDate(), alternativeOffered.stream()
                    .map(unassigned -> applicationById.get(unassigned.candidateId()))
                    .map(application -> new MatchFailedEvent.FailedApplication(application.applicationId(),
                            application.userId()))
                    .toList()));
        }
        eventSlotCommandService.markMatched(slotId);

        log.info("회차 {} 매칭 완료: 후보 {}명, 확정 {}명({}개 테이블), 이월 {}명, 대체 안내 {}명", slot.eventDate(),
                candidates.size(), result.assignedCount(), result.tables().size(), carriedOver.size(),
                alternativeOffered.size());
        return MatchRunSummary.of(run, false);
    }

    private static Candidate toCandidate(MatchCandidateApplication application, MatchingProfile profile,
                                         EventSlotInfo slot) {
        return new Candidate(application.applicationId(), profile.ageIn(slot.eventDate().getYear()),
                profile.gender(), profile.mbti(), application.regions(), profile.interests(), profile.traits(),
                profile.preferredTraits());
    }

    /** 후보들이 고른 회차 중 이번 회차보다 늦고 아직 모집 중인 회차 */
    private Map<Long, EventSlotInfo> laterRecruitingSlots(EventSlotInfo current,
                                                         Iterable<MatchCandidateApplication> applications) {
        Set<Long> slotIds = new HashSet<>();
        applications.forEach(application -> slotIds.addAll(application.preferredSlotIds()));
        slotIds.remove(current.id());
        return eventSlotQueryService.findSlots(slotIds).stream()
                .filter(EventSlotInfo::isRecruiting)
                .filter(slot -> slot.eventDate().isAfter(current.eventDate()))
                .collect(Collectors.toMap(EventSlotInfo::id, slot -> slot));
    }

    /** 본인이 고른 다음 날짜 회차 (PRD 7.4 3·4순위) */
    private static Optional<EventSlotInfo> nextSlot(MatchCandidateApplication application,
                                                    Map<Long, EventSlotInfo> laterSlots) {
        return application.preferredSlotIds().stream()
                .map(laterSlots::get)
                .filter(Objects::nonNull)
                .min(Comparator.comparing(EventSlotInfo::eventDate));
    }

    private void saveTables(MatchRun run, Long slotId, MatchResult result,
                            Map<Long, MatchCandidateApplication> applicationById, LocalDateTime confirmedAt) {
        Map<Long, Long> userIdByApplication = applicationById.values().stream()
                .collect(Collectors.toMap(MatchCandidateApplication::applicationId, MatchCandidateApplication::userId));
        List<DiningTable> tables = new ArrayList<>();
        for (MatchedTable matched : result.tables()) {
            tables.add(DiningTable.confirmed(run.getId(), slotId, matched, userIdByApplication, confirmedAt));
        }
        diningTableRepository.saveAll(tables);
    }

    private void saveFailures(MatchRun run, Long slotId, Map<Long, MatchCandidateApplication> applicationById,
                              List<UnassignedCandidate> carriedOver, Map<Long, Long> nextSlotByApplication,
                              List<UnassignedCandidate> alternativeOffered) {
        List<MatchFailure> failures = new ArrayList<>();
        for (UnassignedCandidate unassigned : carriedOver) {
            failures.add(MatchFailure.carriedOver(run.getId(), slotId,
                    applicationById.get(unassigned.candidateId()).userId(), unassigned,
                    nextSlotByApplication.get(unassigned.candidateId())));
        }
        for (UnassignedCandidate unassigned : alternativeOffered) {
            failures.add(MatchFailure.alternativeOffered(run.getId(), slotId,
                    applicationById.get(unassigned.candidateId()).userId(), unassigned));
        }
        matchFailureRepository.saveAll(failures);
    }

    private static List<Long> confirmedApplicationIds(MatchResult result) {
        return result.tables().stream()
                .flatMap(table -> table.members().stream())
                .map(TableMember::candidateId)
                .toList();
    }
}
