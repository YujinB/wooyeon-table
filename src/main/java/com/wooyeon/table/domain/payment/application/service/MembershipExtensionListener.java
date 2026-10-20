package com.wooyeon.table.domain.payment.application.service;

import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.matching.application.event.MatchFailedEvent;
import com.wooyeon.table.domain.matching.application.event.MatchFailedEvent.FailedApplication;
import com.wooyeon.table.domain.payment.domain.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 멤버십으로 신청했다가 매칭에 실패하면, 그 모임 날짜에 쓰던 멤버십 기간을 늘린다 (주최 측 Q&A) */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipExtensionListener {

    private final ApplicationQueryService applicationQueryService;
    private final MembershipRepository membershipRepository;

    @EventListener
    public void onMatchFailed(MatchFailedEvent event) {
        for (FailedApplication failed : event.applications()) {
            if (!applicationQueryService.getApplication(failed.applicationId()).ticketType().isMembership()) {
                continue;
            }
            membershipRepository.findAllByUserId(failed.userId()).stream()
                    .filter(membership -> membership.covers(event.eventDate()))
                    .findFirst()
                    .ifPresent(membership -> {
                        membership.extendForFailure();
                        log.info("매칭 실패로 멤버십 {} 기간 연장 → {} (신청 {})", membership.getId(),
                                membership.getExpiresOn(), failed.applicationId());
                    });
        }
    }
}
