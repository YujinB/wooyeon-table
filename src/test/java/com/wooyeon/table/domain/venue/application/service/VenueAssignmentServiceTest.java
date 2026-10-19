package com.wooyeon.table.domain.venue.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.matching.application.dto.ConfirmedTable;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
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
import com.wooyeon.table.domain.venue.application.dto.VenueAssignmentView;
import com.wooyeon.table.domain.venue.domain.model.Venue;
import com.wooyeon.table.domain.venue.domain.model.VenueAssignmentStatus;
import com.wooyeon.table.domain.venue.domain.repository.VenueRepository;
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

/** 매칭 확정 → 식당 자동 배정·재탐색·실패 예외·재시도·참가자 안내 (INF-02, INF-03) */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class VenueAssignmentServiceTest {

    private static final LocalDate DATE = LocalDate.of(2031, 3, 6);

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private MatchingQueryService matchingQueryService;
    @Autowired
    private VenueAssignmentService venueAssignmentService;
    @Autowired
    private VenueQueryService venueQueryService;
    @Autowired
    private VenueRepository venueRepository;
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
    void 확정되면_바로_같은_지역_식당이_배정되고_자동_예약이_거절되면_다음_식당으로_간다() {
        Long slot = eventSlotCommandService.findOrCreate(DATE);
        apply(5, Region.HONGDAE_HAPJEONG, MealBudget.OVER_30K, slot);

        matchingService.runForSlot(slot);

        ConfirmedTable table = matchingQueryService.findConfirmedTables(slot).get(0);
        VenueAssignmentView assignment = venueQueryService.findByTable(table.tableId()).orElseThrow();
        assertThat(assignment.status()).isEqualTo(VenueAssignmentStatus.ASSIGNED);
        // 예산이 맞는 상수 와인키친(자동 예약 불가)이 1순위 → 거절 → 남은 자리가 많은 합정 담소식당
        assertThat(assignment.attemptedVenues()).isEqualTo(2);
        assertThat(assignment.venue().name()).isEqualTo("[가상] 합정 담소식당");
        assertThat(assignment.venue().region()).isEqualTo("홍대/합정");
        assertThat(assignment.reservationName()).isEqualTo("우연한식탁 0306-" + table.tableNo());

        venueAssignmentService.assignSlot(slot);
        assertThat(venueQueryService.findBySlot(slot)).singleElement()
                .satisfies(again -> assertThat(again.bookingReference()).isEqualTo(assignment.bookingReference()));
    }

    @Test
    void 같은_지역에_자리가_없으면_운영자_예외로_남고_식당을_열면_재시도로_배정된다() {
        List<Venue> jamsil = venueRepository.findActiveByRegion(Region.JAMSIL);
        jamsil.forEach(Venue::deactivate);
        Long slot = eventSlotCommandService.findOrCreate(DATE);
        apply(5, Region.JAMSIL, MealBudget.FROM_20K_TO_30K, slot);

        matchingService.runForSlot(slot);

        Long tableId = matchingQueryService.findConfirmedTables(slot).get(0).tableId();
        assertThat(venueQueryService.findFailures()).filteredOn(failure -> failure.tableId().equals(tableId))
                .singleElement()
                .satisfies(failure -> {
                    assertThat(failure.venue()).isNull();
                    assertThat(failure.failureReason()).contains("잠실");
                });

        jamsil.get(0).activate();
        venueAssignmentService.retryTable(tableId);

        assertThat(venueQueryService.findByTable(tableId).orElseThrow().venue().name())
                .isEqualTo(jamsil.get(0).getName());
        assertThat(venueQueryService.findFailures()).noneMatch(failure -> failure.tableId().equals(tableId));
    }

    @Test
    void 확정된_참가자는_식당명_주소_지도_집합_시간_예약명_가격을_본다() throws Exception {
        Long slot = eventSlotCommandService.findOrCreate(DATE);
        List<long[]> members = apply(5, Region.EULJIRO_JONGNO, MealBudget.FROM_10K_TO_20K, slot);
        matchingService.runForSlot(slot);
        long userId = members.get(0)[0];
        long applicationId = members.get(0)[1];
        String token = authTokenProvider.issue(userId, UserRole.PARTICIPANT);

        mockMvc.perform(get("/api/app/applications/" + applicationId + "/venue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigned").value(true))
                .andExpect(jsonPath("$.data.name").value("[가상] 을지로 노포"))
                .andExpect(jsonPath("$.data.address").isNotEmpty())
                .andExpect(jsonPath("$.data.mapUrl").value(org.hamcrest.Matchers.startsWith("https://map.naver.com")))
                .andExpect(jsonPath("$.data.gatherAt").value(DATE + "T19:30:00"))
                .andExpect(jsonPath("$.data.reservationName").value(org.hamcrest.Matchers.startsWith("우연한식탁 0306-")))
                .andExpect(jsonPath("$.data.priceGuide").value("1인 1.5~2만원"));

        String stranger = authTokenProvider.issue(members.get(1)[0], UserRole.PARTICIPANT);
        mockMvc.perform(get("/api/app/applications/" + applicationId + "/venue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger))
                .andExpect(status().isNotFound());
    }

    /** 서로 잘 맞는 신청자 count명. [userId, applicationId] 목록 */
    private List<long[]> apply(int count, Region region, MealBudget budget, Long slotId) {
        return IntStream.range(0, count).mapToObj(i -> {
            int n = sequence.incrementAndGet();
            Long userId = userCommandService.createParticipantIfAbsent("venue-test-" + n, "식당테스트" + n);
            profileCommandService.createIfAbsent(Profile.builder()
                    .userId(userId)
                    .birthYear(1996)
                    .gender(n % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                    .mbti(Mbti.of(n % 2 == 0 ? "ENFP" : "INFJ"))
                    .interests(Set.of(Interest.FOOD_CAFE, Interest.TRAVEL))
                    .traits(Set.of(Trait.LIGHT_HUMOR, Trait.GOOD_LISTENER))
                    .preferredTraits(Set.of(PreferredTrait.HUMOROUS))
                    .mealBudgets(Set.of(budget))
                    .build());
            Long applicationId = applicationJpaRepository.save(Application.paidSubmission(userId, TicketType.SINGLE,
                    Set.of(region), List.of(slotId))).getId();
            return new long[]{userId, applicationId};
        }).toList();
    }
}
