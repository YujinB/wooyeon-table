package com.wooyeon.table.domain.chat.presentation;

import com.wooyeon.table.domain.chat.application.dto.ChatRoomView;
import com.wooyeon.table.domain.chat.application.service.ChatService;
import com.wooyeon.table.domain.chat.presentation.request.SendMessageRequest;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/chat-rooms")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ApiResponse<List<ChatRoomView.Summary>> myRooms(@LoginUser AuthUser user) {
        return ApiResponse.ok(chatService.myRooms(user.userId()));
    }

    /** 단체방: 멤버(별칭·제한된 소개)와 지금 보이는 메시지. 화면은 몇 초마다 다시 불러온다 */
    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomView> room(@LoginUser AuthUser user, @PathVariable Long roomId) {
        return ApiResponse.ok(chatService.view(user.userId(), roomId));
    }

    @PostMapping("/{roomId}/messages")
    public ApiResponse<ChatRoomView> send(@LoginUser AuthUser user, @PathVariable Long roomId,
                                          @Valid @RequestBody SendMessageRequest request) {
        chatService.send(user.userId(), roomId, request.content());
        return ApiResponse.ok(chatService.view(user.userId(), roomId));
    }
}
