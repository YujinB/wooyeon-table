package com.wooyeon.table.domain.application.application.dto;

/**
 * @param skippedOutsideSeoul 서울 4개 지역을 하나도 고르지 않아 제외
 * @param skippedNoDate       참가 가능 날짜가 없어 제외
 * @param alreadyImported     이미 적재된 신청자 (재실행)
 */
public record ApplicantImportResult(int totalRows, int imported, int skippedOutsideSeoul, int skippedNoDate,
                                    int alreadyImported) {
}
