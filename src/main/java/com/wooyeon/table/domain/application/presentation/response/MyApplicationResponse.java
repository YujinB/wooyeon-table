package com.wooyeon.table.domain.application.presentation.response;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.domain.model.ApplicationStatus;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.ParticipantStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 내 신청 한 건. 화면용 상태 하나(status, STA-01)와 신청·결제·매칭 상태를 함께 주고(PRD 12절, PAY-01),
 * 다음 모임 회차의 예상 일정(schedule, STA-02)을 붙인다
 */
public record MyApplicationResponse(Long applicationId, TicketType ticketType, String ticketLabel, int price,
                                    List<String> regions, List<LocalDate> eventDates, LocalDate confirmedEventDate,
                                    ParticipantStatus status, String statusLabel, String statusMessage,
                                    ApplicationStatus applicationStatus, PaymentStatus paymentStatus,
                                    String paymentStatusLabel, MatchStatus matchStatus, Schedule schedule,
                                    LocalDateTime submittedAt) {

    /**
     * 다음(또는 확정된) 회차 일정. matchAnnounceAt = 신청·결제 마감이자 자동 매칭 결과 발표,
     * refundClosesAt 이후 취소는 환불 불가, reminderAt에 장소·예약명 안내
     */
    public record Schedule(LocalDate eventDate, LocalDateTime eventStartsAt, LocalDateTime matchAnnounceAt,
                           LocalDateTime refundClosesAt, LocalDateTime reminderAt) {

        static Schedule of(EventSlotInfo slot) {
            return new Schedule(slot.eventDate(), slot.startsAt(), slot.matchAt(), slot.refundClosesAt(),
                    slot.reminderAt());
        }
    }

    public static MyApplicationResponse of(ApplicationInfo application, Map<Long, EventSlotInfo> slots) {
        List<EventSlotInfo> preferred = application.preferredSlotIds().stream()
                .map(slots::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(EventSlotInfo::eventDate))
                .toList();
        EventSlotInfo confirmedSlot = application.confirmedSlotId() == null
                ? null : slots.get(application.confirmedSlotId());
        ParticipantStatus status = ParticipantStatus.of(application.applicationStatus(),
                application.paymentStatus(), application.matchStatus());
        List<String> regions = application.regions().stream()
                .sorted(Comparator.naturalOrder())
                .map(Region::getLabel)
                .toList();
        return new MyApplicationResponse(application.id(), application.ticketType(),
                application.ticketType().getLabel(), application.ticketType().getPrice(), regions,
                preferred.stream().map(EventSlotInfo::eventDate).toList(),
                confirmedSlot == null ? null : confirmedSlot.eventDate(),
                status, status.getLabel(), status.getMessage(),
                application.applicationStatus(), application.paymentStatus(),
                application.paymentStatus().getLabel(), application.matchStatus(),
                schedule(status, confirmedSlot, preferred), application.submittedAt());
    }

    /** 확정이면 확정 회차, 대기 중이면 아직 모집 중인 가장 가까운 회차. 취소·실패면 없음 */
    private static Schedule schedule(ParticipantStatus status, EventSlotInfo confirmedSlot,
                                     List<EventSlotInfo> preferred) {
        if (status == ParticipantStatus.MATCH_CONFIRMED && confirmedSlot != null) {
            return Schedule.of(confirmedSlot);
        }
        if (status == ParticipantStatus.CANCELED || status == ParticipantStatus.MATCH_FAILED) {
            return null;
        }
        return preferred.stream().filter(EventSlotInfo::isRecruiting).findFirst().map(Schedule::of).orElse(null);
    }
}
