package com.wooyeon.table.domain.profile.application.service;

import com.wooyeon.table.domain.profile.application.dto.MatchingProfile;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.repository.ProfileRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
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

    /** 사용자 id → 희망 식사 예산 (식당 가격대 고르기용) */
    public Map<Long, Set<MealBudget>> findMealBudgets(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, profile -> Set.copyOf(profile.getMealBudgets())));
    }

    /** 신청할 수 있을 만큼 프로필을 채웠는지 (APP-03 필수 항목 확인) */
    public boolean isCompleteForApplication(Long userId) {
        return profileRepository.findByUserId(userId).map(Profile::isCompleteForApplication).orElse(false);
    }

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
