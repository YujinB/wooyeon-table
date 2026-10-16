package com.wooyeon.table.domain.profile.domain.repository;

import com.wooyeon.table.domain.profile.domain.model.Profile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProfileRepository {

    Profile save(Profile profile);

    Optional<Profile> findByUserId(Long userId);

    List<Profile> findAllByUserIdIn(Collection<Long> userIds);
}
