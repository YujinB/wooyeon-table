package com.wooyeon.table.domain.user.infrastructure.persistence;

import com.wooyeon.table.domain.user.domain.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<User> findAllByIdIn(Collection<Long> ids);
}
