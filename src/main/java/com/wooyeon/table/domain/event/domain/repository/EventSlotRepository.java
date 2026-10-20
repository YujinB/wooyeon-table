package com.wooyeon.table.domain.event.domain.repository;

import com.wooyeon.table.domain.event.domain.model.EventSlot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventSlotRepository {

    EventSlot save(EventSlot slot);

    void delete(EventSlot slot);

    Optional<EventSlot> findById(Long id);

    /** 같은 회차 매칭이 동시에 두 번 돌지 않도록 행 잠금으로 조회한다 */
    Optional<EventSlot> findByIdForUpdate(Long id);

    Optional<EventSlot> findByEventDate(LocalDate eventDate);

    List<EventSlot> findAllByIdIn(Collection<Long> ids);

    /** 모집 중이고 신청 마감 전인 회차 (날짜순) */
    List<EventSlot> findOpenForApplication(LocalDateTime now);

    /** 전체 회차 (최근 날짜부터) */
    List<EventSlot> findAllLatestFirst();

    /** 매칭 실행 시각이 지났는데 아직 모집 중인 회차 (날짜순) */
    List<EventSlot> findDueForMatching(LocalDateTime now);
}
