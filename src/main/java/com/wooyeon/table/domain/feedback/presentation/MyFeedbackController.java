package com.wooyeon.table.domain.feedback.presentation;

import com.wooyeon.table.domain.feedback.application.dto.FeedbackForm;
import com.wooyeon.table.domain.feedback.application.service.FeedbackService;
import com.wooyeon.table.domain.feedback.presentation.request.SubmitFeedbackRequest;
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
@RequestMapping("/api/app/applications/{applicationId}/feedback")
public class MyFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ApiResponse<FeedbackForm> form(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(feedbackService.form(user.userId(), applicationId));
    }

    @PostMapping
    public ApiResponse<FeedbackForm> submit(@LoginUser AuthUser user, @PathVariable Long applicationId,
                                            @Valid @RequestBody SubmitFeedbackRequest request) {
        feedbackService.submit(user.userId(), applicationId, request.toCommand());
        return ApiResponse.ok(feedbackService.form(user.userId(), applicationId));
    }
}
