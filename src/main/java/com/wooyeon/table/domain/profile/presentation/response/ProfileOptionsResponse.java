package com.wooyeon.table.domain.profile.presentation.response;

import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** 신청서·프로필 화면의 선택지 (코드 + 화면 문구) */
public record ProfileOptionsResponse(List<Option> genders, List<Option> interests, List<Option> traits,
                                     List<Option> preferredTraits, List<Option> mealBudgets, List<Option> regions) {

    public record Option(String code, String label) {
    }

    public static ProfileOptionsResponse create() {
        return new ProfileOptionsResponse(
                options(Gender.values(), Gender::getLabel),
                options(Interest.values(), Interest::getLabel),
                options(Trait.values(), Trait::getLabel),
                options(PreferredTrait.values(), PreferredTrait::getLabel),
                options(MealBudget.values(), MealBudget::getLabel),
                options(Region.values(), Region::getLabel));
    }

    private static <E extends Enum<E>> List<Option> options(E[] values, Function<E, String> label) {
        return Arrays.stream(values).map(value -> new Option(value.name(), label.apply(value))).toList();
    }
}
