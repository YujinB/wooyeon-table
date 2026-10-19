package com.wooyeon.table.domain.operation.application.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 예외 처리함 한 건 (MAT-13). 자동으로 해결되지 않아 운영자 확인이 필요한 것만 올라온다.
 *
 * @param refId 처리 API에 넘길 id (식당·신고·환불은 테이블·신고·신청 id, 알림은 알림 id, 노쇼는 참석 id)
 */
public record ExceptionItem(ExceptionType type, String typeLabel, boolean urgent, String title, String detail,
                            Long refId, Long slotId, Long tableId, Long applicationId, LocalDateTime at) {

    @Getter
    @RequiredArgsConstructor
    public enum ExceptionType {
        REPORT("신고"),
        VENUE("식당 배정"),
        REFUND("환불"),
        NOTIFICATION("알림 발송"),
        NO_SHOW("노쇼 후보");

        private final String label;
    }
}
