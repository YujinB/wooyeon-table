package com.wooyeon.table.domain.feedback.application.scheduler;

import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.feedback.application.service.EventCloseService;
import com.wooyeon.table.domain.feedback.application.service.FeedbackReminderService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 행사 시작 후 일정 시간이 지나면 운영자 버튼 없이 종료 처리한다: 미체크인 → 노쇼 후보, 후기 요청 (FDB-01·02).
 * 이어서 모임 다음 날부터 후기를 안 쓴 참석자에게 한 번 리마인드한다 (PRD 15절).
 * 매칭 스케줄러와 같이 기본은 꺼 두고 배포에서 켠다
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "attendance.auto-close.enabled", havingValue = "true")
public class EventCloseScheduler {

    private final EventSlotQueryService eventSlotQueryService;
    private final EventCloseService eventCloseService;
    private final FeedbackReminderService feedbackReminderService;
    private final Clock clock;
    private volatile boolean ready;

    @Value("${attendance.auto-close.hours-after-start:4}")
    private int hoursAfterStart;

    /** 서버 시작 작업(데모 시드 등)이 끝난 뒤부터 돈다. 시드가 회차를 채우는 중에 먼저 매칭·종료하지 않게 */
    @EventListener(ApplicationReadyEvent.class)
    public void markReady() {
        ready = true;
    }

    @Scheduled(fixedDelayString = "${attendance.auto-close.interval-ms:60000}")
    public void closeFinishedEvents() {
        if (!ready) {
            return;
        }
        for (Long slotId : eventSlotQueryService.findSlotIdsDueForClosing(LocalDateTime.now(clock), hoursAfterStart)) {
            try {
                eventCloseService.close(slotId);
            } catch (RuntimeException e) {
                log.error("회차 {} 자동 종료 처리 실패", slotId, e);
            }
        }
        // 모임 다음 날부터 후기를 안 쓴 참석자에게 한 번 리마인드 (PRD 15절)
        feedbackReminderService.remindMissingFeedback();
    }
}
