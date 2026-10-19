import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { Alternatives, ResolutionChoice, ResolutionResult } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day, won } from '../format';
import { useApi } from '../useApi';

const CHOICE_ICON: Record<ResolutionChoice, string> = {
  MOVE_SCHEDULE: 'calendar',
  KEEP_TICKET: 'ticket',
  REFUND: 'refund',
};

/** 매칭 실패: 사유와 대체 회차(지역별 대기 인원)를 보고 일정 이동·이용권 보관·환불 중 고른다 (STA-04~06, PAY-03) */
export default function ResolvePage() {
  const { id } = useParams();
  const alternatives = useApi<Alternatives>(`/api/app/applications/${id}/alternatives`);
  const [choice, setChoice] = useState<ResolutionChoice | null>(null);
  const [slotId, setSlotId] = useState<number | null>(null);
  const [regions, setRegions] = useState<string[]>([]);
  const [result, setResult] = useState<ResolutionResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!alternatives.data) return alternatives.error ? <ErrorBox message={alternatives.error} /> : <Loading />;
  const a = alternatives.data;
  const slot = a.slots.find((s) => s.slotId === slotId);

  if (result) {
    return (
      <div className="center" style={{ paddingTop: 30 }}>
        <div className="done-mark"><Icon name="check" size={34} stroke={2.4} /></div>
        <h1 className="h1">
          {result.choice === 'MOVE_SCHEDULE' && `${day(result.movedToEventDate!)} 매칭으로 옮겼어요`}
          {result.choice === 'KEEP_TICKET' && '이용권을 보관했어요'}
          {result.choice === 'REFUND' && '환불했어요'}
        </h1>
        <p className="muted">
          {result.choice === 'MOVE_SCHEDULE' && '결제는 그대로예요. 마감 후 매칭 결과를 알려드릴게요.'}
          {result.choice === 'KEEP_TICKET' && '다음에 신청할 때 결제 없이 바로 사용돼요.'}
          {result.choice === 'REFUND' && result.refund && `${won(result.refund.amount)}이 결제수단으로 돌아가요.`}
        </p>
        <Link to="/app" className="btn btn-primary btn-block" style={{ marginTop: 20 }}>홈으로</Link>
      </div>
    );
  }

  const toggleRegion = (code: string) =>
    setRegions((prev) => (prev.includes(code) ? prev.filter((r) => r !== code) : [...prev, code]));

  const submit = async () => {
    setBusy(true);
    try {
      setResult(await api.post<ResolutionResult>(`/api/app/applications/${id}/resolution`, {
        choice,
        slotId: choice === 'MOVE_SCHEDULE' ? slotId : null,
        regions: choice === 'MOVE_SCHEDULE' && regions.length > 0 ? regions : null,
      }));
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };

  const ready = choice && (choice !== 'MOVE_SCHEDULE' || slotId);

  return (
    <div>
      <span className="pill danger">매칭 실패</span>
      <h1 className="h1">이번엔 식탁을 만들지 못했어요</h1>
      {a.failure && (
        <div className="card">
          <p className="h2">{day(a.failure.eventDate)}</p>
          <p className="muted" style={{ margin: 0 }}>{a.failure.reason}</p>
        </div>
      )}
      <p className="muted small" style={{ marginTop: 10 }}>아래에서 원하시는 방법을 직접 골라 주세요. 고르면 바로 처리돼요.</p>

      <p className="section-title">어떻게 할까요?</p>
      {a.choices.map((c) => (
        <button key={c.choice} className={`option ${choice === c.choice ? 'on' : ''}`} disabled={!c.available}
          onClick={() => setChoice(c.choice)}>
          <div className="row">
            <span style={{ color: 'var(--brand)' }}><Icon name={CHOICE_ICON[c.choice]} /></span>
            <b>{c.label}</b>
          </div>
          <div className="small muted" style={{ marginTop: 4 }}>{c.available ? c.description : c.unavailableReason}</div>
        </button>
      ))}

      {choice === 'MOVE_SCHEDULE' && (
        <>
          <p className="section-title">옮길 회차</p>
          {a.slots.length === 0 && <div className="alert info">지금 옮길 수 있는 회차가 없어요</div>}
          {a.slots.map((s) => (
            <button key={s.slotId} className={`option ${slotId === s.slotId ? 'on' : ''}`}
              onClick={() => { setSlotId(s.slotId); setRegions([]); }}>
              <div className="spread">
                <b>{day(s.eventDate)} {s.eventStartsAt.slice(11, 16)}</b>
                <span className="small muted">마감 {dateTime(s.applyDeadline)}</span>
              </div>
            </button>
          ))}
          {slot && (
            <>
              <p className="section-title">지역 (비우면 원래 고른 지역)</p>
              <p className="small muted" style={{ marginTop: -4 }}>대기 인원이 많을수록 식탁이 만들어질 가능성이 높아요</p>
              <div className="chips">
                {slot.recommendedRegions.map((r) => (
                  <button type="button" key={r.code} className={`chip ${regions.includes(r.code) ? 'on' : ''}`}
                    onClick={() => toggleRegion(r.code)}>
                    {r.label} · 대기 {r.waitingCount}명{r.mine ? ' · 내 지역' : ''}
                  </button>
                ))}
              </div>
            </>
          )}
        </>
      )}

      <ErrorBox message={error} />
      <div className="bottom-cta">
        <button className="btn btn-primary btn-block" disabled={!ready || busy} onClick={submit}>
          {choice === 'REFUND' ? '환불받기' : choice === 'KEEP_TICKET' ? '이용권 보관하기' : '이 회차로 옮기기'}
        </button>
      </div>
    </div>
  );
}
