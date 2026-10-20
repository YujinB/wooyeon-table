package com.wooyeon.table.domain.payment.domain.model;

import java.util.List;

/**
 * 환불·취소 규정 (PAY-02, PRD 15절 "진행 2일 전 환불 불가, 당일 취소 및 단체 채팅방 퇴장 불가", 주최 측 Q&A).
 * 결제 전 화면과 신청 완료 화면에 같은 문구를 보여준다
 */
public final class RefundPolicy {

    public static final String SUMMARY = "모임 2일 전(화요일 24:00)까지 취소하면 전액 자동 환불돼요";

    public static final List<String> RULES = List.of(
            "모임 2일 전(목요일 모임이면 화요일 24:00)까지는 앱에서 바로 취소할 수 있고, 결제수단으로 전액 자동 환불돼요.",
            "그 이후에는 환불되지 않아요. 신청 마감과 자동 매칭도 같은 시각에 진행돼요.",
            "매칭이 확정된 뒤 당일 취소·노쇼를 하면 다음 매칭 우선순위가 낮아지고, 반복되면 신청이 제한돼요.",
            "확정된 모임의 단체 채팅방은 모임이 끝날 때까지 나갈 수 없어요.",
            "매칭에 실패하면 추천 일정으로 이동·이용권 보관·환불 중에서 고를 수 있어요. 환불은 운영자 승인 없이 자동으로 처리돼요.",
            "멤버십은 기간 안에 횟수 제한 없이 신청할 수 있고, 매칭에 실패하면 실패한 횟수만큼 기간이 늘어나요.");

    private RefundPolicy() {
    }
}
