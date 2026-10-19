package com.wooyeon.table.domain.feedback.application.service;

import com.wooyeon.table.domain.feedback.application.dto.SlotFeedbackSummary;
import com.wooyeon.table.domain.feedback.application.dto.SlotFeedbackSummary.Averages;
import com.wooyeon.table.domain.feedback.application.dto.SlotFeedbackSummary.Comment;
import com.wooyeon.table.domain.feedback.application.dto.SlotFeedbackSummary.TableFeedback;
import com.wooyeon.table.domain.feedback.domain.model.Feedback;
import com.wooyeon.table.domain.feedback.domain.repository.FeedbackRepository;
import com.wooyeon.table.domain.user.application.service.UserQueryService;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자가 회차 후기를 한눈에 보는 요약: 항목별 평균, 순추천지수, 테이블별 평균, 한마디 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackSummaryService {

    private final FeedbackRepository feedbackRepository;
    private final UserQueryService userQueryService;

    public SlotFeedbackSummary summarize(Long slotId) {
        List<Feedback> feedbacks = feedbackRepository.findAllBySlotId(slotId);
        Map<Long, List<Feedback>> byTable = feedbacks.stream()
                .collect(Collectors.groupingBy(Feedback::getTableId, TreeMap::new, Collectors.toList()));
        List<TableFeedback> tables = byTable.entrySet().stream()
                .map(entry -> new TableFeedback(entry.getKey(), entry.getValue().size(), averages(entry.getValue())))
                .toList();
        Map<Long, String> names = userQueryService.findNames(feedbacks.stream().map(Feedback::getUserId).toList());
        List<Comment> comments = feedbacks.stream()
                .filter(feedback -> feedback.getComment() != null)
                .map(feedback -> new Comment(feedback.getTableId(), names.get(feedback.getUserId()),
                        feedback.getTableMood(), feedback.getComment()))
                .toList();
        return new SlotFeedbackSummary(slotId, feedbacks.size(), averages(feedbacks), nps(feedbacks), tables,
                comments);
    }

    private static Averages averages(List<Feedback> feedbacks) {
        return new Averages(average(feedbacks, Feedback::getTableMood), average(feedbacks, Feedback::getConversation),
                average(feedbacks, Feedback::getVenue), average(feedbacks, Feedback::getRejoinIntent),
                average(feedbacks, Feedback::getRecommendScore));
    }

    private static double average(List<Feedback> feedbacks, ToIntFunction<Feedback> score) {
        double average = feedbacks.stream().mapToInt(score).average().orElse(0);
        return Math.round(average * 10) / 10.0;
    }

    private static int nps(List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return 0;
        }
        long promoters = feedbacks.stream().filter(feedback -> feedback.getRecommendScore() >= 9).count();
        long detractors = feedbacks.stream().filter(feedback -> feedback.getRecommendScore() <= 6).count();
        return (int) Math.round((promoters - detractors) * 100.0 / feedbacks.size());
    }
}
