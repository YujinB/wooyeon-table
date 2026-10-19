package com.wooyeon.table.domain.operation;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.feedback.application.service.EventCloseService;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.domain.profile.application.service.ProfileCommandService;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.domain.user.domain.model.UserRole;
import com.wooyeon.table.global.auth.AuthTokenProvider;
import com.wooyeon.table.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 운영 대시보드·회차 신청자·예외 처리함 (PRD 18절 12단계, MAT-13) */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class AdminOperationFlowTest {

    private static final LocalDate EVENT_DATE = LocalDate.of(2031, 12, 4);

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private EventCloseService eventCloseService;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private UserCommandService userCommandService;
    @Autowired
    private ProfileCommandService profileCommandService;
    @Autowired
    private ApplicationJpaRepository applicationJpaRepository;
    @Autowired
    private AuthTokenProvider authTokenProvider;
    @Autowired
    private MockMvc mockMvc;

    private final AtomicInteger sequence = new AtomicInteger();

    @Test
    void 매칭과_행사_종료_후_대시보드에_회차_현황과_노쇼_후보_예외가_보인다() throws Exception {
        Long slotId = eventSlotCommandService.findOrCreate(EVENT_DATE);
        // 홍대 5명은 테이블이 되고, 잠실 1명은 인원 부족으로 대체 안내
        IntStream.range(0, 5).forEach(i -> apply(newUser(), Region.HONGDAE_HAPJEONG, slotId));
        apply(newUser(), Region.JAMSIL, slotId);
        matchingService.runForSlot(slotId);
        eventCloseService.close(slotId);

        String slot = "$.data.slots[?(@.slotId == " + slotId + ")]";
        mockMvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(slot + ".status").value("COMPLETED"))
                .andExpect(jsonPath(slot + ".applicants").value(6))
                .andExpect(jsonPath(slot + ".confirmed").value(5))
                .andExpect(jsonPath(slot + ".matchFailed").value(1))
                .andExpect(jsonPath(slot + ".tables").value(1))
                .andExpect(jsonPath("$.data.exceptions.noShow").value(5))
                .andExpect(jsonPath("$.data.exceptions.total").value(5))
                // 6명 신청·결제 → 5명 확정, 1명 대체 안내 → 매칭 성공률 83%, 종료 후 5명 모두 노쇼 후보
                .andExpect(jsonPath("$.data.totals.applications").value(6))
                .andExpect(jsonPath("$.data.totals.paid").value(6))
                .andExpect(jsonPath("$.data.totals.confirmed").value(5))
                .andExpect(jsonPath("$.data.totals.matchSuccessRate").value(83))
                .andExpect(jsonPath("$.data.totals.autoConfirmRate").value(100))
                .andExpect(jsonPath("$.data.totals.awaitingChoice").value(1))
                .andExpect(jsonPath("$.data.totals.attendanceRate").value(0));

        mockMvc.perform(get("/api/admin/slots/" + slotId + "/applicants").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[?(@.confirmedHere == true)]", hasSize(5)))
                .andExpect(jsonPath("$.data[?(@.status == 'MATCH_FAILED')].regions[0]").value("잠실"));

        mockMvc.perform(get("/api/admin/exceptions").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type == 'NO_SHOW' && @.slotId == " + slotId + ")]", hasSize(5)));
    }

    @Test
    void 참가자는_운영_화면_API를_볼_수_없다() throws Exception {
        String participant = "Bearer " + authTokenProvider.issue(newUser(), UserRole.PARTICIPANT);
        mockMvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, participant))
                .andExpect(status().isForbidden());
    }

    private String admin() {
        return "Bearer " + authTokenProvider.issue(-1L, UserRole.ADMIN);
    }

    private Long newUser() {
        int n = sequence.incrementAndGet();
        Long userId = userCommandService.createParticipantIfAbsent("operation-test-" + n, "운영테스트" + n);
        profileCommandService.createIfAbsent(Profile.builder()
                .userId(userId)
                .birthYear(1994)
                .gender(n % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                .mbti(Mbti.of(n % 2 == 0 ? "INFJ" : "ENFP"))
                .interests(Set.of(Interest.FOOD_CAFE, Interest.TRAVEL))
                .traits(Set.of(Trait.LIGHT_HUMOR, Trait.GOOD_LISTENER))
                .preferredTraits(Set.of(PreferredTrait.HUMOROUS))
                .mealBudgets(Set.of(MealBudget.FROM_20K_TO_30K))
                .build());
        return userId;
    }

    private void apply(Long userId, Region region, Long slotId) {
        applicationJpaRepository.save(Application.paidSubmission(userId, TicketType.SINGLE, Set.of(region),
                List.of(slotId)));
    }
}
