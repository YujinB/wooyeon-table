import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { MyProfile, ProfileOptions } from '../api/types';
import { Chips, ErrorBox, Field, Loading, SingleChips } from '../components/ui';
import { useApi } from '../useApi';

interface Form {
  name: string;
  phone: string;
  birthYear: string;
  gender: string | null;
  job: string;
  mbti: string;
  interests: string[];
  traits: string[];
  preferredTraits: string[];
  mealBudgets: string[];
  foodRestriction: string;
}

/** 기본 정보·매칭 프로필·식사 조건 (ACC-02~04). 한 번 저장하면 다음 신청에도 그대로 쓴다 */
export default function ProfilePage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const options = useApi<ProfileOptions>('/api/app/profile-options');
  const profile = useApi<MyProfile>('/api/app/me/profile');
  const [form, setForm] = useState<Form | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const p = profile.data;
    if (!p || form) return;
    setForm({
      name: p.name ?? '',
      phone: p.phone ?? '',
      birthYear: p.birthYear ? String(p.birthYear) : '',
      gender: p.gender,
      job: p.job ?? '',
      mbti: p.mbti ?? '',
      interests: p.interests,
      traits: p.traits,
      preferredTraits: p.preferredTraits,
      mealBudgets: p.mealBudgets,
      foodRestriction: p.foodRestriction ?? '',
    });
  }, [profile.data, form]);

  if (!options.data || !form) return <Loading />;
  const o = options.data;
  const set = <K extends keyof Form>(key: K, value: Form[K]) => setForm({ ...form, [key]: value });

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    try {
      await api.put('/api/app/me/profile', {
        ...form,
        birthYear: form.birthYear ? Number(form.birthYear) : null,
        mbti: form.mbti || null,
      });
      navigate(params.get('next') ?? '/app', { replace: true });
    } catch (e) {
      setError(errorText(e));
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit}>
      <h1 className="h1">내 프로필</h1>
      <p className="muted">테이블을 고를 때만 쓰여요. 다른 참가자에게는 이름·연락처가 보이지 않아요.</p>
      <ErrorBox message={error} />

      <p className="section-title">기본 정보</p>
      <Field label="이름">
        <input className="input" value={form.name} onChange={(e) => set('name', e.target.value)} />
      </Field>
      <Field label="휴대전화">
        <input className="input" inputMode="tel" placeholder="010-0000-0000" value={form.phone}
          onChange={(e) => set('phone', e.target.value)} />
      </Field>
      <div className="row" style={{ alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          <Field label="출생연도">
            <input className="input" inputMode="numeric" placeholder="1996" value={form.birthYear}
              onChange={(e) => set('birthYear', e.target.value.replace(/\D/g, '').slice(0, 4))} />
          </Field>
        </div>
        <div style={{ flex: 1 }}>
          <Field label="MBTI (모르면 비워 두세요)">
            <input className="input" placeholder="ENFP" maxLength={4} value={form.mbti}
              onChange={(e) => set('mbti', e.target.value.toUpperCase())} />
          </Field>
        </div>
      </div>
      <Field label="성별">
        <SingleChips options={o.genders} value={form.gender} onChange={(v) => set('gender', v)} />
      </Field>
      <Field label="직업 (선택)">
        <input className="input" value={form.job} onChange={(e) => set('job', e.target.value)} />
      </Field>

      <p className="section-title">매칭 프로필</p>
      <Field label="관심사 (여러 개)">
        <Chips options={o.interests} value={form.interests} onChange={(v) => set('interests', v)} />
      </Field>
      <Field label="나는 어떤 사람인가요?">
        <Chips options={o.traits} value={form.traits} onChange={(v) => set('traits', v)} />
      </Field>
      <Field label="어떤 사람을 만나고 싶나요?">
        <Chips options={o.preferredTraits} value={form.preferredTraits} onChange={(v) => set('preferredTraits', v)} />
      </Field>

      <p className="section-title">식사 조건</p>
      <Field label="희망 식사 가격대 (1인)">
        <Chips options={o.mealBudgets} value={form.mealBudgets} onChange={(v) => set('mealBudgets', v)} />
      </Field>
      <Field label="못 먹는 음식·알레르기 (선택)">
        <input className="input" placeholder="예: 갑각류 알레르기" value={form.foodRestriction}
          onChange={(e) => set('foodRestriction', e.target.value)} />
      </Field>

      <div className="bottom-cta">
        <button className="btn btn-primary btn-block" disabled={busy}>저장하기</button>
      </div>
    </form>
  );
}
