import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { MyApplication } from '../api/types';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day, statusTone } from '../format';
import { useApi } from '../useApi';

/** 신청 상세: 상태 하나·신청/결제/매칭 상태·예상 일정 (STA-01·02), 확정 전 취소 (PAY-02) */
export default function ApplicationPage() {
  const { id } = useParams();
  const application = useApi<MyApplication>(`/api/app/applications/${id}`);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!application.data) return application.error ? <ErrorBox message={application.error} /> : <Loading />;
  const a = application.data;
  const cancelable = a.matchStatus === 'WAITING' && a.applicationStatus === 'SUBMITTED';

  const cancel = async () => {
    if (!window.confirm('신청을 취소할까요? 결제했다면 결제수단으로 바로 환불돼요.')) return;
    setBusy(true);
    try {
      await api.post(`/api/app/applications/${id}/cancel`);
      await application.reload();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <span className={`pill ${statusTone(a.status)}`}>{a.statusLabel}</span>
      <h1 className="h1">{(a.confirmedEventDate ? [a.confirmedEventDate] : a.eventDates).map(day).join(', ')}</h1>
      <p>{a.statusMessage}</p>
      <ErrorBox message={error} />

      {a.schedule && (
        <>
          <p className="section-title">예상 일정</p>
          <div className="card">
            <dl className="kv">
              <dt>매칭 발표</dt><dd>{dateTime(a.schedule.matchAnnounceAt)}</dd>
              <dt>환불 마감</dt><dd>{dateTime(a.schedule.refundClosesAt)}</dd>
              <dt>장소 안내</dt><dd>{dateTime(a.schedule.reminderAt)}</dd>
              <dt>모임 시작</dt><dd>{dateTime(a.schedule.eventStartsAt)}</dd>
            </dl>
          </div>
        </>
      )}

      <p className="section-title">신청 내용</p>
      <div className="card">
        <dl className="kv">
          <dt>이용권</dt><dd>{a.ticketLabel}</dd>
          <dt>날짜</dt><dd>{a.eventDates.map(day).join(', ')}</dd>
          <dt>지역</dt><dd>{a.regions.join(', ')}</dd>
          <dt>신청</dt><dd>{a.applicationStatus === 'CANCELED' ? '취소' : '신청 완료'}</dd>
          <dt>결제</dt><dd>{a.paymentStatusLabel}</dd>
        </dl>
      </div>

      <div className="stack" style={{ marginTop: 18 }}>
        {(a.status === 'PAYMENT_PENDING' || a.status === 'PAYMENT_FAILED') && (
          <Link to={`/app/applications/${a.applicationId}/pay`} className="btn btn-primary btn-block">결제하기</Link>
        )}
        {a.status === 'MATCH_CONFIRMED' && (
          <Link to={`/app/applications/${a.applicationId}/match`} className="btn btn-primary btn-block">내 식탁 보기</Link>
        )}
        {a.status === 'MATCH_FAILED' && (
          <Link to={`/app/applications/${a.applicationId}/resolve`} className="btn btn-primary btn-block">다른 일정 고르기</Link>
        )}
        {cancelable && (
          <button className="btn btn-danger btn-block" disabled={busy} onClick={cancel}>신청 취소</button>
        )}
      </div>
    </div>
  );
}
