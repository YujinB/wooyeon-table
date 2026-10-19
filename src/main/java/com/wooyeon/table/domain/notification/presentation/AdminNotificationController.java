package com.wooyeon.table.domain.notification.presentation;

import com.wooyeon.table.domain.notification.application.dto.NotificationFailure;
import com.wooyeon.table.domain.notification.application.service.NotificationDispatcher;
import com.wooyeon.table.domain.notification.application.service.NotificationDispatcher.DispatchResult;
import com.wooyeon.table.domain.notification.infrastructure.push.MockPushGateway;
import com.wooyeon.table.global.response.ApiResponse;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationDispatcher notificationDispatcher;
    private final MockPushGateway mockPushGateway;

    /** 재시도까지 모두 실패한 알림 (INF-08 운영자 예외) */
    @GetMapping("/failures")
    public ApiResponse<List<NotificationFailure>> failures() {
        return ApiResponse.ok(notificationDispatcher.findGaveUp().stream().map(NotificationFailure::from).toList());
    }

    @PostMapping("/{notificationId}/resend")
    public ApiResponse<NotificationFailure> resend(@PathVariable Long notificationId) {
        return ApiResponse.ok(NotificationFailure.from(notificationDispatcher.resend(notificationId)));
    }

    /** 데모용 "지금 발송 실행": 예약 시각·재시도 시각이 지난 알림을 바로 보낸다 */
    @PostMapping("/dispatch")
    public ApiResponse<DispatchResult> dispatch() {
        return ApiResponse.ok(notificationDispatcher.dispatchDue());
    }

    /** 데모용 모의 푸시 장애: 이 사용자에게 가는 푸시를 실패시키거나 되돌린다 */
    @PostMapping("/mock-push-failure")
    public ApiResponse<Set<Long>> mockPushFailure(@RequestParam Long userId, @RequestParam boolean failing) {
        mockPushGateway.failFor(userId, failing);
        return ApiResponse.ok(mockPushGateway.failingUserIds());
    }
}
