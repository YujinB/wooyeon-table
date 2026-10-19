package com.wooyeon.table.domain.matching.domain.service;

import com.wooyeon.table.domain.matching.domain.model.Candidate;
import com.wooyeon.table.domain.matching.domain.model.MatchResult;
import com.wooyeon.table.domain.matching.domain.model.MatchStats;
import com.wooyeon.table.domain.matching.domain.model.MatchedTable;
import com.wooyeon.table.domain.matching.domain.model.MatchingPolicy;
import com.wooyeon.table.domain.matching.domain.model.PairRelations;
import com.wooyeon.table.domain.matching.domain.model.PlacementReason;
import com.wooyeon.table.domain.matching.domain.model.TableMember;
import com.wooyeon.table.domain.matching.domain.model.UnassignedCandidate;
import com.wooyeon.table.domain.matching.domain.model.UnassignedReason;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 한 회차(날짜)의 후보를 4~6인 테이블로 나눈다. Spring에 의존하지 않는 순수 도메인 로직이다.
 *
 * <ol>
 *   <li>인원 적은 지역부터, 나이순으로 가장 어린 사람을 씨앗 삼아 나이 차 이내에서 점수가 가장 오르는 사람을 더한다 (그리디)</li>
 *   <li>남은 사람을 빈자리가 있는 테이블에 넣는다 (같은 날짜 안에서 본인이 고른 다른 지역 포함, PRD 7.4의 1·2순위)</li>
 *   <li>(개선 켜짐) 테이블 간 이동·교환으로 총점을 올리고, 남은 사람을 넣으려고 기존 멤버를 다른 테이블로 밀어낸다</li>
 *   <li>최소 점수 미달 테이블을 하나씩 해제하고 그 멤버를 다시 넣는다</li>
 *   <li>모든 테이블을 필수 조건으로 재검증한다 (MAT-07)</li>
 * </ol>
 * 무작위를 쓰지 않고 동점은 id 순으로 정하므로 같은 입력이면 항상 같은 결과가 나온다.
 * 다른 날짜로의 이월(7.4의 3·4순위)은 회차별로 이 엔진을 부르는 쪽에서 처리한다.
 */
public final class MatchingEngine {

    private static final double EPSILON = 1e-9;
    /** 개선 단계에서 이 값보다 작은 이득은 무시한다 (부동소수 오차로 같은 이동을 반복하지 않게) */
    private static final double MIN_GAIN = 1e-6;
    /** 최소 점수에 못 미친 만큼의 몇 배를 목적함수에서 깎는다. 개선 단계가 미달 테이블부터 끌어올리게 한다 */
    private static final double BELOW_MIN_PENALTY = 3.0;

    private final MatchingPolicy policy;

    public MatchingEngine(MatchingPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public MatchResult match(List<Candidate> candidates, PairRelations relations) {
        return new Run(candidates, relations).execute();
    }

    private static final class Draft {
        private final int seq;
        private final Region region;
        private final List<Integer> members;

        private Draft(int seq, Region region, List<Integer> members) {
            this.seq = seq;
            this.region = region;
            this.members = members;
        }
    }

    /** 실행 한 번의 상태. 후보는 id 순으로 정렬해 인덱스로 다룬다. */
    private final class Run {

        private final List<Candidate> candidates;
        private final PairRelations relations;
        private final int count;
        private final HardConstraints constraints;
        private final TableScorer scorer;
        private final List<Draft> tables = new ArrayList<>();
        private final Draft[] tableOf;
        private final int[] relocations;
        private final PlacementReason[] placements;
        private final boolean[] dissolved;
        private int nextSeq = 1;

        private int initialTables;
        private int reallocations;
        private int displacements;
        private int moves;
        private int swaps;
        private int dissolvedTables;
        private int improvementRounds;

        private Run(List<Candidate> input, PairRelations relations) {
            this.candidates = input.stream().sorted(Comparator.comparingLong(Candidate::id)).toList();
            this.relations = Objects.requireNonNull(relations, "relations");
            this.count = candidates.size();
            for (int i = 1; i < count; i++) {
                if (candidates.get(i).id() == candidates.get(i - 1).id()) {
                    throw new IllegalArgumentException("후보 id 중복: " + candidates.get(i).id());
                }
            }
            this.constraints = new HardConstraints(candidates, relations, policy);
            this.scorer = new TableScorer(candidates, relations, policy.weights());
            this.tableOf = new Draft[count];
            this.relocations = new int[count];
            this.placements = new PlacementReason[count];
            this.dissolved = new boolean[count];
        }

        private MatchResult execute() {
            buildInitialTables();
            reallocateLeftovers();
            if (policy.improvementEnabled()) {
                improve();
            }
            dissolveBelowMinScore();
            verify();
            return toResult();
        }

        // ---------- 1. 그리디 ----------

        private void buildInitialTables() {
            Set<Region> done = EnumSet.noneOf(Region.class);
            while (true) {
                Region next = null;
                int nextSize = Integer.MAX_VALUE;
                for (Region region : Region.values()) {
                    if (done.contains(region)) {
                        continue;
                    }
                    int size = unassignedAccepting(region).size();
                    if (size >= policy.minTableSize() && size < nextSize) {
                        next = region;
                        nextSize = size;
                    }
                }
                if (next == null) {
                    return;
                }
                done.add(next);
                buildRegion(next);
            }
        }

        private void buildRegion(Region region) {
            List<Integer> pool = unassignedAccepting(region);
            pool.sort(Comparator.<Integer>comparingInt(this::age).thenComparingInt(i -> i));
            for (int seed : pool) {
                if (tableOf[seed] != null) {
                    continue;
                }
                List<Integer> window = new ArrayList<>();
                int inAgeRangeCount = 0;
                for (int other : pool) {
                    boolean inAgeRange = age(other) >= age(seed) && age(other) - age(seed) <= policy.maxAgeGap();
                    if (tableOf[other] != null || !inAgeRange) {
                        continue;
                    }
                    inAgeRangeCount++;
                    if (other == seed || !constraints.isExcluded(seed, other)) {
                        window.add(other);
                    }
                }
                if (window.size() < policy.minTableSize()) {
                    continue;
                }
                // 크기는 씨앗과의 제외 관계를 빼기 전 인원으로 정한다. 제외된 사람도 남은 인원으로 다음 테이블을 만들어야 하므로
                int size = Math.min(chooseTableSize(inAgeRangeCount), window.size());
                Map<Gender, Integer> genderTarget = genderTarget(size, pool);
                List<Integer> members = new ArrayList<>(List.of(seed));
                while (members.size() < size) {
                    int next = pickNext(members, window, region, genderTarget);
                    if (next < 0) {
                        break;
                    }
                    members.add(next);
                }
                if (members.size() >= policy.minTableSize()) {
                    open(region, members);
                    members.forEach(member -> placements[member] = PlacementReason.INITIAL);
                    initialTables++;
                }
            }
        }

        /**
         * 바구니 성비에 맞춘 목표 인원을 아직 못 채운 성별을 먼저, 그다음 점수가 가장 오르는 사람을 고른다.
         * 동점이면 window 순서(나이 → id)가 빠른 사람, 즉 씨앗과 나이가 가까운 사람.
         */
        private int pickNext(List<Integer> members, List<Integer> window, Region region,
                             Map<Gender, Integer> genderTarget) {
            int best = -1;
            boolean bestWanted = false;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int candidate : window) {
                if (members.contains(candidate) || !constraints.canJoin(members, candidate, region)) {
                    continue;
                }
                Gender gender = candidates.get(candidate).gender();
                boolean wanted = countGender(members, gender) < genderTarget.get(gender);
                double score = scoreWith(members, candidate);
                boolean better = best < 0
                        || (wanted && !bestWanted)
                        || (wanted == bestWanted && score > bestScore + EPSILON);
                if (better) {
                    best = candidate;
                    bestWanted = wanted;
                    bestScore = score;
                }
            }
            return best;
        }

        /** 남은 인원이 계속 4~6명으로 나눠지도록 이번 테이블 크기를 정한다. 가운데(5)를 먼저 시도한다 */
        private int chooseTableSize(int available) {
            int min = policy.minTableSize();
            int max = policy.maxTableSize();
            if (available <= max) {
                return available;
            }
            int middle = (min + max) / 2;
            List<Integer> order = new ArrayList<>(List.of(middle));
            for (int size = max; size >= min; size--) {
                if (size != middle) {
                    order.add(size);
                }
            }
            for (int size : order) {
                if (isPartitionable(available - size)) {
                    return size;
                }
            }
            return max;
        }

        private boolean isPartitionable(int people) {
            if (people == 0) {
                return true;
            }
            for (int tableCount = 1; tableCount * policy.minTableSize() <= people; tableCount++) {
                if (people <= tableCount * policy.maxTableSize()) {
                    return true;
                }
            }
            return false;
        }

        private Map<Gender, Integer> genderTarget(int size, List<Integer> pool) {
            int male = 0;
            int female = 0;
            for (int candidate : pool) {
                if (tableOf[candidate] == null) {
                    if (candidates.get(candidate).gender() == Gender.MALE) {
                        male++;
                    } else {
                        female++;
                    }
                }
            }
            int maleTarget = male + female == 0 ? 0 : (int) Math.round(size * male / (double) (male + female));
            if (male > 0 && maleTarget == 0) {
                maleTarget = 1;
            }
            if (female > 0 && maleTarget == size) {
                maleTarget = size - 1;
            }
            Map<Gender, Integer> target = new EnumMap<>(Gender.class);
            target.put(Gender.MALE, maleTarget);
            target.put(Gender.FEMALE, size - maleTarget);
            return target;
        }

        // ---------- 2. 재배치 ----------

        /** 남은 사람을 빈자리가 있는 테이블 중 목적함수가 가장 좋아지는(덜 나빠지는) 곳에 넣는다 */
        private boolean reallocateLeftovers() {
            boolean changed = false;
            boolean placed = true;
            while (placed) {
                placed = false;
                for (int leftover = 0; leftover < count; leftover++) {
                    if (tableOf[leftover] != null) {
                        continue;
                    }
                    Draft best = null;
                    double bestGain = Double.NEGATIVE_INFINITY;
                    for (Draft table : tables) {
                        if (!constraints.canJoin(table.members, leftover, table.region)) {
                            continue;
                        }
                        double before = scorer.total(table.members);
                        double after = scoreWith(table.members, leftover);
                        if (!acceptable(before, after)) {
                            continue;
                        }
                        double gain = objective(after) - objective(before);
                        if (gain > bestGain + EPSILON) {
                            best = table;
                            bestGain = gain;
                        }
                    }
                    if (best != null) {
                        join(best, leftover);
                        markReallocated(leftover);
                        reallocations++;
                        placed = true;
                        changed = true;
                    }
                }
            }
            return changed;
        }

        /** 남은 사람 x가 테이블 A의 멤버 y 자리에 들어가고, y는 빈자리가 있는 다른 테이블 B로 옮긴다 */
        private boolean displaceForLeftovers() {
            boolean changed = false;
            for (int leftover = 0; leftover < count; leftover++) {
                if (tableOf[leftover] != null) {
                    continue;
                }
                Draft bestFrom = null;
                Draft bestTo = null;
                int bestOut = -1;
                double bestGain = Double.NEGATIVE_INFINITY;
                for (Draft from : tables) {
                    if (!candidates.get(leftover).accepts(from.region)) {
                        continue;
                    }
                    double fromBefore = scorer.total(from.members);
                    for (int out : from.members) {
                        if (!constraints.canReplace(from.members, out, leftover, from.region)) {
                            continue;
                        }
                        double fromAfter = scorer.total(replaced(from.members, out, leftover));
                        if (!acceptable(fromBefore, fromAfter)) {
                            continue;
                        }
                        for (Draft to : tables) {
                            if (to == from || !constraints.canJoin(to.members, out, to.region)) {
                                continue;
                            }
                            double toBefore = scorer.total(to.members);
                            double toAfter = scoreWith(to.members, out);
                            if (!acceptable(toBefore, toAfter)) {
                                continue;
                            }
                            double gain = objective(fromAfter) - objective(fromBefore)
                                    + objective(toAfter) - objective(toBefore);
                            if (gain > bestGain + EPSILON) {
                                bestFrom = from;
                                bestTo = to;
                                bestOut = out;
                                bestGain = gain;
                            }
                        }
                    }
                }
                if (bestFrom != null) {
                    leave(bestFrom, bestOut);
                    join(bestFrom, leftover);
                    join(bestTo, bestOut);
                    markReallocated(leftover);
                    markOptimized(bestOut);
                    displacements++;
                    changed = true;
                }
            }
            return changed;
        }

        // ---------- 3. 개선 ----------

        private void improve() {
            for (int round = 0; round < policy.maxImprovementRounds(); round++) {
                improvementRounds++;
                boolean changed = false;
                for (int i = 0; i < tables.size(); i++) {
                    for (int j = i + 1; j < tables.size(); j++) {
                        changed |= improvePair(tables.get(i), tables.get(j));
                    }
                }
                changed |= reallocateLeftovers();
                changed |= displaceForLeftovers();
                if (!changed) {
                    return;
                }
            }
        }

        /** 두 테이블 사이의 이동(a→b, b→a)과 교환 중 목적함수 합이 가장 많이 오르는 것 하나를 적용한다 */
        private boolean improvePair(Draft a, Draft b) {
            double base = objective(scorer.total(a.members)) + objective(scorer.total(b.members));
            double bestGain = MIN_GAIN;
            int bestFromA = -1;
            int bestFromB = -1;

            if (a.members.size() > policy.minTableSize()) {
                for (int x : a.members) {
                    if (constraints.canJoin(b.members, x, b.region)) {
                        double gain = objective(scorer.total(without(a.members, x)))
                                + objective(scoreWith(b.members, x)) - base;
                        if (gain > bestGain) {
                            bestGain = gain;
                            bestFromA = x;
                            bestFromB = -1;
                        }
                    }
                }
            }
            if (b.members.size() > policy.minTableSize()) {
                for (int y : b.members) {
                    if (constraints.canJoin(a.members, y, a.region)) {
                        double gain = objective(scoreWith(a.members, y))
                                + objective(scorer.total(without(b.members, y))) - base;
                        if (gain > bestGain) {
                            bestGain = gain;
                            bestFromA = -1;
                            bestFromB = y;
                        }
                    }
                }
            }
            for (int x : a.members) {
                for (int y : b.members) {
                    if (!constraints.canReplace(a.members, x, y, a.region)
                            || !constraints.canReplace(b.members, y, x, b.region)) {
                        continue;
                    }
                    double gain = objective(scorer.total(replaced(a.members, x, y)))
                            + objective(scorer.total(replaced(b.members, y, x))) - base;
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestFromA = x;
                        bestFromB = y;
                    }
                }
            }

            if (bestFromA < 0 && bestFromB < 0) {
                return false;
            }
            if (bestFromA >= 0) {
                leave(a, bestFromA);
            }
            if (bestFromB >= 0) {
                leave(b, bestFromB);
            }
            if (bestFromA >= 0) {
                join(b, bestFromA);
                markOptimized(bestFromA);
            }
            if (bestFromB >= 0) {
                join(a, bestFromB);
                markOptimized(bestFromB);
            }
            if (bestFromA >= 0 && bestFromB >= 0) {
                swaps++;
            } else {
                moves++;
            }
            return true;
        }

        // ---------- 4. 최소 점수 ----------

        /** 최소 점수 미달 테이블을 점수가 낮은 것부터 하나씩 해제하고 멤버를 다른 테이블로 재배치한다 */
        private void dissolveBelowMinScore() {
            while (true) {
                Draft worst = null;
                double worstScore = policy.minScore() - EPSILON;
                for (Draft table : tables) {
                    double score = scorer.total(table.members);
                    if (score < worstScore) {
                        worst = table;
                        worstScore = score;
                    }
                }
                if (worst == null) {
                    return;
                }
                tables.remove(worst);
                for (int member : worst.members) {
                    tableOf[member] = null;
                    dissolved[member] = true;
                }
                dissolvedTables++;
                reallocateLeftovers();
                if (policy.improvementEnabled()) {
                    improve();
                }
            }
        }

        // ---------- 5. 검증·결과 ----------

        private void verify() {
            for (Draft table : tables) {
                List<HardConstraints.Violation> violations = constraints.violations(table.members, table.region);
                if (!violations.isEmpty()) {
                    throw new IllegalStateException("필수 조건 위반 테이블: " + violations);
                }
                if (scorer.total(table.members) < policy.minScore() - EPSILON) {
                    throw new IllegalStateException("최소 점수 미달 테이블이 남음");
                }
            }
        }

        private MatchResult toResult() {
            List<Draft> ordered = tables.stream().sorted(Comparator.comparingInt(t -> t.seq)).toList();
            List<MatchedTable> matched = new ArrayList<>();
            for (Draft table : ordered) {
                matched.add(describe(matched.size() + 1, table));
            }
            List<UnassignedCandidate> unassigned = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if (tableOf[i] == null) {
                    unassigned.add(new UnassignedCandidate(candidates.get(i).id(), reasonFor(i), relocations[i]));
                }
            }
            MatchStats stats = new MatchStats(count, initialTables, reallocations, displacements, moves, swaps,
                    dissolvedTables, improvementRounds);
            return new MatchResult(List.copyOf(matched), List.copyOf(unassigned), stats);
        }

        private MatchedTable describe(int tableNo, Draft table) {
            List<Integer> members = table.members.stream().sorted().toList();
            List<TableMember> tableMembers = new ArrayList<>();
            List<String> mbti = new ArrayList<>();
            Map<Interest, Integer> interestCount = new EnumMap<>(Interest.class);
            int minAge = Integer.MAX_VALUE;
            int maxAge = Integer.MIN_VALUE;
            int male = 0;
            int metPairs = 0;
            for (int i = 0; i < members.size(); i++) {
                int index = members.get(i);
                Candidate candidate = candidates.get(index);
                tableMembers.add(new TableMember(candidate.id(), placements[index], relocations[index],
                        preferenceMet(index, members)));
                mbti.add(candidate.mbti() == null ? "?" : candidate.mbti().code());
                candidate.interests().forEach(interest -> interestCount.merge(interest, 1, Integer::sum));
                minAge = Math.min(minAge, candidate.age());
                maxAge = Math.max(maxAge, candidate.age());
                if (candidate.gender() == Gender.MALE) {
                    male++;
                }
                for (int j = i + 1; j < members.size(); j++) {
                    if (relations.haveMet(candidate.id(), candidates.get(members.get(j)).id())) {
                        metPairs++;
                    }
                }
            }
            List<Interest> common = interestCount.entrySet().stream()
                    .filter(entry -> entry.getValue() >= 2)
                    .sorted(Map.Entry.<Interest, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .map(Map.Entry::getKey)
                    .toList();
            return new MatchedTable(tableNo, table.region, List.copyOf(tableMembers), minAge, maxAge, male,
                    members.size() - male, List.copyOf(mbti), common, metPairs, scorer.score(members));
        }

        private boolean preferenceMet(int who, List<Integer> members) {
            Candidate candidate = candidates.get(who);
            if (candidate.preferredTraits().isEmpty()) {
                return true;
            }
            for (int other : members) {
                if (other != who && TableScorer.preferenceSatisfaction(candidate, candidates.get(other)) > 0) {
                    return true;
                }
            }
            return false;
        }

        /** 필수 조건부터 차례로 따져 가장 앞선 원인을 사유로 남긴다 */
        private UnassignedReason reasonFor(int index) {
            Candidate candidate = candidates.get(index);
            int needed = policy.minTableSize() - 1;
            boolean enoughInRegion = false;
            boolean enoughInAge = false;
            boolean enoughWithoutExcluded = false;
            for (Region region : candidate.regions()) {
                List<Integer> peers = new ArrayList<>();
                List<Integer> allowedPeers = new ArrayList<>();
                for (int other = 0; other < count; other++) {
                    if (other != index && candidates.get(other).accepts(region)) {
                        peers.add(other);
                        if (!constraints.isExcluded(index, other)) {
                            allowedPeers.add(other);
                        }
                    }
                }
                enoughInRegion |= peers.size() >= needed;
                enoughInAge |= hasAgeWindow(candidate.age(), peers, needed);
                enoughWithoutExcluded |= hasAgeWindow(candidate.age(), allowedPeers, needed);
            }
            if (!enoughInRegion) {
                return UnassignedReason.NOT_ENOUGH_CANDIDATES;
            }
            if (!enoughInAge) {
                return UnassignedReason.AGE_GAP;
            }
            if (!enoughWithoutExcluded) {
                return UnassignedReason.EXCLUDED_RELATION;
            }
            if (dissolved[index]) {
                return UnassignedReason.BELOW_MIN_SCORE;
            }
            return UnassignedReason.NO_FEASIBLE_TABLE;
        }

        /** age를 포함하는 나이 차 이내 구간에 peers가 needed명 이상 있는지 */
        private boolean hasAgeWindow(int age, List<Integer> peers, int needed) {
            for (int start = age - policy.maxAgeGap(); start <= age; start++) {
                int inWindow = 0;
                for (int peer : peers) {
                    if (age(peer) >= start && age(peer) <= start + policy.maxAgeGap()) {
                        inWindow++;
                    }
                }
                if (inWindow >= needed) {
                    return true;
                }
            }
            return false;
        }

        // ---------- 공통 ----------

        private double objective(double score) {
            return score >= policy.minScore() ? score : score - (policy.minScore() - score) * BELOW_MIN_PENALTY;
        }

        /** 넣은 뒤 최소 점수 이상이거나, 이미 미달인 테이블을 더 나쁘게 만들지 않을 때만 허용 */
        private boolean acceptable(double before, double after) {
            return after + EPSILON >= policy.minScore()
                    || (before < policy.minScore() && after + EPSILON >= before);
        }

        private List<Integer> unassignedAccepting(Region region) {
            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if (tableOf[i] == null && candidates.get(i).accepts(region)) {
                    result.add(i);
                }
            }
            return result;
        }

        private int age(int index) {
            return candidates.get(index).age();
        }

        private int countGender(List<Integer> members, Gender gender) {
            int result = 0;
            for (int member : members) {
                if (candidates.get(member).gender() == gender) {
                    result++;
                }
            }
            return result;
        }

        private double scoreWith(List<Integer> members, int joining) {
            members.add(joining);
            double score = scorer.total(members);
            members.remove(members.size() - 1);
            return score;
        }

        private List<Integer> without(List<Integer> members, int leaving) {
            List<Integer> result = new ArrayList<>(members);
            result.remove(Integer.valueOf(leaving));
            return result;
        }

        private List<Integer> replaced(List<Integer> members, int leaving, int joining) {
            List<Integer> result = without(members, leaving);
            result.add(joining);
            return result;
        }

        private void open(Region region, List<Integer> members) {
            Draft table = new Draft(nextSeq++, region, new ArrayList<>(members));
            tables.add(table);
            members.forEach(member -> tableOf[member] = table);
        }

        private void join(Draft table, int member) {
            table.members.add(member);
            tableOf[member] = table;
        }

        private void leave(Draft table, int member) {
            table.members.remove(Integer.valueOf(member));
            tableOf[member] = null;
        }

        private void markReallocated(int member) {
            relocations[member]++;
            placements[member] = PlacementReason.REALLOCATED;
        }

        private void markOptimized(int member) {
            if (placements[member] == PlacementReason.INITIAL) {
                placements[member] = PlacementReason.OPTIMIZED;
            }
        }
    }
}
