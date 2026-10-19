package com.wooyeon.table.domain.matching.application.event;

import java.time.LocalDate;
import java.util.List;

/** 이번 회차 테이블에 들지 못해 본인이 고른 다음 날짜 회차로 이월된 신청들 (자동 재배치 중 안내) */
public record MatchCarriedOverEvent(LocalDate eventDate, List<CarriedApplication> applications) {

    public record CarriedApplication(Long applicationId, Long userId, LocalDate nextEventDate) {
    }
}
