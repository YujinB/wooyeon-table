package com.wooyeon.table.domain.profile.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 매칭 프로필 (ACC-02~04). 나이는 출생연도로 저장하고 회차 연도 기준으로 계산한다 */
@Getter
@Entity
@Table(name = "profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private int birthYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    /** 4글자 코드. 모르면 null */
    @Column(length = 4)
    private String mbti;

    @Column(length = 100)
    private String job;

    @ElementCollection
    @CollectionTable(name = "profile_interests", joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interest", length = 30)
    private Set<Interest> interests = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "profile_traits", joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "trait", length = 30)
    private Set<Trait> traits = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "profile_preferred_traits", joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_trait", length = 30)
    private Set<PreferredTrait> preferredTraits = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "profile_meal_budgets", joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_budget", length = 30)
    private Set<MealBudget> mealBudgets = new HashSet<>();

    /** 못 먹는 음식·알레르기 원문 */
    @Column(length = 500)
    private String foodRestriction;

    @Builder
    private Profile(Long userId, int birthYear, Gender gender, Mbti mbti, String job, Set<Interest> interests,
                    Set<Trait> traits, Set<PreferredTrait> preferredTraits, Set<MealBudget> mealBudgets,
                    String foodRestriction) {
        this.userId = userId;
        this.birthYear = birthYear;
        this.gender = gender;
        this.mbti = mbti == null ? null : mbti.code();
        this.job = job;
        this.interests = copy(interests);
        this.traits = copy(traits);
        this.preferredTraits = copy(preferredTraits);
        this.mealBudgets = copy(mealBudgets);
        this.foodRestriction = foodRestriction;
    }

    /** 매칭 프로필·식사 조건 수정 (ACC-02~04). 다음 신청에도 그대로 쓰인다 (ACC-06) */
    public void update(int birthYear, Gender gender, Mbti mbti, String job, Set<Interest> interests,
                       Set<Trait> traits, Set<PreferredTrait> preferredTraits, Set<MealBudget> mealBudgets,
                       String foodRestriction) {
        this.birthYear = birthYear;
        this.gender = gender;
        this.mbti = mbti == null ? null : mbti.code();
        this.job = job;
        replace(this.interests, interests);
        replace(this.traits, traits);
        replace(this.preferredTraits, preferredTraits);
        replace(this.mealBudgets, mealBudgets);
        this.foodRestriction = foodRestriction;
    }

    /** 신청에 필요한 항목(관심사·본인 성향·희망 성향·식사 예산)을 모두 채웠는지 */
    public boolean isCompleteForApplication() {
        return !interests.isEmpty() && !traits.isEmpty() && !preferredTraits.isEmpty() && !mealBudgets.isEmpty();
    }

    public int ageIn(int year) {
        return year - birthYear;
    }

    public Mbti mbtiValue() {
        return mbti == null ? null : Mbti.of(mbti);
    }

    private static <E> void replace(Set<E> target, Set<E> source) {
        target.clear();
        if (source != null) {
            target.addAll(source);
        }
    }

    private static <E> Set<E> copy(Set<E> source) {
        return source == null ? new HashSet<>() : new HashSet<>(source);
    }
}
