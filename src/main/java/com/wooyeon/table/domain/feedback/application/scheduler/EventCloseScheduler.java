package com.wooyeon.table.domain.feedback.application.scheduler;

import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.feedback.application.service.EventCloseService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 행사 시작 후 일정 시간이 지나면 운영자 버튼 없이 종료 처리한다: 미체크인 → 노쇼 후보, 후기 요청 (FDB-01·02).
 * 매칭 스케줄러와 같이 기본은 꺼 두고 배포에서 켠다
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "attendance.auto-close.enabled", havingValue = "true")
public class EventCloseScheduler {

    private final EventSlotQueryService eventSlotQueryService;
    private final EventCloseService eventCloseService;
    private final Clock clock;

    @Value("${attendance.auto-close.hours-after-start:4}")
    private int hoursAfterStart;

    @Scheduled(fixedDelayString = "${attendance.auto-close.interval-ms:60000}")
    public void closeFinishedEvents() {
        for (Long slotId : eventSlotQueryService.findSlotIdsDueForClosing(LocalDateTime.now(clock), hoursAfterStart)) {
            try {
                eventCloseService.close(slotId);
            } catch (RuntimeException e) {
                log.error("회차 {} 자동 종료 처리 실패", slotId, e);
            }
        }
    }
}
