package com.wooyeon.table.domain.feedback.application.dto;

/** 후기 작성 화면: 작성 가능 여부와 이미 낸 후기 */
public record FeedbackForm(Long applicationId, boolean submittable, String unavailableReason, boolean submitted,
                           FeedbackCommand mine) {
}
