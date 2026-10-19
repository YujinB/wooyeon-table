package com.wooyeon.table.domain.user.infrastructure.persistence;

import com.wooyeon.table.domain.user.domain.model.User;
import com.wooyeon.table.domain.user.domain.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        return jpaRepository.findByLoginId(loginId);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return jpaRepository.existsByLoginId(loginId);
    }

    @Override
    public List<User> findAllByIdIn(Collection<Long> ids) {
        return jpaRepository.findAllByIdIn(ids);
    }
}
