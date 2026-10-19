package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.application.dto.EventCloseResult;
import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import com.wooyeon.table.domain.notification.application.service.NotificationService;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 종료 처리 (FDB-01, PRD 14절 '행사 종료 후'): 체크인하지 않은 참가자를 노쇼 후보로 표시하고,
 * 취소하지 않은 참가자에게 피드백을 요청한다. 운영자는 노쇼 후보만 확인해 확정·정정한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventCloseService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN);

    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;
    private final EventSlotQueryService eventSlotQueryService;
    private final EventSlotCommandService eventSlotCommandService;
    private final NotificationService notificationService;
    private final Clock clock;

    public EventCloseResult close(Long slotId) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(slotId);
        attendanceService.scheduleForSlot(slotId);
        eventSlotCommandService.complete(slotId);

        List<Attendance> attendances = attendanceRepository.findAllBySlotId(slotId);
        int candidates = (int) attendances.stream().filter(Attendance::markNoShowCandidateIfAbsent).count();
        int attended = 0;
        for (Attendance attendance : attendances) {
            if (attendance.getStatus() == AttendanceStatus.ATTENDED) {
                attended++;
            }
            if (!attendance.getStatus().isCanceled()) {
                notificationService.notifyNow(attendance.getUserId(), attendance.getApplicationId(),
                        NotificationType.FEEDBACK_REQUESTED, Map.of("date", slot.eventDate().format(DATE)),
                        "FEEDBACK_REQUESTED:" + attendance.getApplicationId());
            }
        }
        int canceled = (int) attendances.stream().filter(attendance -> attendance.getStatus().isCanceled()).count();
        log.info("회차 {} 종료 처리: 참석 {} · 노쇼 후보 {} · 취소 {}", slot.eventDate(), attended, candidates, canceled);
        return new EventCloseResult(slotId, attendances.size(), attended, candidates, canceled);
    }

    /** 운영자 확인 대기 중인 노쇼 후보 */
    @Transactional(readOnly = true)
    public List<AttendanceView> noShowCandidates() {
        return attendanceRepository.findAllByStatus(AttendanceStatus.NO_SHOW_CANDIDATE).stream()
                .map(AttendanceView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceView> slotAttendances(Long slotId) {
        return attendanceRepository.findAllBySlotId(slotId).stream().map(AttendanceView::from).toList();
    }

    /** 노쇼 확정(noShow=true) 또는 참석으로 정정 */
    public AttendanceView review(Long attendanceId, boolean noShow) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "참석 기록을 찾을 수 없습니다"));
        attendance.review(noShow, LocalDateTime.now(clock));
        return AttendanceView.from(attendance);
    }
}
