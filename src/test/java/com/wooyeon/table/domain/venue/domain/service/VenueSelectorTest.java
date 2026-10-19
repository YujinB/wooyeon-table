package com.wooyeon.table.domain.venue.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.venue.domain.model.Venue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VenueSelectorTest {

    private final Venue cheap = venue(1L, MealBudget.FROM_10K_TO_20K, 2);
    private final Venue middle = venue(2L, MealBudget.FROM_20K_TO_30K, 3);
    private final Venue pricey = venue(3L, MealBudget.OVER_30K, 2);

    @Test
    void 멤버_희망_예산과_맞는_식당이_먼저() {
        List<Set<MealBudget>> budgets = List.of(Set.of(MealBudget.OVER_30K), Set.of(MealBudget.OVER_30K),
                Set.of(MealBudget.FROM_10K_TO_20K, MealBudget.OVER_30K), Set.of(MealBudget.FROM_10K_TO_20K));

        List<Venue> ranked = VenueSelector.rank(List.of(cheap, middle, pricey), Map.of(), budgets);

        assertThat(ranked).extracting(Venue::getId).containsExactly(3L, 1L, 2L);
    }

    @Test
    void 예산이_같으면_남은_테이블이_많은_식당_그다음_id_순() {
        List<Set<MealBudget>> nobody = List.of(Set.of(), Set.of());

        assertThat(VenueSelector.rank(List.of(cheap, middle, pricey), Map.of(), nobody))
                .extracting(Venue::getId).containsExactly(2L, 1L, 3L);
        assertThat(VenueSelector.rank(List.of(cheap, middle, pricey), Map.of(2L, 2L), nobody))
                .extracting(Venue::getId).containsExactly(1L, 3L, 2L);
    }

    @Test
    void 이번_회차_자리가_다_찬_식당은_후보에서_빠진다() {
        List<Venue> ranked = VenueSelector.rank(List.of(cheap, middle, pricey), Map.of(1L, 2L, 2L, 3L),
                List.of(Set.of(MealBudget.FROM_10K_TO_20K)));

        assertThat(ranked).extracting(Venue::getId).containsExactly(3L);
    }

    private static Venue venue(Long id, MealBudget budget, int tablesPerSlot) {
        Venue venue = Venue.builder()
                .name("식당" + id)
                .region(Region.JAMSIL)
                .address("주소")
                .mapUrl("https://example.com")
                .mealBudget(budget)
                .priceGuide("가격")
                .tablesPerSlot(tablesPerSlot)
                .autoBookable(true)
                .build();
        ReflectionTestUtils.setField(venue, "id", id);
        return venue;
    }
}
