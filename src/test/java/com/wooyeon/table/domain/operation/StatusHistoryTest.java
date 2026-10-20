package com.wooyeon.table.domain.operation;

import static org.assertj.core.api.Assertions.assertThat;

import com.wooyeon.table.domain.application.application.service.ApplicationCommandService;
import com.wooyeon.table.domain.application.domain.model.Application;
import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.application.infrastructure.persistence.ApplicationJpaRepository;
import com.wooyeon.table.domain.event.application.service.EventSlotCommandService;
import com.wooyeon.table.domain.operation.application.service.OperationQueryService;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.user.application.service.UserCommandService;
import com.wooyeon.table.global.audit.ActorContext;
import com.wooyeon.table.global.audit.StatusChange;
import com.wooyeon.table.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 상태 변경 이력 (PRD 16절). 이력은 트랜잭션이 커밋될 때 남으므로 이 테스트는 롤백하지 않고, 끝나면 신청을 지운다
 */
@IntegrationTest
class StatusHistoryTest {

    @Autowired
    private ApplicationCommandService applicationCommandService;
    @Autowired
    private OperationQueryService operationQueryService;
    @Autowired
    private EventSlotCommandService eventSlotCommandService;
    @Autowired
    private UserCommandService userCommandService;
    @Autowired
    private ApplicationJpaRepository applicationJpaRepository;

    private Long applicationId;

    @AfterEach
    void 신청_정리() {
        ActorContext.clear();
        if (applicationId != null) {
            applicationJpaRepository.deleteById(applicationId);
        }
    }

    @Test
    void 신청_결제_취소가_시각_주체_이전_값과_함께_남는다() {
        Long userId = userCommandService.createParticipantIfAbsent("history-test", "이력테스트");
        Long slotId = eventSlotCommandService.findOrCreate(LocalDate.of(2032, 3, 4));
        applicationId = applicationCommandService.save(Application.submit(userId, TicketType.SINGLE,
                Set.of(Region.JAMSIL), List.of(slotId)));

        ActorContext.set("참가자 #" + userId);
        applicationCommandService.markPaid(applicationId);
        ActorContext.set("운영자 #1");
        applicationCommandService.cancel(applicationId);

        List<StatusChange> history = operationQueryService.statusHistory(applicationId);

        // 처음 생성: 세 상태가 한 번에 기록되고, 이후에는 바뀐 필드만
        assertThat(history).extracting(StatusChange::field, StatusChange::from, StatusChange::to, StatusChange::actor)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("신청", null, "신청 완료", ActorContext.SYSTEM),
                        org.assertj.core.groups.Tuple.tuple("결제", null, "결제 대기", ActorContext.SYSTEM),
                        org.assertj.core.groups.Tuple.tuple("매칭", null, "매칭 대기", ActorContext.SYSTEM),
                        org.assertj.core.groups.Tuple.tuple("결제", "결제 대기", "결제 완료", "참가자 #" + userId),
                        org.assertj.core.groups.Tuple.tuple("신청", "신청 완료", "신청 취소", "운영자 #1"));
        assertThat(history).allSatisfy(change -> assertThat(change.at()).isNotNull());
    }
}
