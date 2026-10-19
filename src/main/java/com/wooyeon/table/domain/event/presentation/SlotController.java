package com.wooyeon.table.domain.event.presentation;

import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.presentation.response.OpenSlotResponse;
import com.wooyeon.table.global.response.ApiResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/slots")
public class SlotController {

    private final EventSlotQueryService eventSlotQueryService;
    private final Clock clock;

    /** 지금 신청할 수 있는 회차 (모집 중 + 마감 전, 날짜순) */
    @GetMapping
    public ApiResponse<List<OpenSlotResponse>> openSlots() {
        return ApiResponse.ok(eventSlotQueryService.findOpenSlots(LocalDateTime.now(clock)).stream()
                .map(OpenSlotResponse::from)
                .toList());
    }
}
