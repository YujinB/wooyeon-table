package com.wooyeon.table.domain.profile.application.dto;

import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.Set;

/**
 * 참가자 본인 프로필. 아직 매칭 프로필을 만들지 않았으면 기본 정보만 채워지고 나머지는 비어 있다.
 * completed = 신청할 수 있는 상태인지
 */
public record MyProfile(String loginId, String name, String phone, Integer birthYear, Gender gender, String job,
                        String mbti, Set<Interest> interests, Set<Trait> traits, Set<PreferredTrait> preferredTraits,
                        Set<MealBudget> mealBudgets, String foodRestriction, boolean completed) {
}
