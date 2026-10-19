package com.wooyeon.table.domain.feedback.presentation.request;

import com.wooyeon.table.domain.feedback.application.dto.FeedbackCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * 만족도 1~5점 (FDB-02), 재참여 의향 1~5점·추천 의향 0~10점 (FDB-03),
 * 다시 만나고 싶은/싶지 않은 사람의 좌석 번호 (FDB-04, 비공개, 선택)
 */
public record SubmitFeedbackRequest(
        @NotNull @Min(1) @Max(5) Integer tableMood,
        @NotNull @Min(1) @Max(5) Integer conversation,
        @NotNull @Min(1) @Max(5) Integer venue,
        @NotNull @Min(1) @Max(5) Integer rejoinIntent,
        @NotNull @Min(0) @Max(10) Integer recommendScore,
        @Size(max = 500) String comment,
        Set<Integer> wantAgainSeats,
        Set<Integer> avoidSeats) {

    public FeedbackCommand toCommand() {
        return new FeedbackCommand(tableMood, conversation, venue, rejoinIntent, recommendScore,
                comment == null || comment.isBlank() ? null : comment.strip(),
                wantAgainSeats == null ? Set.of() : Set.copyOf(wantAgainSeats),
                avoidSeats == null ? Set.of() : Set.copyOf(avoidSeats));
    }
}
