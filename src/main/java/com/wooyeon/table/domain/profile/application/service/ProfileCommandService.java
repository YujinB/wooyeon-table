package com.wooyeon.table.domain.profile.application.service;

import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileCommandService {

    private final ProfileRepository profileRepository;

    /** 이미 프로필이 있으면 그대로 둔다 (적재 재실행 대비) */
    public void createIfAbsent(Profile profile) {
        if (profileRepository.findByUserId(profile.getUserId()).isEmpty()) {
            profileRepository.save(profile);
        }
    }
}
