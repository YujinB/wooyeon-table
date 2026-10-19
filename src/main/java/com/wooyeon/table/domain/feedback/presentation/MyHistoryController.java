package com.wooyeon.table.domain.feedback.presentation;

import com.wooyeon.table.domain.feedback.application.dto.MyHistory;
import com.wooyeon.table.domain.feedback.application.service.HistoryService;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyHistoryController {

    private final HistoryService historyService;

    /** 지난 참가 날짜·지역·참석·후기 여부 (RET-01)와 다음 일정 추천 (RET-02) */
    @GetMapping("/api/app/me/history")
    public ApiResponse<MyHistory> history(@LoginUser AuthUser user) {
        return ApiResponse.ok(historyService.history(user.userId()));
    }
}
