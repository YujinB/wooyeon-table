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
public record Dashboard(Totals totals, List<SlotOverview> slots, ExceptionCounts exceptions) {

    /**
     * 전체 지표 (PRD 3·11절). 비율은 0~100 정수, 분모가 0이면 null
     *
     * @param paid             결제를 마친 적이 있는 신청 (이후 환불 포함)
     * @param matchSuccessRate 매칭이 결정된 신청(확정·실패) 중 확정 비율
     * @param autoConfirmRate  확정 테이블 중 운영자 개입 없이 확정된 비율 (수동 확정 경로가 없어 테이블이 있으면 100)
     * @param awaitingChoice   매칭 실패 후 대체 일정·보관·환불 선택을 기다리는 신청
     * @param attendanceRate   종료된 모임 확정자 중 참석 비율 (취소 제외)
     */
    public record Totals(long applications, long paid, long confirmed, Integer matchSuccessRate, long tables,
                         Integer autoConfirmRate, long awaitingChoice, long attended, long noShow,
                         Integer attendanceRate) {
    }

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
                                  int refund, int total) {

        public static ExceptionCounts of(int venue, int notification, int noShow, int reports, int urgentReports,
                                         int refund) {
            return new ExceptionCounts(venue, notification, noShow, reports, urgentReports, refund,
                    venue + notification + noShow + reports + refund);
        }
    }
}
