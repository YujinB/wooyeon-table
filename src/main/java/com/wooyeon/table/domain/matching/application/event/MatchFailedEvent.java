package com.wooyeon.table.domain.matching.application.event;

import java.time.LocalDate;
import java.util.List;

/**
 * 고른 마지막 날짜까지 테이블에 들지 못해 대체 안내로 넘어간 신청들 (MAT-11).
 * 매칭 트랜잭션 안에서 동기로 발행되므로 받는 쪽 변경도 같은 트랜잭션에 묶인다
 */
public record MatchFailedEvent(LocalDate eventDate, List<FailedApplication> applications) {

    public record FailedApplication(Long applicationId, Long userId) {
    }
}
