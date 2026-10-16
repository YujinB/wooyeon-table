package com.wooyeon.table.domain.user.domain.repository;

import com.wooyeon.table.domain.user.domain.model.User;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
