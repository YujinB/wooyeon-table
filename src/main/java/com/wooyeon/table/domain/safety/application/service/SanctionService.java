package com.wooyeon.table.domain.safety.application.service;

import com.wooyeon.table.domain.safety.application.dto.ReportView;
import com.wooyeon.table.domain.safety.domain.model.ReportStatus;
import com.wooyeon.table.domain.safety.domain.model.SafetyReport;
import com.wooyeon.table.domain.safety.domain.model.SanctionLevel;
import com.wooyeon.table.domain.safety.domain.model.UserSanction;
import com.wooyeon.table.domain.safety.domain.repository.SafetyReportRepository;
import com.wooyeon.table.domain.safety.domain.repository.UserSanctionRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자 신고 검토·조치 기록 (SAF-02): 경고 · 참가 제한(N일) · 영구 제한 · 조치 없이 종결 */
@Service
@RequiredArgsConstructor
@Transactional
public class SanctionService {

    private final SafetyReportRepository safetyReportRepository;
    private final UserSanctionRepository userSanctionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ReportView> reports(boolean openOnly) {
        List<SafetyReport> reports = openOnly ? safetyReportRepository.findAllByStatus(ReportStatus.RECEIVED)
                : safetyReportRepository.findAll();
        return reports.stream().map(ReportView::from).toList();
    }

    /** action이 null이면 조치 없이 종결. 참가 제한은 days일 */
    public ReportView act(Long reportId, SanctionLevel action, Integer days, String memo) {
        SafetyReport report = safetyReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "신고를 찾을 수 없습니다"));
        if (report.getStatus() != ReportStatus.RECEIVED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "이미 처리한 신고입니다");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String reason = "신고 #" + reportId + " " + report.getReason().getLabel();
        if (action != null) {
            userSanctionRepository.save(switch (action) {
                case WARNING -> UserSanction.warning(report.getReportedUserId(), reason, reportId, now);
                case RESTRICTED -> {
                    if (days == null || days < 1) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT, "참가 제한 일수를 입력해 주세요");
                    }
                    yield UserSanction.restricted(report.getReportedUserId(), reason, reportId, now, days);
                }
                case BANNED -> UserSanction.banned(report.getReportedUserId(), reason, reportId, now);
            });
        }
        report.resolve(action, memo, now);
        return ReportView.from(report);
    }

    @Transactional(readOnly = true)
    public List<UserSanction> sanctionsOf(Long userId) {
        return userSanctionRepository.findAllByUserId(userId);
    }
}
