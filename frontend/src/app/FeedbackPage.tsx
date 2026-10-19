import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { FeedbackAnswers, FeedbackForm } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorBox, Field, Loading } from '../components/ui';
import { useApi } from '../useApi';

function Stars({ value, onChange }: { value: number; onChange: (next: number) => void }) {
  return (
    <div className="rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button type="button" key={n} className={n <= value ? 'on' : ''} onClick={() => onChange(n)} aria-label={`${n}점`}>
          <Icon name="star" size={30} />
        </button>
      ))}
    </div>
  );
}

type Pick = 'again' | 'avoid' | null;

/** 후기: 만족도 (FDB-02), 재참여·추천 의향 (FDB-03), 다시 만나고 싶은/싶지 않은 사람 (FDB-04, 비공개) */
export default function FeedbackPage() {
  const { id } = useParams();
  const form = useApi<FeedbackForm>(`/api/app/applications/${id}/feedback`);
  const [answers, setAnswers] = useState<FeedbackAnswers>({
    tableMood: 0, conversation: 0, venue: 0, rejoinIntent: 0, recommendScore: -1,
    comment: '', wantAgainSeats: [], avoidSeats: [],
  });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!form.data) return form.error ? <ErrorBox message={form.error} /> : <Loading />;
  const f = form.data;

  if (f.submitted) {
    return (
      <div className="center" style={{ paddingTop: 30 }}>
        <div className="done-mark"><Icon name="check" size={34} stroke={2.4} /></div>
        <h1 className="h1">후기 고마워요</h1>
        <p className="muted">남겨 주신 선호는 다음 매칭에 반영돼요. 다른 분들께는 공개되지 않아요.</p>
        <Link to="/app/me" className="btn btn-primary btn-block" style={{ marginTop: 20 }}>참가 이력 보기</Link>
      </div>
    );
  }
  if (!f.submittable) return <div className="alert info">{f.unavailableReason}</div>;

  const set = <K extends keyof FeedbackAnswers>(key: K, value: FeedbackAnswers[K]) =>
    setAnswers((prev) => ({ ...prev, [key]: value }));

  const pickOf = (seat: number): Pick =>
    answers.wantAgainSeats.includes(seat) ? 'again' : answers.avoidSeats.includes(seat) ? 'avoid' : null;

  const pick = (seat: number, next: Pick) => {
    const current = pickOf(seat);
    const target = current === next ? null : next;
    setAnswers((prev) => ({
      ...prev,
      wantAgainSeats: target === 'again' ? [...prev.wantAgainSeats, seat] : prev.wantAgainSeats.filter((s) => s !== seat),
      avoidSeats: target === 'avoid' ? [...prev.avoidSeats, seat] : prev.avoidSeats.filter((s) => s !== seat),
    }));
  };

  const complete = answers.tableMood > 0 && answers.conversation > 0 && answers.venue > 0
    && answers.rejoinIntent > 0 && answers.recommendScore >= 0;

  const submit = async () => {
    setBusy(true);
    try {
      form.setData(await api.post<FeedbackForm>(`/api/app/applications/${id}/feedback`, answers));
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h1 className="h1">오늘 식탁은 어땠나요?</h1>
      <p className="muted">솔직하게 남겨 주세요. 다른 참가자에게는 공개되지 않아요.</p>

      <div className="card" style={{ marginTop: 14 }}>
        <Field label="테이블 분위기"><Stars value={answers.tableMood} onChange={(v) => set('tableMood', v)} /></Field>
        <Field label="대화"><Stars value={answers.conversation} onChange={(v) => set('conversation', v)} /></Field>
        <Field label="식당"><Stars value={answers.venue} onChange={(v) => set('venue', v)} /></Field>
        <Field label="다시 참여하고 싶나요?"><Stars value={answers.rejoinIntent} onChange={(v) => set('rejoinIntent', v)} /></Field>
        <Field label="친구에게 추천할 가능성 (0~10)">
          <div className="scale">
            {Array.from({ length: 11 }, (_, n) => (
              <button type="button" key={n} className={answers.recommendScore === n ? 'on' : ''}
                onClick={() => set('recommendScore', n)}>{n}</button>
            ))}
          </div>
        </Field>
      </div>

      {f.others.length > 0 && (
        <>
          <p className="section-title">함께한 분들 (선택)</p>
          <div className="card" style={{ padding: '4px 16px' }}>
            {f.others.map((o) => (
              <div key={o.seatNo} className="list-item">
                <div style={{ flex: 1 }}>
                  <b>{o.alias}</b>
                  <div className="small muted">{o.intro}</div>
                </div>
                <button type="button" className={`chip ${pickOf(o.seatNo) === 'again' ? 'on' : ''}`}
                  onClick={() => pick(o.seatNo, 'again')}>또 만나요</button>
                <button type="button" className={`chip ${pickOf(o.seatNo) === 'avoid' ? 'on' : ''}`}
                  onClick={() => pick(o.seatNo, 'avoid')}>다음엔 따로</button>
              </div>
            ))}
            <p className="small muted">"다음엔 따로"를 고른 분과는 다음 매칭에서 같은 식탁에 배정되지 않아요.</p>
          </div>
        </>
      )}

      <p className="section-title">한마디 (선택)</p>
      <textarea className="input" rows={3} maxLength={500} value={answers.comment ?? ''}
        onChange={(e) => set('comment', e.target.value)} placeholder="좋았던 점, 아쉬웠던 점을 남겨 주세요" />

      <ErrorBox message={error} />
      <div className="bottom-cta">
        <button className="btn btn-primary btn-block" disabled={!complete || busy} onClick={submit}>후기 보내기</button>
      </div>
    </div>
  );
}
