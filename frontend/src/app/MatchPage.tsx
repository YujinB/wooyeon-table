import { CSSProperties, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { AttendanceView, ChatRoom, ChatRoomSummary, MyMatchResult, MyVenue } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day, time } from '../format';
import { useApi } from '../useApi';

const AVATAR_COLORS = ['#c8763f', '#8a5a3c', '#b4864f', '#6f7d5a', '#a0614a', '#7a6a8c'];

export function avatarStyle(index: number): CSSProperties {
  return { background: AVATAR_COLORS[index % AVATAR_COLORS.length] };
}

/** 매칭 완료: 테이블 요약·취향 매칭 점수·이 테이블인 이유 (MAT-12), 식당 안내 (INF-03), 체크인·취소 (FDB-01) */
export default function MatchPage() {
  const { id } = useParams();
  const match = useApi<MyMatchResult>(`/api/app/applications/${id}/match`);
  const confirmed = !!match.data?.table;
  const venue = useApi<MyVenue>(confirmed ? `/api/app/applications/${id}/venue` : null);
  const attendance = useApi<AttendanceView>(confirmed ? `/api/app/applications/${id}/attendance` : null);
  const rooms = useApi<ChatRoomSummary[]>(confirmed ? '/api/app/chat-rooms' : null);
  const roomId = rooms.data?.find((r) => r.eventDate === match.data?.table?.eventDate)?.roomId;
  const room = useApi<ChatRoom>(roomId ? `/api/app/chat-rooms/${roomId}` : null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!match.data) return match.error ? <ErrorBox message={match.error} /> : <Loading />;
  const result = match.data;
  const table = result.table;

  if (!table) {
    return (
      <div>
        <h1 className="h1">아직 식탁이 정해지지 않았어요</h1>
        <p>{result.message}</p>
        {result.failure && (
          <div className="card">
            <p className="h2">{day(result.failure.eventDate)} 매칭 결과</p>
            <p className="muted">{result.failure.reason}</p>
            <p className="small">{result.failure.outcomeDescription}
              {result.failure.nextEventDate && ` (${day(result.failure.nextEventDate)})`}</p>
          </div>
        )}
        {result.failure?.outcome === 'ALTERNATIVE_OFFERED' && (
          <Link to={`/app/applications/${id}/resolve`} className="btn btn-primary btn-block" style={{ marginTop: 16 }}>
            다른 일정 고르기
          </Link>
        )}
      </div>
    );
  }

  const act = async (path: string, confirmText: string) => {
    if (!window.confirm(confirmText)) return;
    setBusy(true);
    try {
      attendance.setData(await api.post<AttendanceView>(`/api/app/applications/${id}/attendance/${path}`));
      setError(null);
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };

  const members = room.data?.members ?? [];
  const status = attendance.data?.status;

  return (
    <div>
      <section className="match-head">
        <span className="pill ok"><span className="icon-inline" style={{ color: 'inherit' }}><Icon name="check" size={16} /></span>매칭 완료</span>
        <h1 className="h1">함께할 식탁이 정해졌어요</h1>
        <p className="muted">{day(table.eventDate)} {time(table.startTime)} · {table.region}</p>
        <div className="score-ring" style={{ '--p': table.score } as CSSProperties}>
          <div><b>{table.score}%</b><span>취향 매칭</span></div>
        </div>
      </section>

      {members.length > 0 && (
        <div className="avatars" style={{ margin: '14px 0 4px' }}>
          {members.map((m, i) => (
            <div key={m.seatNo} className="center" style={{ width: 64 }}>
              <div className="avatar" style={{ ...avatarStyle(i), margin: '0 auto' }}>{m.me ? '나' : m.seatNo}</div>
              <div className="small" style={{ marginTop: 4 }}>{m.alias}</div>
            </div>
          ))}
        </div>
      )}

      <div className="card" style={{ marginTop: 16 }}>
        <div className="stats">
          <div><b>{table.size}명</b><span>인원</span></div>
          <div><b>{table.minAge}~{table.maxAge}</b><span>나이</span></div>
          <div><b>{table.femaleCount}:{table.maleCount}</b><span>여:남</span></div>
        </div>
        {table.commonInterests.length > 0 && (
          <div className="tags" style={{ marginTop: 14, justifyContent: 'center' }}>
            {table.commonInterests.map((interest) => <span key={interest} className="tag">{interest}</span>)}
          </div>
        )}
      </div>

      <p className="section-title">이 식탁인 이유</p>
      <div className="card">
        {table.reasons.map((reason) => (
          <div key={reason} className="reason">
            <span className="icon"><Icon name="sparkle" size={18} /></span>
            <span>{reason}</span>
          </div>
        ))}
      </div>

      <p className="section-title">식당 안내</p>
      {venue.data?.assigned ? (
        <div className="card">
          <div className="venue-card">
            <div className="thumb"><Icon name="dining" size={30} /></div>
            <div>
              <p className="h2" style={{ margin: 0 }}>{venue.data.name}</p>
              <p className="muted" style={{ margin: '2px 0' }}>{venue.data.address}</p>
              <p className="small" style={{ margin: 0 }}>{venue.data.priceGuide}</p>
            </div>
          </div>
          <dl className="kv" style={{ marginTop: 14 }}>
            <dt>모이는 시간</dt><dd>{venue.data.gatherAt && dateTime(venue.data.gatherAt)}</dd>
            <dt>예약명</dt><dd>{venue.data.reservationName}</dd>
          </dl>
          <p className="small muted" style={{ marginBottom: 10 }}>{venue.data.message}</p>
          {venue.data.mapUrl && (
            <a href={venue.data.mapUrl} target="_blank" rel="noreferrer" className="btn btn-ghost btn-block btn-sm">
              <span className="icon-inline"><Icon name="map" size={18} /></span>지도에서 보기
            </a>
          )}
        </div>
      ) : (
        <div className="alert info">{venue.data?.message ?? '식당 정보를 불러오는 중이에요'}</div>
      )}

      <p className="section-title">참석</p>
      <ErrorBox message={error} />
      {attendance.data && (
        <div className="card">
          <div className="spread">
            <span>참석 상태</span>
            <span className={`pill ${status === 'ATTENDED' ? 'ok' : status === 'SCHEDULED' ? '' : 'gray'}`}>
              {attendance.data.statusLabel}
            </span>
          </div>
          {status === 'SCHEDULED' && (
            <div className="stack" style={{ marginTop: 12 }}>
              <button className="btn btn-primary btn-block" disabled={busy}
                onClick={() => act('check-in', '식당에 도착하셨나요? 체크인할게요.')}>
                도착했어요 · 체크인
              </button>
              <button className="btn btn-ghost btn-block btn-sm" disabled={busy}
                onClick={() => act('cancel', '확정 후 취소는 환불되지 않고, 당일 취소는 이용 제한에 반영될 수 있어요. 취소할까요?')}>
                참석 취소
              </button>
            </div>
          )}
          {status === 'ATTENDED' && (
            <Link to={`/app/applications/${id}/feedback`} className="btn btn-primary btn-block" style={{ marginTop: 12 }}>
              후기 남기기
            </Link>
          )}
        </div>
      )}

      {roomId && (
        <Link to={`/app/chats/${roomId}`} className="btn btn-secondary btn-block" style={{ marginTop: 16 }}>
          <span className="icon-inline" style={{ color: 'inherit' }}><Icon name="chat" size={18} /></span>단체방 열기
        </Link>
      )}
    </div>
  );
}
