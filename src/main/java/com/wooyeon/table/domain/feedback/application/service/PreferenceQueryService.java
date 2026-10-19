package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.feedback.domain.model.PreferenceType;
import com.wooyeon.table.domain.feedback.domain.repository.PersonPreferenceRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다음 매칭이 쓰는 사람별 선호: "다시 만나고 싶지 않음" 쌍 (FDB-04 → MAT-05) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreferenceQueryService {

    private final PersonPreferenceRepository personPreferenceRepository;

    /** [보낸 사람, 받는 사람] 목록 */
    public List<long[]> avoidPairs(Collection<Long> userIds) {
        return personPreferenceRepository.findAmong(userIds, PreferenceType.AVOID).stream()
                .map(preference -> new long[]{preference.getFromUserId(), preference.getToUserId()})
                .toList();
    }
}
