package com.wooyeon.table.domain.operation.application.service;

import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.dto.SeatDetail;
import com.wooyeon.table.domain.matching.application.dto.TableDetail;
import com.wooyeon.table.domain.matching.application.dto.TableSummary;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.operation.application.dto.SlotApplicant;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참가자 목록과 테이블 결과 CSV (PRD 16절). 엑셀에서 한글이 깨지지 않게 UTF-8 BOM을 붙인다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CsvExportService {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final OperationQueryService operationQueryService;
    private final MatchingQueryService matchingQueryService;
    private final EventSlotQueryService eventSlotQueryService;

    public record CsvFile(String filename, byte[] content) {
    }

    public CsvFile applicants(Long slotId) {
        LocalDate date = eventSlotQueryService.getSlot(slotId).eventDate();
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("신청ID", "이름", "나이", "성별", "MBTI", "이용권", "희망 지역", "상태", "결제", "이 회차 확정",
                "신청 시각", "지난 참석", "노쇼", "늦은 취소", "신고당함"));
        for (SlotApplicant a : operationQueryService.applicants(slotId)) {
            rows.add(Stream.<Object>of(a.applicationId(), a.name(), a.age(), a.gender(), a.mbti(), a.ticketLabel(),
                    String.join(" ", a.regions()), a.statusLabel(), a.paymentStatusLabel(),
                    a.confirmedHere() ? "Y" : "N", a.submittedAt(), a.attended(), a.noShow(), a.canceledLate(),
                    a.reported()).toList());
        }
        return new CsvFile("applicants-" + date + ".csv", encode(rows));
    }

    /** 좌석 한 줄씩: 테이블 정보 + 좌석별 참가자·배치 사유 */
    public CsvFile tables(Long slotId) {
        LocalDate date = eventSlotQueryService.getSlot(slotId).eventDate();
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("테이블", "지역", "인원", "나이 범위", "여:남", "점수", "성비", "MBTI 궁합", "관심사", "희망 성향",
                "새로운 사람", "좌석", "이름", "나이", "성별", "MBTI", "관심사", "배치 사유", "희망 성향 충족"));
        for (TableSummary summary : matchingQueryService.getSlotResult(slotId).tables()) {
            TableDetail t = matchingQueryService.getTableDetail(summary.tableId());
            for (int i = 0; i < t.seats().size(); i++) {
                SeatDetail s = t.seats().get(i);
                rows.add(Stream.<Object>of(t.tableNo(), t.regionLabel(), t.size(), t.minAge() + "~" + t.maxAge(),
                        t.femaleCount() + ":" + t.maleCount(), t.score().total(), t.score().genderBalance(),
                        t.score().mbti(), t.score().interest(), t.score().preference(), t.score().novelty(), i + 1,
                        s.name(), s.age(), s.gender(), s.mbti(), String.join(" / ", s.interests()),
                        s.placementDescription(), s.preferenceMet() ? "Y" : "N").toList());
            }
        }
        return new CsvFile("tables-" + date + ".csv", encode(rows));
    }

    private static byte[] encode(List<List<Object>> rows) {
        String body = rows.stream()
                .map(row -> row.stream().map(CsvExportService::cell).collect(Collectors.joining(",")))
                .collect(Collectors.joining("\r\n", "", "\r\n"));
        byte[] text = body.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[BOM.length + text.length];
        System.arraycopy(BOM, 0, content, 0, BOM.length);
        System.arraycopy(text, 0, content, BOM.length, text.length);
        return content;
    }

    /** 쉼표·따옴표·줄바꿈이 있으면 따옴표로 감싼다 */
    private static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
