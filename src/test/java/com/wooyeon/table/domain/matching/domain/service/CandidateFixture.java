package com.wooyeon.table.domain.matching.domain.service;

import com.wooyeon.table.domain.matching.domain.model.Candidate;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.Set;

/** 테스트용 후보. 기본값은 서로 잘 맞는 사람이라 4명만 모아도 최소 점수를 넘는다. */
final class CandidateFixture {

    private long id;
    private int age = 28;
    private Gender gender;
    private Mbti mbti;
    private Set<Region> regions = Set.of(Region.HONGDAE_HAPJEONG);
    private Set<Interest> interests = Set.of(Interest.FOOD_CAFE, Interest.TRAVEL);
    private Set<Trait> traits = Set.of(Trait.LIGHT_HUMOR, Trait.GOOD_LISTENER);
    private Set<PreferredTrait> preferred = Set.of(PreferredTrait.HUMOROUS);

    private CandidateFixture(long id) {
        this.id = id;
        this.gender = id % 2 == 0 ? Gender.FEMALE : Gender.MALE;
        this.mbti = Mbti.of(id % 2 == 0 ? "ENFP" : "INFJ");
    }

    static CandidateFixture candidate(long id) {
        return new CandidateFixture(id);
    }

    CandidateFixture age(int age) {
        this.age = age;
        return this;
    }

    CandidateFixture gender(Gender gender) {
        this.gender = gender;
        return this;
    }

    CandidateFixture mbti(String code) {
        this.mbti = code == null ? null : Mbti.of(code);
        return this;
    }

    CandidateFixture regions(Region... regions) {
        this.regions = Set.of(regions);
        return this;
    }

    CandidateFixture interests(Interest... interests) {
        this.interests = Set.of(interests);
        return this;
    }

    CandidateFixture traits(Trait... traits) {
        this.traits = Set.of(traits);
        return this;
    }

    CandidateFixture preferred(PreferredTrait... preferred) {
        this.preferred = Set.of(preferred);
        return this;
    }

    Candidate build() {
        return new Candidate(id, age, gender, mbti, regions, interests, traits, preferred);
    }
}
