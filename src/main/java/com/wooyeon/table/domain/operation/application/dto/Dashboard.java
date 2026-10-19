package com.wooyeon.table.domain.operation.application.dto;

import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 운영 대시보드: 회차별 신청·결제·매칭 현황과 처리해야 할 예외 건수.
 * 정상 흐름은 자동으로 끝나므로 운영자는 예외 건수만 보면 된다 (PRD 18절 12단계)
 */
public record Dashboard(List<SlotOverview> slots, ExceptionCounts exceptions) {

    /**
     * @param applicants     이 날짜를 고른 신청 (다른 날짜로 확정된 신청 제외)
     * @param paymentPending 결제 대기·실패
     * @param waiting        결제 완료 · 매칭 대기
     * @param confirmed      이 회차로 확정
     * @param matchFailed    대체 일정·보관·환불 선택 대기
     * @param closed         취소·환불·이용권 보관
     */
    public record SlotOverview(Long slotId, LocalDate eventDate, LocalTime startTime, LocalDateTime matchAt,
                               EventSlotStatus status, long applicants, long paymentPending, long waiting,
                               long confirmed, long matchFailed, long closed, long tables) {
    }

    public record ExceptionCounts(int venue, int notification, int noShow, int reports, int urgentReports,
                                  int refund) {

        public int total() {
            return venue + notification + noShow + reports + refund;
        }
    }
}
