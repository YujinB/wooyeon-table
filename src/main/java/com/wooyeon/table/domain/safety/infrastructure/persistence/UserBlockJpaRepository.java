package com.wooyeon.table.domain.safety.infrastructure.persistence;

import com.wooyeon.table.domain.safety.domain.model.UserBlock;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockJpaRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerUserIdAndBlockedUserId(Long blockerUserId, Long blockedUserId);

    List<UserBlock> findAllByBlockerUserIdInAndBlockedUserIdIn(Collection<Long> blockers, Collection<Long> blocked);
}
