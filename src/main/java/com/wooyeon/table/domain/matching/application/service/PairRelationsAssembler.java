package com.wooyeon.table.domain.matching.application.service;

import com.wooyeon.table.domain.feedback.application.service.PreferenceQueryService;
import com.wooyeon.table.domain.matching.domain.model.PairRelations;
import com.wooyeon.table.domain.matching.domain.repository.DiningTableRepository;
import com.wooyeon.table.domain.safety.application.service.SafetyRelationQueryService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 이번 회차 후보 사이의 관계를 모은다 (MAT-05·06, 피드백 반영).
 * 제외(필수 조건): 차단 · 신고 · "다시 만나고 싶지 않음" — 한쪽만 해도 같은 테이블 금지.
 * 이전 만남(선호 점수 '새로운 사람'): 전에 같은 테이블에 앉았던 쌍.
 * 엔진은 신청 id로 후보를 다루므로 사용자 쌍을 신청 쌍으로 바꿔 넘긴다
 */
@Component
@RequiredArgsConstructor
public class PairRelationsAssembler {

    private final SafetyRelationQueryService safetyRelationQueryService;
    private final PreferenceQueryService preferenceQueryService;
    private final DiningTableRepository diningTableRepository;

    /** applicationIdByUser: 후보 사용자 id → 이번 회차 신청 id */
    public PairRelations assemble(Map<Long, Long> applicationIdByUser) {
        Collection<Long> userIds = applicationIdByUser.keySet();
        PairRelations.Builder builder = PairRelations.builder();
        List<long[]> excluded = new ArrayList<>(safetyRelationQueryService.excludedPairs(userIds));
        excluded.addAll(preferenceQueryService.avoidPairs(userIds));
        excluded.forEach(pair -> link(applicationIdByUser, pair[0], pair[1], builder::exclude));

        for (List<Long> seated : diningTableRepository.findSeatedUsersByTable(userIds).values()) {
            for (int i = 0; i < seated.size(); i++) {
                for (int j = i + 1; j < seated.size(); j++) {
                    link(applicationIdByUser, seated.get(i), seated.get(j), builder::met);
                }
            }
        }
        return builder.build();
    }

    private static void link(Map<Long, Long> applicationIdByUser, long userA, long userB, PairSink sink) {
        Long a = applicationIdByUser.get(userA);
        Long b = applicationIdByUser.get(userB);
        if (a != null && b != null && !a.equals(b)) {
            sink.accept(a, b);
        }
    }

    @FunctionalInterface
    private interface PairSink {
        Object accept(long a, long b);
    }
}
