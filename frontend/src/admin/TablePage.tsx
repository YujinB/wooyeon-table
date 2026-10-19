import { Link, useParams } from 'react-router-dom';
import { AdminChatRoom, TableDetail, VenueAssignment } from '../api/adminTypes';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day, time } from '../format';
import { useApi } from '../useApi';

const SCORE_ITEMS = [
  { key: 'genderBalance', label: '성비', max: 30 },
  { key: 'mbti', label: 'MBTI 궁합', max: 25 },
  { key: 'interest', label: '공통 관심사', max: 20 },
  { key: 'preference', label: '희망 상대 성향', max: 15 },
  { key: 'novelty', label: '새로운 사람', max: 10 },
] as const;

const NOTICE_LABEL: Record<string, string> = {
  WELCOME: '환영', PLACE: '시간·장소', CURATION: '큐레이션', POLICY: '취소 정책',
  REMINDER: '전날 리마인드', CONTENT: '대화 주제', CHAT: '대화',
};

/** 테이블 상세: 구성·점수 항목별 내역·좌석별 배치 사유 (MAT-12), 식당 배정, 단체방 안내 이력 */
export default function TablePage() {
  const { tableId } = useParams();
  const detail = useApi<TableDetail>(`/api/admin/tables/${tableId}`);
  const venue = useApi<VenueAssignment>(`/api/admin/tables/${tableId}/venue`);
  const chat = useApi<AdminChatRoom>(`/api/admin/tables/${tableId}/chat`);

  if (!detail.data) return detail.error ? <ErrorBox message={detail.error} /> : <Loading />;
  const t = detail.data;

  return (
    <div>
      <div className="admin-head">
        <div>
          <div className="crumb">
            <Link to="/admin">대시보드</Link> › <Link to={`/admin/slots/${t.slotId}`}>{day(t.eventDate)} 회차</Link> ›
          </div>
          <h1 className="h1">{t.regionLabel} {t.tableNo}번 테이블</h1>
          <p className="muted" style={{ margin: 0 }}>
            {day(t.eventDate)} {time(t.startTime)} · {t.size}명 · {t.minAge}~{t.maxAge}세 · 여 {t.femaleCount} : 남 {t.maleCount}
            · 자동 확정 {dateTime(t.confirmedAt)}
          </p>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="spread">
            <p className="h2">선호 점수</p>
            <b style={{ fontSize: 24, color: 'var(--brand-strong)' }}>{t.score.total.toFixed(1)}</b>
          </div>
          {SCORE_ITEMS.map((item) => (
            <div key={item.key} className="score-row">
              <span>{item.label}</span>
              <div className="bar"><div style={{ width: `${(t.score[item.key] / item.max) * 100}%` }} /></div>
              <span className="small muted">{t.score[item.key].toFixed(1)} / {item.max}</span>
            </div>
          ))}
          <dl className="kv" style={{ marginTop: 14 }}>
            <dt>MBTI</dt><dd>{t.mbtiComposition.join(' · ') || '-'}</dd>
            <dt>공통 관심사</dt><dd>{t.commonInterests.join(', ') || '-'}</dd>
            <dt>이전 만남</dt><dd>{t.previouslyMetPairs}쌍</dd>
          </dl>
        </div>

        <div className="card">
          <p className="h2">식당</p>
          {!venue.data ? <p className="muted">{venue.error ?? '불러오는 중'}</p> : venue.data.venue ? (
            <dl className="kv">
              <dt>식당</dt><dd><b>{venue.data.venue.name}</b></dd>
              <dt>주소</dt><dd>{venue.data.venue.address}</dd>
              <dt>예약명</dt><dd>{venue.data.reservationName}</dd>
              <dt>가격</dt><dd>{venue.data.venue.priceGuide}</dd>
              <dt>배정</dt><dd>{venue.data.decidedAt && dateTime(venue.data.decidedAt)} · 식당 {venue.data.attemptedVenues}곳 시도</dd>
            </dl>
          ) : (
            <div className="alert error">배정 실패 — {venue.data.failureReason} (예외 처리함에서 다시 배정)</div>
          )}
        </div>
      </div>

      <p className="section-title">좌석</p>
      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr><th>좌석</th><th>이름</th><th>나이·성별</th><th>MBTI</th><th>관심사</th><th>배치 사유</th><th>희망 성향</th></tr>
          </thead>
          <tbody>
            {t.seats.map((s, i) => (
              <tr key={s.applicationId}>
                <td>{i + 1}</td>
                <td><b>{s.name}</b></td>
                <td>{s.age ?? '-'} · {s.gender}</td>
                <td>{s.mbti ?? '-'}</td>
                <td className="small">{s.interests.join(', ')}</td>
                <td className="small">{s.placementDescription}{s.relocationCount > 0 && ` (재배치 ${s.relocationCount})`}</td>
                <td>{s.preferenceMet ? <span className="pill ok">충족</span> : <span className="pill gray">미충족</span>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="section-title">단체방 안내 이력</p>
      {!chat.data ? <Loading /> : !chat.data.created ? (
        <div className="alert error">단체방이 아직 만들어지지 않았어요</div>
      ) : (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>시각</th><th>종류</th><th>내용</th><th>상태</th></tr></thead>
            <tbody>
              {chat.data.messages.map((m, i) => (
                <tr key={i}>
                  <td className="small" style={{ whiteSpace: 'nowrap' }}>{dateTime(m.visibleAt)}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>{m.senderAlias ?? NOTICE_LABEL[m.kind] ?? m.kind}</td>
                  <td className="small" style={{ whiteSpace: 'pre-line', overflowWrap: 'anywhere' }}>{m.content}</td>
                  <td>{m.visible ? <span className="pill ok">게시</span> : <span className="pill">예약</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
