package com.wooyeon.table.domain.matching.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.wooyeon.table.domain.application.infrastructure.excel.ApplicantExcelParser;
import com.wooyeon.table.domain.application.infrastructure.excel.ApplicantRow;
import com.wooyeon.table.domain.matching.domain.model.Candidate;
import com.wooyeon.table.domain.matching.domain.model.MatchResult;
import com.wooyeon.table.domain.matching.domain.model.MatchedTable;
import com.wooyeon.table.domain.matching.domain.model.MatchingPolicy;
import com.wooyeon.table.domain.matching.domain.model.PairRelations;
import com.wooyeon.table.domain.matching.domain.model.UnassignedCandidate;
import com.wooyeon.table.domain.matching.domain.model.UnassignedReason;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 주최 측 가상데이터(294명)를 날짜순으로 처리하며 "그리디만"과 "그리디+개선"을 비교한다.
 * 데이터 파일은 저장소에 올리지 않으므로(data/ gitignore) 파일이 없으면 건너뛴다.
 * 결과는 build/reports/matching-benchmark.txt 에 남긴다.
 */
@Tag("benchmark")
class MatchingBenchmarkTest {

    private static final Path DATA = Path.of("data", "applicants.xlsx");
    private static final Path REPORT = Path.of("build", "reports", "matching-benchmark.txt");
    private static final int TIMED_RUNS = 5;

    @Test
    void 가상데이터_벤치마크() throws IOException {
        assumeTrue(Files.exists(DATA), "data/applicants.xlsx 없음");
        List<ApplicantRow> rows;
        try (InputStream input = Files.newInputStream(DATA)) {
            rows = new ApplicantExcelParser().parse(input);
        }
        List<ApplicantRow> seoul = rows.stream().filter(row -> !row.regions().isEmpty()).toList();

        StringBuilder report = new StringBuilder();
        report.append("가상데이터 ").append(rows.size()).append("명 중 서울 4개 지역 선택 ").append(seoul.size())
                .append("명 (서울 밖만 고른 ").append(rows.size() - seoul.size()).append("명 제외)\n\n");

        Simulation greedy = null;
        Simulation improved = null;
        for (boolean improvement : new boolean[]{false, true}) {
            MatchingEngine engine = new MatchingEngine(MatchingPolicy.defaults().withImprovement(improvement));
            simulate(engine, seoul); // 워밍업
            long[] nanos = new long[TIMED_RUNS];
            Simulation simulation = null;
            for (int i = 0; i < TIMED_RUNS; i++) {
                long start = System.nanoTime();
                simulation = simulate(engine, seoul);
                nanos[i] = System.nanoTime() - start;
            }
            Arrays.sort(nanos);
            simulation.medianMillis = nanos[TIMED_RUNS / 2] / 1_000_000.0;
            report.append(simulation.describe(improvement ? "그리디+개선" : "그리디만", seoul.size()));
            if (improvement) {
                improved = simulation;
            } else {
                greedy = simulation;
            }
        }

        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, report, StandardCharsets.UTF_8);
        System.out.println(report);

        assertThat(improved.confirmed).isGreaterThanOrEqualTo(greedy.confirmed);
    }

    /** 날짜 오름차순으로 회차를 돌고, 확정되지 않은 사람은 본인이 고른 다음 날짜에 다시 후보가 된다 */
    private static Simulation simulate(MatchingEngine engine, List<ApplicantRow> rows) {
        Set<LocalDate> dates = new TreeSet<>();
        rows.forEach(row -> dates.addAll(row.availableDates()));
        Set<Integer> confirmed = new HashSet<>();
        Map<Integer, UnassignedReason> lastReason = new HashMap<>();
        Simulation simulation = new Simulation();
        for (LocalDate date : dates) {
            List<Candidate> candidates = rows.stream()
                    .filter(row -> row.availableDates().contains(date) && !confirmed.contains(row.rowNumber()))
                    .map(MatchingBenchmarkTest::toCandidate)
                    .toList();
            if (candidates.isEmpty()) {
                continue;
            }
            MatchResult result = engine.match(candidates, PairRelations.none());
            simulation.sessions++;
            for (MatchedTable table : result.tables()) {
                simulation.scores.add(table.score().total());
                simulation.sizes.merge(table.size(), 1, Integer::sum);
                simulation.ageGaps.add(table.ageGap());
                table.members().forEach(member -> confirmed.add((int) member.candidateId()));
            }
            for (UnassignedCandidate unassigned : result.unassigned()) {
                lastReason.put((int) unassigned.candidateId(), unassigned.reason());
            }
            simulation.reallocations += result.stats().reallocations() + result.stats().displacements();
            simulation.moves += result.stats().moves() + result.stats().swaps();
            simulation.dissolved += result.stats().dissolvedTables();
            simulation.splits += result.stats().splitTables();
        }
        simulation.confirmed = confirmed.size();
        for (ApplicantRow row : rows) {
            if (!confirmed.contains(row.rowNumber())) {
                simulation.failedReasons.merge(lastReason.get(row.rowNumber()), 1, Integer::sum);
            }
        }
        return simulation;
    }

    private static Candidate toCandidate(ApplicantRow row) {
        return new Candidate(row.rowNumber(), row.age(), row.gender(), row.mbti(), row.regions(), row.interests(),
                row.traits(), row.preferredTraits());
    }

    private static final class Simulation {
        private int sessions;
        private int confirmed;
        private final List<Double> scores = new ArrayList<>();
        private final List<Integer> ageGaps = new ArrayList<>();
        private final Map<Integer, Integer> sizes = new TreeMap<>();
        private final Map<UnassignedReason, Integer> failedReasons = new EnumMap<>(UnassignedReason.class);
        private int reallocations;
        private int moves;
        private int dissolved;
        private int splits;
        private double medianMillis;

        private String describe(String label, int total) {
            double average = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double lowest = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            int maxGap = ageGaps.stream().mapToInt(Integer::intValue).max().orElse(0);
            return """
                    [%s]
                      회차 %d개 처리, 실행 시간(중앙값 %d회) %.1fms
                      배정 %d / %d명 (%.1f%%), 대체 안내 %d명 %s
                      테이블 %d개, 인원 분포 %s, 최대 나이 차 %d
                      점수 평균 %.1f / 최저 %.1f
                      재배치 %d회, 개선 이동·교환 %d회, 분리로 연 테이블 %d개, 최소 점수 미달 해제 %d개

                    """.formatted(label, sessions, TIMED_RUNS, medianMillis,
                    confirmed, total, confirmed * 100.0 / total, total - confirmed, failedReasons,
                    scores.size(), sizes, maxGap,
                    average, lowest,
                    reallocations, moves, splits, dissolved);
        }
    }
}
