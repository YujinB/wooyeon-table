package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인(이용 제한 판단·참가 이력·운영 예외)이 참석 기록을 볼 때 쓴다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceQueryService {

    private final AttendanceRepository attendanceRepository;

    public List<AttendanceView> findByUser(Long userId) {
        return attendanceRepository.findAllByUserIdIn(List.of(userId)).stream().map(AttendanceView::from).toList();
    }

    /** 참석 상태별 인원 (운영 대시보드) */
    public Map<AttendanceStatus, Long> countByStatus() {
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        for (AttendanceStatus status : AttendanceStatus.values()) {
            counts.put(status, (long) attendanceRepository.findAllByStatus(status).size());
        }
        return counts;
    }

    /** 운영자가 확인할 노쇼 후보 */
    public List<AttendanceView> findNoShowCandidates() {
        return attendanceRepository.findAllByStatus(AttendanceStatus.NO_SHOW_CANDIDATE).stream()
                .map(AttendanceView::from)
                .toList();
    }
}
