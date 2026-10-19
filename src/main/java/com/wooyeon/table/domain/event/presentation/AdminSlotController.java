package com.wooyeon.table.domain.event.presentation;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.presentation.request.CreateSlotRequest;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/slots")
public class AdminSlotController {

    private final EventSlotCommandService eventSlotCommandService;
    private final EventSlotQueryService eventSlotQueryService;

    @GetMapping
    public ApiResponse<List<EventSlotInfo>> slots() {
        return ApiResponse.ok(eventSlotQueryService.findAllSlots());
    }

    /** 데모 회차 생성 (가상데이터 날짜는 지난 날짜라 신청을 받으려면 새 회차가 필요) */
    @PostMapping
    public ApiResponse<EventSlotInfo> create(@Valid @RequestBody CreateSlotRequest request) {
        Long slotId = eventSlotCommandService.create(request.eventDate(), request.startTime(), request.matchAt());
        return ApiResponse.ok(eventSlotQueryService.getSlot(slotId));
    }
}
