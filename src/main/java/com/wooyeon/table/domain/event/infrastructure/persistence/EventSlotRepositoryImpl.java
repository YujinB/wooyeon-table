package com.wooyeon.table.domain.event.infrastructure.persistence;

import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.event.domain.repository.EventSlotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventSlotRepositoryImpl implements EventSlotRepository {

    private final EventSlotJpaRepository jpaRepository;

    @Override
    public EventSlot save(EventSlot slot) {
        return jpaRepository.save(slot);
    }

    @Override
    public Optional<EventSlot> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<EventSlot> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id);
    }

    @Override
    public Optional<EventSlot> findByEventDate(LocalDate eventDate) {
        return jpaRepository.findByEventDate(eventDate);
    }

    @Override
    public List<EventSlot> findAllByIdIn(Collection<Long> ids) {
        return jpaRepository.findAllByIdIn(ids);
    }

    @Override
    public List<EventSlot> findDueForMatching(LocalDateTime now) {
        return jpaRepository.findAllByStatusAndMatchAtLessThanEqualOrderByEventDateAsc(EventSlotStatus.RECRUITING,
                now);
    }
}
