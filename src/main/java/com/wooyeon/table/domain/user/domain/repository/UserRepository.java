package com.wooyeon.table.domain.user.domain.repository;

import com.wooyeon.table.domain.user.domain.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<User> findAllByIdIn(Collection<Long> ids);
}
