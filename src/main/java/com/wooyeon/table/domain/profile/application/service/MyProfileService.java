package com.wooyeon.table.domain.profile.application.service;

import com.wooyeon.table.domain.profile.application.dto.MyProfile;
import com.wooyeon.table.domain.profile.application.dto.MyProfileCommand;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.repository.ProfileRepository;
import com.wooyeon.table.domain.user.application.dto.UserInfo;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.domain.user.application.service.UserQueryService;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 참가자 본인 프로필 조회·저장 (ACC-02~04). 한 번 저장하면 다음 신청에도 그대로 쓴다 (ACC-06) */
@Service
@RequiredArgsConstructor
@Transactional
public class MyProfileService {

    private final ProfileRepository profileRepository;
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    @Transactional(readOnly = true)
    public MyProfile get(Long userId) {
        UserInfo user = userQueryService.getUser(userId);
        Optional<Profile> profile = profileRepository.findByUserId(userId);
        if (profile.isEmpty()) {
            return new MyProfile(user.loginId(), user.name(), user.phone(), null, null, null, null,
                    Set.of(), Set.of(), Set.of(), Set.of(), null, false);
        }
        Profile p = profile.get();
        return new MyProfile(user.loginId(), user.name(), user.phone(), p.getBirthYear(), p.getGender(), p.getJob(),
                p.getMbti(), Set.copyOf(p.getInterests()), Set.copyOf(p.getTraits()),
                Set.copyOf(p.getPreferredTraits()), Set.copyOf(p.getMealBudgets()), p.getFoodRestriction(),
                p.isCompleteForApplication());
    }

    public MyProfile save(Long userId, MyProfileCommand command) {
        userCommandService.updateBasicInfo(userId, command.name(), command.phone());
        profileRepository.findByUserId(userId).ifPresentOrElse(
                profile -> profile.update(command.birthYear(), command.gender(), command.mbti(), command.job(),
                        command.interests(), command.traits(), command.preferredTraits(), command.mealBudgets(),
                        command.foodRestriction()),
                () -> profileRepository.save(Profile.builder()
                        .userId(userId)
                        .birthYear(command.birthYear())
                        .gender(command.gender())
                        .mbti(command.mbti())
                        .job(command.job())
                        .interests(command.interests())
                        .traits(command.traits())
                        .preferredTraits(command.preferredTraits())
                        .mealBudgets(command.mealBudgets())
                        .foodRestriction(command.foodRestriction())
                        .build()));
        return get(userId);
    }
}
