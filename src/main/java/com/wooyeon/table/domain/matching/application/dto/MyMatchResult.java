package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.matching.domain.model.FailureOutcome;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 참가자용 내 매칭 결과 (MAT-12). 다른 참가자의 이름·프로필과 항목별 점수는 담지 않고 테이블 총점만 준다.
 *
 * @param table   확정된 경우만
 * @param failure 이번 회차에서 테이블에 들지 못한 경우만 (이월 또는 대체 안내)
 */
public record MyMatchResult(Long applicationId, MatchStatus matchStatus, String message, MyTable table,
                            MyFailure failure) {

    /**
     * @param score   테이블 선호 점수(0~100, 반올림) — 화면의 '취향 매칭'
     * @param reasons 왜 이 테이블인지 참가자에게 보여줄 문장
     */
    public record MyTable(Long tableId, LocalDate eventDate, LocalTime startTime, String region, int size, int minAge, int maxAge,
                          int maleCount, int femaleCount, int score, List<String> commonInterests,
                          List<String> reasons) {
    }

    public record MyFailure(LocalDate eventDate, String reason, FailureOutcome outcome, String outcomeDescription,
                            LocalDate nextEventDate) {
    }
}
