package com.wooyeon.table.domain.resolution.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.payment.application.service.PaymentService;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.resolution.application.dto.Alternatives;
import com.wooyeon.table.domain.resolution.application.dto.Alternatives.AlternativeSlot;
import com.wooyeon.table.domain.resolution.application.dto.Alternatives.ChoiceOption;
import com.wooyeon.table.domain.resolution.application.dto.Alternatives.RegionDemand;
import com.wooyeon.table.domain.resolution.domain.model.ResolutionChoice;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResolutionQueryService {

    /** 대체 회차는 가까운 날짜부터 이만큼만 보여준다 */
    private static final int MAX_ALTERNATIVE_SLOTS = 4;

    private final ApplicationQueryService applicationQueryService;
    private final EventSlotQueryService eventSlotQueryService;
    private final MatchingQueryService matchingQueryService;
    private final PaymentService paymentService;
    private final Clock clock;

    /** 매칭 실패(대체 안내) 상태인 본인 신청의 사유·대체 회차·해결 방식 (STA-04) */
    public Alternatives getAlternatives(Long userId, Long applicationId) {
        ApplicationInfo application = requireAlternativeOffered(userId, applicationId);
        List<AlternativeSlot> slots = alternativeSlots(application);
        boolean refundable = paymentService.hasRefundableCharge(applicationId);
        List<ChoiceOption> choices = List.of(
                option(ResolutionChoice.MOVE_SCHEDULE, !slots.isEmpty(), "지금 신청할 수 있는 회차가 없어요"),
                keepOption(application),
                option(ResolutionChoice.REFUND, refundable, "결제한 금액이 없어요 (멤버십으로 신청)"));
        return new Alternatives(applicationId, matchingQueryService.getMyResult(applicationId).failure(), slots,
                choices);
    }

    /** 해결 방식을 고를 수 있는 신청인지 확인한다 (본인 + 매칭 실패 후 선택 대기) */
    public ApplicationInfo requireAlternativeOffered(Long userId, Long applicationId) {
        ApplicationInfo application = applicationQueryService.getOwnedApplication(userId, applicationId);
        if (application.matchStatus() != MatchStatus.ALTERNATIVE_OFFERED) {
            throw new BusinessException(ErrorCode.RESOLUTION_NOT_AVAILABLE,
                    String.valueOf(application.matchStatus()));
        }
        return application;
    }

    /** 지금 신청할 수 있고, 이미 다른 신청으로 고른 날짜가 아닌 회차 */
    public List<AlternativeSlot> alternativeSlots(ApplicationInfo application) {
        Set<Long> taken = applicationQueryService.findActiveSlotIds(application.userId());
        return eventSlotQueryService.findOpenSlots(LocalDateTime.now(clock)).stream()
                .filter(slot -> !taken.contains(slot.id()) && !application.preferredSlotIds().contains(slot.id()))
                .limit(MAX_ALTERNATIVE_SLOTS)
                .map(slot -> alternativeSlot(slot, application.regions()))
                .toList();
    }

    private AlternativeSlot alternativeSlot(EventSlotInfo slot, Set<Region> myRegions) {
        Map<Region, Long> waiting = applicationQueryService.countCandidatesByRegion(slot.id());
        List<RegionDemand> regions = waiting.entrySet().stream()
                .sorted(Map.Entry.<Region, Long>comparingByValue().reversed()
                        .thenComparing(entry -> !myRegions.contains(entry.getKey()))
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new RegionDemand(entry.getKey().name(), entry.getKey().getLabel(), entry.getValue(),
                        myRegions.contains(entry.getKey())))
                .toList();
        return new AlternativeSlot(slot.id(), slot.eventDate(), slot.startsAt(), slot.matchAt(), regions);
    }

    private ChoiceOption keepOption(ApplicationInfo application) {
        ResolutionChoice keep = ResolutionChoice.KEEP_TICKET;
        String description = application.ticketType().isMembership()
                ? "멤버십 기간이 실패한 횟수만큼 늘어났어요. 다음 신청에 그대로 써요."
                : keep.getDescription();
        return new ChoiceOption(keep, keep.getLabel(), description, true, null);
    }

    private static ChoiceOption option(ResolutionChoice choice, boolean available, String unavailableReason) {
        return new ChoiceOption(choice, choice.getLabel(), choice.getDescription(), available,
                available ? null : unavailableReason);
    }
}
