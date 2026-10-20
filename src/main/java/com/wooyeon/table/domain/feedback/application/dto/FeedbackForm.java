package com.wooyeon.table.domain.feedback.application.dto;

import java.util.List;

/**
 * 후기 작성 화면: 작성 가능 여부, 고를 수 있는 같은 테이블 사람(별칭·소개만), 이미 낸 후기.
 * 다른 사람이 나를 어떻게 골랐는지는 담지 않는다
 */
public record FeedbackForm(Long applicationId, boolean submittable, String unavailableReason, boolean submitted,
                           List<OtherMember> others, FeedbackCommand mine) {

    public record OtherMember(int seatNo, String alias, String intro) {
    }
}
