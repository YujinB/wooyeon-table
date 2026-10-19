package com.wooyeon.table.domain.application.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.wooyeon.table.domain.application.application.dto.ApplicantImportResult;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.event.domain.model.EventSlot;
import com.wooyeon.table.domain.event.infrastructure.persistence.EventSlotJpaRepository;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import com.wooyeon.table.support.IntegrationTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** 주최 측 가상데이터로 적재 → 전 회차 자동 매칭까지 DB에서 끝까지 돌려 본다 (data/ 파일이 있을 때만) */
@IntegrationTest
@Transactional
class ApplicantImportServiceTest {

    private static final Path DATA = Path.of("data", "applicants.xlsx");

    @Autowired
    private ApplicantImportService applicantImportService;
    @Autowired
    private MatchingService matchingService;
    @Autowired
    private EventSlotJpaRepository eventSlotJpaRepository;
    @Autowired
    private ApplicationJpaRepository applicationJpaRepository;

    @Test
    void 가상데이터를_적재하고_날짜순으로_매칭하면_엔진_벤치마크와_같은_결과가_나온다() throws IOException {
        assumeTrue(Files.exists(DATA), "data/applicants.xlsx 없음");

        ApplicantImportResult first = importFile();
        ApplicantImportResult again = importFile();

        assertThat(first.totalRows()).isEqualTo(294);
        assertThat(first.imported()).isEqualTo(291);
        assertThat(first.skippedOutsideSeoul()).isEqualTo(3);
        assertThat(again.imported()).isZero();
        assertThat(again.alreadyImported()).isEqualTo(291);

        eventSlotJpaRepository.findAll().stream()
                .sorted(Comparator.comparing(EventSlot::getEventDate))
                .forEach(slot -> matchingService.runForSlot(slot.getId()));

        Map<MatchStatus, Long> byStatus = applicationJpaRepository.findAll().stream()
                .collect(Collectors.groupingBy(a -> a.getMatchStatus(), Collectors.counting()));
        // MatchingBenchmarkTest(그리디+개선)의 배정 212명 / 대체 안내 79명과 같아야 한다
        assertThat(byStatus).containsEntry(MatchStatus.CONFIRMED, 212L)
                .containsEntry(MatchStatus.ALTERNATIVE_OFFERED, 79L)
                .doesNotContainKey(MatchStatus.WAITING);
    }

    private ApplicantImportResult importFile() throws IOException {
        try (InputStream input = Files.newInputStream(DATA)) {
            return applicantImportService.importApplicants(input);
        }
    }
}
