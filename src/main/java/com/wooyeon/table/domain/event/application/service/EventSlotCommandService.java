package com.wooyeon.table.domain.event.application.service;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.domain.event.domain.repository.EventSlotRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.wooyeon.table.domain.event.application.dto.SlotOpenedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventSlotCommandService {

    private final EventSlotRepository eventSlotRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 운영자 회차 생성. 시작 시각·신청 마감을 비우면 기본값(19:30 시작, 전날 0시 마감)
     */
    public Long create(LocalDate eventDate, LocalTime startTime, LocalDateTime matchAt) {
        if (!eventDate.isAfter(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "회차 날짜는 오늘 이후여야 합니다");
        }
        if (eventSlotRepository.findByEventDate(eventDate).isPresent()) {
            throw new BusinessException(ErrorCode.EVENT_SLOT_ALREADY_EXISTS, eventDate.toString());
        }
        EventSlot slot = EventSlot.of(eventDate,
                startTime == null ? EventSlot.DEFAULT_START_TIME : startTime,
                matchAt == null ? EventSlot.refundClosesAt(eventDate) : matchAt);
        Long slotId = eventSlotRepository.save(slot).getId();
        eventPublisher.publishEvent(new SlotOpenedEvent(slotId));
        return slotId;
    }

    /** 그 날짜 회차가 없으면 기본 일정(19:30 시작, 전날 0시 마감·매칭)으로 만든다 */
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

    public void complete(Long slotId) {
        eventSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_SLOT_NOT_FOUND, String.valueOf(slotId)))
                .complete();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markMatched(Long slotId) {
        eventSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_SLOT_NOT_FOUND, String.valueOf(slotId)))
                .markMatched();
    }
}
