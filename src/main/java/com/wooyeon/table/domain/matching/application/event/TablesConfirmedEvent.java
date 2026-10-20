package com.wooyeon.table.domain.matching.application.event;

/**
 * 회차 매칭으로 테이블이 확정됨 (INF-02·04: 확정과 동시에 식당 배정·단체방 생성).
 * 매칭 트랜잭션 안에서 동기로 발행된다
 */
public record TablesConfirmedEvent(Long slotId) {
}
