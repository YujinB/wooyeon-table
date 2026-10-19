package com.wooyeon.table.domain.payment.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationCommandService;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.payment.application.dto.PaymentResult;
import com.wooyeon.table.domain.payment.domain.model.Membership;
import com.wooyeon.table.domain.payment.domain.model.PaymentRecord;
import com.wooyeon.table.domain.payment.domain.model.PaymentRecordType;
import com.wooyeon.table.domain.payment.domain.model.TicketCredit;
import com.wooyeon.table.domain.payment.domain.repository.MembershipRepository;
import com.wooyeon.table.domain.payment.domain.repository.PaymentRecordRepository;
import com.wooyeon.table.domain.payment.domain.repository.TicketCreditRepository;
import com.wooyeon.table.domain.payment.domain.service.PaymentGateway;
import com.wooyeon.table.domain.payment.domain.service.PaymentGateway.GatewayResult;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 모의 결제 (PAY-01). 결제가 끝난 신청만 매칭 후보가 된다 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final ApplicationQueryService applicationQueryService;
    private final ApplicationCommandService applicationCommandService;
    private final EventSlotQueryService eventSlotQueryService;
    private final PaymentRecordRepository paymentRecordRepository;
    private final MembershipRepository membershipRepository;
    private final TicketCreditRepository ticketCreditRepository;
    private final PaymentGateway paymentGateway;
    private final Clock clock;

    /**
     * 신청 결제. 멤버십 이용권이고 고른 날짜를 모두 덮는 유효한 멤버십이 있으면 0원으로 처리한다.
     * simulateFailure는 데모에서 결제 실패 상태를 보여주기 위한 값
     */
    public PaymentResult pay(Long userId, Long applicationId, boolean simulateFailure) {
        ApplicationInfo application = applicationQueryService.getOwnedApplication(userId, applicationId);
        if (!application.isPayable()) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED,
                    application.applicationStatus() + "/" + application.paymentStatus());
        }
        List<LocalDate> openDates = openEventDates(application);
        TicketType ticket = application.ticketType();

        if (ticket.isMembership()) {
            Optional<Membership> active = activeMembership(userId, openDates);
            if (active.isPresent()) {
                paymentRecordRepository.save(PaymentRecord.succeeded(applicationId, userId,
                        PaymentRecordType.MEMBERSHIP_USE, 0, "MEMBERSHIP-" + active.get().getId()));
                applicationCommandService.markPaid(applicationId);
                return result(applicationId, true, PaymentRecordType.MEMBERSHIP_USE, 0, null,
                        active.get().getExpiresOn());
            }
        }

        if (!ticket.isMembership()) {
            Optional<TicketCredit> credit = ticketCreditRepository.findFirstUnused(userId);
            if (credit.isPresent()) {
                credit.get().use(applicationId);
                paymentRecordRepository.save(PaymentRecord.succeeded(applicationId, userId,
                        PaymentRecordType.TICKET_CREDIT_USE, 0, "CREDIT-" + credit.get().getId()));
                applicationCommandService.markPaid(applicationId);
                return result(applicationId, true, PaymentRecordType.TICKET_CREDIT_USE, 0, null, null);
            }
        }

        GatewayResult charged = paymentGateway.charge(applicationId, ticket.getPrice(), simulateFailure);
        if (!charged.success()) {
            paymentRecordRepository.save(PaymentRecord.failed(applicationId, userId, PaymentRecordType.CHARGE,
                    ticket.getPrice(), charged.failureReason()));
            applicationCommandService.markPaymentFailed(applicationId);
            return result(applicationId, false, PaymentRecordType.CHARGE, ticket.getPrice(),
                    charged.failureReason(), null);
        }
        paymentRecordRepository.save(PaymentRecord.succeeded(applicationId, userId, PaymentRecordType.CHARGE,
                ticket.getPrice(), charged.transactionId()));
        applicationCommandService.markPaid(applicationId);
        LocalDate expiresOn = null;
        if (ticket.isMembership()) {
            expiresOn = membershipRepository.save(
                    Membership.start(userId, ticket, applicationId, LocalDate.now(clock))).getExpiresOn();
        }
        return result(applicationId, true, PaymentRecordType.CHARGE, ticket.getPrice(), null, expiresOn);
    }

    /**
     * 원결제수단으로 자동 환불 (PAY-03 모의). 환불 요청 → 처리 중 → 완료/실패로 상태를 옮기고,
     * 실패하면 REFUND_FAILED로 남겨 운영자 예외 목록에 올린다. 멤버십을 산 결제면 멤버십도 끝낸다
     */
    public PaymentResult refund(Long applicationId, boolean simulateFailure) {
        ApplicationInfo application = applicationQueryService.getApplication(applicationId);
        PaymentRecord charge = paymentRecordRepository.findAllByApplicationId(applicationId).stream()
                .filter(PaymentRecord::isRefundableCharge)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                        "환불할 결제가 없습니다 (신청 " + applicationId + ")"));
        applicationCommandService.requestRefund(applicationId);
        applicationCommandService.startRefund(applicationId);

        GatewayResult refunded = paymentGateway.refund(charge.getTransactionId(), charge.getAmount(),
                simulateFailure);
        if (!refunded.success()) {
            paymentRecordRepository.save(PaymentRecord.failed(applicationId, application.userId(),
                    PaymentRecordType.REFUND, charge.getAmount(), refunded.failureReason()));
            applicationCommandService.failRefund(applicationId);
            return result(applicationId, false, PaymentRecordType.REFUND, charge.getAmount(),
                    refunded.failureReason(), null);
        }
        paymentRecordRepository.save(PaymentRecord.succeeded(applicationId, application.userId(),
                PaymentRecordType.REFUND, charge.getAmount(), refunded.transactionId()));
        applicationCommandService.completeRefund(applicationId);
        membershipRepository.findBySourceApplicationId(applicationId).ifPresent(Membership::cancel);
        return result(applicationId, true, PaymentRecordType.REFUND, charge.getAmount(), null, null);
    }

    /**
     * 매칭 실패한 1회권을 보관한다 (STA-05). 신청당 하나만 만들어지고(유일 제약), 다음 1회권 결제 때 0원으로 쓴다.
     * 멤버십은 실패 시 기간이 이미 늘어났으므로 보관할 것이 없다
     */
    public void keepTicket(Long applicationId) {
        ApplicationInfo application = applicationQueryService.getApplication(applicationId);
        if (application.ticketType().isMembership()) {
            return;
        }
        ticketCreditRepository.save(TicketCredit.keep(application.userId(), applicationId,
                application.ticketType().getPrice()));
    }

    /** 실제로 결제한 금액이 있는지 (멤버십 사용 0원 신청은 환불할 것이 없다) */
    @Transactional(readOnly = true)
    public boolean hasRefundableCharge(Long applicationId) {
        return paymentRecordRepository.findAllByApplicationId(applicationId).stream()
                .anyMatch(PaymentRecord::isRefundableCharge);
    }

    /** 아직 신청 마감 전인 날짜. 모두 마감됐으면 결제할 수 없다 */
    private List<LocalDate> openEventDates(ApplicationInfo application) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<LocalDate> dates = eventSlotQueryService.findSlots(application.preferredSlotIds()).stream()
                .filter(slot -> slot.isOpenForApplication(now))
                .map(EventSlotInfo::eventDate)
                .sorted()
                .toList();
        if (dates.isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED, "고른 날짜의 신청이 모두 마감됐습니다");
        }
        return dates;
    }

    private Optional<Membership> activeMembership(Long userId, List<LocalDate> dates) {
        return membershipRepository.findAllByUserId(userId).stream()
                .filter(membership -> dates.stream().allMatch(membership::covers))
                .max(Comparator.comparing(Membership::getExpiresOn));
    }

    private PaymentResult result(Long applicationId, boolean success, PaymentRecordType type, int amount,
                                 String failureReason, LocalDate membershipExpiresOn) {
        return new PaymentResult(applicationId, success, type, amount,
                applicationQueryService.getApplication(applicationId).paymentStatus(), failureReason,
                membershipExpiresOn);
    }
}
