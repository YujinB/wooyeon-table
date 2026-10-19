package com.wooyeon.table.domain.matching.application.scheduler;

import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.service.MatchingService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매칭 실행 시각(= 신청 마감)이 지난 회차를 운영자 개입 없이 매칭한다 (MAT-01).
 * 날짜가 이른 회차부터 돌려야 이월된 사람이 다음 회차 후보에 들어간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "matching.scheduler.enabled", havingValue = "true")
public class MatchingScheduler {

    private final EventSlotQueryService eventSlotQueryService;
    private final MatchingService matchingService;
    private final Clock clock;
    private volatile boolean ready;

    /** 서버 시작 작업(데모 시드 등)이 끝난 뒤부터 돈다. 시드가 회차를 채우는 중에 먼저 매칭·종료하지 않게 */
    @EventListener(ApplicationReadyEvent.class)
    public void markReady() {
        ready = true;
    }

    @Scheduled(fixedDelayString = "${matching.scheduler.interval-ms:60000}")
    public void runDueSlots() {
        if (!ready) {
            return;
        }
        for (Long slotId : eventSlotQueryService.findSlotIdsDueForMatching(LocalDateTime.now(clock))) {
            try {
                matchingService.runForSlot(slotId);
            } catch (RuntimeException e) {
                // 한 회차가 실패해도 다음 회차는 계속 돌린다. 실패 회차는 다음 주기에 다시 시도된다
                log.error("회차 {} 자동 매칭 실패", slotId, e);
            }
        }
    }
}
