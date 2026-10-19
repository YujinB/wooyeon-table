import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import {
  AdminAttendance, slotStatusView, SlotApplicant, SlotFeedbackSummary, SlotMatchingResult,
} from '../api/adminTypes';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day, statusTone } from '../format';
import { useApi } from '../useApi';
import { ConfirmButton } from './ConfirmButton';
import { EXCEPTIONS_CHANGED } from './AdminLayout';

type Tab = 'matching' | 'applicants' | 'attendance' | 'feedback';

/** 회차 운영: 데모용 실행 버튼, 매칭 결과 (MAT-12), 신청자, 참석, 후기 요약 */
export default function SlotPage() {
  const { slotId } = useParams();
  const result = useApi<SlotMatchingResult>(`/api/admin/slots/${slotId}/matching`);
  const [tab, setTab] = useState<Tab>('matching');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);

  if (!result.data) return result.error ? <ErrorBox message={result.error} /> : <Loading />;
  const r = result.data;

  const run = async (path: string, done: (data: never) => string) => {
    setBusy(true);
    try {
      const data = await api.post<never>(path);
      setMessage({ ok: true, text: done(data) });
      await result.reload();
      window.dispatchEvent(new Event(EXCEPTIONS_CHANGED));
    } catch (e) {
      setMessage({ ok: false, text: errorText(e) });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <div className="admin-head">
        <div>
          <div className="crumb"><Link to="/admin">대시보드</Link> ›</div>
          <h1 className="h1">{day(r.eventDate)} 회차</h1>
          <p className="muted" style={{ margin: 0 }}>
            <span className={`pill ${slotStatusView(r.slotStatus, r.matchAt, r.eventDate).tone}`}>
              {slotStatusView(r.slotStatus, r.matchAt, r.eventDate).label}
            </span> 매칭 마감 {dateTime(r.matchAt)}
          </p>
        </div>
        <div className="row" style={{ flexWrap: 'wrap' }}>
          {r.slotStatus === 'RECRUITING' && (
            <ConfirmButton label="지금 마감 실행" disabled={busy}
              onConfirm={() => run(`/api/admin/slots/${slotId}/match-run`,
                (d: { assignedCount: number; tableCount: number; candidateCount: number }) =>
                  `매칭 완료 — 후보 ${d.candidateCount}명 중 ${d.assignedCount}명, 테이블 ${d.tableCount}개 확정`)} />
          )}
          {r.slotStatus === 'MATCHED' && (
            <>
              <button className="btn btn-ghost btn-sm" disabled={busy}
                onClick={() => run('/api/admin/notifications/dispatch',
                  (d: { attempted: number; sent: number; gaveUp: number }) =>
                    `예약 알림 발송 — 시도 ${d.attempted}, 성공 ${d.sent}, 재시도 포기 ${d.gaveUp}`)}>
                예약 알림 지금 발송
              </button>
              <ConfirmButton label="행사 종료 처리" disabled={busy}
                onConfirm={() => run(`/api/admin/slots/${slotId}/close`,
                  (d: { participants: number; attended: number; noShowCandidates: number }) =>
                    `종료 처리 — ${d.participants}명 중 참석 ${d.attended}, 노쇼 후보 ${d.noShowCandidates}. 후기 요청을 보냈어요`)} />
            </>
          )}
        </div>
      </div>

      {message && <div className={`alert ${message.ok ? 'ok' : 'error'}`}>{message.text}</div>}

      <div className="filter">
        {([['matching', '매칭 결과'], ['applicants', '신청자'], ['attendance', '참석'], ['feedback', '후기']] as const)
          .map(([key, label]) => (
            <button key={key} className={`chip ${tab === key ? 'on' : ''}`} onClick={() => setTab(key)}>{label}</button>
          ))}
      </div>

      {tab === 'matching' && <MatchingTab result={r} />}
      {tab === 'applicants' && <ApplicantsTab slotId={slotId!} />}
      {tab === 'attendance' && <AttendanceTab slotId={slotId!} />}
      {tab === 'feedback' && <FeedbackTab slotId={slotId!} />}
    </div>
  );
}

function MatchingTab({ result: r }: { result: SlotMatchingResult }) {
  const navigate = useNavigate();
  const run = r.run;
  if (!run) return <div className="alert info">아직 매칭 전이에요. 마감 시각에 자동으로 실행되고, 데모에서는 "지금 마감 실행"으로 바로 돌릴 수 있어요.</div>;
  return (
    <>
      <div className="kpis">
        <div className="kpi"><b>{run.candidateCount}</b><span>후보 (결제 완료)</span></div>
        <div className="kpi"><b>{run.assignedCount}</b><span>확정</span></div>
        <div className="kpi"><b>{run.tableCount}</b><span>테이블</span></div>
        <div className="kpi"><b>{run.carriedOverCount}</b><span>다음 날짜로 이월</span></div>
        <div className="kpi"><b>{run.alternativeOfferedCount}</b><span>대체 일정 안내</span></div>
        <div className="kpi"><b>{run.reallocations}</b><span>재배치 · 해제 {run.dissolvedTables}</span></div>
      </div>
      <p className="small muted" style={{ marginTop: 8 }}>
        {run.algorithmVersion} · {dateTime(run.finishedAt)} 실행{r.appliedPolicy && ` · ${r.appliedPolicy}`}
      </p>

      <p className="section-title">테이블 {r.tables.length}개</p>
      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr>
              <th>테이블</th><th>지역</th><th className="num">인원</th><th>나이</th><th>여:남</th>
              <th className="num">점수</th>
            </tr>
          </thead>
          <tbody>
            {r.tables.map((t) => (
              <tr key={t.tableId} className="link" onClick={() => navigate(`/admin/tables/${t.tableId}`)}>
                <td><b>{t.tableNo}번</b></td>
                <td>{t.regionLabel}</td>
                <td className="num">{t.size}명</td>
                <td>{t.minAge}~{t.maxAge}세</td>
                <td>{t.femaleCount}:{t.maleCount}</td>
                <td className="num"><b>{t.totalScore.toFixed(1)}</b></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {r.failures.length > 0 && (
        <>
          <p className="section-title">테이블에 들지 못한 신청 {r.failures.length}건</p>
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr><th>이름</th><th>사유</th><th>처리</th><th className="num">재배치 시도</th></tr>
              </thead>
              <tbody>
                {r.failures.map((f) => (
                  <tr key={f.applicationId}>
                    <td>{f.name}</td>
                    <td>{f.reasonDescription}</td>
                    <td>
                      <span className={`pill ${f.outcome === 'CARRIED_OVER' ? '' : 'warn'}`}>
                        {f.outcome === 'CARRIED_OVER' ? `${f.nextEventDate ? day(f.nextEventDate) : ''} 이월` : '대체 안내'}
                      </span>
                    </td>
                    <td className="num">{f.relocationCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  );
}

function ApplicantsTab({ slotId }: { slotId: string }) {
  const applicants = useApi<SlotApplicant[]>(`/api/admin/slots/${slotId}/applicants`);
  if (!applicants.data) return applicants.error ? <ErrorBox message={applicants.error} /> : <Loading />;
  if (applicants.data.length === 0) return <div className="alert info">이 날짜를 고른 신청이 없어요</div>;
  return (
    <div className="table-wrap">
      <table className="data">
        <thead>
          <tr><th>이름</th><th>이용권</th><th>지역</th><th>상태</th><th>결제</th><th>신청 시각</th></tr>
        </thead>
        <tbody>
          {applicants.data.map((a) => (
            <tr key={a.applicationId}>
              <td><b>{a.name}</b></td>
              <td>{a.ticketLabel}</td>
              <td className="small">{a.regions.join(', ')}</td>
              <td><span className={`pill ${statusTone(a.status)}`}>{a.statusLabel}</span></td>
              <td className="small">{a.paymentStatusLabel}</td>
              <td className="small">{dateTime(a.submittedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AttendanceTab({ slotId }: { slotId: string }) {
  const attendances = useApi<AdminAttendance[]>(`/api/admin/slots/${slotId}/attendances`);
  const applicants = useApi<SlotApplicant[]>(`/api/admin/slots/${slotId}/applicants`);
  if (!attendances.data) return attendances.error ? <ErrorBox message={attendances.error} /> : <Loading />;
  if (attendances.data.length === 0) return <div className="alert info">확정된 참가자가 없어요</div>;
  const names = new Map((applicants.data ?? []).map((a) => [a.userId, a.name]));
  const count = (status: string) => attendances.data!.filter((a) => a.status === status).length;
  return (
    <>
      <div className="kpis" style={{ marginBottom: 14 }}>
        <div className="kpi"><b>{attendances.data.length}</b><span>확정 인원</span></div>
        <div className="kpi"><b>{count('ATTENDED')}</b><span>참석 (체크인)</span></div>
        <div className="kpi"><b>{count('SCHEDULED')}</b><span>참석 예정</span></div>
        <div className="kpi"><b>{count('CANCELED_EARLY') + count('CANCELED_LATE')}</b><span>취소</span></div>
        <div className="kpi"><b>{count('NO_SHOW_CANDIDATE') + count('NO_SHOW')}</b><span>노쇼 후보·확정</span></div>
      </div>
      <div className="table-wrap">
        <table className="data">
          <thead><tr><th>이름</th><th>테이블</th><th>상태</th><th>체크인</th></tr></thead>
          <tbody>
            {attendances.data.map((a) => (
              <tr key={a.attendanceId}>
                <td><b>{names.get(a.userId) ?? `사용자 ${a.userId}`}</b></td>
                <td><Link to={`/admin/tables/${a.tableId}`} style={{ color: 'var(--brand)' }}>#{a.tableId}</Link></td>
                <td>{a.statusLabel}</td>
                <td className="small">{a.checkedInAt ? dateTime(a.checkedInAt) : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

function FeedbackTab({ slotId }: { slotId: string }) {
  const summary = useApi<SlotFeedbackSummary>(`/api/admin/slots/${slotId}/feedback`);
  if (!summary.data) return summary.error ? <ErrorBox message={summary.error} /> : <Loading />;
  const s = summary.data;
  if (s.responses === 0) return <div className="alert info">아직 들어온 후기가 없어요. 행사 종료 처리를 하면 참석자에게 후기 요청이 가요.</div>;
  return (
    <>
      <div className="kpis">
        <div className="kpi"><b>{s.responses}</b><span>응답</span></div>
        <div className="kpi"><b>{s.averages.tableMood}</b><span>분위기 (5점)</span></div>
        <div className="kpi"><b>{s.averages.conversation}</b><span>대화 (5점)</span></div>
        <div className="kpi"><b>{s.averages.venue}</b><span>식당 (5점)</span></div>
        <div className="kpi"><b>{s.averages.rejoinIntent}</b><span>재참여 의향 (5점)</span></div>
        <div className="kpi"><b>{s.nps}</b><span>순추천지수 · 평균 {s.averages.recommendScore}/10</span></div>
      </div>
      <p className="small muted" style={{ marginTop: 8 }}>
        사람별 "또 만나요·다음엔 따로"는 비공개로 다음 매칭에만 반영되고 여기에는 보이지 않아요.
      </p>

      <p className="section-title">테이블별</p>
      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr><th>테이블</th><th className="num">응답</th><th className="num">분위기</th><th className="num">대화</th>
              <th className="num">식당</th><th className="num">재참여</th><th className="num">추천</th></tr>
          </thead>
          <tbody>
            {s.tables.map((t) => (
              <tr key={t.tableId}>
                <td><Link to={`/admin/tables/${t.tableId}`} style={{ color: 'var(--brand)' }}>#{t.tableId}</Link></td>
                <td className="num">{t.responses}</td>
                <td className="num">{t.averages.tableMood}</td>
                <td className="num">{t.averages.conversation}</td>
                <td className="num">{t.averages.venue}</td>
                <td className="num">{t.averages.rejoinIntent}</td>
                <td className="num">{t.averages.recommendScore}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {s.comments.length > 0 && (
        <>
          <p className="section-title">한마디</p>
          {s.comments.map((c, i) => (
            <div key={i} className="card">
              <p style={{ margin: 0 }}>{c.comment}</p>
              <p className="small muted" style={{ margin: '4px 0 0' }}>{c.name} · 테이블 #{c.tableId} · 분위기 {c.tableMood}점</p>
            </div>
          ))}
        </>
      )}
    </>
  );
}
