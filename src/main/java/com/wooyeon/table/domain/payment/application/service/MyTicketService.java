package com.wooyeon.table.domain.payment.application.service;

import com.wooyeon.table.domain.payment.application.dto.MyTickets;
import com.wooyeon.table.domain.payment.application.dto.MyTickets.CreditView;
import com.wooyeon.table.domain.payment.application.dto.MyTickets.MembershipView;
import com.wooyeon.table.domain.payment.domain.model.Membership;
import com.wooyeon.table.domain.payment.domain.repository.MembershipRepository;
import com.wooyeon.table.domain.payment.domain.repository.PaymentRecordRepository;
import com.wooyeon.table.domain.payment.domain.repository.TicketCreditRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 참가자 본인의 멤버십 남은 기간과 보관한 1회권 (RET-03) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyTicketService {

    private final MembershipRepository membershipRepository;
    private final TicketCreditRepository ticketCreditRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final Clock clock;

    public MyTickets getMyTickets(Long userId) {
        LocalDate today = LocalDate.now(clock);
        // 쓸 수 있는 것부터, 그 안에서는 늦게 끝나는 것부터
        List<MembershipView> memberships = membershipRepository.findAllByUserId(userId).stream()
                .filter(membership -> !membership.isCanceled())
                .map(membership -> toView(membership, today))
                .sorted(Comparator.comparing(MembershipView::active).reversed()
                        .thenComparing(MembershipView::expiresOn, Comparator.reverseOrder()))
                .toList();
        List<CreditView> credits = ticketCreditRepository.findAllUnused(userId).stream()
                .map(credit -> new CreditView(credit.getId(), credit.getSourceApplicationId(), credit.getAmount(),
                        credit.getCreatedAt().toLocalDate()))
                .toList();
        return new MyTickets(memberships, credits);
    }

    private MembershipView toView(Membership membership, LocalDate today) {
        boolean active = !today.isAfter(membership.getExpiresOn());
        long daysLeft = active ? ChronoUnit.DAYS.between(today, membership.getExpiresOn()) : 0;
        // 산 신청 1건 + 기간 안에서 0원으로 처리한 신청 (PaymentService가 거래 번호를 MEMBERSHIP-{id}로 남김)
        long usedCount = 1 + paymentRecordRepository.countSucceededByTransactionId("MEMBERSHIP-" + membership.getId());
        return new MembershipView(membership.getId(), membership.getSourceApplicationId(),
                membership.getTicketType().getLabel(), membership.getStartsOn(), membership.getExpiresOn(), daysLeft,
                membership.getExtendedCount(), usedCount, active);
    }
}
