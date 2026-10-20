package com.wooyeon.table.domain.safety.infrastructure.persistence;

import com.wooyeon.table.domain.safety.domain.model.UserBlock;
import com.wooyeon.table.domain.safety.domain.repository.UserBlockRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserBlockRepositoryImpl implements UserBlockRepository {

    private final UserBlockJpaRepository jpaRepository;

    @Override
    public UserBlock save(UserBlock block) {
        return jpaRepository.save(block);
    }

    @Override
    public boolean exists(Long blockerUserId, Long blockedUserId) {
        return jpaRepository.existsByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId);
    }

    @Override
    public List<UserBlock> findAmong(Collection<Long> userIds) {
        return userIds.isEmpty() ? List.of()
                : jpaRepository.findAllByBlockerUserIdInAndBlockedUserIdIn(userIds, userIds);
    }
}
