package com.wooyeon.table.domain.demo.application;

import com.wooyeon.table.domain.application.application.dto.ApplicantImportResult;
import com.wooyeon.table.domain.application.application.service.ApplicantImportService;
import com.wooyeon.table.domain.application.application.service.ApplicationCommandService;
import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.chat.application.dto.TableMember;
import com.wooyeon.table.domain.chat.application.service.AdminChatQueryService;
import com.wooyeon.table.domain.chat.application.service.ChatMemberQueryService;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.feedback.application.dto.FeedbackCommand;
import com.wooyeon.table.domain.feedback.application.service.AttendanceService;
import com.wooyeon.table.domain.feedback.application.service.EventCloseService;
import com.wooyeon.table.domain.feedback.application.service.FeedbackService;
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
import com.wooyeon.table.domain.safety.application.dto.ReportCommand;
import com.wooyeon.table.domain.safety.application.service.SafetyService;
import com.wooyeon.table.domain.safety.domain.model.ReportReason;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.domain.user.application.service.UserQueryService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 시연용 데이터. 무작위 없이 같은 날 돌리면 같은 결과가 나온다.
 * <ul>
 *   <li>신청자 엑셀(가상데이터)을 주면 먼저 적재하고, 지난 목요일 이전 회차를 날짜순으로 자동 매칭한다 (벤치마크와 같은 흐름).
 *       엑셀에는 참석 기록이 없어 그 회차들은 전원 참석으로 종료 처리한다</li>
 *   <li>지난 목요일: 홍대(체험참가자2 포함)·강남 테이블을 매칭 → 체크인 → 행사 종료 → 일부 후기·신고까지 진행.
 *       체험참가자2는 단체방·후기·참가 이력을, 운영자는 노쇼 후보·신고 예외와 후기 요약을 바로 볼 수 있다</li>
 *   <li>다가오는 목요일 4회차: 회차마다 지역별 5명씩 결제 완료 참가자. 체험 계정이 신청하고 "지금 마감 실행"을 누르면 테이블이 잡힌다</li>
 *   <li>체험 계정: 체험참가자1~3은 프로필 작성 완료, 4~5는 프로필부터 시작</li>
 * </ul>
 * 각 서비스 호출이 자기 트랜잭션으로 끝나야 매칭 후 이벤트(식당·단체방·참석)가 순서대로 이어지므로 이 클래스에는 트랜잭션을 걸지 않는다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSeedService {

    static final String MARKER_LOGIN_ID = "seed-001";
    private static final int UPCOMING_SLOTS = 4;
    private static final int PER_REGION = 5;
    private static final List<Region> REGIONS = List.of(Region.HONGDAE_HAPJEONG, Region.GANGNAM_SINNONHYEON,
            Region.EULJIRO_JONGNO, Region.JAMSIL);

    private static final List<String> SURNAMES = List.of("김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
            "한", "오", "서", "신", "권", "황", "안", "송", "류", "홍");
    private static final List<String> GIVEN_NAMES = List.of("서연", "민준", "지우", "도윤", "하은", "시우", "수아",
            "예준", "지민", "주원", "채원", "건우", "다은", "현우", "유나", "우진", "소율", "선우", "예린", "지호", "가은",
            "준서", "나연", "태윤");
    private static final List<String> MBTIS = List.of("ENFP", "ISFJ", "INTJ", "ESFJ", "INFP", "ESTJ", "ENTP", "ISTJ",
            "INFJ", "ESFP", "ENFJ", "ISTP");
    private static final List<Interest> INTERESTS = List.of(Interest.values());
    private static final List<Trait> TRAITS = List.of(Trait.values());
    private static final List<PreferredTrait> PREFERRED = List.of(PreferredTrait.values());
    private static final List<MealBudget> BUDGETS = List.of(MealBudget.values());

    private final Clock clock;
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final ProfileCommandService profileCommandService;
    private final EventSlotCommandService eventSlotCommandService;
    private final EventSlotQueryService eventSlotQueryService;
    private final ApplicantImportService applicantImportService;
    private final ApplicationCommandService applicationCommandService;
    private final MatchingService matchingService;
    private final MatchingQueryService matchingQueryService;
    private final ChatMemberQueryService chatMemberQueryService;
    private final AdminChatQueryService adminChatQueryService;
    private final AttendanceService attendanceService;
    private final EventCloseService eventCloseService;
    private final FeedbackService feedbackService;
    private final SafetyService safetyService;

    private int sequence;

    public boolean seedIfEmpty(List<String> demoLoginIds) {
        return seedIfEmpty(demoLoginIds, null);
    }

    /**
     * @param applicantsFile 신청자 엑셀. 없으면 가상데이터 없이 시연용 참가자만
     * @return 이미 시드가 있으면 false
     */
    public boolean seedIfEmpty(List<String> demoLoginIds, Path applicantsFile) {
        if (userQueryService.existsByLoginId(MARKER_LOGIN_ID)) {
            return false;
        }
        sequence = 0;
        List<Long> demoUsers = demoLoginIds.stream()
                .map(loginId -> userCommandService.createParticipantIfAbsent(loginId, loginId))
                .toList();
        for (int i = 0; i < Math.min(3, demoUsers.size()); i++) {
            profileCommandService.createIfAbsent(demoProfile(demoUsers.get(i), i));
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate lastThursday = today.with(TemporalAdjusters.previous(DayOfWeek.THURSDAY));
        if (applicantsFile != null) {
            importAndMatchPast(applicantsFile, lastThursday);
        }
        seedPastEvent(lastThursday,
                demoUsers.size() > 1 ? demoUsers.get(1) : null);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate thursday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
        int created = 0;
        while (created < UPCOMING_SLOTS) {
            // 신청 마감(전날 0시)이 이미 지난 목요일은 건너뜀
            if (thursday.atStartOfDay().minusDays(1).isAfter(now)) {
                Long slotId = eventSlotCommandService.findOrCreate(thursday);
                for (Region region : REGIONS) {
                    for (int i = 0; i < PER_REGION; i++) {
                        applyPaid(seedUser(), region, slotId);
                    }
                }
                created++;
            }
            thursday = thursday.plusWeeks(1);
        }
        log.info("데모 시드 완료: 시드 참가자 {}명, 다가오는 회차 {}개", sequence, UPCOMING_SLOTS);
        return true;
    }

    /** 가상데이터 적재 후, 지난 목요일보다 앞선 회차를 날짜순으로 매칭 (못 들어간 사람은 다음 희망 날짜로 이월) */
    private void importAndMatchPast(Path file, LocalDate lastThursday) {
        try (InputStream input = Files.newInputStream(file)) {
            ApplicantImportResult result = applicantImportService.importApplicants(input);
            log.info("가상데이터 적재: {}행 중 {}명", result.totalRows(), result.imported());
        } catch (IOException e) {
            throw new UncheckedIOException("신청자 엑셀을 읽지 못했습니다: " + file, e);
        }
        eventSlotQueryService.findAllSlots().stream()
                .filter(slot -> slot.eventDate().isBefore(lastThursday))
                .sorted(Comparator.comparing(EventSlotInfo::eventDate))
                .forEach(slot -> {
                    matchingService.runForSlot(slot.id());
                    closeAsAttended(slot.id());
                });
    }

    /** 가상데이터에는 참석 기록이 없어 지난 회차는 확정 인원 전원 참석으로 보고 종료 처리 (노쇼 후보가 쏟아지지 않게) */
    private void closeAsAttended(Long slotId) {
        matchingQueryService.findConfirmedTables(slotId).stream()
                .flatMap(table -> chatMemberQueryService.membersOfTable(table.tableId()).stream())
                .forEach(member -> attendanceService.checkIn(member.userId(), member.applicationId()));
        eventCloseService.close(slotId);
    }

    /** 지난 목요일 모임을 끝까지 진행해 둔다: 매칭 → 체크인 → 종료 → 후기·신고 */
    private void seedPastEvent(LocalDate date, Long pastDemoUser) {
        Long slotId = eventSlotCommandService.findOrCreate(date);
        List<Long> hongdae = new ArrayList<>();
        for (int i = 0; i < PER_REGION; i++) {
            hongdae.add(seedUser());
        }
        if (pastDemoUser != null) {
            hongdae.add(pastDemoUser);
        }
        hongdae.forEach(user -> applyPaid(user, Region.HONGDAE_HAPJEONG, slotId));
        for (int i = 0; i < PER_REGION; i++) {
            applyPaid(seedUser(), Region.GANGNAM_SINNONHYEON, slotId);
        }
        matchingService.runForSlot(slotId);

        Map<Long, List<TableMember>> membersByTable = matchingQueryService.findConfirmedTables(slotId).stream()
                .collect(Collectors.toMap(ConfirmedTable::tableId,
                        table -> chatMemberQueryService.membersOfTable(table.tableId())));
        // 체험참가자2가 없는 테이블(강남)의 마지막 좌석 한 명은 체크인하지 않음 → 노쇼 후보
        Long otherTable = membersByTable.entrySet().stream()
                .filter(entry -> entry.getValue().stream().noneMatch(member -> member.userId().equals(pastDemoUser)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        TableMember noShow = otherTable == null ? null : membersByTable.get(otherTable).get(
                membersByTable.get(otherTable).size() - 1);
        List<TableMember> attendees = membersByTable.values().stream()
                .flatMap(List::stream)
                .filter(member -> !member.equals(noShow))
                .toList();
        attendees.forEach(member -> attendanceService.checkIn(member.userId(), member.applicationId()));
        eventCloseService.close(slotId);

        // 체험참가자2는 직접 후기를 쓰도록 남겨 두고, 나머지 참석자 셋 중 둘이 후기를 남김
        List<TableMember> reviewers = attendees.stream()
                .filter(member -> !member.userId().equals(pastDemoUser))
                .toList();
        for (int i = 0; i < reviewers.size(); i++) {
            if (i % 3 == 2) {
                continue;
            }
            TableMember member = reviewers.get(i);
            feedbackService.submit(member.userId(), member.applicationId(), new FeedbackCommand(
                    4 + i % 2, 4 + (i + 1) % 2, 3 + i % 3, 4 + i % 2, 7 + i % 4,
                    i % 2 == 0 ? "처음 만난 사이인데 대화 주제 덕분에 금방 편해졌어요" : null, Set.of(), Set.of()));
        }

        // 운영자 예외 처리함 시연용: 강남 테이블에서 신고 한 건 (행사 종료 후라 긴급 아님)
        if (otherTable != null) {
            List<TableMember> members = membersByTable.get(otherTable);
            safetyService.report(members.get(0).userId(), adminChatQueryService.findByTable(otherTable).roomId(),
                    new ReportCommand(members.get(1).seatNo(), ReportReason.UNWANTED_CONTACT,
                            "모임 뒤에 개인 연락처를 계속 물어봤어요", null, true));
        }
    }

    private void applyPaid(Long userId, Region region, Long slotId) {
        applicationCommandService.save(Application.paidSubmission(userId, TicketType.SINGLE, Set.of(region),
                List.of(slotId)));
    }

    /** 나이 25~32, 여:남 = 3:2, MBTI·관심사·성향은 번호에 따라 돌아가며 */
    private Long seedUser() {
        int n = ++sequence;
        String name = SURNAMES.get((n * 7) % SURNAMES.size()) + GIVEN_NAMES.get((n * 5) % GIVEN_NAMES.size());
        Long userId = userCommandService.createParticipantIfAbsent("seed-%03d".formatted(n), name);
        LocalDate today = LocalDate.now(clock);
        profileCommandService.createIfAbsent(Profile.builder()
                .userId(userId)
                .birthYear(today.getYear() - (25 + (n * 3) % 8))
                .gender(n % 5 < 3 ? Gender.FEMALE : Gender.MALE)
                .mbti(Mbti.of(MBTIS.get(n % MBTIS.size())))
                .interests(pick(INTERESTS, n, 3))
                .traits(pick(TRAITS, n, 2))
                .preferredTraits(pick(PREFERRED, n * 2, 2))
                .mealBudgets(pick(BUDGETS, n, 2))
                .build());
        return userId;
    }

    private Profile demoProfile(Long userId, int index) {
        return Profile.builder()
                .userId(userId)
                .birthYear(LocalDate.now(clock).getYear() - (28 + index))
                .gender(index == 1 ? Gender.MALE : Gender.FEMALE)
                .mbti(Mbti.of(List.of("ENFP", "INTJ", "ISFJ").get(index)))
                .interests(Set.of(Interest.FOOD_CAFE, Interest.TRAVEL, INTERESTS.get(index)))
                .traits(Set.of(Trait.LIGHT_HUMOR, Trait.GOOD_LISTENER))
                .preferredTraits(Set.of(PreferredTrait.HUMOROUS, PreferredTrait.DEEP))
                .mealBudgets(Set.of(MealBudget.FROM_20K_TO_30K))
                .build();
    }

    /** 목록에서 start 위치부터 연속 count개 (끝에 닿으면 처음으로) */
    private static <T> Set<T> pick(List<T> values, int start, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> values.get(Math.floorMod(start + i, values.size())))
                .collect(Collectors.toSet());
    }
}
