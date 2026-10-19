package com.wooyeon.table.domain.resolution.presentation;

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
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.domain.payment.domain.model.Membership;
import com.wooyeon.table.domain.payment.domain.model.PaymentRecord;
import com.wooyeon.table.domain.payment.domain.repository.MembershipRepository;
import com.wooyeon.table.domain.payment.domain.repository.PaymentRecordRepository;
import com.wooyeon.table.support.IntegrationTest;
import java.time.LocalDate;
import java.util.HashMap;
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

/** 매칭 실패 → 사유·대체 회차 → 일정 이동·이용권 보관·환불, 멤버십 연장, 중복 처리 방지 (STA-04~06, PAY-03) */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class ResolutionFlowTest {

    private static final String PASSWORD = "wooyeon1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private MatchingService matchingService;
    @Autowired
    private ApplicationQueryService applicationQueryService;
    @Autowired
    private PaymentRecordRepository paymentRecordRepository;
    @Autowired
    private MembershipRepository membershipRepository;

    private final LocalDate failedDate = LocalDate.now().plusDays(40);
    private final LocalDate otherDate = LocalDate.now().plusDays(47);

    @Test
    void 실패한_신청은_사유와_대기_인원이_많은_지역부터_추천된_대체_회차를_본다() throws Exception {
        String token = loginWithProfile("demo1");
        String other = loginWithProfile("demo2");
        Long failedSlot = eventSlotCommandService.findOrCreate(failedDate);
        Long otherSlot = eventSlotCommandService.findOrCreate(otherDate);
        long waitingThere = submitAndPay(other, "SINGLE", otherSlot, "EULJIRO_JONGNO");
        long failed = failAlone(token, "SINGLE", failedSlot);

        alternatives(token, failed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failure.eventDate").value(failedDate.toString()))
                .andExpect(jsonPath("$.data.failure.reason").isNotEmpty())
                .andExpect(jsonPath("$.data.slots[?(@.slotId == " + otherSlot + ")].recommendedRegions[0].code")
                        .value("EULJIRO_JONGNO"))
                .andExpect(jsonPath("$.data.choices[0].choice").value("MOVE_SCHEDULE"))
                .andExpect(jsonPath("$.data.choices[0].available").value(true))
                .andExpect(jsonPath("$.data.choices[2].available").value(true));

        alternatives(token, waitingThere).andExpect(status().isNotFound());
        alternatives(other, waitingThere)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESOLUTION_NOT_AVAILABLE"));
    }

    @Test
    void 일정_이동은_새_결제_없이_새_회차_매칭_대기가_되고_두_번_처리되지_않는다() throws Exception {
        String token = loginWithProfile("demo1");
        Long failedSlot = eventSlotCommandService.findOrCreate(failedDate);
        Long otherSlot = eventSlotCommandService.findOrCreate(otherDate);
        long failed = failAlone(token, "SINGLE", failedSlot);

        resolve(token, failed, Map.of("choice", "MOVE_SCHEDULE", "slotId", otherSlot,
                "regions", List.of("GANGNAM_SINNONHYEON")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.movedToEventDate").value(otherDate.toString()));

        assertThat(applicationQueryService.findMatchCandidates(otherSlot))
                .extracting(MatchCandidateApplication::applicationId).contains(failed);
        assertThat(paymentRecordRepository.findAllByApplicationId(failed))
                .as("이동해도 결제는 처음 한 번뿐").hasSize(1);

        resolve(token, failed, Map.of("choice", "REFUND"))
                .andExpect(status().isConflict());
        myApplication(token, failed)
                .andExpect(jsonPath("$.data.status").value("MATCH_WAITING"))
                .andExpect(jsonPath("$.data.regions[0]").value("강남/신논현"));
    }

    @Test
    void 보관한_1회권은_다음_1회권_신청에_한_번_0원으로_쓰인다() throws Exception {
        String token = loginWithProfile("demo1");
        long failed = failAlone(token, "SINGLE", eventSlotCommandService.findOrCreate(failedDate));

        resolve(token, failed, Map.of("choice", "KEEP_TICKET"))
                .andExpect(jsonPath("$.data.matchStatus").value("NO_MATCH"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
        myApplication(token, failed).andExpect(jsonPath("$.data.status").value("TICKET_KEPT"));

        long next = submit(token, "SINGLE", eventSlotCommandService.findOrCreate(otherDate), "JAMSIL");
        pay(token, next)
                .andExpect(jsonPath("$.data.type").value("TICKET_CREDIT_USE"))
                .andExpect(jsonPath("$.data.amount").value(0));
        long third = submit(token, "SINGLE", eventSlotCommandService.findOrCreate(otherDate.plusDays(7)), "JAMSIL");
        pay(token, third)
                .andExpect(jsonPath("$.data.type").value("CHARGE"))
                .andExpect(jsonPath("$.data.amount").value(9900));
    }

    @Test
    void 환불을_고르면_운영자_승인_없이_환불_완료된다() throws Exception {
        String token = loginWithProfile("demo3");
        long failed = failAlone(token, "SINGLE", eventSlotCommandService.findOrCreate(failedDate));

        resolve(token, failed, Map.of("choice", "REFUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("NO_MATCH"))
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refund.amount").value(9900));
        myApplication(token, failed).andExpect(jsonPath("$.data.status").value("REFUNDED"));
        assertThat(paymentRecordRepository.findAllByApplicationId(failed))
                .extracting(PaymentRecord::getType).extracting(Enum::name).containsExactly("CHARGE", "REFUND");
    }

    @Test
    void 멤버십으로_신청했다_실패하면_기간이_7일_늘고_0원_신청은_환불을_고를_수_없다() throws Exception {
        String token = loginWithProfile("demo2");
        // 멤버십 1개월 기간 안의 두 날짜
        Long failedSlot = eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(15));
        submitAndPay(token, "MEMBERSHIP_1M", eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(12)),
                "JAMSIL");
        Membership membership = membershipRepository.findAllByUserId(userId(token)).get(0);
        LocalDate before = membership.getExpiresOn();

        long usedMembership = failAlone(token, "MEMBERSHIP_1M", failedSlot);

        assertThat(membership.getExpiresOn()).isEqualTo(before.plusDays(Membership.EXTENSION_DAYS_PER_FAILURE));
        assertThat(membership.getExtendedCount()).isEqualTo(1);
        alternatives(token, usedMembership)
                .andExpect(jsonPath("$.data.choices[1].description").value(
                        org.hamcrest.Matchers.containsString("멤버십 기간")))
                .andExpect(jsonPath("$.data.choices[2].available").value(false));
        resolve(token, usedMembership, Map.of("choice", "REFUND"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESOLUTION_NOT_AVAILABLE"));
    }

    /** 혼자 신청·결제한 회차를 매칭해 인원 부족으로 실패(대체 안내)시킨다 */
    private long failAlone(String token, String ticketType, Long slotId) throws Exception {
        long applicationId = submitAndPay(token, ticketType, slotId, "HONGDAE_HAPJEONG");
        matchingService.runForSlot(slotId);
        assertThat(applicationQueryService.getApplication(applicationId).matchStatus().name())
                .isEqualTo("ALTERNATIVE_OFFERED");
        return applicationId;
    }

    private long submitAndPay(String token, String ticketType, Long slotId, String region) throws Exception {
        long applicationId = submit(token, ticketType, slotId, region);
        pay(token, applicationId).andExpect(jsonPath("$.data.success").value(true));
        return applicationId;
    }

    private long submit(String token, String ticketType, Long slotId, String region) throws Exception {
        return body(mockMvc.perform(authed(post("/api/app/applications"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticketType", ticketType, "regions", List.of(region),
                                "slotIds", List.of(slotId)))))
                .andExpect(status().isOk())).at("/data/applicationId").asLong();
    }

    private ResultActions pay(String token, long applicationId) throws Exception {
        return mockMvc.perform(authed(post("/api/app/applications/" + applicationId + "/payment"), token));
    }

    private ResultActions alternatives(String token, long applicationId) throws Exception {
        return mockMvc.perform(authed(get("/api/app/applications/" + applicationId + "/alternatives"), token));
    }

    private ResultActions resolve(String token, long applicationId, Map<String, Object> request) throws Exception {
        return mockMvc.perform(authed(post("/api/app/applications/" + applicationId + "/resolution"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new HashMap<>(request))));
    }

    private ResultActions myApplication(String token, long applicationId) throws Exception {
        return mockMvc.perform(authed(get("/api/app/applications/" + applicationId), token));
    }

    private Long userId(String token) throws Exception {
        String loginId = body(mockMvc.perform(authed(get("/api/app/me/profile"), token))).at("/data/loginId").asText();
        return body(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("loginId", loginId, "password", PASSWORD))))).at("/data/userId").asLong();
    }

    private String loginWithProfile(String loginId) throws Exception {
        String token = body(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("loginId", loginId, "password", PASSWORD))))
                .andExpect(status().isOk())).at("/data/token").asText();
        mockMvc.perform(authed(put("/api/app/me/profile"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "체험", "phone", "01012345678", "birthYear", 1995,
                                "gender", "FEMALE", "interests", List.of("MUSIC"), "traits", List.of("CALM"),
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
