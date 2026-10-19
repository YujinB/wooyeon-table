package com.wooyeon.table.domain.feedback.application.dto;

import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import java.time.LocalDateTime;

public record AttendanceView(Long attendanceId, Long applicationId, Long userId, Long tableId, Long slotId,
                             AttendanceStatus status, String statusLabel, LocalDateTime checkedInAt,
                             LocalDateTime canceledAt, LocalDateTime reviewedAt) {

    public static AttendanceView from(Attendance attendance) {
        return new AttendanceView(attendance.getId(), attendance.getApplicationId(), attendance.getUserId(),
                attendance.getTableId(), attendance.getSlotId(), attendance.getStatus(),
                attendance.getStatus().getLabel(), attendance.getCheckedInAt(), attendance.getCanceledAt(),
                attendance.getReviewedAt());
    }
}
