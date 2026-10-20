package com.wooyeon.table.domain.payment.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.payment.domain.model.PaymentRecord;
import com.wooyeon.table.domain.payment.domain.model.PaymentRecordType;
import com.wooyeon.table.domain.payment.domain.repository.PaymentRecordRepository;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가상데이터·시연용 참가자는 결제 완료 상태로만 적재되므로, 결제 기록이 없는 신청에 모의 결제 기록을 채운다.
 * 1회권은 결제 1건, 멤버십은 사용자별 첫 신청만 결제하고 이후 신청은 0원 멤버십 사용으로 남긴다
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentSeedService {

    private static final Set<PaymentStatus> PAID = EnumSet.of(PaymentStatus.PAID, PaymentStatus.REFUND_AVAILABLE,
            PaymentStatus.REFUND_REQUESTED, PaymentStatus.REFUND_PROCESSING, PaymentStatus.REFUNDED,
            PaymentStatus.REFUND_FAILED);

    private final ApplicationQueryService applicationQueryService;
    private final PaymentRecordRepository paymentRecordRepository;

    /** @return 새로 채운 결제 기록 수 */
    public int fillMissingCharges() {
        Set<Long> membersCharged = new HashSet<>();
        int filled = 0;
        for (ApplicationInfo application : applicationQueryService.findAllSubmitted().stream()
                .sorted(Comparator.comparing(ApplicationInfo::id))
                .toList()) {
            if (!PAID.contains(application.paymentStatus())
                    || !paymentRecordRepository.findAllByApplicationId(application.id()).isEmpty()) {
                continue;
            }
            boolean membershipReuse = application.ticketType().isMembership()
                    && !membersCharged.add(application.userId());
            PaymentRecordType type = membershipReuse ? PaymentRecordType.MEMBERSHIP_USE : PaymentRecordType.CHARGE;
            int amount = membershipReuse ? 0 : application.ticketType().getPrice();
            paymentRecordRepository.save(PaymentRecord.succeeded(application.id(), application.userId(), type, amount,
                    "SEED-" + application.id()));
            filled++;
        }
        return filled;
    }
}
