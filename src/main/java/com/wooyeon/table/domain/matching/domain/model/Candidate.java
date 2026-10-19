package com.wooyeon.table.domain.matching.domain.model;

import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 한 회차 매칭에 들어가는 후보 한 명. 결제 완료·해당 날짜 선택·미확정인 사람만 들어온다고 가정한다.
 *
 * @param mbti 알 수 없으면 null (궁합 점수는 중립값)
 */
public record Candidate(
        long id,
        int age,
        Gender gender,
        Mbti mbti,
        Set<Region> regions,
        Set<Interest> interests,
        Set<Trait> traits,
        Set<PreferredTrait> preferredTraits
) {

    public Candidate {
        Objects.requireNonNull(gender, "gender");
        // EnumSet으로 복사해 순회 순서를 고정한다 (Set.copyOf는 JVM 실행마다 순서가 달라 결과 재현성이 깨진다)
        regions = copy(Region.class, regions);
        interests = copy(Interest.class, interests);
        traits = copy(Trait.class, traits);
        preferredTraits = copy(PreferredTrait.class, preferredTraits);
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("희망 지역이 없는 후보는 매칭할 수 없습니다: " + id);
        }
    }

    public boolean accepts(Region region) {
        return regions.contains(region);
    }

    private static <E extends Enum<E>> Set<E> copy(Class<E> type, Collection<E> source) {
        EnumSet<E> set = EnumSet.noneOf(type);
        if (source != null) {
            set.addAll(source);
        }
        return Collections.unmodifiableSet(set);
    }
}
