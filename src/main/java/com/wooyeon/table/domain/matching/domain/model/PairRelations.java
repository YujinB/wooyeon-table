package com.wooyeon.table.domain.matching.domain.model;

import java.util.HashSet;
import java.util.Set;

/**
 * 후보 쌍 사이의 관계. 제외 관계(차단·신고 확정·다시 만나기 싫음)는 필수 조건, 이전 만남은 선호 점수에 쓴다.
 * 한쪽만 차단해도 같은 테이블 금지이므로 대칭으로 저장한다.
 */
public final class PairRelations {

    private static final PairRelations NONE = new PairRelations(Set.of(), Set.of());

    private final Set<Pair> excluded;
    private final Set<Pair> met;

    private PairRelations(Set<Pair> excluded, Set<Pair> met) {
        this.excluded = excluded;
        this.met = met;
    }

    public static PairRelations none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isExcluded(long a, long b) {
        return !excluded.isEmpty() && excluded.contains(Pair.of(a, b));
    }

    public boolean haveMet(long a, long b) {
        return !met.isEmpty() && met.contains(Pair.of(a, b));
    }

    private record Pair(long low, long high) {
        static Pair of(long a, long b) {
            return a < b ? new Pair(a, b) : new Pair(b, a);
        }
    }

    public static final class Builder {

        private final Set<Pair> excluded = new HashSet<>();
        private final Set<Pair> met = new HashSet<>();

        /** 차단·신고 확정·다시 만나기 싫음 */
        public Builder exclude(long a, long b) {
            excluded.add(Pair.of(a, b));
            return this;
        }

        public Builder met(long a, long b) {
            met.add(Pair.of(a, b));
            return this;
        }

        public PairRelations build() {
            return new PairRelations(Set.copyOf(excluded), Set.copyOf(met));
        }
    }
}
