package com.wooyeon.table.domain.matching.domain.service;

import com.wooyeon.table.domain.matching.domain.model.Candidate;
import com.wooyeon.table.domain.matching.domain.model.MatchingPolicy;
import com.wooyeon.table.domain.matching.domain.model.PairRelations;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.util.ArrayList;
import java.util.List;

/**
 * 필수 조건 (PRD 7.0): 같은 지역, 4~6명, 최고·최저 나이 차 이내, 제외 관계 없음.
 * 날짜는 회차 단위로 실행하므로 후보군에서 이미 맞춰져 있다. 멤버는 후보 목록의 인덱스로 다룬다.
 */
public final class HardConstraints {

    public enum Violation {
        TOO_FEW_MEMBERS,
        TOO_MANY_MEMBERS,
        REGION_MISMATCH,
        AGE_GAP_EXCEEDED,
        EXCLUDED_PAIR
    }

    private final List<Candidate> candidates;
    private final PairRelations relations;
    private final MatchingPolicy policy;

    public HardConstraints(List<Candidate> candidates, PairRelations relations, MatchingPolicy policy) {
        this.candidates = candidates;
        this.relations = relations;
        this.policy = policy;
    }

    /** members 테이블에 joining을 더해도 필수 조건(최소 인원 제외)을 지키는지 */
    public boolean canJoin(List<Integer> members, int joining, Region region) {
        if (members.size() >= policy.maxTableSize()) {
            return false;
        }
        Candidate newcomer = candidates.get(joining);
        if (!newcomer.accepts(region)) {
            return false;
        }
        int minAge = newcomer.age();
        int maxAge = newcomer.age();
        for (int member : members) {
            Candidate other = candidates.get(member);
            if (relations.isExcluded(newcomer.id(), other.id())) {
                return false;
            }
            minAge = Math.min(minAge, other.age());
            maxAge = Math.max(maxAge, other.age());
        }
        return maxAge - minAge <= policy.maxAgeGap();
    }

    /** members에서 leaving을 빼고 joining을 넣어도 되는지 (교환·밀어내기용) */
    public boolean canReplace(List<Integer> members, int leaving, int joining, Region region) {
        List<Integer> rest = new ArrayList<>(members.size());
        for (int member : members) {
            if (member != leaving) {
                rest.add(member);
            }
        }
        return canJoin(rest, joining, region);
    }

    public boolean isExcluded(int a, int b) {
        return relations.isExcluded(candidates.get(a).id(), candidates.get(b).id());
    }

    /** 확정 직전 재검증 (MAT-07) */
    public List<Violation> violations(List<Integer> members, Region region) {
        List<Violation> violations = new ArrayList<>();
        if (members.size() < policy.minTableSize()) {
            violations.add(Violation.TOO_FEW_MEMBERS);
        }
        if (members.size() > policy.maxTableSize()) {
            violations.add(Violation.TOO_MANY_MEMBERS);
        }
        int minAge = Integer.MAX_VALUE;
        int maxAge = Integer.MIN_VALUE;
        boolean regionMismatch = false;
        boolean excludedPair = false;
        for (int i = 0; i < members.size(); i++) {
            Candidate candidate = candidates.get(members.get(i));
            regionMismatch |= !candidate.accepts(region);
            minAge = Math.min(minAge, candidate.age());
            maxAge = Math.max(maxAge, candidate.age());
            for (int j = i + 1; j < members.size(); j++) {
                excludedPair |= isExcluded(members.get(i), members.get(j));
            }
        }
        if (regionMismatch) {
            violations.add(Violation.REGION_MISMATCH);
        }
        if (!members.isEmpty() && maxAge - minAge > policy.maxAgeGap()) {
            violations.add(Violation.AGE_GAP_EXCEEDED);
        }
        if (excludedPair) {
            violations.add(Violation.EXCLUDED_PAIR);
        }
        return violations;
    }
}
