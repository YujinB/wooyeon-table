package com.wooyeon.table.domain.feedback.presentation;

import com.wooyeon.table.domain.feedback.application.dto.SlotFeedbackSummary;
import com.wooyeon.table.domain.feedback.application.service.FeedbackSummaryService;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminFeedbackController {

    private final FeedbackSummaryService feedbackSummaryService;

    /** 회차 후기 요약: 항목별·테이블별 평균, 순추천지수, 한마디 (사람별 선호는 비공개) */
    @GetMapping("/slots/{slotId}/feedback")
    public ApiResponse<SlotFeedbackSummary> slotFeedback(@PathVariable Long slotId) {
        return ApiResponse.ok(feedbackSummaryService.summarize(slotId));
    }
}
