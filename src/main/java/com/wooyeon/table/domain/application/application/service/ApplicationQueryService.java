package com.wooyeon.table.domain.application.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.dto.MatchCandidateApplication;
import com.wooyeon.table.domain.application.domain.repository.ApplicationRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.util.List;
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

    public ApplicationInfo getApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .map(ApplicationInfo::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND,
                        String.valueOf(applicationId)));
    }
}
