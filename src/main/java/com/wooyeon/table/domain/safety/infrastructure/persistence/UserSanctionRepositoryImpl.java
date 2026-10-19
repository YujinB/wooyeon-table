package com.wooyeon.table.domain.safety.infrastructure.persistence;

import com.wooyeon.table.domain.safety.domain.model.UserSanction;
import com.wooyeon.table.domain.safety.domain.repository.UserSanctionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserSanctionRepositoryImpl implements UserSanctionRepository {

    private final UserSanctionJpaRepository jpaRepository;

    @Override
    public UserSanction save(UserSanction sanction) {
        return jpaRepository.save(sanction);
    }

    @Override
    public List<UserSanction> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserIdOrderByIdDesc(userId);
    }
}
