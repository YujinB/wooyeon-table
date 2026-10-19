package com.wooyeon.table.domain.event.application.service;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.domain.repository.EventSlotRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventSlotQueryService {

    private final EventSlotRepository eventSlotRepository;

    public EventSlotInfo getSlot(Long slotId) {
        return eventSlotRepository.findById(slotId)
                .map(EventSlotInfo::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_SLOT_NOT_FOUND, String.valueOf(slotId)));
    }

    public List<EventSlotInfo> findSlots(Collection<Long> slotIds) {
        if (slotIds.isEmpty()) {
            return List.of();
        }
        return eventSlotRepository.findAllByIdIn(slotIds).stream().map(EventSlotInfo::from).toList();
    }

    public List<Long> findSlotIdsDueForMatching(LocalDateTime now) {
        return eventSlotRepository.findDueForMatching(now).stream().map(slot -> slot.getId()).toList();
    }
}
