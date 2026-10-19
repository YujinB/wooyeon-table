package com.wooyeon.table.domain.matching.domain.model;

/** 최종 배치 사유 (MAT-12) */
public enum PlacementReason {
    /** 최초 조합에서 배치 */
    INITIAL,
    /** 테이블 간 이동·교환으로 점수를 높이면서 옮겨짐 */
    OPTIMIZED,
    /** 최초 배치 실패·테이블 해제 후 다른 테이블로 자동 재배치 */
    REALLOCATED
}
