package com.wooyeon.table.domain.application.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.support.IntegrationTest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

/** 데모 계정 로그인 → 프로필 저장 → 회차 조회 → 신청 제출까지 HTTP로 확인 (ACC-01~04, APP-01~04) */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class AccountApplyFlowTest {

    private static final String PASSWORD = "wooyeon1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;

    @Test
    void 잘못된_비밀번호_토큰_없음_변조_토큰_참가자의_운영자_API_호출은_거부된다() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("loginId", "demo1", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
        mockMvc.perform(get("/api/app/me/profile"))
                .andExpect(status().isUnauthorized());

        String token = login("demo1");
        mockMvc.perform(get("/api/app/me/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(authed(get("/api/admin/slots"), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void 운영자는_회차를_만들고_참가자는_마감_전_회차만_본다() throws Exception {
        String admin = login("admin");
        LocalDate open = LocalDate.now().plusDays(400);
        LocalDate closed = LocalDate.now().plusDays(1);
        eventSlotCommandService.findOrCreate(closed);

        mockMvc.perform(authed(post("/api/admin/slots"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventDate", open.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startTime").value("19:30:00"))
                .andExpect(jsonPath("$.data.matchAt").value(open.minusDays(1).atStartOfDay().toString() + ":00"));

        mockMvc.perform(authed(get("/api/app/slots"), login("demo1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].eventDate").value(hasItem(open.toString())))
                .andExpect(jsonPath("$.data[*].eventDate").value(not(hasItem(closed.toString()))))
                .andExpect(jsonPath("$.data[0].regions.length()").value(4));
    }

    @Test
    void 프로필을_저장하면_신청_가능한_상태가_되고_형식이_틀리면_400() throws Exception {
        String token = login("demo1");
        mockMvc.perform(authed(get("/api/app/me/profile"), token))
                .andExpect(jsonPath("$.data.loginId").value("demo1"));

        Map<String, Object> badPhone = new java.util.HashMap<>(profileBody());
        badPhone.put("phone", "12345");
        mockMvc.perform(authed(put("/api/app/me/profile"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(badPhone)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        saveProfile(token);
        mockMvc.perform(authed(get("/api/app/me/profile"), token))
                .andExpect(jsonPath("$.data.name").value("김체험"))
                .andExpect(jsonPath("$.data.mbti").value("ENFP"))
                .andExpect(jsonPath("$.data.interests.length()").value(2))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    @Test
    void 신청은_프로필_필수_항목_마감_날짜_중복을_확인하고_결제_대기로_저장된다() throws Exception {
        String token = login("demo1");
        Long open = eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(401));
        Long closed = eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(1));

        apply(token, "SINGLE", open)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PROFILE_INCOMPLETE"));

        saveProfile(token);
        apply(token, "SINGLE", closed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("APPLICATION_CLOSED"));

        apply(token, "MEMBERSHIP_3M", open)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketLabel").value("멤버십 3개월"))
                .andExpect(jsonPath("$.data.price").value(75000))
                .andExpect(jsonPath("$.data.applicationStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));

        apply(token, "SINGLE", open)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_APPLICATION"));

        mockMvc.perform(authed(get("/api/app/applications"), token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].regions[0]").value("홍대/합정"));
    }

    @Test
    void 다른_참가자의_신청과_매칭_결과는_볼_수_없다() throws Exception {
        String owner = login("demo2");
        String other = login("demo3");
        saveProfile(owner);
        Long slot = eventSlotCommandService.findOrCreate(LocalDate.now().plusDays(402));
        JsonNode created = body(apply(owner, "SINGLE", slot).andExpect(status().isOk()));
        long applicationId = created.at("/data/applicationId").asLong();

        mockMvc.perform(authed(get("/api/app/applications/" + applicationId + "/match"), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchStatus").value("WAITING"));
        mockMvc.perform(authed(get("/api/app/applications/" + applicationId + "/match"), other))
                .andExpect(status().isNotFound());
        mockMvc.perform(authed(get("/api/app/applications/" + applicationId), other))
                .andExpect(status().isNotFound());
    }

    @Test
    void 목요일_회차의_기본_신청_마감은_화요일_24시() {
        LocalDate thursday = LocalDate.of(2030, 1, 3);
        assertThat(thursday.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);

        EventSlot slot = EventSlot.withDefaultSchedule(thursday);

        assertThat(slot.getMatchAt()).isEqualTo(LocalDateTime.of(2030, 1, 2, 0, 0));
        assertThat(slot.isOpenForApplication(LocalDateTime.of(2030, 1, 1, 23, 59))).isTrue();
        assertThat(slot.isOpenForApplication(LocalDateTime.of(2030, 1, 2, 0, 0))).isFalse();
    }

    private ResultActions apply(String token, String ticketType, Long slotId) throws Exception {
        return mockMvc.perform(authed(post("/api/app/applications"), token).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("ticketType", ticketType,
                        "regions", List.of("HONGDAE_HAPJEONG", "JAMSIL"),
                        "slotIds", List.of(slotId)))));
    }

    private void saveProfile(String token) throws Exception {
        mockMvc.perform(authed(put("/api/app/me/profile"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileBody())))
                .andExpect(status().isOk());
    }

    private Map<String, Object> profileBody() {
        return Map.ofEntries(
                Map.entry("name", "김체험"),
                Map.entry("phone", "010-1234-5678"),
                Map.entry("birthYear", 1996),
                Map.entry("gender", "FEMALE"),
                Map.entry("job", "디자이너"),
                Map.entry("mbti", "enfp"),
                Map.entry("interests", List.of("FOOD_CAFE", "TRAVEL")),
                Map.entry("traits", List.of("LIGHT_HUMOR")),
                Map.entry("preferredTraits", List.of("HUMOROUS")),
                Map.entry("mealBudgets", List.of("FROM_20K_TO_30K")),
                Map.entry("foodRestriction", "갑각류 알레르기"));
    }

    private String login(String loginId) throws Exception {
        JsonNode response = body(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("loginId", loginId, "password", PASSWORD))))
                .andExpect(status().isOk()));
        return response.at("/data/token").asText();
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
