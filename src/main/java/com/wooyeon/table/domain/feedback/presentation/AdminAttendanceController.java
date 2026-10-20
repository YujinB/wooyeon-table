package com.wooyeon.table.domain.feedback.presentation;

import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.application.dto.EventCloseResult;
import com.wooyeon.table.domain.feedback.application.service.EventCloseService;
import com.wooyeon.table.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAttendanceController {

    private final EventCloseService eventCloseService;

    /** 데모용 "행사 종료 처리": 미체크인 → 노쇼 후보, 피드백 요청 알림 */
    @PostMapping("/slots/{slotId}/close")
    public ApiResponse<EventCloseResult> close(@PathVariable Long slotId) {
        return ApiResponse.ok(eventCloseService.close(slotId));
    }

    @GetMapping("/slots/{slotId}/attendances")
    public ApiResponse<List<AttendanceView>> attendances(@PathVariable Long slotId) {
        return ApiResponse.ok(eventCloseService.slotAttendances(slotId));
    }

    /** 운영자가 확인할 노쇼 후보만 */
    @GetMapping("/no-show-candidates")
    public ApiResponse<List<AttendanceView>> noShowCandidates() {
        return ApiResponse.ok(eventCloseService.noShowCandidates());
    }

    /** noShow=true 노쇼 확정, false 참석으로 정정 */
    @PostMapping("/attendances/{attendanceId}/review")
    public ApiResponse<AttendanceView> review(@PathVariable Long attendanceId, @RequestParam boolean noShow) {
        return ApiResponse.ok(eventCloseService.review(attendanceId, noShow));
    }
}
