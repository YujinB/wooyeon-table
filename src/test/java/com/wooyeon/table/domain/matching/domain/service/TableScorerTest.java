package com.wooyeon.table.domain.matching.domain.service;

import static com.wooyeon.table.domain.matching.domain.service.CandidateFixture.candidate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.wooyeon.table.domain.matching.domain.model.Candidate;
import com.wooyeon.table.domain.matching.domain.model.PairRelations;
import com.wooyeon.table.domain.matching.domain.model.ScoreWeights;
import com.wooyeon.table.domain.matching.domain.model.TableScore;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableScorerTest {

    @Test
    void 성비는_소수_성별이_절반에_가까울수록_높고_한_성별만이면_0() {
        assertThat(TableScorer.genderBalance(2, 2)).isEqualTo(1.0);
        assertThat(TableScorer.genderBalance(3, 3)).isEqualTo(1.0);
        assertThat(TableScorer.genderBalance(2, 3)).isEqualTo(1.0);
        assertThat(TableScorer.genderBalance(1, 3)).isEqualTo(0.75);
        assertThat(TableScorer.genderBalance(0, 5)).isEqualTo(0.0);
        assertThat(TableScorer.genderBalance(2, 4)).isCloseTo(8.0 / 9, within(1e-9));
    }

    @Test
    void 성비는_남녀를_여러_테이블에_고르게_나누는_쪽이_합이_크다() {
        double spread = TableScorer.genderBalance(2, 4) * 2;
        double lumped = TableScorer.genderBalance(3, 3) + TableScorer.genderBalance(1, 5);
        assertThat(spread).isGreaterThan(lumped);
    }

    @Test
    void MBTI는_NS가_같고_EI_TF가_다를수록_높다() {
        assertThat(TableScorer.mbtiCompatibility(Mbti.of("ENFP"), Mbti.of("INTJ"))).isEqualTo(1.0);
        assertThat(TableScorer.mbtiCompatibility(Mbti.of("ENFP"), Mbti.of("ENFJ"))).isEqualTo(0.5);
        assertThat(TableScorer.mbtiCompatibility(Mbti.of("ESTJ"), Mbti.of("ENTJ"))).isEqualTo(0.0);
        assertThat(TableScorer.mbtiCompatibility(null, Mbti.of("ENTJ"))).isEqualTo(0.5);
    }

    @Test
    void 공통_관심사는_두_개_이상이면_만점() {
        Set<Interest> mine = Set.of(Interest.MUSIC, Interest.TRAVEL, Interest.EXERCISE);
        assertThat(TableScorer.interestOverlap(mine, Set.of(Interest.PHOTO_VIDEO))).isEqualTo(0.0);
        assertThat(TableScorer.interestOverlap(mine, Set.of(Interest.MUSIC))).isEqualTo(0.5);
        assertThat(TableScorer.interestOverlap(mine, Set.of(Interest.MUSIC, Interest.TRAVEL))).isEqualTo(1.0);
    }

    @Test
    void 희망_상대_성향은_매핑된_본인_성향으로_충족된다() {
        Candidate who = candidate(1)
                .traits(Trait.CALM, Trait.DEEP_TALK)
                .preferred(PreferredTrait.STABLE, PreferredTrait.PASSIONATE)
                .build();
        Candidate listener = candidate(2).traits(Trait.GOOD_LISTENER).build();
        Candidate leader = candidate(3).traits(Trait.LEADS_TALK, Trait.GOOD_LISTENER).build();

        assertThat(TableScorer.preferenceSatisfaction(who, listener)).isEqualTo(0.5);
        assertThat(TableScorer.preferenceSatisfaction(who, leader)).isEqualTo(1.0);
    }

    @Test
    void 비슷한_사람은_본인_성향_2개_이상_겹침_다른_자극은_1개_이하() {
        Candidate similarSeeker = candidate(1).traits(Trait.CALM, Trait.DEEP_TALK)
                .preferred(PreferredTrait.SIMILAR).build();
        Candidate differentSeeker = candidate(2).traits(Trait.CALM, Trait.DEEP_TALK)
                .preferred(PreferredTrait.DIFFERENT).build();
        Candidate twin = candidate(3).traits(Trait.CALM, Trait.DEEP_TALK, Trait.ENERGETIC).build();
        Candidate opposite = candidate(4).traits(Trait.ENERGETIC, Trait.CALM).build();

        assertThat(TableScorer.preferenceSatisfaction(similarSeeker, twin)).isEqualTo(1.0);
        assertThat(TableScorer.preferenceSatisfaction(similarSeeker, opposite)).isEqualTo(0.0);
        assertThat(TableScorer.preferenceSatisfaction(differentSeeker, twin)).isEqualTo(0.0);
        assertThat(TableScorer.preferenceSatisfaction(differentSeeker, opposite)).isEqualTo(1.0);
    }

    @Test
    void 모든_항목을_만족하면_100점이고_항목_점수의_합이_총점이다() {
        List<Candidate> candidates = List.of(
                candidate(1).mbti("ENFP").build(),
                candidate(2).mbti("INTJ").build());
        TableScorer scorer = new TableScorer(candidates, PairRelations.none(), ScoreWeights.DEFAULT);

        TableScore score = scorer.score(List.of(0, 1));

        assertThat(score.total()).isCloseTo(100.0, within(1e-9));
        assertThat(score.genderBalance() + score.mbti() + score.interest() + score.preference() + score.novelty())
                .isCloseTo(score.total(), within(1e-9));
    }

    @Test
    void 이전에_만난_쌍이_있으면_새로운_사람_점수가_깎인다() {
        List<Candidate> candidates = List.of(candidate(1).build(), candidate(2).build(), candidate(3).build());
        PairRelations relations = PairRelations.builder().met(1, 2).build();

        TableScore fresh = new TableScorer(candidates, PairRelations.none(), ScoreWeights.DEFAULT)
                .score(List.of(0, 1, 2));
        TableScore withMet = new TableScorer(candidates, relations, ScoreWeights.DEFAULT).score(List.of(0, 1, 2));

        assertThat(fresh.novelty()).isCloseTo(10.0, within(1e-9));
        assertThat(withMet.novelty()).isCloseTo(10.0 * 2 / 3, within(1e-9));
    }

    @Test
    void 가중치_합이_100이_아니어도_100점_만점으로_환산한다() {
        List<Candidate> candidates = List.of(
                candidate(1).mbti("ENFP").build(),
                candidate(2).mbti("INTJ").build());
        ScoreWeights doubled = new ScoreWeights(60, 50, 40, 30, 20);

        TableScore score = new TableScorer(candidates, PairRelations.none(), doubled).score(List.of(0, 1));

        assertThat(score.total()).isCloseTo(100.0, within(1e-9));
    }
}
