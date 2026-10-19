package com.wooyeon.table.domain.feedback.application.dto;

import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 참가 이력 (RET-01)과 다음 일정 추천 (RET-02) */
public record MyHistory(List<Entry> history, List<Recommendation> recommendations) {

    public record Entry(Long applicationId, LocalDate eventDate, String region, int tableSize,
                        AttendanceStatus attendance, String attendanceLabel, boolean feedbackSubmitted) {
    }

    public record Recommendation(Long slotId, LocalDate eventDate, LocalDateTime applyDeadline, String region,
                                 String reason) {
    }
}
