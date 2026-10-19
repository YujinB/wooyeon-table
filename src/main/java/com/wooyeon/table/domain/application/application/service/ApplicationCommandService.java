package com.wooyeon.table.domain.application.application.service;

import com.wooyeon.table.domain.application.application.dto.SubmitApplicationCommand;
import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.repository.ApplicationRepository;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.profile.application.service.ProfileQueryService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationCommandService {

    private final ApplicationRepository applicationRepository;
    private final ProfileQueryService profileQueryService;
    private final EventSlotQueryService eventSlotQueryService;
    private final Clock clock;

    public Long save(Application application) {
        return applicationRepository.save(application).getId();
    }

    /**
     * 참가 신청 제출 (APP-02~04). 프로필 필수 항목, 회차가 모두 신청 마감 전인지,
     * 진행 중인 다른 신청과 날짜가 겹치지 않는지 확인한 뒤 결제 대기 상태로 저장한다
     */
    public Long submit(Long userId, SubmitApplicationCommand command) {
        if (!profileQueryService.isCompleteForApplication(userId)) {
            throw new BusinessException(ErrorCode.PROFILE_INCOMPLETE);
        }
        requireOpenSlots(command.slotIds());
        Set<Long> alreadyApplied = applicationRepository.findAllByUserId(userId).stream()
                .filter(Application::isActive)
                .flatMap(application -> application.getPreferredSlotIds().stream())
                .collect(Collectors.toSet());
        if (command.slotIds().stream().anyMatch(alreadyApplied::contains)) {
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION);
        }
        return applicationRepository.save(
                Application.submit(userId, command.ticketType(), command.regions(), command.slotIds())).getId();
    }

    public void markPaid(Long applicationId) {
        get(applicationId).markPaid();
    }

    public void markPaymentFailed(Long applicationId) {
        get(applicationId).markPaymentFailed();
    }

    public void cancel(Long applicationId) {
        get(applicationId).cancel();
    }

    public void requestRefund(Long applicationId) {
        get(applicationId).requestRefund();
    }

    public void startRefund(Long applicationId) {
        get(applicationId).startRefund();
    }

    public void completeRefund(Long applicationId) {
        get(applicationId).completeRefund();
    }

    public void failRefund(Long applicationId) {
        get(applicationId).failRefund();
    }

    public void confirmMatch(Collection<Long> applicationIds, Long slotId) {
        applicationRepository.findAllByIdIn(applicationIds).forEach(application -> application.confirmMatch(slotId));
    }

    public void offerAlternative(Collection<Long> applicationIds) {
        applicationRepository.findAllByIdIn(applicationIds).forEach(Application::offerAlternative);
    }

    private Application get(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND, String.valueOf(applicationId)));
    }

    private void requireOpenSlots(Set<Long> slotIds) {
        List<EventSlotInfo> slots = eventSlotQueryService.findSlots(slotIds);
        if (slots.size() != slotIds.size()) {
            throw new BusinessException(ErrorCode.EVENT_SLOT_NOT_FOUND, slotIds.toString());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        slots.stream()
                .filter(slot -> !slot.isOpenForApplication(now))
                .findFirst()
                .ifPresent(slot -> {
                    throw new BusinessException(ErrorCode.APPLICATION_CLOSED, slot.eventDate().toString());
                });
    }
}
