package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.feedback.application.dto.MyHistory;
import com.wooyeon.table.domain.feedback.application.dto.MyHistory.Entry;
import com.wooyeon.table.domain.feedback.application.dto.MyHistory.Recommendation;
import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.Feedback;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import com.wooyeon.table.domain.feedback.domain.repository.FeedbackRepository;
import com.wooyeon.table.domain.matching.application.dto.ConfirmedTable;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참가 이력 (RET-01) + 다음 일정 추천 (RET-02).
 * 추천 지역: 후기를 남긴 지역 중 테이블 분위기 평균이 가장 높은 곳, 후기가 없으면 가장 많이 간 곳.
 * 추천 회차: 지금 신청할 수 있고 아직 신청하지 않은 가까운 날짜 3개
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private static final int MAX_RECOMMENDATIONS = 3;

    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final MatchingQueryService matchingQueryService;
    private final EventSlotQueryService eventSlotQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final Clock clock;

    public MyHistory history(Long userId) {
        List<Attendance> attendances = attendanceRepository.findAllByUserIdIn(List.of(userId));
        Map<Region, int[]> regionStats = new EnumMap<>(Region.class);
        List<Entry> entries = attendances.stream()
                .map(attendance -> {
                    ConfirmedTable table = matchingQueryService.getConfirmedTable(attendance.getTableId());
                    EventSlotInfo slot = eventSlotQueryService.getSlot(attendance.getSlotId());
                    Optional<Feedback> feedback = feedbackRepository.findByApplicationId(
                            attendance.getApplicationId());
                    int[] stats = regionStats.computeIfAbsent(table.region(), region -> new int[3]);
                    stats[0]++;
                    feedback.ifPresent(found -> {
                        stats[1] += found.getTableMood();
                        stats[2]++;
                    });
                    return new Entry(attendance.getApplicationId(), slot.eventDate(), table.region().getLabel(),
                            table.size(), attendance.getStatus(), attendance.getStatus().getLabel(),
                            feedback.isPresent());
                })
                .sorted(Comparator.comparing(Entry::eventDate).reversed())
                .toList();
        return new MyHistory(entries, recommend(userId, regionStats));
    }

    private List<Recommendation> recommend(Long userId, Map<Region, int[]> regionStats) {
        Optional<Map.Entry<Region, int[]>> rated = regionStats.entrySet().stream()
                .filter(entry -> entry.getValue()[2] > 0)
                .max(Comparator.comparingDouble(entry -> (double) entry.getValue()[1] / entry.getValue()[2]));
        Optional<Map.Entry<Region, int[]>> frequent = regionStats.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue()[0]));
        Optional<Region> region = rated.or(() -> frequent).map(Map.Entry::getKey);
        String reason = rated.map(entry -> String.format("지난 %s 모임 분위기를 평균 %.1f점으로 평가하셨어요",
                        entry.getKey().getLabel(), (double) entry.getValue()[1] / entry.getValue()[2]))
                .or(() -> frequent.map(entry -> entry.getKey().getLabel() + "에서 " + entry.getValue()[0]
                        + "번 참여하셨어요"))
                .orElse("가장 가까운 모집 일정이에요");

        Set<Long> taken = applicationQueryService.findActiveSlotIds(userId);
        return eventSlotQueryService.findOpenSlots(LocalDateTime.now(clock)).stream()
                .filter(slot -> !taken.contains(slot.id()))
                .limit(MAX_RECOMMENDATIONS)
                .map(slot -> new Recommendation(slot.id(), slot.eventDate(), slot.matchAt(),
                        region.map(Region::getLabel).orElse(null), reason))
                .toList();
    }
}
