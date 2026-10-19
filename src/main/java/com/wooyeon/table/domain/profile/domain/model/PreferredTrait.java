package com.wooyeon.table.domain.profile.domain.model;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 만나고 싶은 상대 성향 ("어떤 사람을 만나고 싶으신가요?") */
@Getter
@RequiredArgsConstructor
public enum PreferredTrait {
    HUMOROUS("편하고 유머 있는 사람"),
    STABLE("잔잔하고 안정적인 사람"),
    DEEP("깊은 대화가 되는 사람"),
    PASSIONATE("열정 있는 사람"),
    SIMILAR("나와 비슷한 사람"),
    DIFFERENT("나와 다른 자극을 주는 사람");

    private static final int SIMILAR_MIN_OVERLAP = 2;

    private final String label;

    /**
     * 상대(other)의 본인 성향이 이 희망 성향을 충족하는지. 비슷함/다름은 두 사람의 본인 성향 겹침 수로 판단한다.
     */
    public boolean isSatisfiedBy(Set<Trait> mine, Set<Trait> other) {
        return switch (this) {
            case HUMOROUS -> other.contains(Trait.LIGHT_HUMOR);
            case STABLE -> other.contains(Trait.CALM) || other.contains(Trait.GOOD_LISTENER);
            case DEEP -> other.contains(Trait.DEEP_TALK);
            case PASSIONATE -> other.contains(Trait.ENERGETIC) || other.contains(Trait.LEADS_TALK);
            case SIMILAR -> overlap(mine, other) >= SIMILAR_MIN_OVERLAP;
            case DIFFERENT -> overlap(mine, other) < SIMILAR_MIN_OVERLAP;
        };
    }

    private static long overlap(Set<Trait> a, Set<Trait> b) {
        return a.stream().filter(b::contains).count();
    }
}
