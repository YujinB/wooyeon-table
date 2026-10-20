import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { MyApplication, MyConsent, MyProfile, OpenSlot, ProfileOptions, RefundPolicy, TICKETS } from '../api/types';
import { Chips, ErrorBox, Loading } from '../components/ui';
import { dateTime, day, time, won } from '../format';
import { useApi } from '../useApi';

/** 참가 가능 날짜·희망 지역 복수 선택 + 이용권 선택 + 개인정보·운영 정책 동의 → 신청 제출 (APP-01~04, ACC-05) */
export default function ApplyPage() {
  const navigate = useNavigate();
  const slots = useApi<OpenSlot[]>('/api/app/slots');
  const options = useApi<ProfileOptions>('/api/app/profile-options');
  const profile = useApi<MyProfile>('/api/app/me/profile');
  const policy = useApi<RefundPolicy>('/api/app/policies/refund');
  const consent = useApi<MyConsent>('/api/app/me/consent');
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [opened, setOpened] = useState<string | null>(null);
  const [slotIds, setSlotIds] = useState<string[]>([]);
  const [regions, setRegions] = useState<string[]>([]);
  const [ticket, setTicket] = useState<string>('SINGLE');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!slots.data || !options.data) return <Loading />;

  if (profile.data && !profile.data.completed) {
    return (
      <div className="card center">
        <p>신청하려면 먼저 프로필을 채워 주세요.</p>
        <button className="btn btn-primary" onClick={() => navigate('/app/profile?next=/app/apply')}>
          프로필 입력하기
        </button>
      </div>
    );
  }

  const slotOptions = slots.data.map((slot) => ({
    code: String(slot.slotId),
    label: `${day(slot.eventDate)} ${time(slot.startTime)}`,
  }));
  const firstDeadline = slots.data
    .filter((slot) => slotIds.includes(String(slot.slotId)))
    .map((slot) => slot.applyDeadline)
    .sort()[0];

  const needsConsent = !!consent.data && !consent.data.agreed;
  const consentReady = !needsConsent || consent.data!.sections.every((section) => checked[section.key]);

  const submit = async () => {
    setBusy(true);
    try {
      if (needsConsent) {
        await api.post('/api/app/me/consent', { privacy: !!checked.privacy, policy: !!checked.policy });
      }
      const created = await api.post<MyApplication>('/api/app/applications', {
        ticketType: ticket,
        regions,
        slotIds: slotIds.map(Number),
      });
      navigate(`/app/applications/${created.applicationId}/pay`, { replace: true });
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h1 className="h1">식탁 신청</h1>
      <p className="muted">갈 수 있는 날짜와 지역을 모두 골라 주세요. 많이 고를수록 테이블이 만들어질 가능성이 높아요.</p>
      <ErrorBox message={error} />

      <p className="section-title">참가 가능한 날짜 (목요일 저녁)</p>
      {slotOptions.length === 0 ? (
        <div className="card muted center">지금 모집 중인 날짜가 없어요</div>
      ) : (
        <Chips options={slotOptions} value={slotIds} onChange={setSlotIds} />
      )}
      {firstDeadline && (
        <p className="muted small" style={{ marginTop: 8 }}>
          신청·결제 마감과 자동 매칭: {dateTime(firstDeadline)} (이 시각 이후 취소는 환불되지 않아요)
        </p>
      )}

      <p className="section-title">희망 지역</p>
      <Chips options={options.data.regions} value={regions} onChange={setRegions} />

      <p className="section-title">이용권</p>
      <div className="stack">
        {TICKETS.map((t) => (
          <button
            key={t.code}
            type="button"
            className="card spread"
            onClick={() => setTicket(t.code)}
            style={{
              width: '100%',
              cursor: 'pointer',
              borderColor: ticket === t.code ? 'var(--brand)' : undefined,
              background: ticket === t.code ? 'var(--surface-2)' : '#fff',
              textAlign: 'left',
            }}
          >
            <span>
              <strong>{t.label}</strong>
              <span className="muted small" style={{ display: 'block' }}>{t.note}</span>
            </span>
            <strong style={{ color: 'var(--brand)' }}>{won(t.price)}</strong>
          </button>
        ))}
      </div>
      <p className="muted small">멤버십은 기간 안에 횟수 제한 없이 신청할 수 있고, 매칭에 실패하면 실패 1회당 7일 늘어나요.</p>

      {policy.data && (
        <div className="alert info small" style={{ marginTop: 14 }}>
          <strong>환불 규정</strong> · {policy.data.summary}
        </div>
      )}

      <p className="section-title">동의</p>
      {consent.data?.agreed ? (
        <p className="muted small">
          개인정보 수집·이용과 운영 정책에 동의했어요{consent.data.agreedAt && ` (${dateTime(consent.data.agreedAt)})`}
        </p>
      ) : consent.data && (
        <div className="card">
          <label className="row" style={{ fontWeight: 700, paddingBottom: 10, borderBottom: '1px solid var(--line)' }}>
            <input type="checkbox"
              checked={consent.data.sections.every((section) => checked[section.key])}
              onChange={(e) => setChecked(Object.fromEntries(consent.data!.sections.map((section) => [section.key, e.target.checked])))} />
            전체 동의
          </label>
          {consent.data.sections.map((section) => (
            <div key={section.key} style={{ marginTop: 10 }}>
              <div className="spread">
                <label className="row small">
                  <input type="checkbox" checked={!!checked[section.key]}
                    onChange={(e) => setChecked({ ...checked, [section.key]: e.target.checked })} />
                  {section.title}
                </label>
                <button type="button" className="btn btn-ghost btn-sm" style={{ padding: '4px 10px' }}
                  onClick={() => setOpened(opened === section.key ? null : section.key)}>
                  {opened === section.key ? '접기' : '보기'}
                </button>
              </div>
              {opened === section.key && (
                <ul className="small muted" style={{ margin: '8px 0 0', paddingLeft: 18 }}>
                  {section.items.map((item) => <li key={item} style={{ marginBottom: 4 }}>{item}</li>)}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="bottom-cta">
        <button className="btn btn-primary btn-block"
          disabled={busy || slotIds.length === 0 || regions.length === 0 || !consentReady}
          onClick={submit}>
          신청하고 결제하기
        </button>
      </div>
    </div>
  );
}
