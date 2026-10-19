package com.wooyeon.table.domain.operation.application.service;

import com.wooyeon.table.domain.application.application.dto.ApplicationInfo;
import com.wooyeon.table.domain.application.application.service.ApplicationQueryService;
import com.wooyeon.table.domain.application.domain.model.ApplicationStatus;
import com.wooyeon.table.domain.application.domain.model.MatchStatus;
import com.wooyeon.table.domain.application.domain.model.ParticipantStatus;
import com.wooyeon.table.domain.application.domain.model.PaymentStatus;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.feedback.application.dto.AttendanceView;
import com.wooyeon.table.domain.feedback.application.service.AttendanceQueryService;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.notification.application.dto.NotificationFailure;
import com.wooyeon.table.domain.notification.application.service.NotificationQueryService;
import com.wooyeon.table.domain.operation.application.dto.AdminRows.NotificationRow;
import com.wooyeon.table.domain.operation.application.dto.AdminRows.PaymentRow;
import com.wooyeon.table.domain.operation.application.dto.AdminRows.ReportRow;
import com.wooyeon.table.domain.operation.application.dto.Dashboard;
import com.wooyeon.table.domain.payment.application.dto.PaymentRecordView;
import com.wooyeon.table.domain.payment.application.service.PaymentQueryService;
import com.wooyeon.table.domain.operation.application.dto.Dashboard.ExceptionCounts;
import com.wooyeon.table.domain.operation.application.dto.Dashboard.SlotOverview;
import com.wooyeon.table.domain.operation.application.dto.ExceptionItem;
import com.wooyeon.table.domain.operation.application.dto.ExceptionItem.ExceptionType;
import com.wooyeon.table.domain.operation.application.dto.SlotApplicant;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.safety.application.dto.ReportView;
import com.wooyeon.table.domain.safety.application.service.SanctionService;
import com.wooyeon.table.domain.user.application.service.UserQueryService;
import com.wooyeon.table.domain.venue.application.dto.VenueAssignmentView;
import com.wooyeon.table.domain.venue.application.service.VenueQueryService;
import java.time.LocalDateTime;
import com.wooyeon.table.domain.feedback.domain.model.AttendanceStatus;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자 화면용 조회. 각 도메인의 QueryService 결과를 모아 대시보드·회차 신청자·예외 처리함으로 묶는다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationQueryService {

    private final EventSlotQueryService eventSlotQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final MatchingQueryService matchingQueryService;
    private final VenueQueryService venueQueryService;
    private final NotificationQueryService notificationQueryService;
    private final AttendanceQueryService attendanceQueryService;
    private final SanctionService sanctionService;
    private final UserQueryService userQueryService;
    private final PaymentQueryService paymentQueryService;

    public Dashboard dashboard() {
        List<ApplicationInfo> applications = applicationQueryService.findAllSubmitted();
        Map<Long, Long> tables = matchingQueryService.countTablesBySlot();
        List<SlotOverview> slots = eventSlotQueryService.findAllSlots().stream()
                .sorted(Comparator.comparing(EventSlotInfo::eventDate).reversed())
                .map(slot -> overview(slot, applications, tables.getOrDefault(slot.id(), 0L)))
                .toList();
        List<ReportView> reports = sanctionService.reports(true);
        ExceptionCounts counts = ExceptionCounts.of(venueQueryService.findFailures().size(),
                notificationQueryService.findGaveUp().size(), attendanceQueryService.findNoShowCandidates().size(),
                reports.size(), (int) reports.stream().filter(ReportView::urgent).count(),
                applicationQueryService.findByPaymentStatus(PaymentStatus.REFUND_FAILED).size());
        return new Dashboard(totals(applications, tables), slots, counts);
    }

    private Dashboard.Totals totals(List<ApplicationInfo> applications, Map<Long, Long> tablesBySlot) {
        Set<PaymentStatus> everPaid = EnumSet.of(PaymentStatus.PAID, PaymentStatus.REFUND_AVAILABLE,
                PaymentStatus.REFUND_REQUESTED, PaymentStatus.REFUND_PROCESSING, PaymentStatus.REFUNDED,
                PaymentStatus.REFUND_FAILED);
        long paid = applications.stream().filter(application -> everPaid.contains(application.paymentStatus())).count();
        long confirmed = count(applications, MatchStatus.CONFIRMED);
        long awaitingChoice = count(applications, MatchStatus.ALTERNATIVE_OFFERED);
        long failed = awaitingChoice + count(applications, MatchStatus.NO_MATCH);
        long tables = tablesBySlot.values().stream().mapToLong(Long::longValue).sum();
        Map<AttendanceStatus, Long> attendance = attendanceQueryService.countByStatus();
        long attended = attendance.get(AttendanceStatus.ATTENDED);
        long noShow = attendance.get(AttendanceStatus.NO_SHOW) + attendance.get(AttendanceStatus.NO_SHOW_CANDIDATE);
        return new Dashboard.Totals(applications.size(), paid, confirmed, percent(confirmed, confirmed + failed),
                tables, tables > 0 ? 100 : null, awaitingChoice, attended, noShow,
                percent(attended, attended + noShow));
    }

    private static long count(List<ApplicationInfo> applications, MatchStatus status) {
        return applications.stream().filter(application -> application.matchStatus() == status).count();
    }

    private static Integer percent(long part, long whole) {
        return whole == 0 ? null : (int) Math.round(part * 100.0 / whole);
    }

    /** 이 날짜를 고른 신청자 (다른 날짜로 확정된 신청 제외), 최근 신청부터 */
    public List<SlotApplicant> applicants(Long slotId) {
        eventSlotQueryService.getSlot(slotId);
        List<ApplicationInfo> applications = applicationQueryService.findAllSubmitted().stream()
                .filter(application -> belongsTo(application, slotId))
                .toList();
        Map<Long, String> names = userQueryService.findNames(
                applications.stream().map(ApplicationInfo::userId).toList());
        return applications.stream()
                .map(application -> {
                    ParticipantStatus status = ParticipantStatus.of(application.applicationStatus(),
                            application.paymentStatus(), application.matchStatus());
                    return new SlotApplicant(application.id(), application.userId(), names.get(application.userId()),
                            application.ticketType().getLabel(),
                            application.regions().stream().sorted().map(Region::getLabel).toList(), status,
                            status.getLabel(), application.paymentStatus().getLabel(),
                            slotId.equals(application.confirmedSlotId()), application.submittedAt());
                })
                .toList();
    }

    /** 결제·환불 내역 (최근 500건) */
    public List<PaymentRow> payments() {
        List<PaymentRecordView> payments = paymentQueryService.findRecent();
        Map<Long, String> names = userQueryService.findNames(payments.stream().map(PaymentRecordView::userId).toList());
        return payments.stream().map(payment -> new PaymentRow(names.get(payment.userId()), payment)).toList();
    }

    /** 자동 메시지 발송 이력 (최근 500건) */
    public List<NotificationRow> notifications() {
        List<NotificationFailure> notifications = notificationQueryService.findRecent();
        Map<Long, String> names = userQueryService.findNames(
                notifications.stream().map(NotificationFailure::userId).toList());
        return notifications.stream()
                .map(notification -> new NotificationRow(names.get(notification.userId()), notification))
                .toList();
    }

    /** 신고 전체 이력 (처리된 것 포함, 최신부터) */
    public List<ReportRow> reportHistory() {
        List<ReportView> reports = sanctionService.reports(false);
        Map<Long, String> names = userQueryService.findNames(reports.stream()
                .flatMap(report -> Stream.of(report.reporterUserId(), report.reportedUserId()))
                .collect(Collectors.toSet()));
        return reports.stream()
                .sorted(Comparator.comparing(ReportView::reportedAt).reversed())
                .map(report -> new ReportRow(names.get(report.reporterUserId()), names.get(report.reportedUserId()),
                        report))
                .toList();
    }

    /** 미해결 예외 전체: 긴급 신고 먼저, 그다음 최근 것부터 */
    public List<ExceptionItem> exceptions() {
        List<ReportView> reports = sanctionService.reports(true);
        List<NotificationFailure> notifications = notificationQueryService.findGaveUp();
        List<AttendanceView> noShows = attendanceQueryService.findNoShowCandidates();
        List<ApplicationInfo> refunds = applicationQueryService.findByPaymentStatus(PaymentStatus.REFUND_FAILED);

        Set<Long> userIds = new HashSet<>();
        reports.forEach(report -> {
            userIds.add(report.reporterUserId());
            userIds.add(report.reportedUserId());
        });
        notifications.forEach(notification -> userIds.add(notification.userId()));
        noShows.forEach(attendance -> userIds.add(attendance.userId()));
        refunds.forEach(application -> userIds.add(application.userId()));
        Map<Long, String> names = userQueryService.findNames(userIds);
        Function<Long, String> name = userId -> names.getOrDefault(userId, "사용자 " + userId);

        List<ExceptionItem> items = new ArrayList<>();
        reports.forEach(report -> items.add(item(ExceptionType.REPORT, report.urgent(),
                "%s → %s · %s".formatted(name.apply(report.reporterUserId()), name.apply(report.reportedUserId()),
                        report.reasonLabel()),
                Objects.requireNonNullElse(report.detail(), "상세 내용 없음"), report.reportId(), null,
                report.tableId(), null, report.reportedAt())));
        venueQueryService.findFailures().forEach(venue -> items.add(item(ExceptionType.VENUE, false,
                "테이블 #%d 식당 배정 실패".formatted(venue.tableId()),
                "%s (식당 %d곳 시도)".formatted(Objects.requireNonNullElse(venue.failureReason(), "사유 없음"),
                        venue.attemptedVenues()),
                venue.tableId(), venue.slotId(), venue.tableId(), null, venue.decidedAt())));
        refunds.forEach(application -> items.add(item(ExceptionType.REFUND, false,
                "%s 환불 실패".formatted(name.apply(application.userId())),
                "신청 #%d · %s".formatted(application.id(), application.ticketType().getLabel()),
                application.id(), null, null, application.id(), null)));
        notifications.forEach(notification -> items.add(item(ExceptionType.NOTIFICATION, false,
                "%s · %s".formatted(name.apply(notification.userId()), notification.title()),
                "%d번 시도 실패 · %s".formatted(notification.attempts(),
                        Objects.requireNonNullElse(notification.lastError(), "")),
                notification.notificationId(), null, null, notification.applicationId(),
                notification.scheduledAt())));
        noShows.forEach(attendance -> items.add(item(ExceptionType.NO_SHOW, false,
                "%s 노쇼 후보".formatted(name.apply(attendance.userId())),
                "행사 종료까지 체크인하지 않았어요. 노쇼로 확정하거나 참석으로 정정하세요",
                attendance.attendanceId(), attendance.slotId(), attendance.tableId(), attendance.applicationId(),
                null)));

        return items.stream()
                .sorted(Comparator.comparing(ExceptionItem::urgent).reversed()
                        .thenComparing(ExceptionItem::at, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static ExceptionItem item(ExceptionType type, boolean urgent, String title, String detail, Long refId,
                                      Long slotId, Long tableId, Long applicationId, LocalDateTime at) {
        return new ExceptionItem(type, type.getLabel(), urgent, title, detail, refId, slotId, tableId, applicationId,
                at);
    }

    private static SlotOverview overview(EventSlotInfo slot, List<ApplicationInfo> applications, long tables) {
        Map<ParticipantStatus, Long> counts = applications.stream()
                .filter(application -> belongsTo(application, slot.id()))
                .collect(Collectors.groupingBy(application -> ParticipantStatus.of(application.applicationStatus(),
                        application.paymentStatus(), application.matchStatus()), Collectors.counting()));
        long applicants = counts.values().stream().mapToLong(Long::longValue).sum();
        return new SlotOverview(slot.id(), slot.eventDate(), slot.startTime(), slot.matchAt(), slot.status(),
                applicants,
                sum(counts, ParticipantStatus.PAYMENT_PENDING, ParticipantStatus.PAYMENT_FAILED),
                sum(counts, ParticipantStatus.MATCH_WAITING),
                sum(counts, ParticipantStatus.MATCH_CONFIRMED),
                sum(counts, ParticipantStatus.MATCH_FAILED),
                sum(counts, ParticipantStatus.CANCELED, ParticipantStatus.REFUNDED, ParticipantStatus.TICKET_KEPT),
                tables);
    }

    /** 이 날짜를 고른 신청. 다른 날짜로 확정된 신청은 그 날짜에만 센다 */
    private static boolean belongsTo(ApplicationInfo application, Long slotId) {
        if (application.applicationStatus() == ApplicationStatus.DRAFT) {
            return false;
        }
        if (application.matchStatus() == MatchStatus.CONFIRMED) {
            return slotId.equals(application.confirmedSlotId());
        }
        return application.preferredSlotIds().contains(slotId);
    }

    private static long sum(Map<ParticipantStatus, Long> counts, ParticipantStatus... statuses) {
        return Stream.of(statuses).mapToLong(status -> counts.getOrDefault(status, 0L)).sum();
    }
}
