import { Link } from 'react-router-dom';
import { session } from '../api/client';
import { Eligibility, MyApplication, MyHistory, MyProfile, OpenSlot } from '../api/types';
import { LogoMark } from '../components/Logo';
import { Loading } from '../components/ui';
import { dateTime, day, statusTone, time } from '../format';
import { useApi } from '../useApi';

/** 홈: 다가오는 모임·현재 신청 상태와 다음 행동 (STA-01·02) */
export default function HomePage() {
  const user = session.user();
  const applications = useApi<MyApplication[]>('/api/app/applications');
  const profile = useApi<MyProfile>('/api/app/me/profile');
  const slots = useApi<OpenSlot[]>('/api/app/slots');
  const eligibility = useApi<Eligibility>('/api/app/me/eligibility');
  const history = useApi<MyHistory>('/api/app/me/history');

  if (applications.loading && !applications.data) return <Loading />;
  const list = applications.data ?? [];
  const active = list.filter((a) => !['CANCELED', 'REFUNDED', 'TICKET_KEPT'].includes(a.status));
  const reviewed = new Set((history.data?.history ?? []).filter((h) => h.feedbackSubmitted).map((h) => h.applicationId));
  const confirmed = active.find((a) => a.status === 'MATCH_CONFIRMED' && !isEnded(a));
  const toReview = active.find((a) => isEnded(a) && !reviewed.has(a.applicationId));
  const nextSlot = slots.data?.[0];

  const heroTag = confirmed?.schedule
    ? `${confirmed.regions[0] ?? ''} · ${day(confirmed.schedule.eventDate)} · ${confirmed.schedule.eventStartsAt.slice(11, 16)}`
    : nextSlot
      ? `다음 모임 · ${day(nextSlot.eventDate)} · ${time(nextSlot.startTime)}`
      : '새 모임을 준비하고 있어요';

  return (
    <div>
      <section className="hero">
        <div className="art">
          <LogoMark size={120} color="rgba(255, 226, 196, 0.35)" />
        </div>
        <span className="pill">{heroTag}</span>
        <h2>{confirmed ? '함께할 식탁이 정해졌어요' : toReview ? '지난 식탁은 어땠나요?' : '오늘, 누구와 식사할까요?'}</h2>
        <p>{user?.name ? `${user.name}님, ` : ''}성향이 맞는 4~6인과 함께하는 저녁</p>
        {confirmed ? (
          <Link to={`/app/applications/${confirmed.applicationId}/match`} className="btn btn-primary btn-block">
            내 식탁 보기
          </Link>
        ) : toReview ? (
          <Link to={`/app/applications/${toReview.applicationId}/feedback`} className="btn btn-primary btn-block">
            후기 남기기
          </Link>
        ) : (
          <Link to={profile.data?.completed ? '/app/apply' : '/app/profile?next=/app/apply'}
            className="btn btn-primary btn-block">
            내 식탁 찾기
          </Link>
        )}
      </section>

      {eligibility.data && !eligibility.data.eligible && (
        <div className="alert error" style={{ marginTop: 14 }}>
          지금은 신청할 수 없어요 — {eligibility.data.reason}
          {eligibility.data.until && ` (${eligibility.data.until.slice(0, 10)}까지)`}
        </div>
      )}
      {profile.data && !profile.data.completed && (
        <Link to="/app/profile" className="alert info" style={{ display: 'block', marginTop: 14 }}>
          신청 전에 프로필(관심사·성향·식사 예산)을 채워 주세요 ›
        </Link>
      )}

      <p className="section-title">내 신청</p>
      {active.length === 0 && (
        <div className="card center muted">아직 진행 중인 신청이 없어요</div>
      )}
      {active.map((application) => (
        <ApplicationCard key={application.applicationId} application={application}
          reviewed={reviewed.has(application.applicationId)} />
      ))}

      {list.length > active.length && (
        <>
          <p className="section-title">지난 신청</p>
          {list.filter((a) => !active.includes(a)).map((application) => (
            <ApplicationCard key={application.applicationId} application={application} />
          ))}
        </>
      )}
    </div>
  );
}

/** 확정된 모임 시작 후 3시간이 지나면 끝난 모임으로 본다 (그때부터 후기) */
function isEnded(a: MyApplication): boolean {
  return a.status === 'MATCH_CONFIRMED' && !!a.schedule
    && Date.now() > new Date(a.schedule.eventStartsAt).getTime() + 3 * 60 * 60 * 1000;
}

/** 상태별 다음 행동 */
function nextAction(a: MyApplication, reviewed: boolean): { label: string; to: string } | null {
  if (isEnded(a)) {
    return reviewed ? null : { label: '후기 남기기', to: `/app/applications/${a.applicationId}/feedback` };
  }
  switch (a.status) {
    case 'PAYMENT_PENDING':
    case 'PAYMENT_FAILED':
      return { label: '결제하기', to: `/app/applications/${a.applicationId}/pay` };
    case 'MATCH_CONFIRMED':
      return { label: '내 식탁 보기', to: `/app/applications/${a.applicationId}/match` };
    case 'MATCH_FAILED':
      return { label: '다른 일정 고르기', to: `/app/applications/${a.applicationId}/resolve` };
    default:
      return null;
  }
}

function ApplicationCard({ application: a, reviewed = false }: { application: MyApplication; reviewed?: boolean }) {
  const action = nextAction(a, reviewed);
  const ended = isEnded(a);
  return (
    <div className="card">
      <Link to={`/app/applications/${a.applicationId}`}>
        <div className="spread">
          <span className={`pill ${ended ? 'gray' : statusTone(a.status)}`}>{ended ? '모임 종료' : a.statusLabel}</span>
          <span className="muted small">{a.ticketLabel}</span>
        </div>
        <div className="h2" style={{ marginTop: 10 }}>
          {(a.confirmedEventDate ? [a.confirmedEventDate] : a.eventDates).map(day).join(', ')}
        </div>
        <div className="muted small">{a.regions.join(' · ')}</div>
        <p className="small" style={{ margin: '8px 0 0' }}>
          {ended
            ? (reviewed ? '후기 고마워요. 남겨 주신 선호는 다음 매칭에 반영돼요.' : '함께한 식탁은 어땠나요? 후기를 남기면 다음 매칭에 반영돼요.')
            : a.statusMessage}
        </p>
        {a.schedule && a.status === 'MATCH_WAITING' && (
          <p className="muted small" style={{ margin: '4px 0 0' }}>
            매칭 발표 {dateTime(a.schedule.matchAnnounceAt)}
          </p>
        )}
      </Link>
      {action && (
        <Link to={action.to} className="btn btn-secondary btn-block btn-sm" style={{ marginTop: 12 }}>
          {action.label}
        </Link>
      )}
    </div>
  );
}
