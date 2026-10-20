package com.wooyeon.table.domain.event.presentation;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.application.service.SlotDeletionService;
import com.wooyeon.table.domain.event.presentation.request.CreateSlotRequest;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final SlotDeletionService slotDeletionService;

    @GetMapping
    public ApiResponse<List<EventSlotInfo>> slots() {
        return ApiResponse.ok(eventSlotQueryService.findAllSlots());
    }

    /** 회차 만들기. 만들면 지난 참가자에게 모집 알림이 간다 */
    @PostMapping
    public ApiResponse<EventSlotInfo> create(@Valid @RequestBody CreateSlotRequest request) {
        Long slotId = eventSlotCommandService.create(request.eventDate(), request.startTime(), request.matchAt());
        return ApiResponse.ok(eventSlotQueryService.getSlot(slotId));
    }

    /** 잘못 만든 회차 지우기: 모집 중이고 신청자가 없을 때만. 보낸 모집 알림도 거둬들인다 */
    @DeleteMapping("/{slotId}")
    public ApiResponse<Void> delete(@PathVariable Long slotId) {
        slotDeletionService.delete(slotId);
        return ApiResponse.ok(null);
    }
}
