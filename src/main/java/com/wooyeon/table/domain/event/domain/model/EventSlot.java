package com.wooyeon.table.domain.event.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모집 회차 (날짜 단위). 한 회차 안에서 지역별 테이블이 여러 개 만들어진다.
 * 신청 마감 시각에 자동 매칭을 실행한다.
 */
@Getter
@Entity
@Table(name = "event_slots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventSlot extends BaseTimeEntity {

    public static final LocalTime DEFAULT_START_TIME = LocalTime.of(19, 30);
    /**
     * 환불 불가 시점 = 행사 전날 0시 (목요일 행사 → 화요일 24:00, 주최 측 Q&A·PAY-02).
     * 신청 마감·자동 매칭도 기본적으로 이 시각에 한다 — 환불이 막히기 직전까지 신청을 받고, 마감하자마자 결과를 알린다
     */
    public static final int REFUND_CLOSE_DAYS_BEFORE = 1;
    /** 모임 전날 이 시각에 시간·장소·예약명을 안내한다 (INF, W3-2) */
    public static final LocalTime REMINDER_TIME = LocalTime.NOON;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate eventDate;

    @Column(nullable = false)
    private LocalTime startTime;

    /** 신청 마감 = 자동 매칭 실행 시각 */
    @Column(nullable = false)
    private LocalDateTime matchAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventSlotStatus status;

    private EventSlot(LocalDate eventDate, LocalTime startTime, LocalDateTime matchAt) {
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.matchAt = matchAt;
        this.status = EventSlotStatus.RECRUITING;
    }

    public static EventSlot withDefaultSchedule(LocalDate eventDate) {
        return new EventSlot(eventDate, DEFAULT_START_TIME, refundClosesAt(eventDate));
    }

    public static EventSlot of(LocalDate eventDate, LocalTime startTime, LocalDateTime matchAt) {
        if (!matchAt.isBefore(eventDate.atTime(startTime))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "매칭 실행 시각은 행사 시작 전이어야 합니다");
        }
        return new EventSlot(eventDate, startTime, matchAt);
    }

    public static LocalDateTime refundClosesAt(LocalDate eventDate) {
        return eventDate.minusDays(REFUND_CLOSE_DAYS_BEFORE).atStartOfDay();
    }

    public static LocalDateTime reminderAt(LocalDate eventDate) {
        return eventDate.minusDays(1).atTime(REMINDER_TIME);
    }

    public boolean isRecruiting() {
        return status == EventSlotStatus.RECRUITING;
    }

    /** 모집 중이고 신청 마감(매칭 실행 시각) 전 */
    public boolean isOpenForApplication(LocalDateTime now) {
        return isRecruiting() && now.isBefore(matchAt);
    }

    public void markMatched() {
        if (status != EventSlotStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "이미 매칭한 회차: " + id);
        }
        this.status = EventSlotStatus.MATCHED;
    }
}
