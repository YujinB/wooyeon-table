package com.wooyeon.table.domain.profile.infrastructure.persistence;

import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.repository.ProfileRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepository {

    private final ProfileJpaRepository jpaRepository;

    @Override
    public Profile save(Profile profile) {
        return jpaRepository.save(profile);
    }

    @Override
    public Optional<Profile> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public List<Profile> findAllByUserIdIn(Collection<Long> userIds) {
        return jpaRepository.findAllByUserIdIn(userIds);
    }
}
