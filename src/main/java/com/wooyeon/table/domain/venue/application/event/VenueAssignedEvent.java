package com.wooyeon.table.domain.venue.application.event;

/** 운영자 재시도로 식당이 배정됨 (확정 시점 자동 배정은 단체방 공지에 바로 들어가므로 발행하지 않는다) */
public record VenueAssignedEvent(Long tableId) {
}
