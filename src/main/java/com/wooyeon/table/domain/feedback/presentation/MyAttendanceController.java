package com.wooyeon.table.domain.feedback.presentation;

import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.application.service.AttendanceService;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/applications/{applicationId}/attendance")
public class MyAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public ApiResponse<AttendanceView> attendance(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(attendanceService.get(user.userId(), applicationId));
    }

    /** 앱 체크인 → 참석 완료 (FDB-01) */
    @PostMapping("/check-in")
    public ApiResponse<AttendanceView> checkIn(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(attendanceService.checkIn(user.userId(), applicationId));
    }

    /** 확정 후 취소 (환불 없음, 제한 시점 이후 취소로 기록) */
    @PostMapping("/cancel")
    public ApiResponse<AttendanceView> cancel(@LoginUser AuthUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(attendanceService.cancel(user.userId(), applicationId));
    }
}
