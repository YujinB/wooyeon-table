package com.wooyeon.table.domain.venue.domain.service;

import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.venue.domain.model.Venue;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 같은 지역 식당 후보 순위 (INF-02). 남은 자리가 있는 식당만,
 * ① 그 가격대를 희망 예산으로 고른 멤버가 많은 순 ② 남은 테이블 수가 많은 순 ③ id 순.
 * Spring 의존 없는 순수 로직이라 같은 입력이면 같은 순서가 나온다
 */
public final class VenueSelector {

    private VenueSelector() {
    }

    public static List<Venue> rank(Collection<Venue> sameRegion, Map<Long, Long> usedTables,
                                   Collection<Set<MealBudget>> memberBudgets) {
        return sameRegion.stream()
                .filter(venue -> remaining(venue, usedTables) > 0)
                .sorted(Comparator.comparingLong((Venue venue) -> budgetMatches(venue, memberBudgets)).reversed()
                        .thenComparing(Comparator.comparingLong((Venue venue) -> remaining(venue, usedTables))
                                .reversed())
                        .thenComparing(Venue::getId))
                .toList();
    }

    public static long budgetMatches(Venue venue, Collection<Set<MealBudget>> memberBudgets) {
        return memberBudgets.stream().filter(budgets -> budgets.contains(venue.getMealBudget())).count();
    }

    public static long remaining(Venue venue, Map<Long, Long> usedTables) {
        return venue.getTablesPerSlot() - usedTables.getOrDefault(venue.getId(), 0L);
    }
}
