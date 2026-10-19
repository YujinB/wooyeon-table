package com.wooyeon.table.domain.notification.domain.model;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 시나리오와 문구 템플릿 (PRD 14절). {이름} 자리를 값으로 채운다.
 * 운영자가 템플릿을 바꾸는 기능(PRD 운영자 '자동 메시지 관리')은 P1이라 코드 상수로 둔다
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    APPLICATION_SUBMITTED("신청이 접수됐어요",
            "{dates} 신청이 접수됐어요. {deadline}까지 결제하면 매칭 대기에 들어가요. 신청 마감 전에는 앱에서 바로 취소할 수 있어요."),
    PAYMENT_COMPLETED("결제가 완료됐어요",
            "{ticket} {amount}원 결제가 완료됐어요. {matchAt}에 자동 매칭 결과를 알려드려요."),
    REALLOCATING("다른 날짜 테이블을 찾고 있어요",
            "{date}에는 조건에 맞는 테이블을 만들지 못했어요. 고르신 다음 날짜({nextDate}) 매칭에 자동으로 참여해요."),
    MATCH_CONFIRMED("매칭이 확정됐어요",
            "{date} {time} {region} 테이블이 확정됐어요. 식당: {venue}. 앱 단체방에서 함께할 분들과 안내를 확인해 주세요."),
    MATCH_FAILED("이번에는 테이블을 만들지 못했어요",
            "{date} 매칭에 실패했어요 ({reason}). 추천 일정 이동·이용권 보관·환불 중에서 골라 주세요."),
    SCHEDULE_MOVED("일정을 옮겼어요",
            "{date} 회차 매칭 대기로 옮겼어요. 결제는 그대로 유지되고 {matchAt}에 자동 매칭돼요."),
    REFUND_COMPLETED("환불이 완료됐어요",
            "{amount}원이 결제수단으로 환불됐어요."),
    EVENT_REMINDER("내일 모임이 있어요",
            "내일 {time} {venue}에서 만나요. 주소: {address}. 예약명: {reservationName}. {policy}");

    private final String title;
    private final String template;

    public String render(Map<String, String> values) {
        String body = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            body = body.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return body;
    }
}
