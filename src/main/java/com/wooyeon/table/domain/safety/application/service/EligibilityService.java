package com.wooyeon.table.domain.safety.application.service;

import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.application.service.AttendanceQueryService;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.domain.safety.application.dto.Eligibility;
import com.wooyeon.table.domain.safety.domain.model.UserSanction;
import com.wooyeon.table.domain.safety.domain.repository.UserSanctionRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신청 가능 여부 (SAF-02, 우리가 정한 당일 취소·노쇼 정책).
 * 막는 경우: 영구 제한 / 기한 안의 참가 제한 / 노쇼 2회 이상 또는 노쇼+제한 시점 이후 취소 3회 이상 →
 * 마지막 위반부터 30일 자동 참가 제한 (조치 기록으로 남김)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EligibilityService {

    public static final int NO_SHOW_LIMIT = 2;
    public static final int NO_SHOW_OR_LATE_CANCEL_LIMIT = 3;
    public static final int AUTO_RESTRICT_DAYS = 30;

    private final UserSanctionRepository userSanctionRepository;
    private final AttendanceQueryService attendanceQueryService;
    private final Clock clock;

    public Eligibility check(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        applyAutoRestriction(userId, now);
        Optional<UserSanction> blocking = userSanctionRepository.findAllByUserId(userId).stream()
                .filter(sanction -> sanction.blocksParticipation(now))
                .findFirst();
        return blocking.map(sanction -> new Eligibility(false, sanction.getLevel(), sanction.getReason(),
                        sanction.getEndsAt()))
                .orElseGet(() -> new Eligibility(true, null, null, null));
    }

    /** 신청 제출 전에 부른다 */
    public void requireEligible(Long userId) {
        Eligibility eligibility = check(userId);
        if (!eligibility.eligible()) {
            throw new BusinessException(ErrorCode.PARTICIPATION_RESTRICTED, eligibility.message());
        }
    }

    /** 노쇼·당일 취소가 기준을 넘었고 아직 그 위반으로 제한한 적이 없으면 30일 제한을 기록한다 */
    private void applyAutoRestriction(Long userId, LocalDateTime now) {
        List<AttendanceView> violations = attendanceQueryService.findByUser(userId).stream()
                .filter(attendance -> attendance.status() == AttendanceStatus.NO_SHOW
                        || attendance.status() == AttendanceStatus.CANCELED_LATE)
                .toList();
        long noShows = violations.stream().filter(attendance -> attendance.status() == AttendanceStatus.NO_SHOW)
                .count();
        if (noShows < NO_SHOW_LIMIT && violations.size() < NO_SHOW_OR_LATE_CANCEL_LIMIT) {
            return;
        }
        String reason = "노쇼 " + noShows + "회 · 제한 시점 이후 취소 " + (violations.size() - noShows) + "회";
        boolean alreadyApplied = userSanctionRepository.findAllByUserId(userId).stream()
                .anyMatch(sanction -> sanction.getReportId() == null && reason.equals(sanction.getReason()));
        if (!alreadyApplied) {
            userSanctionRepository.save(UserSanction.restricted(userId, reason, null, now, AUTO_RESTRICT_DAYS));
        }
    }
}
