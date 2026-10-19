package com.wooyeon.table.domain.payment.infrastructure.persistence;

import com.wooyeon.table.domain.payment.domain.model.Membership;
import com.wooyeon.table.domain.payment.domain.repository.MembershipRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MembershipRepositoryImpl implements MembershipRepository {

    private final MembershipJpaRepository jpaRepository;

    @Override
    public Membership save(Membership membership) {
        return jpaRepository.save(membership);
    }

    @Override
    public List<Membership> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserIdOrderByIdDesc(userId);
    }

    @Override
    public Optional<Membership> findBySourceApplicationId(Long applicationId) {
        return jpaRepository.findBySourceApplicationId(applicationId);
    }
}
