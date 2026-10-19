import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { MyApplication, PaymentResult, RefundPolicy } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day, won } from '../format';
import { useApi } from '../useApi';

/** 모의 결제 (PAY-01) + 결제 전과 신청 완료 화면의 환불·노쇼 규정 (PAY-02) */
export default function PayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const application = useApi<MyApplication>(`/api/app/applications/${id}`);
  const policy = useApi<RefundPolicy>('/api/app/policies/refund');
  const [simulateFailure, setSimulateFailure] = useState(false);
  const [result, setResult] = useState<PaymentResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!application.data) return application.error ? <ErrorBox message={application.error} /> : <Loading />;
  const a = application.data;

  const pay = async () => {
    setBusy(true);
    setError(null);
    try {
      const paid = await api.post<PaymentResult>(`/api/app/applications/${id}/payment`, { simulateFailure });
      setResult(paid);
      application.reload();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };

  if (result?.success) {
    return (
      <div className="center" style={{ paddingTop: 30 }}>
        <div className="done-mark"><Icon name="check" size={34} stroke={2.4} /></div>
        <h1 className="h1">결제가 완료됐어요</h1>
        <p className="muted">
          {result.type === 'CHARGE' ? `${won(result.amount)} 결제` : result.type === 'MEMBERSHIP_USE' ? '멤버십으로 신청' : '보관한 이용권으로 신청'}
          {result.membershipExpiresOn && ` · 멤버십 ${result.membershipExpiresOn}까지`}
        </p>
        <div className="card" style={{ textAlign: 'left', marginTop: 18 }}>
          <p style={{ margin: 0 }}>{a.statusMessage}</p>
          {a.schedule && <p className="muted small" style={{ margin: '6px 0 0' }}>매칭 발표 {dateTime(a.schedule.matchAnnounceAt)}</p>}
        </div>
        {policy.data && (
          <div className="card" style={{ textAlign: 'left', marginTop: 12 }}>
            <p className="small" style={{ margin: 0, fontWeight: 700 }}>환불·취소 규정</p>
            <ul className="small muted" style={{ margin: '6px 0 0', paddingLeft: 18 }}>
              {policy.data.rules.slice(0, 3).map((rule) => <li key={rule} style={{ marginBottom: 4 }}>{rule}</li>)}
            </ul>
            {a.schedule && (
              <p className="small" style={{ margin: '6px 0 0' }}>
                이 신청은 <b>{dateTime(a.schedule.refundClosesAt)}</b>까지 취소하면 전액 환불돼요.
              </p>
            )}
          </div>
        )}
        <div className="spacer" />
        <button className="btn btn-primary btn-block" onClick={() => navigate('/app', { replace: true })}>홈으로</button>
      </div>
    );
  }

  return (
    <div>
      <h1 className="h1">결제</h1>
      <div className="card">
        <dl className="kv">
          <dt>이용권</dt><dd>{a.ticketLabel}</dd>
          <dt>날짜</dt><dd>{a.eventDates.map(day).join(', ')}</dd>
          <dt>지역</dt><dd>{a.regions.join(', ')}</dd>
          <dt>금액</dt><dd><strong style={{ color: 'var(--brand)' }}>{won(a.price)}</strong></dd>
        </dl>
        <p className="muted small" style={{ marginBottom: 0 }}>
          멤버십 기간 안이거나 보관한 이용권이 있으면 0원으로 처리돼요.
        </p>
      </div>

      {policy.data && (
        <>
          <p className="section-title">환불·취소 규정</p>
          <div className="card small">
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              {policy.data.rules.map((rule) => <li key={rule} style={{ marginBottom: 4 }}>{rule}</li>)}
            </ul>
          </div>
        </>
      )}

      {result && !result.success && (
        <div className="alert error" style={{ marginTop: 14 }}>
          결제가 승인되지 않았어요 ({result.failureReason}). 다시 시도해 주세요.
        </div>
      )}
      <ErrorBox message={error} />

      <label className="row muted small" style={{ marginTop: 14 }}>
        <input type="checkbox" checked={simulateFailure} onChange={(e) => setSimulateFailure(e.target.checked)} />
        데모: 결제 실패 흉내 내기
      </label>

      <div className="bottom-cta">
        <button className="btn btn-primary btn-block" disabled={busy} onClick={pay}>
          {won(a.price)} 결제하기 (모의)
        </button>
      </div>
    </div>
  );
}
