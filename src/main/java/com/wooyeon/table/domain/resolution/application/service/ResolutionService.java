package com.wooyeon.table.domain.resolution.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationCommandService;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.payment.application.dto.PaymentResult;
import com.wooyeon.table.domain.payment.application.service.PaymentService;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.resolution.application.dto.Alternatives.AlternativeSlot;
import com.wooyeon.table.domain.resolution.application.dto.ResolutionResult;
import com.wooyeon.table.domain.resolution.domain.model.Resolution;
import com.wooyeon.table.domain.resolution.domain.model.ResolutionChoice;
import com.wooyeon.table.domain.resolution.domain.repository.ResolutionRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매칭 실패 해결 (STA-05·06, PAY-03). 참가자가 고른 방식을 운영자 개입 없이 바로 처리한다.
 * 신청당 한 번만 처리된다: 상태 확인(대체 안내 상태만) + resolutions 유일 제약
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ResolutionService {

    private final ResolutionQueryService resolutionQueryService;
    private final ResolutionRepository resolutionRepository;
    private final ApplicationQueryService applicationQueryService;
    private final ApplicationCommandService applicationCommandService;
    private final PaymentService paymentService;

    public ResolutionResult resolve(Long userId, Long applicationId, ResolutionChoice choice, Long slotId,
                                    Set<Region> regions) {
        ApplicationInfo application = resolutionQueryService.requireAlternativeOffered(userId, applicationId);
        if (resolutionRepository.findByApplicationId(applicationId).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST, "이미 해결 방식을 골랐습니다");
        }
        resolutionRepository.save(Resolution.of(applicationId, userId, choice,
                choice == ResolutionChoice.MOVE_SCHEDULE ? slotId : null));

        LocalDate movedTo = null;
        PaymentResult refund = null;
        switch (choice) {
            case MOVE_SCHEDULE -> movedTo = move(application, slotId, regions);
            case KEEP_TICKET -> {
                applicationCommandService.keepTicket(applicationId);
                paymentService.keepTicket(applicationId);
            }
            case REFUND -> {
                if (!paymentService.hasRefundableCharge(applicationId)) {
                    throw new BusinessException(ErrorCode.RESOLUTION_NOT_AVAILABLE, "환불할 결제 금액이 없습니다");
                }
                applicationCommandService.closeForRefund(applicationId);
                refund = paymentService.refund(applicationId, false);
            }
        }
        ApplicationInfo resolved = applicationQueryService.getApplication(applicationId);
        return new ResolutionResult(applicationId, choice, resolved.matchStatus(), resolved.paymentStatus(), movedTo,
                refund);
    }

    /** 대체 회차 목록에 있는 회차로만 옮긴다. 결제는 새로 하지 않는다 (STA-06 결제 중복 승인 방지) */
    private LocalDate move(ApplicationInfo application, Long slotId, Set<Region> regions) {
        if (slotId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "옮겨 갈 회차를 골라 주세요");
        }
        AlternativeSlot target = resolutionQueryService.alternativeSlots(application).stream()
                .filter(slot -> slot.slotId().equals(slotId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_CLOSED,
                        "옮겨 갈 수 없는 회차: " + slotId));
        applicationCommandService.moveTo(application.id(), slotId, regions);
        return target.eventDate();
    }
}
