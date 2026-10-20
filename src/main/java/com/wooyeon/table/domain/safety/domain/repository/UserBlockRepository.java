package com.wooyeon.table.domain.safety.domain.repository;

import com.wooyeon.table.domain.safety.domain.model.UserBlock;
import java.util.Collection;
import java.util.List;

public interface UserBlockRepository {

    UserBlock save(UserBlock block);

    boolean exists(Long blockerUserId, Long blockedUserId);

    /** 두 사람 모두 이 집합에 속한 차단 (매칭 제외 관계) */
    List<UserBlock> findAmong(Collection<Long> userIds);
}
