package com.wooyeon.table.domain.application.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 참가자 화면에 보여줄 한 가지 상태 (STA-01). 저장하지 않고 신청·결제·매칭 상태에서 계산한다
 */
@Getter
@RequiredArgsConstructor
public enum ParticipantStatus {
    PAYMENT_PENDING("신청 완료 · 결제 대기", "결제하면 매칭 대기에 들어가요. 신청 마감 전까지 결제해 주세요."),
    PAYMENT_FAILED("결제 실패", "결제가 승인되지 않았어요. 신청 마감 전까지 다시 결제할 수 있어요."),
    MATCH_WAITING("매칭 대기", "신청 마감 시각에 자동으로 테이블을 만들고 결과를 알려드려요."),
    MATCH_CONFIRMED("매칭 확정", "테이블이 확정됐어요. 모임 전날 낮 12시에 장소와 예약명을 안내해요."),
    MATCH_FAILED("매칭 실패", "고른 날짜에 테이블을 만들지 못했어요. 추천 일정 이동·이용권 보관·환불 중에서 골라 주세요."),
    CANCELED("취소", "신청을 취소했어요.");

    private final String label;
    private final String message;

    public static ParticipantStatus of(ApplicationStatus application, PaymentStatus payment, MatchStatus match) {
        if (application == ApplicationStatus.CANCELED) {
            return CANCELED;
        }
        if (match == MatchStatus.CONFIRMED) {
            return MATCH_CONFIRMED;
        }
        if (match == MatchStatus.ALTERNATIVE_OFFERED || match == MatchStatus.NO_MATCH) {
            return MATCH_FAILED;
        }
        if (payment == PaymentStatus.PENDING) {
            return PAYMENT_PENDING;
        }
        if (payment == PaymentStatus.FAILED) {
            return PAYMENT_FAILED;
        }
        return MATCH_WAITING;
    }
}
