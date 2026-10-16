package com.wooyeon.table.domain.matching.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 최종 배치 사유 (MAT-12) */
@Getter
@RequiredArgsConstructor
public enum PlacementReason {
    /** 최초 조합에서 배치 */
    INITIAL("처음 조합에서 나이대·성비·궁합 점수가 가장 잘 맞는 테이블에 배치됨"),
    /** 테이블 간 이동·교환으로 점수를 높이면서 옮겨짐 */
    OPTIMIZED("테이블 간 자리 조정으로 더 잘 맞는 테이블로 옮겨짐"),
    /** 최초 배치 실패·테이블 해제 후 다른 테이블로 자동 재배치 */
    REALLOCATED("처음 조합에서 자리가 맞지 않아 조건에 맞는 다른 테이블로 자동 재배치됨");

    private final String description;
}
