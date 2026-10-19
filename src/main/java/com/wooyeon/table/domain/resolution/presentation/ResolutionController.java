package com.wooyeon.table.domain.resolution.presentation;

import com.wooyeon.table.domain.resolution.application.dto.Alternatives;
import com.wooyeon.table.domain.resolution.application.dto.ResolutionResult;
import com.wooyeon.table.domain.resolution.application.service.ResolutionQueryService;
import com.wooyeon.table.domain.resolution.application.service.ResolutionService;
import com.wooyeon.table.domain.resolution.presentation.request.ResolveRequest;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/applications/{applicationId}")
public class ResolutionController {

    private final ResolutionQueryService resolutionQueryService;
    private final ResolutionService resolutionService;

    /** 매칭 실패 사유·대체 회차(지역별 대기 인원)·해결 방식 (STA-04) */
    @GetMapping("/alternatives")
    public ApiResponse<Alternatives> alternatives(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(resolutionQueryService.getAlternatives(user.userId(), applicationId));
    }

    /** 추천 일정 이동·이용권 보관·환불 중 하나를 골라 바로 처리 (STA-05·06, PAY-03). 신청당 한 번 */
    @PostMapping("/resolution")
    public ApiResponse<ResolutionResult> resolve(@LoginUser AuthUser user, @PathVariable Long applicationId,
                                                 @Valid @RequestBody ResolveRequest request) {
        return ApiResponse.ok(resolutionService.resolve(user.userId(), applicationId, request.choice(),
                request.slotId(), request.regions()));
    }
}
