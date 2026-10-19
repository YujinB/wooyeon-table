package com.wooyeon.table.domain.event.infrastructure.persistence;

import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EventSlotJpaRepository extends JpaRepository<EventSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from EventSlot s where s.id = :id")
    Optional<EventSlot> findByIdForUpdate(Long id);

    Optional<EventSlot> findByEventDate(LocalDate eventDate);

    List<EventSlot> findAllByIdIn(Collection<Long> ids);

    List<EventSlot> findAllByStatusAndMatchAtAfterOrderByEventDateAsc(EventSlotStatus status, LocalDateTime now);

    List<EventSlot> findAllByOrderByEventDateDesc();

    List<EventSlot> findAllByStatusAndMatchAtLessThanEqualOrderByEventDateAsc(EventSlotStatus status,
                                                                             LocalDateTime now);
}
