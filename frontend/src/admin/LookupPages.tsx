import { useState } from 'react';
import { Link } from 'react-router-dom';
import { NotificationRow, PaymentRow, ReportRow, VenueInfo } from '../api/adminTypes';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, won } from '../format';
import { useApi } from '../useApi';

const REGION_LABEL: Record<string, string> = {
  HONGDAE_HAPJEONG: '홍대/합정', GANGNAM_SINNONHYEON: '강남/신논현', EULJIRO_JONGNO: '을지로/종로', JAMSIL: '잠실',
};

function Head({ title, note }: { title: string; note: string }) {
  return (
    <div className="admin-head">
      <div>
        <h1 className="h1">{title}</h1>
        <p className="muted" style={{ margin: 0 }}>{note}</p>
      </div>
    </div>
  );
}

/** 식당 재고: 파트너 식당과 회차당 받을 수 있는 테이블 수 (PRD 11절 식당 재고 관리) */
export function VenuesPage() {
  const venues = useApi<VenueInfo[]>('/api/admin/venues');
  if (!venues.data) return venues.error ? <ErrorBox message={venues.error} /> : <Loading />;
  return (
    <div>
      <Head title="식당 재고" note="테이블이 확정되면 같은 지역에서 멤버 예산에 맞고 남은 자리가 많은 식당부터 자동 배정해요." />
      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr><th>식당</th><th>지역</th><th>가격</th><th className="num">회차당 테이블</th><th>자동 예약</th><th>주소</th></tr>
          </thead>
          <tbody>
            {venues.data.map((v) => (
              <tr key={v.venueId}>
                <td className="nowrap"><b>{v.name}</b></td>
                <td className="nowrap">{REGION_LABEL[v.region] ?? v.region}</td>
                <td className="nowrap small">{v.priceGuide}</td>
                <td className="num">{v.tablesPerSlot}</td>
                <td>{v.autoBookable ? <span className="pill ok">가능</span> : <span className="pill warn">전화 확인</span>}</td>
                <td className="small" style={{ minWidth: 200 }}>
                  {v.address} <a href={v.mapUrl} target="_blank" rel="noreferrer" style={{ color: 'var(--brand)' }}>지도</a>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/** 결제·환불 내역 (PRD 11절 결제·환불) */
export function PaymentsPage() {
  const payments = useApi<PaymentRow[]>('/api/admin/payments');
  const [failedOnly, setFailedOnly] = useState(false);
  if (!payments.data) return payments.error ? <ErrorBox message={payments.error} /> : <Loading />;
  const failed = payments.data.filter((row) => !row.payment.success);
  const rows = failedOnly ? failed : payments.data;
  return (
    <div>
      <Head title="결제·환불" note="모의 결제 기준. 환불은 운영자 승인 없이 자동 처리되고, 실패한 환불만 예외 처리함에 올라가요." />
      <div className="filter">
        <button className={`chip ${!failedOnly ? 'on' : ''}`} onClick={() => setFailedOnly(false)}>전체 {payments.data.length}</button>
        <button className={`chip ${failedOnly ? 'on' : ''}`} onClick={() => setFailedOnly(true)}>실패 {failed.length}</button>
      </div>
      {rows.length === 0 ? <div className="alert info">내역이 없어요</div> : (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr><th>시각</th><th>이름</th><th>종류</th><th className="num">금액</th><th>결과</th><th>신청</th></tr>
            </thead>
            <tbody>
              {rows.map(({ name, payment: p }) => (
                <tr key={p.recordId}>
                  <td className="small nowrap">{dateTime(p.at)}</td>
                  <td className="nowrap"><b>{name}</b></td>
                  <td className="nowrap">{p.typeLabel}</td>
                  <td className="num">{won(p.amount)}</td>
                  <td>{p.success ? <span className="pill ok">성공</span> : <span className="pill danger">실패 · {p.failureReason}</span>}</td>
                  <td className="small">#{p.applicationId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

const NOTIFICATION_STATUS = {
  SCHEDULED: { label: '예약', tone: '' },
  SENT: { label: '발송', tone: 'ok' },
  RETRYING: { label: '재시도 대기', tone: 'warn' },
  GAVE_UP: { label: '실패', tone: 'danger' },
} as const;

type NotificationStatus = keyof typeof NOTIFICATION_STATUS;

/** 자동 메시지 발송 이력 (PRD 11절 자동 메시지 관리, 16절 발송 성공·실패·재시도 기록) */
export function NotificationsLogPage() {
  const notifications = useApi<NotificationRow[]>('/api/admin/notifications');
  const [status, setStatus] = useState<NotificationStatus | 'ALL'>('ALL');
  if (!notifications.data) return notifications.error ? <ErrorBox message={notifications.error} /> : <Loading />;
  const all = notifications.data;
  const rows = status === 'ALL' ? all : all.filter((row) => row.notification.status === status);
  return (
    <div>
      <Head title="자동 메시지 이력"
        note="확정·리마인드·후기 요청 등 시나리오별 알림의 예약·발송·재시도 기록이에요. 실패하면 1분·5분 뒤 다시 보내요." />
      <div className="filter">
        <button className={`chip ${status === 'ALL' ? 'on' : ''}`} onClick={() => setStatus('ALL')}>전체 {all.length}</button>
        {(Object.keys(NOTIFICATION_STATUS) as NotificationStatus[]).map((key) => (
          <button key={key} className={`chip ${status === key ? 'on' : ''}`} onClick={() => setStatus(key)}>
            {NOTIFICATION_STATUS[key].label} {all.filter((row) => row.notification.status === key).length}
          </button>
        ))}
      </div>
      {rows.length === 0 ? <div className="alert info">내역이 없어요</div> : (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr><th>예약 시각</th><th>받는 사람</th><th>알림</th><th>상태</th><th className="num">시도</th><th>발송 시각</th></tr>
            </thead>
            <tbody>
              {rows.map(({ name, notification: n }) => (
                <tr key={n.notificationId}>
                  <td className="small nowrap">{dateTime(n.scheduledAt)}</td>
                  <td className="nowrap"><b>{name}</b></td>
                  <td style={{ minWidth: 180 }}>{n.title}</td>
                  <td>
                    <span className={`pill ${NOTIFICATION_STATUS[n.status].tone}`}>{NOTIFICATION_STATUS[n.status].label}</span>
                    {n.lastError && <div className="small muted">{n.lastError}</div>}
                  </td>
                  <td className="num">{n.attempts}</td>
                  <td className="small nowrap">{n.sentAt ? dateTime(n.sentAt) : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

const REPORT_STATUS = {
  RECEIVED: { label: '검토 대기', tone: 'warn' },
  ACTIONED: { label: '조치함', tone: 'danger' },
  DISMISSED: { label: '종결', tone: 'gray' },
} as const;
const ACTION_LABEL = { WARNING: '경고', RESTRICTED: '참가 제한', BANNED: '영구 제한' } as const;

/** 신고 이력: 검토 대기는 예외 처리함에서 조치하고, 여기서는 처리된 것까지 전체 (PRD 11절 피드백·안전) */
export function ReportsPage() {
  const reports = useApi<ReportRow[]>('/api/admin/report-history');
  if (!reports.data) return reports.error ? <ErrorBox message={reports.error} /> : <Loading />;
  return (
    <div>
      <Head title="신고 이력" note="신고 내용은 상대에게 공개되지 않아요. 검토 대기 건은 예외 처리함에서 조치할 수 있어요." />
      {reports.data.length === 0 ? <div className="alert info">신고가 없어요</div> : (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr><th>접수</th><th>신고자 → 대상</th><th>사유</th><th>상태</th><th>조치</th><th>테이블</th></tr>
            </thead>
            <tbody>
              {reports.data.map(({ reporterName, reportedName, report: r }) => (
                <tr key={r.reportId}>
                  <td className="small nowrap">{dateTime(r.reportedAt)}</td>
                  <td className="nowrap"><b>{reporterName}</b> → <b>{reportedName}</b></td>
                  <td style={{ minWidth: 200 }}>
                    {r.urgent && <span className="pill danger" style={{ marginRight: 6 }}>긴급</span>}{r.reasonLabel}
                    {r.detail && <div className="small muted">{r.detail}</div>}
                  </td>
                  <td><span className={`pill ${REPORT_STATUS[r.status].tone}`}>{REPORT_STATUS[r.status].label}</span></td>
                  <td className="small">
                    {r.action ? ACTION_LABEL[r.action] : '-'}{r.operatorMemo && ` · ${r.operatorMemo}`}
                    {r.resolvedAt && <div className="muted">{dateTime(r.resolvedAt)}</div>}
                  </td>
                  <td><Link to={`/admin/tables/${r.tableId}`} style={{ color: 'var(--brand)' }}>#{r.tableId}</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
