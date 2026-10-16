package com.wooyeon.table.domain.matching.domain.service;

import static com.wooyeon.table.domain.matching.domain.service.CandidateFixture.candidate;
import static org.assertj.core.api.Assertions.assertThat;

import com.wooyeon.table.domain.matching.domain.model.Candidate;
import com.wooyeon.table.domain.matching.domain.model.MatchResult;
import com.wooyeon.table.domain.matching.domain.model.MatchedTable;
import com.wooyeon.table.domain.matching.domain.model.MatchingPolicy;
import com.wooyeon.table.domain.matching.domain.model.PairRelations;
import com.wooyeon.table.domain.matching.domain.model.PlacementReason;
import com.wooyeon.table.domain.matching.domain.model.TableMember;
import com.wooyeon.table.domain.matching.domain.model.UnassignedCandidate;
import com.wooyeon.table.domain.matching.domain.model.UnassignedReason;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MatchingEngineTest {

    private final MatchingPolicy policy = MatchingPolicy.defaults();
    private final MatchingEngine engine = new MatchingEngine(policy);

    @Test
    void 조건이_맞는_4명은_한_테이블로_확정된다() {
        List<Candidate> candidates = range(1, 4, id -> candidate(id).build());

        MatchResult result = engine.match(candidates, PairRelations.none());

        assertThat(result.tables()).hasSize(1);
        assertThat(result.unassigned()).isEmpty();
        MatchedTable table = result.tables().get(0);
        assertThat(table.region()).isEqualTo(Region.HONGDAE_HAPJEONG);
        assertThat(table.maleCount()).isEqualTo(2);
        assertThat(table.femaleCount()).isEqualTo(2);
        assertThat(table.commonInterests()).containsExactly(Interest.FOOD_CAFE, Interest.TRAVEL);
        assertThat(table.members()).extracting(TableMember::placement).containsOnly(PlacementReason.INITIAL);
    }

    @Test
    void 지역_신청자가_3명이면_테이블을_만들지_않는다() {
        List<Candidate> candidates = range(1, 3, id -> candidate(id).build());

        MatchResult result = engine.match(candidates, PairRelations.none());

        assertThat(result.tables()).isEmpty();
        assertThat(result.unassigned()).extracting(UnassignedCandidate::reason)
                .containsOnly(UnassignedReason.NOT_ENOUGH_CANDIDATES);
    }

    @Test
    void 나이_차가_8살을_넘는_사람은_같은_테이블에_넣지_않는다() {
        List<Candidate> candidates = new ArrayList<>(range(1, 4, id -> candidate(id).age(20 + id.intValue()).build()));
        candidates.add(candidate(5).age(33).build());

        MatchResult result = engine.match(candidates, PairRelations.none());

        assertThat(result.tables()).hasSize(1);
        assertThat(result.tables().get(0).ageGap()).isLessThanOrEqualTo(8);
        assertThat(result.unassigned()).singleElement().satisfies(u -> {
            assertThat(u.candidateId()).isEqualTo(5);
            assertThat(u.reason()).isEqualTo(UnassignedReason.AGE_GAP);
        });
    }

    @Test
    void 차단_관계인_두_사람은_다른_테이블로_나뉜다() {
        List<Candidate> candidates = range(1, 8, id -> candidate(id).build());
        PairRelations relations = PairRelations.builder().exclude(1, 3).exclude(2, 4).build();

        MatchResult result = engine.match(candidates, relations);

        assertThat(result.assignedCount()).isEqualTo(8);
        assertThat(tableOf(result, 1)).isNotEqualTo(tableOf(result, 3));
        assertThat(tableOf(result, 2)).isNotEqualTo(tableOf(result, 4));
    }

    @Test
    void 차단_관계를_빼면_인원이_모자라면_사유가_남는다() {
        List<Candidate> candidates = range(1, 4, id -> candidate(id).build());
        PairRelations relations = PairRelations.builder().exclude(1, 2).build();

        MatchResult result = engine.match(candidates, relations);

        assertThat(result.tables()).isEmpty();
        assertThat(result.unassigned()).filteredOn(u -> u.candidateId() == 1).singleElement()
                .extracting(UnassignedCandidate::reason).isEqualTo(UnassignedReason.EXCLUDED_RELATION);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 6, 8, 9, 10, 11, 12, 13, 17, 23, 30})
    void 인원이_4명_이상이고_7명이_아니면_전원_4에서_6명_테이블에_배정된다(int size) {
        List<Candidate> candidates = range(1, size, id -> candidate(id).build());

        MatchResult result = engine.match(candidates, PairRelations.none());

        assertThat(result.unassigned()).isEmpty();
        assertThat(result.tables()).allSatisfy(t -> assertThat(t.size()).isBetween(4, 6));
    }

    @Test
    void 일곱_명은_4에서_6명으로_나눌_수_없어_한_명이_남는다() {
        List<Candidate> candidates = range(1, 7, id -> candidate(id).build());

        MatchResult result = engine.match(candidates, PairRelations.none());

        assertThat(result.assignedCount()).isEqualTo(6);
        assertThat(result.unassigned()).singleElement()
                .extracting(UnassignedCandidate::reason).isEqualTo(UnassignedReason.NO_FEASIBLE_TABLE);
    }

    @Test
    void 여러_지역을_고른_사람은_인원이_모자란_지역으로_가서_두_테이블이_모두_성립한다() {
        List<Candidate> candidates = new ArrayList<>();
        candidates.addAll(range(1, 3, id -> candidate(id).regions(Region.JAMSIL).build()));
        candidates.addAll(range(4, 7, id -> candidate(id).regions(Region.GANGNAM_SINNONHYEON).build()));
        candidates.add(candidate(8).regions(Region.JAMSIL, Region.GANGNAM_SINNONHYEON).build());

        MatchResult result = engine.match(candidates, PairRelations.none());

        assertThat(result.unassigned()).isEmpty();
        assertThat(tableOf(result, 8).region()).isEqualTo(Region.JAMSIL);
    }

    @Test
    void 첫_희망_지역에_사람이_없으면_다른_희망_지역_테이블에_들어간다() {
        List<Candidate> candidates = new ArrayList<>();
        candidates.addAll(range(1, 5, id -> candidate(id).regions(Region.JAMSIL).build()));
        candidates.add(candidate(6).regions(Region.EULJIRO_JONGNO, Region.JAMSIL).build());

        MatchResult result = engine.match(candidates, PairRelations.none());

        assertThat(result.unassigned()).isEmpty();
        assertThat(result.tables()).singleElement().extracting(MatchedTable::size).isEqualTo(6);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 최소_점수_미달_테이블을_해제하고_멤버를_빈자리가_있는_테이블로_재배치한다(boolean improvement) {
        // 잘 맞는 4명(30세) + 서로 안 맞는 여성 4명(38세). 나이 차 8 이내라 한 바구니에서 섞인다
        List<Candidate> candidates = new ArrayList<>(range(1, 4, id -> candidate(id).age(30).build()));
        candidates.addAll(range(5, 8, id -> candidate(id).age(38)
                .gender(Gender.FEMALE).mbti("ESTJ")
                .interests(Interest.values()[id.intValue()])
                .traits(Trait.CALM).preferred(PreferredTrait.PASSIONATE)
                .build()));

        MatchResult result = new MatchingEngine(policy.withImprovement(improvement))
                .match(candidates, PairRelations.none());

        assertThat(result.stats().dissolvedTables()).isPositive();
        assertThat(result.tables()).flatExtracting(MatchedTable::members)
                .filteredOn(m -> m.placement() == PlacementReason.REALLOCATED)
                .allSatisfy(m -> assertThat(m.relocationCount()).isPositive())
                .isNotEmpty();
        assertThat(result.unassigned()).extracting(UnassignedCandidate::reason)
                .containsOnly(UnassignedReason.BELOW_MIN_SCORE);
    }

    @Test
    void 최소_점수에_못_미치는_조합은_확정하지_않는다() {
        List<Candidate> candidates = range(1, 4, id -> candidate(id)
                .gender(Gender.FEMALE).mbti("ESTJ")
                .interests(Interest.values()[id.intValue()])
                .traits(Trait.CALM).preferred(PreferredTrait.PASSIONATE)
                .build());

        MatchResult strict = engine.match(candidates, PairRelations.none());
        MatchResult lenient = new MatchingEngine(policy.withMinScore(0)).match(candidates, PairRelations.none());

        assertThat(strict.tables()).isEmpty();
        assertThat(strict.unassigned()).extracting(UnassignedCandidate::reason)
                .containsOnly(UnassignedReason.BELOW_MIN_SCORE);
        assertThat(lenient.tables()).hasSize(1);
        assertThat(lenient.lowestScore()).isLessThan(50);
    }

    @Test
    void 입력_순서가_달라도_결과가_같다() {
        List<Candidate> candidates = randomCandidates(80, 7);
        List<Candidate> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, new Random(1));

        assertThat(engine.match(shuffled, PairRelations.none()))
                .isEqualTo(engine.match(candidates, PairRelations.none()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 무작위_입력에서도_모든_테이블이_필수_조건과_최소_점수를_지킨다(boolean improvement) {
        MatchingEngine target = new MatchingEngine(policy.withImprovement(improvement));
        for (long seed = 1; seed <= 20; seed++) {
            List<Candidate> candidates = randomCandidates(60, seed);
            PairRelations relations = randomRelations(candidates, seed);

            MatchResult result = target.match(candidates, relations);

            assertValid(candidates, relations, result);
        }
    }

    private void assertValid(List<Candidate> candidates, PairRelations relations, MatchResult result) {
        Map<Long, Candidate> byId = candidates.stream()
                .collect(Collectors.toMap(Candidate::id, Function.identity()));
        Set<Long> seen = new HashSet<>();
        for (MatchedTable table : result.tables()) {
            assertThat(table.size()).isBetween(policy.minTableSize(), policy.maxTableSize());
            assertThat(table.score().total()).isGreaterThanOrEqualTo(policy.minScore());
            List<Candidate> members = table.members().stream().map(m -> byId.get(m.candidateId())).toList();
            int minAge = members.stream().mapToInt(Candidate::age).min().orElseThrow();
            int maxAge = members.stream().mapToInt(Candidate::age).max().orElseThrow();
            assertThat(maxAge - minAge).isLessThanOrEqualTo(policy.maxAgeGap());
            for (int i = 0; i < members.size(); i++) {
                assertThat(members.get(i).accepts(table.region())).isTrue();
                assertThat(seen.add(members.get(i).id())).isTrue();
                for (int j = i + 1; j < members.size(); j++) {
                    assertThat(relations.isExcluded(members.get(i).id(), members.get(j).id())).isFalse();
                }
            }
        }
        result.unassigned().forEach(u -> assertThat(seen.add(u.candidateId())).isTrue());
        assertThat(seen).hasSize(candidates.size());
    }

    private static MatchedTable tableOf(MatchResult result, long candidateId) {
        return result.tables().stream()
                .filter(t -> t.members().stream().anyMatch(m -> m.candidateId() == candidateId))
                .findFirst()
                .orElseThrow();
    }

    private static List<Candidate> range(long from, long to, Function<Long, Candidate> factory) {
        return IntStream.rangeClosed((int) from, (int) to).mapToObj(id -> factory.apply((long) id)).toList();
    }

    private static List<Candidate> randomCandidates(int size, long seed) {
        Random random = new Random(seed);
        String[] codes = {"ENFP", "INFJ", "ESTJ", "ISFP", "INTP", "ENTJ", "ISFJ", "ESFP"};
        List<Candidate> result = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            Set<Region> regions = EnumSet.noneOf(Region.class);
            regions.add(Region.values()[random.nextInt(Region.values().length)]);
            if (random.nextInt(4) == 0) {
                regions.add(Region.values()[random.nextInt(Region.values().length)]);
            }
            result.add(new Candidate(i, 20 + random.nextInt(24),
                    random.nextInt(3) == 0 ? Gender.MALE : Gender.FEMALE,
                    random.nextInt(10) == 0 ? null : Mbti.of(codes[random.nextInt(codes.length)]),
                    regions,
                    randomSubset(Interest.values(), random),
                    randomSubset(Trait.values(), random),
                    randomSubset(PreferredTrait.values(), random)));
        }
        return result;
    }

    private static PairRelations randomRelations(List<Candidate> candidates, long seed) {
        Random random = new Random(seed * 31);
        PairRelations.Builder builder = PairRelations.builder();
        for (int i = 0; i < candidates.size() / 2; i++) {
            long a = candidates.get(random.nextInt(candidates.size())).id();
            long b = candidates.get(random.nextInt(candidates.size())).id();
            if (a != b) {
                if (random.nextBoolean()) {
                    builder.exclude(a, b);
                } else {
                    builder.met(a, b);
                }
            }
        }
        return builder.build();
    }

    private static <E> Set<E> randomSubset(E[] values, Random random) {
        Set<E> result = new HashSet<>();
        for (E value : values) {
            if (random.nextInt(3) == 0) {
                result.add(value);
            }
        }
        return result;
    }
}
