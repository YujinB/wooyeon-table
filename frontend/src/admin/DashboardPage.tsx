import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { Dashboard, SlotOverview, slotStartsAt, slotStatusView } from '../api/adminTypes';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day, time } from '../format';
import { useApi } from '../useApi';

/** 운영 대시보드: 처리할 예외 건수와 회차별 신청·결제·매칭 현황 */
export default function DashboardPage() {
  const dashboard = useApi<Dashboard>('/api/admin/dashboard');
  const [eventDate, setEventDate] = useState('');
  const [createError, setCreateError] = useState<string | null>(null);
  if (!dashboard.data) return dashboard.error ? <ErrorBox message={dashboard.error} /> : <Loading />;
  const { totals, slots, exceptions } = dashboard.data;
  const rate = (value: number | null) => (value === null ? '-' : `${value}%`);
  // 행사 시작 전이면 다가오는 회차(가까운 날짜부터), 지났으면 지난 회차(최근부터)
  const now = Date.now();
  const upcoming = slots.filter((s) => slotStartsAt(s.eventDate, s.startTime) > now).reverse();
  const past = slots.filter((s) => slotStartsAt(s.eventDate, s.startTime) <= now);

  // 회차 생성: 목요일 19:30, 매칭(신청) 마감은 전날 0시(화요일 24:00)가 기본값
  const createSlot = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await api.post('/api/admin/slots', { eventDate });
      setEventDate('');
      setCreateError(null);
      await dashboard.reload();
    } catch (e) {
      setCreateError(errorText(e));
    }
  };

  const kpi = (label: string, value: number, alertWhenPositive = true) => (
    <div className={`kpi ${value > 0 && alertWhenPositive ? 'alert' : value === 0 ? 'zero' : ''}`}>
      <b>{value}</b><span>{label}</span>
    </div>
  );

  return (
    <div>
      <div className="admin-head">
        <div>
          <h1 className="h1">운영 대시보드</h1>
          <p className="muted" style={{ margin: 0 }}>매칭·식당·단체방·안내는 자동으로 처리돼요. 아래 예외만 확인하면 됩니다.</p>
        </div>
        <form className="row" onSubmit={createSlot}>
          <input className="input" type="date" value={eventDate} onChange={(e) => setEventDate(e.target.value)}
            style={{ width: 170 }} aria-label="행사 날짜" />
          <button className="btn btn-primary btn-sm" disabled={!eventDate}>회차 만들기</button>
        </form>
      </div>
      <ErrorBox message={createError} />

      <p className="section-title" style={{ marginTop: 0 }}>전체 현황</p>
      <div className="kpis">
        <div className="kpi"><b>{totals.applications}</b><span>신청 · 결제 완료 {totals.paid}</span></div>
        <div className="kpi"><b>{rate(totals.matchSuccessRate)}</b><span>매칭 성공률 · 확정 {totals.confirmed}명</span></div>
        <div className="kpi"><b>{rate(totals.autoConfirmRate)}</b><span>자동 확정률 · 테이블 {totals.tables}개</span></div>
        <div className={`kpi ${totals.awaitingChoice > 0 ? 'alert' : 'zero'}`}>
          <b>{totals.awaitingChoice}</b><span>대체 일정·환불 선택 대기</span>
        </div>
        <div className="kpi"><b>{rate(totals.attendanceRate)}</b><span>참석률 · 참석 {totals.attended} / 노쇼 {totals.noShow}</span></div>
      </div>

      <p className="section-title">처리할 예외</p>
      <Link to="/admin/exceptions" className="kpis" style={{ display: 'grid' }}>
        {kpi('전체 미해결', exceptions.total)}
        {kpi(`신고 (긴급 ${exceptions.urgentReports})`, exceptions.reports)}
        {kpi('식당 배정 실패', exceptions.venue)}
        {kpi('환불 실패', exceptions.refund)}
        {kpi('알림 발송 실패', exceptions.notification)}
        {kpi('노쇼 후보', exceptions.noShow)}
      </Link>

      <p className="section-title">다가오는 회차 {upcoming.length}</p>
      {upcoming.length === 0
        ? <div className="alert info">다가오는 회차가 없어요. 오른쪽 위에서 회차를 만들어 주세요.</div>
        : <SlotTable slots={upcoming} />}

      <p className="section-title">지난 회차 {past.length}</p>
      {past.length > 0 && <SlotTable slots={past} />}
    </div>
  );
}

/** 회차 한 줄: 누르면 회차 화면으로 */
function SlotTable({ slots }: { slots: SlotOverview[] }) {
  const navigate = useNavigate();
  return (
    <div className="table-wrap">
      <table className="data">
        <thead>
          <tr>
            <th>회차</th><th>상태</th><th>매칭 마감</th>
            <th className="num">신청</th><th className="num">결제 대기</th><th className="num">매칭 대기</th>
            <th className="num">확정</th><th className="num">테이블</th><th className="num">실패·선택 대기</th>
            <th className="num">취소·환불·보관</th>
          </tr>
        </thead>
        <tbody>
          {slots.map((s) => {
            const view = slotStatusView(s.status, s.matchAt, s.eventDate, s.startTime);
            return (
              <tr key={s.slotId} className="link" onClick={() => navigate(`/admin/slots/${s.slotId}`)}>
                <td style={{ whiteSpace: 'nowrap' }}><b>{day(s.eventDate)}</b> <span className="muted small">{time(s.startTime)}</span></td>
                <td><span className={`pill ${view.tone}`}>{view.label}</span></td>
                <td className="small" style={{ whiteSpace: 'nowrap' }}>{dateTime(s.matchAt)}</td>
                <td className="num">{s.applicants}</td>
                <td className="num">{s.paymentPending}</td>
                <td className="num">{s.waiting}</td>
                <td className="num">{s.confirmed}</td>
                <td className="num">{s.tables}</td>
                <td className="num">{s.matchFailed}</td>
                <td className="num">{s.closed}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
