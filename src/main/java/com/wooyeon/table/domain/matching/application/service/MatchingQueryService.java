package com.wooyeon.table.domain.matching.application.service;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.dto.FailureDetail;
import com.wooyeon.table.domain.matching.application.dto.MatchRunSummary;
import com.wooyeon.table.domain.matching.application.dto.ScoreBreakdown;
import com.wooyeon.table.domain.matching.application.dto.SeatDetail;
import com.wooyeon.table.domain.matching.application.dto.SlotMatchingResult;
import com.wooyeon.table.domain.matching.application.dto.TableDetail;
import com.wooyeon.table.domain.matching.application.dto.TableSummary;
import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.matching.domain.model.MatchFailure;
import com.wooyeon.table.domain.matching.domain.model.MatchRun;
import com.wooyeon.table.domain.matching.domain.model.TableSeat;
import com.wooyeon.table.domain.matching.domain.repository.DiningTableRepository;
import com.wooyeon.table.domain.matching.domain.repository.MatchFailureRepository;
import com.wooyeon.table.domain.matching.domain.repository.MatchRunRepository;
import com.wooyeon.table.domain.profile.application.dto.MatchingProfile;
import com.wooyeon.table.domain.profile.application.service.ProfileQueryService;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.user.application.service.UserQueryService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
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
