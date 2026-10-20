package com.wooyeon.table.domain.event.application.service;

import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.event.application.dto.SlotDeletedEvent;
import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.event.domain.repository.EventSlotRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 잘못 만든 회차 지우기. 모집 중이고 아무도 고르지 않은 회차만 지운다 (신청·결제 이력이 생긴 회차는 남긴다).
 * 신청 도메인을 조회해야 해서 회차 생성(EventSlotCommandService)과 나눴다
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SlotDeletionService {

    private final EventSlotRepository eventSlotRepository;
    private final ApplicationQueryService applicationQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public void delete(Long slotId) {
        EventSlot slot = eventSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_SLOT_NOT_FOUND, String.valueOf(slotId)));
        if (slot.getStatus() != EventSlotStatus.RECRUITING || applicationQueryService.hasApplications(slotId)) {
            throw new BusinessException(ErrorCode.EVENT_SLOT_NOT_DELETABLE, slot.getEventDate().toString());
        }
        eventSlotRepository.delete(slot);
        eventPublisher.publishEvent(new SlotDeletedEvent(slotId));
        log.info("회차 {} ({}) 삭제", slotId, slot.getEventDate());
    }
}
