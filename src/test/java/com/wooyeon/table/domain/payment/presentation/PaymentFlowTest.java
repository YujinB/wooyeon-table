package com.wooyeon.table.domain.payment.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wooyeon.table.domain.application.application.dto.MatchCandidateApplication;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.payment.application.service.PaymentService;
import com.wooyeon.table.domain.payment.domain.model.PaymentRecordType;
import com.wooyeon.table.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** 모의 결제·멤버십·취소·자동 환불·상태와 일정 (PAY-01~03, STA-01·02) */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class PaymentFlowTest {

    private static final String PASSWORD = "wooyeon1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private ApplicationQueryService applicationQueryService;
    @Autowired
    private PaymentService paymentService;

    @Test
    void 결제에_실패하면_다시_결제할_수_있고_성공하면_매칭_후보가_된다() throws Exception {
        String token = loginWithProfile("demo1");
        LocalDate date = LocalDate.now().plusDays(20);
        Long slot = eventSlotCommandService.findOrCreate(date);
        long applicationId = submit(token, "SINGLE", slot);

        pay(token, applicationId, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.paymentStatus").value("FAILED"));
        myApplication(token, applicationId)
                .andExpect(jsonPath("$.data.status").value("PAYMENT_FAILED"));

        pay(token, applicationId, false)
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.type").value("CHARGE"))
                .andExpect(jsonPath("$.data.amount").value(9900))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
        assertThat(applicationQueryService.findMatchCandidates(slot))
                .extracting(MatchCandidateApplication::applicationId).contains(applicationId);

        myApplication(token, applicationId)
                .andExpect(jsonPath("$.data.status").value("MATCH_WAITING"))
                .andExpect(jsonPath("$.data.statusLabel").value("매칭 대기"))
                .andExpect(jsonPath("$.data.paymentStatusLabel").value("결제 완료"))
                .andExpect(jsonPath("$.data.schedule.eventDate").value(date.toString()))
                .andExpect(jsonPath("$.data.schedule.matchAnnounceAt")
                        .value(date.minusDays(1).atStartOfDay() + ":00"))
                .andExpect(jsonPath("$.data.schedule.reminderAt").value(date.minusDays(1) + "T12:00:00"))
                .andExpect(jsonPath("$.data.schedule.eventStartsAt").value(date + "T19:30:00"));

        pay(token, applicationId, false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_NOT_ALLOWED"));
    }

    @Test
    void 멤버십을_사면_기간_안의_다음_신청은_0원으로_처리된다() throws Exception {
        String token = loginWithProfile("demo1");
        long first = submit(token, "MEMBERSHIP_1M", eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(20)));
        long second = submit(token, "MEMBERSHIP_1M",
                eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(27)));

        pay(token, first, false)
                .andExpect(jsonPath("$.data.amount").value(35000))
                .andExpect(jsonPath("$.data.membershipExpiresOn").value(LocalDate.now().plusMonths(1).toString()));
        pay(token, second, false)
                .andExpect(jsonPath("$.data.type").value(PaymentRecordType.MEMBERSHIP_USE.name()))
                .andExpect(jsonPath("$.data.amount").value(0))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
    }

    @Test
    void 결제한_신청을_취소하면_자동_환불되고_그_멤버십도_끝난다() throws Exception {
        String token = loginWithProfile("demo2");
        long bought = submit(token, "MEMBERSHIP_1M",
                eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(21)));
        pay(token, bought, false);

        cancel(token, bought)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationStatus").value("CANCELED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refund.amount").value(35000));
        myApplication(token, bought)
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.schedule").doesNotExist());

        long next = submit(token, "MEMBERSHIP_1M", eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(28)));
        pay(token, next, false)
                .andExpect(jsonPath("$.data.type").value("CHARGE"))
                .andExpect(jsonPath("$.data.amount").value(35000));
    }

    @Test
    void 결제_전_취소는_환불_없이_취소되고_환불_마감이_지난_결제_신청은_취소할_수_없다() throws Exception {
        String token = loginWithProfile("demo3");
        long unpaid = submit(token, "SINGLE", eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(22)));
        cancel(token, unpaid)
                .andExpect(jsonPath("$.data.applicationStatus").value("CANCELED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.refund").doesNotExist());

        // 마감(매칭)은 내일 낮이지만 환불 마감(전날 0시 = 오늘 0시)은 이미 지난 회차
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long lateSlot = eventSlotCommandService.create(tomorrow, null, tomorrow.atTime(12, 0));
        long paid = submit(token, "SINGLE", lateSlot);
        pay(token, paid, false).andExpect(jsonPath("$.data.paymentStatus").value("PAID"));

        cancel(token, paid)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CANCEL_NOT_ALLOWED"));
    }

    @Test
    void 환불이_실패하면_REFUND_FAILED로_예외_처리함에_올라가고_운영자가_다시_환불할_수_있다() throws Exception {
        String token = loginWithProfile("demo1");
        long applicationId = submit(token, "SINGLE",
                eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(23)));
        pay(token, applicationId, false);

        assertThat(paymentService.refund(applicationId, true).success()).isFalse();
        assertThat(applicationQueryService.getApplication(applicationId).paymentStatus())
                .isEqualTo(PaymentStatus.REFUND_FAILED);

        String admin = login("admin");
        String refundItem = "$.data[?(@.type == 'REFUND' && @.refId == " + applicationId + ")]";
        mockMvc.perform(authed(get("/api/admin/exceptions"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath(refundItem).exists());
        mockMvc.perform(authed(post("/api/admin/applications/" + applicationId + "/refund/retry"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED"));
        mockMvc.perform(authed(get("/api/admin/exceptions"), admin))
                .andExpect(jsonPath(refundItem).doesNotExist());

        mockMvc.perform(authed(get("/api/app/policies/refund"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value(org.hamcrest.Matchers.containsString("화요일 24:00")))
                .andExpect(jsonPath("$.data.rules.length()").value(6));
    }

    @Test
    void 다른_참가자의_신청은_결제하거나_취소할_수_없다() throws Exception {
        String owner = loginWithProfile("demo2");
        String other = loginWithProfile("demo3");
        long applicationId = submit(owner, "SINGLE",
                eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(24)));

        pay(other, applicationId, false).andExpect(status().isNotFound());
        cancel(other, applicationId).andExpect(status().isNotFound());
    }

    private long submit(String token, String ticketType, Long slotId) throws Exception {
        JsonNode response = body(mockMvc.perform(authed(post("/api/app/applications"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticketType", ticketType, "regions", List.of("JAMSIL"),
                                "slotIds", List.of(slotId)))))
                .andExpect(status().isOk()));
        return response.at("/data/applicationId").asLong();
    }

    private ResultActions pay(String token, long applicationId, boolean simulateFailure) throws Exception {
        return mockMvc.perform(authed(post("/api/app/applications/" + applicationId + "/payment"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("simulateFailure", simulateFailure))));
    }

    private ResultActions cancel(String token, long applicationId) throws Exception {
        return mockMvc.perform(authed(post("/api/app/applications/" + applicationId + "/cancel"), token));
    }

    private ResultActions myApplication(String token, long applicationId) throws Exception {
        return mockMvc.perform(authed(get("/api/app/applications/" + applicationId), token));
    }

    private String login(String loginId) throws Exception {
        JsonNode login = body(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("loginId", loginId, "password", PASSWORD))))
                .andExpect(status().isOk()));
        return login.at("/data/token").asText();
    }

    private String loginWithProfile(String loginId) throws Exception {
        String token = login(loginId);
        mockMvc.perform(authed(put("/api/app/me/profile"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "체험", "phone", "01012345678", "birthYear", 1995,
                                "gender", "MALE", "interests", List.of("MUSIC"), "traits", List.of("CALM"),
                                "preferredTraits", List.of("STABLE"), "mealBudgets", List.of("OVER_30K")))))
                .andExpect(status().isOk());
        mockMvc.perform(authed(post("/api/app/me/consent"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("privacy", true, "policy", true))))
                .andExpect(status().isOk());
        return token;
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
