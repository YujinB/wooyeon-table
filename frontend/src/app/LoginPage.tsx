import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, errorText, session } from '../api/client';
import { LoginResult } from '../api/types';
import { LogoIcon } from '../components/Logo';
import { ErrorBox, Field } from '../components/ui';

/** 테스트 계정 로그인 (ACC-01) */
export default function LoginPage() {
  const navigate = useNavigate();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const login = async (id: string, pw: string) => {
    setBusy(true);
    try {
      const result = await api.post<LoginResult>('/api/auth/login', { loginId: id, password: pw });
      session.save(result.token, { userId: result.userId, name: result.name, role: result.role });
      navigate(result.role === 'ADMIN' ? '/admin' : '/app', { replace: true });
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    login(loginId, password);
  };

  return (
    <div className="app-shell" style={{ justifyContent: 'center', padding: 24 }}>
      <div className="center" style={{ marginBottom: 28 }}>
        <div style={{ display: 'inline-block' }}>
          <LogoIcon size={84} />
        </div>
        <h1 className="h1" style={{ fontSize: 30, marginTop: 18 }}>우연한 식탁</h1>
        <p className="muted">성향이 맞는 4~6인과 함께하는 저녁</p>
      </div>

      <form onSubmit={submit} className="card">
        <ErrorBox message={error} />
        <Field label="아이디">
          <input className="input" autoComplete="username" value={loginId}
            onChange={(e) => setLoginId(e.target.value)} />
        </Field>
        <Field label="비밀번호">
          <input className="input" type="password" autoComplete="current-password" value={password}
            onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <button className="btn btn-primary btn-block" disabled={busy || !loginId || !password}>로그인</button>
      </form>
    </div>
  );
}
