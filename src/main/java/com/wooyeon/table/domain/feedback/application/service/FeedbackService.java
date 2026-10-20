package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.chat.application.dto.TableMember;
import com.wooyeon.table.domain.chat.application.service.ChatMemberQueryService;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.event.domain.model.EventSlotStatus;
import com.wooyeon.table.domain.feedback.application.dto.FeedbackCommand;
import com.wooyeon.table.domain.feedback.application.dto.FeedbackForm;
import com.wooyeon.table.domain.feedback.application.dto.FeedbackForm.OtherMember;
import com.wooyeon.table.domain.feedback.domain.model.Attendance;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import com.wooyeon.table.domain.feedback.domain.model.Feedback;
import com.wooyeon.table.domain.feedback.domain.model.PersonPreference;
import com.wooyeon.table.domain.feedback.domain.model.PreferenceType;
import com.wooyeon.table.domain.feedback.domain.repository.AttendanceRepository;
import com.wooyeon.table.domain.feedback.domain.repository.FeedbackRepository;
import com.wooyeon.table.domain.feedback.domain.repository.PersonPreferenceRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모임 후기 (FDB-02·03)와 사람별 선호 (FDB-04, 비공개). 행사 종료 처리 후, 참석했거나 체크인만 빠뜨린(노쇼 후보) 참가자가 한 번 낸다
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
    private final PersonPreferenceRepository personPreferenceRepository;
    private final ChatMemberQueryService chatMemberQueryService;

    @Transactional(readOnly = true)
    public FeedbackForm form(Long userId, Long applicationId) {
        applicationQueryService.getOwnedApplication(userId, applicationId);
        Optional<Feedback> mine = feedbackRepository.findByApplicationId(applicationId);
        String reason = unavailableReason(applicationId).orElse(null);
        List<TableMember> members = attendanceRepository.findByApplicationId(applicationId)
                .map(attendance -> chatMemberQueryService.membersOfTable(attendance.getTableId()))
                .orElse(List.of());
        List<OtherMember> others = members.stream()
                .filter(member -> !member.userId().equals(userId))
                .map(member -> new OtherMember(member.seatNo(), member.alias(), member.intro()))
                .toList();
        return new FeedbackForm(applicationId, reason == null && mine.isEmpty(), reason, mine.isPresent(), others,
                mine.map(feedback -> toCommand(feedback, members)).orElse(null));
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
        savePreferences(userId, attendance.getTableId(), command);
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

    /** 좌석 번호 → 같은 테이블 사람. 나 자신·없는 좌석·양쪽에 모두 고른 좌석은 거절 (FDB-04) */
    private void savePreferences(Long userId, Long tableId, FeedbackCommand command) {
        Set<Integer> both = new HashSet<>(command.wantAgainSeats());
        both.retainAll(command.avoidSeats());
        if (!both.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 사람을 양쪽에 고를 수 없습니다: " + both);
        }
        Map<Integer, Long> others = chatMemberQueryService.membersOfTable(tableId).stream()
                .filter(member -> !member.userId().equals(userId))
                .collect(Collectors.toMap(TableMember::seatNo, TableMember::userId));
        List<PersonPreference> preferences = new ArrayList<>();
        command.wantAgainSeats().forEach(seat -> preferences.add(PersonPreference.of(userId,
                seatUser(others, seat), tableId, PreferenceType.WANT_AGAIN)));
        command.avoidSeats().forEach(seat -> preferences.add(PersonPreference.of(userId,
                seatUser(others, seat), tableId, PreferenceType.AVOID)));
        personPreferenceRepository.saveAll(preferences);
    }

    private static Long seatUser(Map<Integer, Long> others, Integer seat) {
        Long target = others.get(seat);
        if (target == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 테이블의 다른 참가자 좌석이 아닙니다: " + seat);
        }
        return target;
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

    private FeedbackCommand toCommand(Feedback feedback, List<TableMember> members) {
        Map<Long, Integer> seatOf = members.stream()
                .collect(Collectors.toMap(TableMember::userId, TableMember::seatNo));
        List<PersonPreference> mine = personPreferenceRepository.findByFromUserIdAndTableId(feedback.getUserId(),
                feedback.getTableId());
        return new FeedbackCommand(feedback.getTableMood(), feedback.getConversation(), feedback.getVenue(),
                feedback.getRejoinIntent(), feedback.getRecommendScore(), feedback.getComment(),
                seats(mine, PreferenceType.WANT_AGAIN, seatOf), seats(mine, PreferenceType.AVOID, seatOf));
    }

    private static Set<Integer> seats(List<PersonPreference> preferences, PreferenceType type,
                                      Map<Long, Integer> seatOf) {
        return preferences.stream()
                .filter(preference -> preference.getType() == type)
                .map(preference -> seatOf.get(preference.getToUserId()))
                .collect(Collectors.toSet());
    }
}
