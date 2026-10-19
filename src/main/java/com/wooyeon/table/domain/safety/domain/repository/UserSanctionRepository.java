package com.wooyeon.table.domain.safety.domain.repository;

import com.wooyeon.table.domain.safety.domain.model.UserSanction;
import java.util.List;

public interface UserSanctionRepository {

    UserSanction save(UserSanction sanction);

    /** 최근 것부터 */
    List<UserSanction> findAllByUserId(Long userId);
}
