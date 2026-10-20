package com.wooyeon.table.domain.resolution.application.service;

import com.wooyeon.table.domain.matching.application.dto.MyMatchResult.MyFailure;
import com.wooyeon.table.domain.matching.application.event.MatchCarriedOverEvent;
import com.wooyeon.table.domain.matching.application.event.MatchFailedEvent;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.notification.application.service.NotificationService;
import com.wooyeon.table.domain.notification.domain.model.NotificationType;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 매칭 실패·자동 재배치(다음 날짜 이월) 알림 (PRD 14절, STA-04) */
@Component
@RequiredArgsConstructor
public class MatchOutcomeNotifier {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN);

    private final NotificationService notificationService;
    private final MatchingQueryService matchingQueryService;

    @EventListener
    public void onCarriedOver(MatchCarriedOverEvent event) {
        for (MatchCarriedOverEvent.CarriedApplication carried : event.applications()) {
            notificationService.notifyNow(carried.userId(), carried.applicationId(), NotificationType.REALLOCATING,
                    Map.of("date", event.eventDate().format(DATE), "nextDate", carried.nextEventDate().format(DATE)),
                    "REALLOCATING:" + carried.applicationId() + ":" + event.eventDate());
        }
    }

    @EventListener
    public void onFailed(MatchFailedEvent event) {
        for (MatchFailedEvent.FailedApplication failed : event.applications()) {
            String reason = Optional.ofNullable(matchingQueryService.getMyResult(failed.applicationId()).failure())
                    .map(MyFailure::reason)
                    .orElse("조건에 맞는 인원이 부족했어요");
            notificationService.notifyNow(failed.userId(), failed.applicationId(), NotificationType.MATCH_FAILED,
                    Map.of("date", event.eventDate().format(DATE), "reason", reason),
                    "MATCH_FAILED:" + failed.applicationId() + ":" + event.eventDate());
        }
    }
}
