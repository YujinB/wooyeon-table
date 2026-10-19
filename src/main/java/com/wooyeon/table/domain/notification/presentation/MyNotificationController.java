package com.wooyeon.table.domain.notification.presentation;

import com.wooyeon.table.domain.notification.application.dto.MyNotifications;
import com.wooyeon.table.domain.notification.application.service.NotificationQueryService;
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
@RequestMapping("/api/app/notifications")
public class MyNotificationController {

    private final NotificationQueryService notificationQueryService;

    /** 앱 내 알림함 + 안 읽은 수 */
    @GetMapping
    public ApiResponse<MyNotifications> mine(@LoginUser AuthUser user) {
        return ApiResponse.ok(notificationQueryService.findMine(user.userId()));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> read(@LoginUser AuthUser user, @PathVariable Long notificationId) {
        notificationQueryService.markRead(user.userId(), notificationId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> readAll(@LoginUser AuthUser user) {
        notificationQueryService.markAllRead(user.userId());
        return ApiResponse.ok(null);
    }
}
