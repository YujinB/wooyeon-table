package com.wooyeon.table.domain.operation.application.dto;

import com.wooyeon.table.domain.application.domain.model.ParticipantStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 운영자용 회차 신청자 한 줄 (PRD 11절 신청자 목록: 프로필·희망 일정·결제 상태·이전 참석·신고 이력)
 *
 * @param attended     지금까지 참석 완료 횟수
 * @param noShow       노쇼(후보 포함) 횟수
 * @param canceledLate 제한 시점 이후 취소 횟수
 * @param reported     신고당한 횟수
 */
public record SlotApplicant(Long applicationId, Long userId, String name, Integer age, String gender, String mbti,
                            String ticketLabel, List<String> regions, ParticipantStatus status, String statusLabel,
                            String paymentStatusLabel, boolean confirmedHere, LocalDateTime submittedAt,
                            int attended, int noShow, int canceledLate, int reported) {
}
