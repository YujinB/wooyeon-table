package com.wooyeon.table.domain.matching.domain.model;

/**
 * @param preferenceMet   다른 멤버 중 한 명 이상이 이 사람이 원하는 상대 성향을 하나라도 충족하는지
 * @param relocationCount 배치 실패·테이블 해제로 인한 자동 재배치 횟수
 */
public record TableMember(long candidateId, PlacementReason placement, int relocationCount, boolean preferenceMet) {
}
