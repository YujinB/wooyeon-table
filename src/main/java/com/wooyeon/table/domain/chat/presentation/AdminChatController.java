package com.wooyeon.table.domain.chat.presentation;

import com.wooyeon.table.domain.chat.application.dto.AdminChatRoomView;
import com.wooyeon.table.domain.chat.application.service.AdminChatQueryService;
import com.wooyeon.table.domain.chat.application.service.TableContentService;
import com.wooyeon.table.domain.chat.domain.service.TableContentGenerator.TableContent;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tables/{tableId}")
public class AdminChatController {

    private final AdminChatQueryService adminChatQueryService;
    private final TableContentService tableContentService;

    /** 테이블 대화 콘텐츠 미리 보기 */
    @GetMapping("/content")
    public ApiResponse<TableContent> content(@PathVariable Long tableId) {
        return ApiResponse.ok(tableContentService.forTable(tableId));
    }

    /** 테이블 단체방: 생성 여부, 멤버, 예약된 것 포함 전체 시스템 공지·대화 */
    @GetMapping("/chat")
    public ApiResponse<AdminChatRoomView> chat(@PathVariable Long tableId) {
        return ApiResponse.ok(adminChatQueryService.findByTable(tableId));
    }
}
