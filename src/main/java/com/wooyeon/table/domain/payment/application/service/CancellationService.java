package com.wooyeon.table.domain.payment.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationCommandService;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.payment.application.dto.CancelResult;
import com.wooyeon.table.domain.payment.application.dto.PaymentResult;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매칭 확정 전 신청 취소 (PAY-02). 결제했다면 환불 가능 시점(가장 가까운 모임 2일 전 = 화 24:00) 전까지만
 * 취소할 수 있고 바로 자동 환불한다. 확정 후 취소(사전·당일 취소, 노쇼)는 W4에서 다룬다
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CancellationService {

    private final ApplicationQueryService applicationQueryService;
    private final ApplicationCommandService applicationCommandService;
    private final EventSlotQueryService eventSlotQueryService;
    private final PaymentService paymentService;
    private final Clock clock;

    public CancelResult cancel(Long userId, Long applicationId) {
        ApplicationInfo application = applicationQueryService.getOwnedApplication(userId, applicationId);
        if (application.paymentStatus() == PaymentStatus.PAID) {
            requireBeforeRefundClose(application);
        }
        applicationCommandService.cancel(applicationId);

        PaymentResult refund = null;
        if (application.paymentStatus() == PaymentStatus.PAID && paymentService.hasRefundableCharge(applicationId)) {
            refund = paymentService.refund(applicationId, false);
        }
        ApplicationInfo canceled = applicationQueryService.getApplication(applicationId);
        return new CancelResult(applicationId, canceled.applicationStatus(), canceled.paymentStatus(), refund);
    }

    private void requireBeforeRefundClose(ApplicationInfo application) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime refundClosesAt = eventSlotQueryService.findSlots(application.preferredSlotIds()).stream()
                .filter(EventSlotInfo::isRecruiting)
                .map(EventSlotInfo::refundClosesAt)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED, "남은 모집 회차가 없습니다"));
        if (!now.isBefore(refundClosesAt)) {
            throw new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED,
                    "환불 가능 시점(" + refundClosesAt + ")이 지나 취소할 수 없습니다");
        }
    }
}
