package com.wooyeon.table.domain.application.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.dto.MatchCandidateApplication;
import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.application.domain.repository.ApplicationRepository;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationQueryService {

    private final ApplicationRepository applicationRepository;

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
    public List<ApplicationInfo> findAllSubmitted() {
        return applicationRepository.findAllSubmitted().stream().map(ApplicationInfo::from).toList();
    }

    public List<ApplicationInfo> findByPaymentStatus(PaymentStatus paymentStatus) {
        return applicationRepository.findAllByPaymentStatus(paymentStatus).stream().map(ApplicationInfo::from).toList();
    }

    public ApplicationInfo getApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .map(ApplicationInfo::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND,
                        String.valueOf(applicationId)));
    }
}
