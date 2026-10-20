package com.wooyeon.table.domain.chat.domain.service;

import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 단체방에 보여줄 제한된 소개 (INF-06). 이름·연락처·직업·정확한 나이·성별은 넣지 않고
 * 나이대(초·중·후반), MBTI, 관심사, 대화 성향 하나만 쓴다
 */
public final class MemberIntro {

    private MemberIntro() {
    }

    public static String alias(int seatNo) {
        return "참가자 " + seatNo;
    }

    public static String of(int age, Mbti mbti, Set<Interest> interests, Set<Trait> traits) {
        List<String> parts = new ArrayList<>();
        parts.add(ageBand(age));
        if (mbti != null) {
            parts.add(mbti.code());
        }
        if (!interests.isEmpty()) {
            parts.add(String.join(", ", interests.stream()
                    .sorted(Comparator.naturalOrder())
                    .limit(3)
                    .map(Interest::getLabel)
                    .toList()));
        }
        traits.stream().sorted(Comparator.naturalOrder()).findFirst().ifPresent(trait -> parts.add(trait.getLabel()));
        return String.join(" · ", parts);
    }

    /** 27 → "20대 후반" */
    public static String ageBand(int age) {
        int decade = age / 10 * 10;
        int rest = age % 10;
        String part = rest <= 3 ? "초반" : rest <= 6 ? "중반" : "후반";
        return decade + "대 " + part;
    }
}
