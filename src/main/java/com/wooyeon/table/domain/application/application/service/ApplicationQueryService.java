package com.wooyeon.table.domain.application.application.service;

import com.wooyeon.table.domain.application.application.dto.MatchCandidateApplication;
import com.wooyeon.table.domain.application.domain.repository.ApplicationRepository;
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
}
