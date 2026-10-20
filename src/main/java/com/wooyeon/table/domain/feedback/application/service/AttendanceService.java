package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import com.wooyeon.table.domain.matching.application.dto.ConfirmedTable;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참석 기록 (FDB-01). 확정되면 참석 예정으로 만들고, 앱 체크인으로 참석 완료가 된다.
 * 확정 후 취소는 환불 마감 전이면 사전 취소, 이후면 제한 시점 이후 취소로 남는다 (환불 없음, 이용 제한 판단에 쓰임)
 */
@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MatchingQueryService matchingQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final EventSlotQueryService eventSlotQueryService;
    private final Clock clock;
    private final long checkInOpensHoursBefore;
    private final boolean checkInAnytime;

    public AttendanceService(AttendanceRepository attendanceRepository, MatchingQueryService matchingQueryService,
                             ApplicationQueryService applicationQueryService,
                             EventSlotQueryService eventSlotQueryService, Clock clock,
                             @Value("${attendance.check-in-opens-hours-before:2}") long checkInOpensHoursBefore,
                             @Value("${attendance.check-in-anytime:false}") boolean checkInAnytime) {
        this.attendanceRepository = attendanceRepository;
        this.matchingQueryService = matchingQueryService;
        this.applicationQueryService = applicationQueryService;
        this.eventSlotQueryService = eventSlotQueryService;
        this.clock = clock;
        this.checkInOpensHoursBefore = checkInOpensHoursBefore;
        this.checkInAnytime = checkInAnytime;
    }

    /** 회차 확정 테이블 멤버의 참석 기록을 만든다 (이미 있으면 건너뜀) */
    public int scheduleForSlot(Long slotId) {
        int created = 0;
        for (ConfirmedTable table : matchingQueryService.findConfirmedTables(slotId)) {
            for (int i = 0; i < table.applicationIds().size(); i++) {
                Long applicationId = table.applicationIds().get(i);
                if (attendanceRepository.findByApplicationId(applicationId).isEmpty()) {
                    attendanceRepository.save(Attendance.scheduled(applicationId, table.userIds().get(i),
                            table.tableId(), slotId));
                    created++;
                }
            }
        }
        return created;
    }

    public AttendanceView checkIn(Long userId, Long applicationId) {
        Attendance attendance = owned(userId, applicationId);
        EventSlotInfo slot = eventSlotQueryService.getSlot(attendance.getSlotId());
        LocalDateTime now = LocalDateTime.now(clock);
        if (slot.status() == EventSlotStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "이미 종료된 모임입니다");
        }
        if (!checkInAnytime && now.isBefore(slot.startsAt().minusHours(checkInOpensHoursBefore))) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "체크인은 모임 시작 " + checkInOpensHoursBefore + "시간 전부터 할 수 있어요");
        }
        attendance.checkIn(now);
        return AttendanceView.from(attendance);
    }

    public AttendanceView cancel(Long userId, Long applicationId) {
        Attendance attendance = owned(userId, applicationId);
        EventSlotInfo slot = eventSlotQueryService.getSlot(attendance.getSlotId());
        LocalDateTime now = LocalDateTime.now(clock);
        attendance.cancel(now.isBefore(slot.refundClosesAt()), now);
        return AttendanceView.from(attendance);
    }

    @Transactional(readOnly = true)
    public AttendanceView get(Long userId, Long applicationId) {
        return AttendanceView.from(owned(userId, applicationId));
    }

    private Attendance owned(Long userId, Long applicationId) {
        ApplicationInfo application = applicationQueryService.getOwnedApplication(userId, applicationId);
        return attendanceRepository.findByApplicationId(application.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                        "매칭이 확정된 신청만 참석을 기록할 수 있습니다"));
    }
}
