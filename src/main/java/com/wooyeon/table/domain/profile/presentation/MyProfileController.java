package com.wooyeon.table.domain.profile.presentation;

import com.wooyeon.table.domain.profile.application.dto.MyProfile;
import com.wooyeon.table.domain.profile.application.service.MyProfileService;
import com.wooyeon.table.domain.profile.presentation.request.UpdateMyProfileRequest;
import com.wooyeon.table.domain.profile.presentation.response.ProfileOptionsResponse;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app")
public class MyProfileController {

    private final MyProfileService myProfileService;

    @GetMapping("/me/profile")
    public ApiResponse<MyProfile> myProfile(@LoginUser AuthUser user) {
        return ApiResponse.ok(myProfileService.get(user.userId()));
    }

    /** 기본 정보·매칭 프로필·식사 조건을 한 번에 저장 (ACC-02~04) */
    @PutMapping("/me/profile")
    public ApiResponse<MyProfile> saveMyProfile(@LoginUser AuthUser user,
                                                @Valid @RequestBody UpdateMyProfileRequest request) {
        return ApiResponse.ok(myProfileService.save(user.userId(), request.toCommand()));
    }

    @GetMapping("/profile-options")
    public ApiResponse<ProfileOptionsResponse> options() {
        return ApiResponse.ok(ProfileOptionsResponse.create());
    }
}
