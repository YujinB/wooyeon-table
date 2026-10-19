package com.wooyeon.table.domain.resolution.application.dto;

import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.payment.application.dto.PaymentResult;
import com.wooyeon.table.domain.resolution.domain.model.ResolutionChoice;
import java.time.LocalDate;

/**
 * @param movedToEventDate 일정 이동이면 옮겨 간 날짜
 * @param refund           환불이면 환불 결과
 */
public record ResolutionResult(Long applicationId, ResolutionChoice choice, MatchStatus matchStatus,
                               PaymentStatus paymentStatus, LocalDate movedToEventDate, PaymentResult refund) {
}
