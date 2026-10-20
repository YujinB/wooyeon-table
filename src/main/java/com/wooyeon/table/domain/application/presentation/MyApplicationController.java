package com.wooyeon.table.domain.application.presentation;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationCommandService;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.presentation.request.SubmitApplicationRequest;
import com.wooyeon.table.domain.application.presentation.response.MyApplicationResponse;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/applications")
public class MyApplicationController {

    private final ApplicationCommandService applicationCommandService;
    private final ApplicationQueryService applicationQueryService;
    private final EventSlotQueryService eventSlotQueryService;

    /** 신청 제출 → 결제 대기 (APP-02~04) */
    @PostMapping
    public ApiResponse<MyApplicationResponse> submit(@LoginUser AuthUser user,
                                                     @Valid @RequestBody SubmitApplicationRequest request) {
        Long applicationId = applicationCommandService.submit(user.userId(), request.toCommand());
        return ApiResponse.ok(toResponse(List.of(
                applicationQueryService.getOwnedApplication(user.userId(), applicationId))).get(0));
    }

    @GetMapping
    public ApiResponse<List<MyApplicationResponse>> myApplications(@LoginUser AuthUser user) {
        return ApiResponse.ok(toResponse(applicationQueryService.findMyApplications(user.userId())));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<MyApplicationResponse> myApplication(@LoginUser AuthUser user,
                                                            @PathVariable Long applicationId) {
        return ApiResponse.ok(toResponse(List.of(
                applicationQueryService.getOwnedApplication(user.userId(), applicationId))).get(0));
    }

    private List<MyApplicationResponse> toResponse(List<ApplicationInfo> applications) {
        List<Long> slotIds = applications.stream()
                .flatMap(application -> Stream.concat(application.preferredSlotIds().stream(),
                        Stream.ofNullable(application.confirmedSlotId())))
                .distinct()
                .toList();
        Map<Long, EventSlotInfo> slots = eventSlotQueryService.findSlots(slotIds).stream()
                .collect(Collectors.toMap(EventSlotInfo::id, Function.identity()));
        return applications.stream().map(application -> MyApplicationResponse.of(application, slots)).toList();
    }
}
