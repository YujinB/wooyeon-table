package com.wooyeon.table.domain.profile.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 본인 성향 ("나는 어떤 사람인가요?") */
@Getter
@RequiredArgsConstructor
public enum Trait {
    CALM("잔잔하고 조용한 편"),
    ENERGETIC("활기차고 에너지 있는 편"),
    DEEP_TALK("깊은 대화를 좋아함"),
    LIGHT_HUMOR("가볍고 유머 있는 대화를 좋아함"),
    GOOD_LISTENER("경청을 잘 함"),
    LEADS_TALK("이야기를 이끄는 편");

    private final String label;
}
