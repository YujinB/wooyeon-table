package com.wooyeon.table.domain.application.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이용권 (APP-04, 주최 측 Q&A 10/16). 멤버십은 기간 안에 횟수 무제한이고,
 * 매칭에 실패하면 실패 횟수만큼 기한을 늘린다 (W2-3)
 */
@Getter
@RequiredArgsConstructor
public enum TicketType {
    SINGLE("1회권", 9_900, 0),
    MEMBERSHIP_1M("멤버십 1개월", 35_000, 1),
    MEMBERSHIP_3M("멤버십 3개월", 75_000, 3),
    MEMBERSHIP_6M("멤버십 6개월", 120_000, 6);

    private final String label;
    private final int price;
    /** 멤버십 기간(개월). 1회권은 0 */
    private final int months;

    public boolean isMembership() {
        return months > 0;
    }
}
