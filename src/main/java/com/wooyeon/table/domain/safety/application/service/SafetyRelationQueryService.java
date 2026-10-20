package com.wooyeon.table.domain.safety.application.service;

import com.wooyeon.table.domain.safety.domain.repository.SafetyReportRepository;
import com.wooyeon.table.domain.safety.domain.repository.UserBlockRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 매칭이 같은 테이블에 앉히면 안 되는 쌍 (차단·신고, 방향 무관) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafetyRelationQueryService {

    private final UserBlockRepository userBlockRepository;
    private final SafetyReportRepository safetyReportRepository;

    /** [userIdA, userIdB] 목록 */
    public List<long[]> excludedPairs(Collection<Long> userIds) {
        List<long[]> pairs = new ArrayList<>();
        userBlockRepository.findAmong(userIds)
                .forEach(block -> pairs.add(new long[]{block.getBlockerUserId(), block.getBlockedUserId()}));
        safetyReportRepository.findAmong(userIds)
                .forEach(report -> pairs.add(new long[]{report.getReporterUserId(), report.getReportedUserId()}));
        return pairs;
    }
}
