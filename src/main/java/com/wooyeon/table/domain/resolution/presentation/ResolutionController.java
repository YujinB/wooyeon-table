package com.wooyeon.table.domain.resolution.presentation;

import com.wooyeon.table.domain.resolution.application.dto.Alternatives;
import com.wooyeon.table.domain.resolution.application.service.ResolutionQueryService;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/applications/{applicationId}")
public class ResolutionController {

    private final ResolutionQueryService resolutionQueryService;

    /** 매칭 실패 사유·대체 회차(지역별 대기 인원)·해결 방식 (STA-04) */
    @GetMapping("/alternatives")
    public ApiResponse<Alternatives> alternatives(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(resolutionQueryService.getAlternatives(user.userId(), applicationId));
    }
}
