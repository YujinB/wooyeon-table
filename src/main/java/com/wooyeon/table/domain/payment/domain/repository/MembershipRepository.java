package com.wooyeon.table.domain.payment.domain.repository;

import com.wooyeon.table.domain.payment.domain.model.Membership;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository {

    Membership save(Membership membership);

    List<Membership> findAllByUserId(Long userId);

    Optional<Membership> findBySourceApplicationId(Long applicationId);
}
