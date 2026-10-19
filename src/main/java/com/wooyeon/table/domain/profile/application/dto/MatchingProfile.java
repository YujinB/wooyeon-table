package com.wooyeon.table.domain.profile.application.dto;

import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.Set;

/** 매칭에 필요한 프로필 값만 담은 조회 결과 */
public record MatchingProfile(
        Long userId,
        int birthYear,
        Gender gender,
        Mbti mbti,
        Set<Interest> interests,
        Set<Trait> traits,
        Set<PreferredTrait> preferredTraits
) {

    public static MatchingProfile from(Profile profile) {
        return new MatchingProfile(profile.getUserId(), profile.getBirthYear(), profile.getGender(),
                profile.mbtiValue(), Set.copyOf(profile.getInterests()), Set.copyOf(profile.getTraits()),
                Set.copyOf(profile.getPreferredTraits()));
    }

    public int ageIn(int year) {
        return year - birthYear;
    }
}
