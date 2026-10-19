package com.wooyeon.table.domain.matching.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.dto.FailureDetail;
import com.wooyeon.table.domain.matching.application.dto.MatchRunSummary;
import com.wooyeon.table.domain.matching.application.dto.MyMatchResult;
import com.wooyeon.table.domain.matching.application.dto.MyMatchResult.MyFailure;
import com.wooyeon.table.domain.matching.application.dto.MyMatchResult.MyTable;
import com.wooyeon.table.domain.matching.application.dto.ScoreBreakdown;
import com.wooyeon.table.domain.matching.application.dto.SeatDetail;
import com.wooyeon.table.domain.matching.application.dto.SlotMatchingResult;
import com.wooyeon.table.domain.matching.application.dto.TableDetail;
import com.wooyeon.table.domain.matching.application.dto.TableSummary;
import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.matching.domain.model.MatchFailure;
import com.wooyeon.table.domain.matching.domain.model.MatchRun;
import com.wooyeon.table.domain.matching.domain.model.PlacementReason;
import com.wooyeon.table.domain.matching.domain.model.TableSeat;
import com.wooyeon.table.domain.matching.domain.repository.DiningTableRepository;
import com.wooyeon.table.domain.matching.domain.repository.MatchFailureRepository;
import com.wooyeon.table.domain.matching.domain.repository.MatchRunRepository;
import com.wooyeon.table.domain.profile.application.dto.MatchingProfile;
import com.wooyeon.table.domain.profile.application.service.ProfileQueryService;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.user.application.service.UserQueryService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 매칭 결과·사유 조회 (MAT-12). 이름·프로필·회차는 각 도메인의 QueryService로 가져온다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingQueryService {

    private final MatchRunRepository matchRunRepository;
    private final DiningTableRepository diningTableRepository;
    private final MatchFailureRepository matchFailureRepository;
    private final EventSlotQueryService eventSlotQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final UserQueryService userQueryService;
    private final ProfileQueryService profileQueryService;

    /** 운영자용 회차 결과: 실행 요약 + 테이블 목록 + 미배정자 사유·처리 결과 */
    public SlotMatchingResult getSlotResult(Long slotId) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(slotId);
        Optional<MatchRun> run = matchRunRepository.findBySlotId(slotId);
        List<TableSummary> tables = diningTableRepository.findAllBySlotId(slotId).stream()
                .map(TableSummary::from)
                .toList();
        List<FailureDetail> failures = run
                .map(r -> failureDetails(matchFailureRepository.findAllByMatchRunId(r.getId())))
                .orElse(List.of());
        return new SlotMatchingResult(slot.id(), slot.eventDate(), slot.matchAt(), slot.status(),
                run.map(r -> MatchRunSummary.of(r, true)).orElse(null),
                run.map(MatchRun::getPolicySnapshot).orElse(null), tables, failures);
    }

    /** 운영자용 테이블 상세: 구성·점수 항목별 내역·좌석별 배치 사유 */
    public TableDetail getTableDetail(Long tableId) {
        DiningTable table = diningTableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DINING_TABLE_NOT_FOUND, String.valueOf(tableId)));
        EventSlotInfo slot = eventSlotQueryService.getSlot(table.getSlotId());
        List<Long> userIds = table.getSeats().stream().map(TableSeat::getUserId).toList();
        Map<Long, String> names = userQueryService.findNames(userIds);
        Map<Long, MatchingProfile> profiles = profileQueryService.findMatchingProfiles(userIds);
        List<SeatDetail> seats = table.getSeats().stream()
                .map(seat -> seatDetail(seat, names.get(seat.getUserId()), profiles.get(seat.getUserId()), slot))
                .toList();
        return new TableDetail(table.getId(), table.getSlotId(), slot.eventDate(), slot.startTime(),
                table.getTableNo(), table.getRegion(), table.getRegion().getLabel(), table.getStatus(),
                table.getConfirmedAt(), table.size(), table.getMinAge(), table.getMaxAge(), table.getMaleCount(),
                table.getFemaleCount(), split(table.getMbtiComposition()), interestLabels(table.getCommonInterests()),
                table.getPreviouslyMetPairs(), ScoreBreakdown.from(table), seats);
    }

    /** 참가자용 내 매칭 결과: 확정이면 테이블 요약과 "왜 이 테이블인지", 실패면 사유와 다음 단계 */
    public MyMatchResult getMyResult(Long applicationId) {
        ApplicationInfo application = applicationQueryService.getApplication(applicationId);
        if (application.matchStatus() == MatchStatus.CONFIRMED) {
            DiningTable table = diningTableRepository.findBySeatApplicationId(applicationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DINING_TABLE_NOT_FOUND,
                            "application=" + applicationId));
            return new MyMatchResult(applicationId, application.matchStatus(), "매칭이 확정됐어요",
                    myTable(table, applicationId, application.userId()), null);
        }
        Optional<MyFailure> failure = matchFailureRepository.findLatestByApplicationId(applicationId)
                .map(this::myFailure);
        String message = switch (application.matchStatus()) {
            case ALTERNATIVE_OFFERED -> "선택한 날짜에 테이블을 만들지 못했어요. 대체 일정 이동·이용권 보관·환불 중에서 골라 주세요";
            case WAITING -> failure.isPresent()
                    ? "이번 회차에는 테이블을 만들지 못해 선택한 다음 날짜 매칭에 자동으로 참여해요"
                    : "매칭 대기 중이에요. 신청 마감 시각에 자동으로 매칭돼요";
            default -> "매칭 상태: " + application.matchStatus();
        };
        return new MyMatchResult(applicationId, application.matchStatus(), message, null, failure.orElse(null));
    }

    private MyTable myTable(DiningTable table, Long applicationId, Long userId) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(table.getSlotId());
        TableSeat mySeat = table.getSeats().stream()
                .filter(seat -> seat.getApplicationId().equals(applicationId))
                .findFirst()
                .orElseThrow();
        Map<Long, MatchingProfile> profiles = profileQueryService.findMatchingProfiles(
                table.getSeats().stream().map(TableSeat::getUserId).toList());
        MatchingProfile me = profiles.get(userId);
        List<MatchingProfile> others = profiles.values().stream()
                .filter(profile -> !profile.userId().equals(userId))
                .toList();

        List<String> reasons = new ArrayList<>();
        reasons.add("%s 지역을 고른 %d명이 나이 차 %d살 이내(%d~%d세)로 모였어요".formatted(table.getRegion().getLabel(),
                table.size(), table.getMaxAge() - table.getMinAge(), table.getMinAge(), table.getMaxAge()));
        int minority = Math.min(table.getMaleCount(), table.getFemaleCount());
        if (minority * 3 >= table.size()) {
            reasons.add("남 %d명 · 여 %d명으로 성비를 맞췄어요".formatted(table.getMaleCount(), table.getFemaleCount()));
        } else {
            // 신청자 성비(여 199 : 남 95)상 고르게 못 맞춘 테이블은 구성만 알린다
            reasons.add("남 %d명 · 여 %d명이 함께해요".formatted(table.getMaleCount(), table.getFemaleCount()));
        }
        if (me != null) {
            List<String> sharedInterests = me.interests().stream()
                    .filter(interest -> others.stream().anyMatch(other -> other.interests().contains(interest)))
                    .sorted()
                    .map(Interest::getLabel)
                    .toList();
            if (!sharedInterests.isEmpty()) {
                reasons.add("나와 같은 관심사(%s)를 가진 멤버가 있어요".formatted(String.join(", ", sharedInterests)));
            }
            List<String> metPreferences = me.preferredTraits().stream()
                    .filter(trait -> others.stream().anyMatch(other -> trait.isSatisfiedBy(me.traits(), other.traits())))
                    .sorted()
                    .map(PreferredTrait::getLabel)
                    .toList();
            if (!metPreferences.isEmpty()) {
                reasons.add("원하신 '%s'에 맞는 멤버가 있어요".formatted(String.join("', '", metPreferences)));
            }
        }
        if (mySeat.getPlacement() != PlacementReason.INITIAL) {
            reasons.add(mySeat.getPlacement().getDescription());
        }
        return new MyTable(slot.eventDate(), slot.startTime(), table.getRegion().getLabel(), table.size(),
                table.getMinAge(), table.getMaxAge(), table.getMaleCount(), table.getFemaleCount(),
                interestLabels(table.getCommonInterests()), reasons);
    }

    private MyFailure myFailure(MatchFailure failure) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(failure.getSlotId());
        LocalDate nextEventDate = failure.getNextSlotId() == null
                ? null
                : eventSlotQueryService.getSlot(failure.getNextSlotId()).eventDate();
        return new MyFailure(slot.eventDate(), failure.getReason().getDescription(), failure.getOutcome(),
                failure.getOutcome().getDescription(), nextEventDate);
    }

    private List<FailureDetail> failureDetails(List<MatchFailure> failures) {
        Map<Long, String> names = userQueryService.findNames(
                failures.stream().map(MatchFailure::getUserId).toList());
        Map<Long, EventSlotInfo> nextSlots = slotsById(failures.stream()
                .map(MatchFailure::getNextSlotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return failures.stream()
                .map(failure -> {
                    EventSlotInfo next = nextSlots.get(failure.getNextSlotId());
                    return new FailureDetail(failure.getApplicationId(), failure.getUserId(),
                            names.get(failure.getUserId()), failure.getReason(), failure.getReason().getDescription(),
                            failure.getRelocationCount(), failure.getOutcome(), failure.getOutcome().getDescription(),
                            failure.getNextSlotId(), next == null ? null : next.eventDate());
                })
                .toList();
    }

    private static SeatDetail seatDetail(TableSeat seat, String name, MatchingProfile profile, EventSlotInfo slot) {
        if (profile == null) {
            return new SeatDetail(seat.getApplicationId(), seat.getUserId(), name, null, null, null, List.of(),
                    seat.getPlacement(), seat.getPlacement().getDescription(), seat.getRelocationCount(),
                    seat.isPreferenceMet());
        }
        return new SeatDetail(seat.getApplicationId(), seat.getUserId(), name,
                profile.ageIn(slot.eventDate().getYear()), profile.gender().getLabel(),
                profile.mbti() == null ? null : profile.mbti().code(),
                profile.interests().stream().sorted().map(Interest::getLabel).toList(),
                seat.getPlacement(), seat.getPlacement().getDescription(), seat.getRelocationCount(),
                seat.isPreferenceMet());
    }

    private Map<Long, EventSlotInfo> slotsById(Collection<Long> slotIds) {
        return eventSlotQueryService.findSlots(slotIds).stream()
                .collect(Collectors.toMap(EventSlotInfo::id, Function.identity()));
    }

    private static List<String> split(String joined) {
        return joined.isBlank() ? List.of() : Arrays.asList(joined.split(","));
    }

    private static List<String> interestLabels(String joined) {
        return split(joined).stream().map(name -> Interest.valueOf(name).getLabel()).toList();
    }
}
