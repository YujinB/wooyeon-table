package com.wooyeon.table.domain.profile.application.service;

import com.wooyeon.table.domain.profile.application.dto.MatchingProfile;
import com.wooyeon.table.domain.profile.domain.repository.ProfileRepository;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileQueryService {

    private final ProfileRepository profileRepository;

    /** 사용자 id → 매칭용 프로필. 프로필이 없는 사용자는 결과에 없다 */
    public Map<Long, MatchingProfile> findMatchingProfiles(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findAllByUserIdIn(userIds).stream()
                .map(MatchingProfile::from)
                .collect(Collectors.toMap(MatchingProfile::userId, Function.identity()));
    }
}
