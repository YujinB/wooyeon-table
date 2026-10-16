package com.wooyeon.table.domain.event.application.service;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.domain.event.domain.repository.EventSlotRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventSlotCommandService {

    private final EventSlotRepository eventSlotRepository;

    /** 그 날짜 회차가 없으면 기본 일정(19:30 시작, 2일 전 12:00 매칭)으로 만든다 */
    public Long findOrCreate(LocalDate eventDate) {
        return eventSlotRepository.findByEventDate(eventDate)
                .orElseGet(() -> eventSlotRepository.save(EventSlot.withDefaultSchedule(eventDate)))
                .getId();
    }

    /**
     * 매칭을 시작할 회차를 행 잠금으로 가져온다. 같은 회차에 대한 다른 매칭 실행은 이 트랜잭션이 끝날 때까지 기다린다.
     * 호출하는 쪽 트랜잭션에 참여해야 잠금이 유지된다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public EventSlotInfo lockForMatching(Long slotId) {
        return eventSlotRepository.findByIdForUpdate(slotId)
                .map(EventSlotInfo::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_SLOT_NOT_FOUND, String.valueOf(slotId)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markMatched(Long slotId) {
        eventSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_SLOT_NOT_FOUND, String.valueOf(slotId)))
                .markMatched();
    }
}
