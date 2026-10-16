package com.wooyeon.table.domain.application.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicantImportResult;
import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.repository.ApplicationRepository;
import com.wooyeon.table.domain.application.infrastructure.excel.ApplicantExcelParser;
import com.wooyeon.table.domain.application.infrastructure.excel.ApplicantRow;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.profile.application.service.ProfileCommandService;
import com.wooyeon.table.domain.profile.domain.model.Profile;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주최 측 신청자 엑셀을 사용자·프로필·신청으로 적재한다. 기존 신청자는 이미 입금했으므로 모의 결제 완료로 넣는다.
 * 엑셀의 참가 가능 날짜마다 회차를 만든다(날짜는 옮기지 않음). 같은 파일을 다시 올려도 중복 생성하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ApplicantImportService {

    /** 엑셀의 나이는 2026년 신청 당시 나이 */
    private static final int DATA_YEAR = 2026;
    private static final String LOGIN_ID_PREFIX = "applicant-";

    private final ApplicantExcelParser parser = new ApplicantExcelParser();
    private final UserCommandService userCommandService;
    private final ProfileCommandService profileCommandService;
    private final EventSlotCommandService eventSlotCommandService;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public ApplicantImportResult importApplicants(InputStream input) {
        List<ApplicantRow> rows = parse(input);
        int imported = 0;
        int outsideSeoul = 0;
        int noDate = 0;
        int already = 0;
        for (ApplicantRow row : rows) {
            if (row.regions().isEmpty()) {
                outsideSeoul++;
                continue;
            }
            if (row.availableDates().isEmpty()) {
                noDate++;
                continue;
            }
            Long userId = userCommandService.createParticipantIfAbsent(LOGIN_ID_PREFIX + row.rowNumber(),
                    row.name().isBlank() ? "신청자" + row.rowNumber() : row.name());
            if (applicationRepository.existsByUserId(userId)) {
                already++;
                continue;
            }
            profileCommandService.createIfAbsent(toProfile(userId, row));
            List<Long> slotIds = row.availableDates().stream().map(this::slotId).toList();
            applicationRepository.save(
                    Application.paidSubmission(userId, row.ticketType(), row.regions(), slotIds));
            imported++;
        }
        return new ApplicantImportResult(rows.size(), imported, outsideSeoul, noDate, already);
    }

    private List<ApplicantRow> parse(InputStream input) {
        try {
            return parser.parse(input);
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(ErrorCode.IMPORT_FAILED, e.getMessage());
        }
    }

    private Long slotId(LocalDate date) {
        return eventSlotCommandService.findOrCreate(date);
    }

    private static Profile toProfile(Long userId, ApplicantRow row) {
        return Profile.builder()
                .userId(userId)
                .birthYear(DATA_YEAR - row.age())
                .gender(row.gender())
                .mbti(row.mbti())
                .interests(row.interests())
                .traits(row.traits())
                .preferredTraits(row.preferredTraits())
                .mealBudgets(row.mealBudgets())
                .foodRestriction(row.foodRestriction())
                .build();
    }
}
