package com.wooyeon.table.domain.application.infrastructure.persistence;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.ApplicationStatus;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationJpaRepository extends JpaRepository<Application, Long> {

    List<Application> findAllByIdIn(Collection<Long> ids);

    @Query("""
            select distinct a from Application a join a.preferredSlotIds slotId
            where slotId = :slotId
              and a.applicationStatus = :applicationStatus
              and a.paymentStatus = :paymentStatus
              and a.matchStatus = :matchStatus
            order by a.id
            """)
    List<Application> findBySlotAndStatuses(Long slotId, ApplicationStatus applicationStatus,
                                            PaymentStatus paymentStatus, MatchStatus matchStatus);

    List<Application> findAllByApplicationStatusNotOrderByIdDesc(ApplicationStatus applicationStatus);

    List<Application> findAllByPaymentStatusOrderByIdDesc(PaymentStatus paymentStatus);

    boolean existsByUserId(Long userId);

    List<Application> findAllByUserIdOrderByIdDesc(Long userId);
}
