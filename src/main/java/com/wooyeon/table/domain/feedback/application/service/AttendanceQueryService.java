package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.global.audit.AuditHistoryReader;
import com.wooyeon.table.global.audit.StatusChange;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인(이용 제한 판단·참가 이력·운영 예외)이 참석 기록을 볼 때 쓴다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceQueryService {

    private final AttendanceRepository attendanceRepository;
    private final AuditHistoryReader auditHistoryReader;

    public List<AttendanceView> findByUser(Long userId) {
        return attendanceRepository.findAllByUserIdIn(List.of(userId)).stream().map(AttendanceView::from).toList();
    }

    /** 사용자별 참석 기록 (운영자 신청자 목록) */
    public Map<Long, List<AttendanceView>> findByUsers(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return attendanceRepository.findAllByUserIdIn(userIds).stream()
                .map(AttendanceView::from)
                .collect(Collectors.groupingBy(AttendanceView::userId));
    }

    /** 참석 상태별 인원 (운영 대시보드) */
    public Map<AttendanceStatus, Long> countByStatus() {
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        for (AttendanceStatus status : AttendanceStatus.values()) {
            counts.put(status, (long) attendanceRepository.findAllByStatus(status).size());
        }
        return counts;
    }

    /** 이 신청의 참석 상태 변경 이력 (PRD 16절) */
    public List<StatusChange> statusHistory(Long applicationId) {
        Map<String, Function<Attendance, String>> fields = new LinkedHashMap<>();
        fields.put("참석", attendance -> attendance.getStatus().getLabel());
        return auditHistoryReader.changes(Attendance.class, "applicationId", applicationId, fields);
    }

    /** 운영자가 확인할 노쇼 후보 */
    public List<AttendanceView> findNoShowCandidates() {
        return attendanceRepository.findAllByStatus(AttendanceStatus.NO_SHOW_CANDIDATE).stream()
                .map(AttendanceView::from)
                .toList();
    }
}
