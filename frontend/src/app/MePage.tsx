import { Link, useNavigate } from 'react-router-dom';
import { session } from '../api/client';
import { Eligibility, MyHistory, MyProfile } from '../api/types';
import { Icon } from '../components/Icon';
import { Loading } from '../components/ui';
import { dateTime, day } from '../format';
import { useApi } from '../useApi';

/** 내 정보: 프로필, 이용 제한 여부 (SAF-02), 참가 이력 (RET-01), 다음 일정 추천 (RET-02) */
export default function MePage() {
  const navigate = useNavigate();
  const user = session.user();
  const profile = useApi<MyProfile>('/api/app/me/profile');
  const eligibility = useApi<Eligibility>('/api/app/me/eligibility');
  const history = useApi<MyHistory>('/api/app/me/history');

  const logout = () => {
    session.clear();
    navigate('/app/login', { replace: true });
  };

  return (
    <div>
      <div className="row" style={{ gap: 14 }}>
        <div className="avatar" style={{ background: 'var(--brand)' }}>{user?.name.slice(0, 1)}</div>
        <div>
          <p className="h2" style={{ margin: 0 }}>{user?.name}</p>
          <p className="small muted" style={{ margin: 0 }}>
            {profile.data ? (profile.data.completed ? '프로필 작성 완료' : '프로필을 채워 주세요') : ''}
          </p>
        </div>
      </div>

      {eligibility.data && !eligibility.data.eligible && (
        <div className="alert error" style={{ marginTop: 14 }}>
          <span className="icon-inline" style={{ color: 'inherit' }}><Icon name="alert" size={18} /></span>
          이용이 제한된 상태예요 — {eligibility.data.reason}
          {eligibility.data.until && ` (${eligibility.data.until.slice(0, 10)}까지)`}
        </div>
      )}

      <div className="card" style={{ marginTop: 16, padding: '4px 16px' }}>
        <Link to="/app/profile" className="list-item">
          <Icon name="user" /><span style={{ flex: 1 }}>프로필 수정</span><Icon name="chevron" size={18} />
        </Link>
        <Link to="/app/notifications" className="list-item">
          <Icon name="bell" /><span style={{ flex: 1 }}>알림</span><Icon name="chevron" size={18} />
        </Link>
      </div>

      <p className="section-title">다음 일정 추천</p>
      {!history.data ? <Loading /> : history.data.recommendations.length === 0 ? (
        <p className="muted small">지금 추천할 회차가 없어요</p>
      ) : (
        history.data.recommendations.map((r) => (
          <Link key={`${r.slotId}-${r.region}`} to="/app/apply" className="card" style={{ display: 'block' }}>
            <div className="spread">
              <b><span className="icon-inline"><Icon name="calendar" size={18} /></span>{day(r.eventDate)} · {r.region}</b>
              <Icon name="chevron" size={18} />
            </div>
            <p className="small muted" style={{ margin: '4px 0 0' }}>{r.reason} · 마감 {dateTime(r.applyDeadline)}</p>
          </Link>
        ))
      )}

      <p className="section-title">참가 이력</p>
      {history.data && (history.data.history.length === 0 ? (
        <p className="muted small">아직 참가한 모임이 없어요</p>
      ) : (
        <div className="card" style={{ padding: '4px 16px' }}>
          {history.data.history.map((h) => (
            <div key={h.applicationId} className="list-item">
              <div style={{ flex: 1 }}>
                <b>{day(h.eventDate)} · {h.region}</b>
                <div className="small muted">{h.tableSize}명 · {h.attendanceLabel}</div>
              </div>
              {h.attendance === 'ATTENDED' && !h.feedbackSubmitted ? (
                <Link to={`/app/applications/${h.applicationId}/feedback`} className="btn btn-secondary btn-sm">후기 쓰기</Link>
              ) : h.feedbackSubmitted ? (
                <span className="pill ok">후기 완료</span>
              ) : null}
            </div>
          ))}
        </div>
      ))}

      <button className="btn btn-ghost btn-block" style={{ marginTop: 24 }} onClick={logout}>
        <span className="icon-inline"><Icon name="logout" size={18} /></span>로그아웃
      </button>
    </div>
  );
}
