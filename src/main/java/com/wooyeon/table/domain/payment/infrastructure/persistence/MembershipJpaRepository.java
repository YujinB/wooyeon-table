package com.wooyeon.table.domain.payment.infrastructure.persistence;

import com.wooyeon.table.domain.payment.domain.model.Membership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<Membership, Long> {

    List<Membership> findAllByUserIdOrderByIdDesc(Long userId);

    Optional<Membership> findBySourceApplicationId(Long sourceApplicationId);
}
