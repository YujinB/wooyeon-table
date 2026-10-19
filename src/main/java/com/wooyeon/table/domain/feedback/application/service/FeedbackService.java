package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.feedback.application.dto.FeedbackCommand;
import com.wooyeon.table.domain.feedback.application.dto.FeedbackForm;
import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.domain.feedback.domain.model.Feedback;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import com.wooyeon.table.domain.feedback.domain.repository.FeedbackRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모임 후기 (FDB-02·03). 행사 종료 처리 후, 참석했거나 체크인만 빠뜨린(노쇼 후보) 참가자가 한 번 낸다
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private static final Set<AttendanceStatus> CAN_REVIEW = Set.of(AttendanceStatus.ATTENDED,
            AttendanceStatus.NO_SHOW_CANDIDATE);

    private final FeedbackRepository feedbackRepository;
    private final AttendanceRepository attendanceRepository;
    private final ApplicationQueryService applicationQueryService;
    private final EventSlotQueryService eventSlotQueryService;

    @Transactional(readOnly = true)
    public FeedbackForm form(Long userId, Long applicationId) {
        applicationQueryService.getOwnedApplication(userId, applicationId);
        Optional<Feedback> mine = feedbackRepository.findByApplicationId(applicationId);
        String reason = unavailableReason(applicationId).orElse(null);
        return new FeedbackForm(applicationId, reason == null && mine.isEmpty(), reason, mine.isPresent(),
                mine.map(FeedbackService::toCommand).orElse(null));
    }

    public Feedback submit(Long userId, Long applicationId, FeedbackCommand command) {
        applicationQueryService.getOwnedApplication(userId, applicationId);
        unavailableReason(applicationId).ifPresent(reason -> {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, reason);
        });
        if (feedbackRepository.findByApplicationId(applicationId).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST, "이미 후기를 냈습니다");
        }
        Attendance attendance = attendanceRepository.findByApplicationId(applicationId).orElseThrow();
        return feedbackRepository.save(Feedback.builder()
                .applicationId(applicationId)
                .userId(userId)
                .tableId(attendance.getTableId())
                .slotId(attendance.getSlotId())
                .tableMood(command.tableMood())
                .conversation(command.conversation())
                .venue(command.venue())
                .rejoinIntent(command.rejoinIntent())
                .recommendScore(command.recommendScore())
                .comment(command.comment())
                .build());
    }

    private Optional<String> unavailableReason(Long applicationId) {
        Optional<Attendance> attendance = attendanceRepository.findByApplicationId(applicationId);
        if (attendance.isEmpty()) {
            return Optional.of("매칭이 확정된 모임만 후기를 남길 수 있어요");
        }
        if (eventSlotQueryService.getSlot(attendance.get().getSlotId()).status() != EventSlotStatus.COMPLETED) {
            return Optional.of("모임이 끝난 뒤에 후기를 남길 수 있어요");
        }
        if (!CAN_REVIEW.contains(attendance.get().getStatus())) {
            return Optional.of("참석한 모임만 후기를 남길 수 있어요 (" + attendance.get().getStatus().getLabel() + ")");
        }
        return Optional.empty();
    }

    private static FeedbackCommand toCommand(Feedback feedback) {
        return new FeedbackCommand(feedback.getTableMood(), feedback.getConversation(), feedback.getVenue(),
                feedback.getRejoinIntent(), feedback.getRecommendScore(), feedback.getComment());
    }
}
