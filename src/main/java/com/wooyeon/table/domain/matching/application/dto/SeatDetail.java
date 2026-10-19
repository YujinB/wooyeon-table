package com.wooyeon.table.domain.matching.application.dto;

import com.wooyeon.table.domain.matching.domain.model.PlacementReason;
import java.util.List;

/**
 * 운영자용 좌석 상세 — 참가자 한 명의 프로필 요약과 배치 사유
 *
 * @param preferenceMet 원하는 상대 성향을 다른 멤버 중 누군가 충족하는지
 */
public record SeatDetail(Long applicationId, Long userId, String name, Integer age, String gender, String mbti,
                         List<String> interests, PlacementReason placement, String placementDescription,
                         int relocationCount, boolean preferenceMet) {
}
