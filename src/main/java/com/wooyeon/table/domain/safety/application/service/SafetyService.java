package com.wooyeon.table.domain.safety.application.service;

import com.wooyeon.table.domain.chat.application.dto.RoomMembers;
import com.wooyeon.table.domain.chat.application.dto.TableMember;
import com.wooyeon.table.domain.chat.application.service.ChatMemberQueryService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.safety.application.dto.ReportCommand;
import com.wooyeon.table.domain.safety.application.dto.ReportReceipt;
import com.wooyeon.table.domain.safety.domain.model.SafetyReport;
import com.wooyeon.table.domain.safety.domain.model.UserBlock;
import com.wooyeon.table.domain.safety.domain.repository.SafetyReportRepository;
import com.wooyeon.table.domain.safety.domain.repository.UserBlockRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고·차단 (SAF-01). 같은 단체방(테이블) 사람을 좌석 번호로 고른다. 신고하면 기본으로 차단도 한다.
 * 차단·신고 관계는 다음 매칭부터 같은 테이블 금지로 반영된다. 아직 끝나지 않은 테이블이면 긴급 신고로 표시해
 * 운영자 예외함 맨 위에 올린다 (확정 후 이동·분리는 안전 문제에서만 운영자가 한다, PRD 15절)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SafetyService {

    private final SafetyReportRepository safetyReportRepository;
    private final UserBlockRepository userBlockRepository;
    private final ChatMemberQueryService chatMemberQueryService;
    private final EventSlotQueryService eventSlotQueryService;

    public ReportReceipt report(Long userId, Long roomId, ReportCommand command) {
        RoomMembers room = chatMemberQueryService.roomOfMember(userId, roomId);
        TableMember target = other(room, userId, command.seatNo());
        boolean urgent = eventSlotQueryService.getSlot(room.slotId()).status() != EventSlotStatus.COMPLETED;
        SafetyReport report = safetyReportRepository.save(SafetyReport.builder()
                .reporterUserId(userId)
                .reportedUserId(target.userId())
                .tableId(room.tableId())
                .reason(command.reason())
                .detail(command.detail())
                .evidenceUrl(command.evidenceUrl())
                .urgent(urgent)
                .build());
        if (command.block()) {
            block(userId, target.userId());
        }
        if (urgent) {
            log.warn("긴급 안전 신고 {}: 진행 전·중인 테이블 {} → 운영자 확인 필요", report.getId(), room.tableId());
        }
        return new ReportReceipt(report.getId(), command.block(), urgent);
    }

    public void block(Long userId, Long roomId, int seatNo) {
        RoomMembers room = chatMemberQueryService.roomOfMember(userId, roomId);
        block(userId, other(room, userId, seatNo).userId());
    }

    private void block(Long blocker, Long blocked) {
        if (!userBlockRepository.exists(blocker, blocked)) {
            userBlockRepository.save(UserBlock.of(blocker, blocked));
        }
    }

    private static TableMember other(RoomMembers room, Long userId, int seatNo) {
        return room.seat(seatNo)
                .filter(member -> !member.userId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT,
                        "같은 단체방의 다른 참가자 좌석이 아닙니다: " + seatNo));
    }
}
