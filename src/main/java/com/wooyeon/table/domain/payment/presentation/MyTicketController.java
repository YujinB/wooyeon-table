package com.wooyeon.table.domain.payment.presentation;

import com.wooyeon.table.domain.payment.application.dto.MyTickets;
import com.wooyeon.table.domain.payment.application.service.MyTicketService;
import com.wooyeon.table.global.auth.AuthUser;
import com.wooyeon.table.global.auth.LoginUser;
import com.wooyeon.table.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 내 이용권: 멤버십 남은 기간·사용 횟수, 보관한 1회권 (RET-03) */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/me/tickets")
public class MyTicketController {

    private final MyTicketService myTicketService;

    @GetMapping
    public ApiResponse<MyTickets> tickets(@LoginUser AuthUser user) {
        return ApiResponse.ok(myTicketService.getMyTickets(user.userId()));
    }
}
