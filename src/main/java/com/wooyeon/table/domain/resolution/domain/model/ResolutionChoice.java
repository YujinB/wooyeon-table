package com.wooyeon.table.domain.resolution.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 매칭 실패 후 참가자가 고르는 해결 방식 (STA-05). 시스템이 임의로 고르지 않는다 (PRD 7.4) */
@Getter
@RequiredArgsConstructor
public enum ResolutionChoice {
    MOVE_SCHEDULE("추천 일정으로 이동", "결제는 그대로 두고 고른 회차의 매칭 대기로 바로 옮겨요."),
    KEEP_TICKET("이용권 보관", "이번 이용권을 보관했다가 다음 신청에 결제 없이 써요."),
    REFUND("환불", "운영자 확인 없이 결제수단으로 바로 환불돼요.");

    private final String label;
    private final String description;
}
