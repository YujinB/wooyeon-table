import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { MyProfile, ProfileOptions } from '../api/types';
import { Chips, ErrorBox, Field, Loading, SingleChips } from '../components/ui';
import { formatPhone } from '../format';
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

type Errors = Partial<Record<keyof Form, string>>;

/** 필수: 이름·휴대전화·출생연도·성별, 관심사·본인 성향·원하는 상대, 식사 가격대. 선택: MBTI·직업·못 먹는 음식 */
function validate(form: Form): Errors {
  const errors: Errors = {};
  const year = Number(form.birthYear);
  if (!form.name.trim()) errors.name = '이름을 입력해 주세요';
  if (!/^01\d-\d{3,4}-\d{4}$/.test(form.phone)) errors.phone = '휴대전화 번호를 끝까지 입력해 주세요';
  if (!form.birthYear) errors.birthYear = '출생연도를 입력해 주세요';
  else if (year < 1940 || year > 2008) errors.birthYear = '1940~2008년생만 신청할 수 있어요';
  if (!form.gender) errors.gender = '성별을 골라 주세요';
  if (form.mbti && !/^[EI][NS][TF][JP]$/.test(form.mbti)) errors.mbti = 'ENFP처럼 4글자로 입력해 주세요';
  if (form.interests.length === 0) errors.interests = '관심사를 하나 이상 골라 주세요';
  if (form.traits.length === 0) errors.traits = '하나 이상 골라 주세요';
  if (form.preferredTraits.length === 0) errors.preferredTraits = '하나 이상 골라 주세요';
  if (form.mealBudgets.length === 0) errors.mealBudgets = '가격대를 하나 이상 골라 주세요';
  return errors;
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
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    const p = profile.data;
    if (!p || form) return;
    setForm({
      name: p.name ?? '',
      phone: formatPhone(p.phone ?? ''),
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
  // 한 번 저장을 눌러 본 뒤부터 칸마다 빠진 것을 보여 준다
  const errors = submitted ? validate(form) : {};

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitted(true);
    if (Object.keys(validate(form)).length > 0) {
      setError('필수 항목을 확인해 주세요');
      window.setTimeout(() => document.querySelector('[data-invalid]')?.scrollIntoView({ behavior: 'smooth', block: 'center' }));
      return;
    }
    setError(null);
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
      <p className="muted">테이블을 고를 때만 쓰여요. 다른 참가자에게는 이름·연락처가 보이지 않아요.
        <span className="small"> (<em className="required" style={{ color: 'var(--danger)', fontStyle: 'normal' }}>*</em> 필수)</span></p>
      <ErrorBox message={error} />

      <p className="section-title">기본 정보</p>
      <Field label="이름" required error={errors.name}>
        <input className="input" value={form.name} onChange={(e) => set('name', e.target.value)} />
      </Field>
      <Field label="휴대전화" required error={errors.phone}>
        <input className="input" inputMode="tel" placeholder="010-0000-0000" value={form.phone}
          onChange={(e) => set('phone', formatPhone(e.target.value))} />
      </Field>
      <div className="row" style={{ alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          <Field label="출생연도" required error={errors.birthYear}>
            <input className="input" inputMode="numeric" placeholder="1996" value={form.birthYear}
              onChange={(e) => set('birthYear', e.target.value.replace(/\D/g, '').slice(0, 4))} />
          </Field>
        </div>
        <div style={{ flex: 1 }}>
          <Field label="MBTI (모르면 비워 두세요)" error={errors.mbti}>
            <input className="input" placeholder="ENFP" maxLength={4} value={form.mbti}
              onChange={(e) => set('mbti', e.target.value.toUpperCase().replace(/[^A-Z]/g, ''))} />
          </Field>
        </div>
      </div>
      <Field label="성별" required error={errors.gender}>
        <SingleChips options={o.genders} value={form.gender} onChange={(v) => set('gender', v)} />
      </Field>
      <Field label="직업 (선택)">
        <input className="input" value={form.job} onChange={(e) => set('job', e.target.value)} />
      </Field>

      <p className="section-title">매칭 프로필</p>
      <Field label="관심사 (여러 개)" required error={errors.interests}>
        <Chips options={o.interests} value={form.interests} onChange={(v) => set('interests', v)} />
      </Field>
      <Field label="나는 어떤 사람인가요?" required error={errors.traits}>
        <Chips options={o.traits} value={form.traits} onChange={(v) => set('traits', v)} />
      </Field>
      <Field label="어떤 사람을 만나고 싶나요?" required error={errors.preferredTraits}>
        <Chips options={o.preferredTraits} value={form.preferredTraits} onChange={(v) => set('preferredTraits', v)} />
      </Field>

      <p className="section-title">식사 조건</p>
      <Field label="희망 식사 가격대 (1인)" required error={errors.mealBudgets}>
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
