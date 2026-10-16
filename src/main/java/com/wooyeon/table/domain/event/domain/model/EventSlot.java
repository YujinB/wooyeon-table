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
    /** 진행 2일 전부터 환불이 제한되므로(PAY-02) 그 전에 매칭 결과가 나오도록 한다 */
    public static final int DEFAULT_MATCH_DAYS_BEFORE = 2;
    public static final LocalTime DEFAULT_MATCH_TIME = LocalTime.NOON;

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
        return new EventSlot(eventDate, DEFAULT_START_TIME,
                eventDate.minusDays(DEFAULT_MATCH_DAYS_BEFORE).atTime(DEFAULT_MATCH_TIME));
    }

    public static EventSlot of(LocalDate eventDate, LocalTime startTime, LocalDateTime matchAt) {
        if (!matchAt.isBefore(eventDate.atTime(startTime))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "매칭 실행 시각은 행사 시작 전이어야 합니다");
        }
        return new EventSlot(eventDate, startTime, matchAt);
    }

    public boolean isRecruiting() {
        return status == EventSlotStatus.RECRUITING;
    }

    public void markMatched() {
        if (status != EventSlotStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "이미 매칭한 회차: " + id);
        }
        this.status = EventSlotStatus.MATCHED;
    }
}
