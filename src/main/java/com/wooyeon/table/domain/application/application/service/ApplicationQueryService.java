package com.wooyeon.table.domain.application.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.dto.MatchCandidateApplication;
import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.ApplicationStatus;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.application.domain.repository.ApplicationRepository;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.global.audit.AuditHistoryReader;
import com.wooyeon.table.global.audit.StatusChange;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationQueryService {

    /** PRD 12절 상태값 이름 */
    private static final Map<ApplicationStatus, String> APPLICATION_LABEL = Map.of(
            ApplicationStatus.DRAFT, "작성 중", ApplicationStatus.SUBMITTED, "신청 완료",
            ApplicationStatus.CANCELED, "신청 취소");
    private static final Map<MatchStatus, String> MATCH_LABEL = Map.of(
            MatchStatus.WAITING, "매칭 대기", MatchStatus.MATCHING, "자동 매칭 실행 중",
            MatchStatus.REALLOCATING, "자동 재배치 중", MatchStatus.CONFIRMED, "자동 확정",
            MatchStatus.ALTERNATIVE_OFFERED, "대체 일정·보관·환불 선택 대기", MatchStatus.TRANSFERRED, "다른 일정으로 이동",
            MatchStatus.NO_MATCH, "매칭 종료", MatchStatus.EXCEPTION, "운영자 확인 필요");

    private final ApplicationRepository applicationRepository;
    private final AuditHistoryReader auditHistoryReader;

    /** 결제 완료 + 그 회차 선택 + 매칭 대기인 신청 (id 순) */
    public List<MatchCandidateApplication> findMatchCandidates(Long slotId) {
        return applicationRepository.findMatchCandidates(slotId).stream()
                .map(MatchCandidateApplication::from)
                .toList();
    }

    /** 그 회차 매칭 후보(결제 완료·대기)를 지역별로 센다. 여러 지역을 고른 사람은 지역마다 센다 */
    public Map<Region, Long> countCandidatesByRegion(Long slotId) {
        Map<Region, Long> counts = new EnumMap<>(Region.class);
        for (Region region : Region.values()) {
            counts.put(region, 0L);
        }
        applicationRepository.findMatchCandidates(slotId)
                .forEach(application -> application.getRegions().forEach(region -> counts.merge(region, 1L, Long::sum)));
        return counts;
    }

    /** 취소하지 않은 신청들이 고른 회차 (같은 날짜 중복 신청 방지·대체 일정 후보 제외용) */
    public Set<Long> findActiveSlotIds(Long userId) {
        return applicationRepository.findAllByUserId(userId).stream()
                .filter(Application::isActive)
                .filter(application -> application.getMatchStatus() == MatchStatus.WAITING
                        || application.getMatchStatus() == MatchStatus.CONFIRMED)
                .flatMap(application -> application.getPreferredSlotIds().stream())
                .collect(Collectors.toSet());
    }

    public List<ApplicationInfo> findMyApplications(Long userId) {
        return applicationRepository.findAllByUserId(userId).stream().map(ApplicationInfo::from).toList();
    }

    /** 본인 신청만. 다른 사람 신청이면 없는 것으로 응답한다 (존재 여부도 노출하지 않음) */
    public ApplicationInfo getOwnedApplication(Long userId, Long applicationId) {
        ApplicationInfo application = getApplication(applicationId);
        if (!application.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND, String.valueOf(applicationId));
        }
        return application;
    }

    /** 신청 완료 이후의 모든 신청 (운영 현황 집계용, 최근 것부터) */
    /** 회차를 지워도 되는지 볼 때: 그 회차를 고른 신청이 있는지 (취소 포함) */
    public boolean hasApplications(Long slotId) {
        return applicationRepository.existsBySlotId(slotId);
    }

    public List<ApplicationInfo> findAllSubmitted() {
        return applicationRepository.findAllSubmitted().stream().map(ApplicationInfo::from).toList();
    }

    public List<ApplicationInfo> findByPaymentStatus(PaymentStatus paymentStatus) {
        return applicationRepository.findAllByPaymentStatus(paymentStatus).stream().map(ApplicationInfo::from).toList();
    }

    /** 신청·결제·매칭 상태 변경 이력 (PRD 16절) */
    public List<StatusChange> statusHistory(Long applicationId) {
        getApplication(applicationId);
        Map<String, Function<Application, String>> fields = new LinkedHashMap<>();
        fields.put("신청", application -> APPLICATION_LABEL.get(application.getApplicationStatus()));
        fields.put("결제", application -> application.getPaymentStatus().getLabel());
        fields.put("매칭", application -> MATCH_LABEL.get(application.getMatchStatus()));
        return auditHistoryReader.changes(Application.class, "id", applicationId, fields);
    }

    public ApplicationInfo getApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .map(ApplicationInfo::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND,
                        String.valueOf(applicationId)));
    }
}
