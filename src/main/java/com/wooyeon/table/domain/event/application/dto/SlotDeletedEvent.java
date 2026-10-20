package com.wooyeon.table.domain.event.application.dto;

/** 운영자가 신청자 없는 회차를 지웠다 (잘못 만든 회차). 그 회차로 보낸 모집 알림을 거둬들인다 */
public record SlotDeletedEvent(Long slotId) {
}
