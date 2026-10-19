package com.wooyeon.table.domain.profile.infrastructure.persistence;

import com.wooyeon.table.domain.profile.domain.model.Profile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileJpaRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    List<Profile> findAllByUserIdIn(Collection<Long> userIds);
}
