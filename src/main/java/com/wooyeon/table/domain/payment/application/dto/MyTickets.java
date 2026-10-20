package com.wooyeon.table.domain.payment.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 내 이용권 (RET-03): 멤버십은 남은 기간, 보관한 1회권은 남은 장수.
 * 멤버십은 기간 안 횟수 무제한이라 남은 횟수 대신 지금까지 쓴 횟수를 보여 준다
 */
public record MyTickets(List<MembershipView> memberships, List<CreditView> credits) {

    /**
     * @param usedCount   이 멤버십으로 결제한 신청 수 (산 신청 포함)
     * @param daysLeft    오늘부터 만료일까지 남은 일수 (만료됐으면 0)
     * @param extendedCount 매칭 실패로 7일씩 늘어난 횟수
     */
    public record MembershipView(Long membershipId, Long sourceApplicationId, String ticketLabel,
                                 LocalDate startsOn, LocalDate expiresOn, long daysLeft, int extendedCount,
                                 long usedCount, boolean active) {
    }

    public record CreditView(Long creditId, Long sourceApplicationId, int amount, LocalDate keptOn) {
    }
}
