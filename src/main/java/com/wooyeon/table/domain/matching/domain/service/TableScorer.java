package com.wooyeon.table.domain.matching.domain.service;

import com.wooyeon.table.domain.matching.domain.model.Candidate;
import com.wooyeon.table.domain.matching.domain.model.PairRelations;
import com.wooyeon.table.domain.matching.domain.model.ScoreWeights;
import com.wooyeon.table.domain.matching.domain.model.TableScore;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import java.util.List;
import java.util.Set;

/**
 * 테이블 선호 점수(0~100). 성비만 테이블 단위로 계산하고, 나머지 네 항목은 두 사람 쌍 점수(0~1)의 평균이다.
 * 쌍 점수는 생성 시 한 번 계산해 두고 재사용한다 (개선 단계에서 같은 쌍을 수천 번 평가하므로).
 */
public final class TableScorer {

    /** 공통 관심사가 이 개수 이상이면 쌍 점수 만점 */
    private static final double INTEREST_FULL_OVERLAP = 2.0;
    private static final double UNKNOWN_MBTI_SCORE = 0.5;

    private final List<Candidate> candidates;
    private final double scale;
    private final ScoreWeights weights;
    private final double[][] mbti;
    private final double[][] interest;
    private final double[][] preference;
    private final double[][] novelty;

    public TableScorer(List<Candidate> candidates, PairRelations relations, ScoreWeights weights) {
        this.candidates = candidates;
        this.weights = weights;
        this.scale = 100.0 / weights.sum();
        int n = candidates.size();
        mbti = new double[n][n];
        interest = new double[n][n];
        preference = new double[n][n];
        novelty = new double[n][n];
        for (int i = 0; i < n; i++) {
            Candidate a = candidates.get(i);
            for (int j = i + 1; j < n; j++) {
                Candidate b = candidates.get(j);
                mbti[i][j] = mbti[j][i] = mbtiCompatibility(a.mbti(), b.mbti());
                interest[i][j] = interest[j][i] = interestOverlap(a.interests(), b.interests());
                preference[i][j] = preference[j][i] =
                        (preferenceSatisfaction(a, b) + preferenceSatisfaction(b, a)) / 2;
                novelty[i][j] = novelty[j][i] = relations.haveMet(a.id(), b.id()) ? 0 : 1;
            }
        }
    }

    public TableScore score(List<Integer> members) {
        int male = 0;
        int female = 0;
        for (int member : members) {
            if (candidates.get(member).gender() == Gender.MALE) {
                male++;
            } else {
                female++;
            }
        }
        double mbtiSum = 0;
        double interestSum = 0;
        double preferenceSum = 0;
        double noveltySum = 0;
        int pairs = 0;
        for (int i = 0; i < members.size(); i++) {
            int a = members.get(i);
            for (int j = i + 1; j < members.size(); j++) {
                int b = members.get(j);
                mbtiSum += mbti[a][b];
                interestSum += interest[a][b];
                preferenceSum += preference[a][b];
                noveltySum += novelty[a][b];
                pairs++;
            }
        }
        double perPair = pairs == 0 ? 0 : 1.0 / pairs;
        double genderPoints = genderBalance(male, female) * weights.genderBalance() * scale;
        double mbtiPoints = mbtiSum * perPair * weights.mbti() * scale;
        double interestPoints = interestSum * perPair * weights.interest() * scale;
        double preferencePoints = preferenceSum * perPair * weights.preference() * scale;
        double noveltyPoints = noveltySum * perPair * weights.novelty() * scale;
        return new TableScore(genderPoints + mbtiPoints + interestPoints + preferencePoints + noveltyPoints,
                genderPoints, mbtiPoints, interestPoints, preferencePoints, noveltyPoints);
    }

    public double total(List<Integer> members) {
        return score(members).total();
    }

    /**
     * 성비 (0~1). 소수 성별이 절반에 가까울수록 높고, 한 성별만이면 0.
     * 1-(1-r)^2 형태라 3:3 + 5:1 보다 4:2 + 4:2 가 높게 나온다 (남녀를 여러 테이블에 고르게 나눔).
     */
    public static double genderBalance(int male, int female) {
        int size = male + female;
        if (size < 2) {
            return 0;
        }
        double ratio = Math.min(male, female) / (double) (size / 2);
        return 1 - (1 - ratio) * (1 - ratio);
    }

    /** MBTI 쌍 궁합 (0~1). N/S가 같으면 0.5, E/I가 다르면 0.25, T/F가 다르면 0.25 */
    public static double mbtiCompatibility(Mbti a, Mbti b) {
        if (a == null || b == null) {
            return UNKNOWN_MBTI_SCORE;
        }
        double score = 0;
        if (a.intuitive() == b.intuitive()) {
            score += 0.5;
        }
        if (a.extrovert() != b.extrovert()) {
            score += 0.25;
        }
        if (a.thinking() != b.thinking()) {
            score += 0.25;
        }
        return score;
    }

    /** 공통 관심사 (0~1). 두 개 이상 겹치면 만점 */
    public static double interestOverlap(Set<Interest> a, Set<Interest> b) {
        long common = a.stream().filter(b::contains).count();
        return Math.min(1.0, common / INTEREST_FULL_OVERLAP);
    }

    /** who가 원하는 상대 성향 중 other가 충족하는 비율 (0~1). 원하는 성향이 없으면 1 */
    public static double preferenceSatisfaction(Candidate who, Candidate other) {
        Set<PreferredTrait> wanted = who.preferredTraits();
        if (wanted.isEmpty()) {
            return 1;
        }
        long satisfied = wanted.stream()
                .filter(trait -> trait.isSatisfiedBy(who.traits(), other.traits()))
                .count();
        return satisfied / (double) wanted.size();
    }
}
