package com.wooyeon.table.domain.profile.application.dto;

import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.Set;

/** 기본 정보(ACC-02) + 매칭 프로필(ACC-03) + 식사 조건(ACC-04). mbti는 모르면 null */
public record MyProfileCommand(String name, String phone, int birthYear, Gender gender, String job, Mbti mbti,
                               Set<Interest> interests, Set<Trait> traits, Set<PreferredTrait> preferredTraits,
                               Set<MealBudget> mealBudgets, String foodRestriction) {
}
