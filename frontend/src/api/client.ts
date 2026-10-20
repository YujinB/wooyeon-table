// 백엔드 API 호출. 응답은 { success, data, error } 형태이고 data만 꺼내 준다

const TOKEN_KEY = 'wooyeon.token';
const USER_KEY = 'wooyeon.user';

export interface SessionUser {
  userId: number;
  name: string;
  role: 'PARTICIPANT' | 'ADMIN';
}

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) {
    super(message);
  }
}

export const session = {
  token: () => localStorage.getItem(TOKEN_KEY),
  user: (): SessionUser | null => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as SessionUser) : null;
  },
  save(token: string, user: SessionUser) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const token = session.token();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const json = await response.json().catch(() => null);
  if (!response.ok || !json?.success) {
    if (response.status === 401) session.clear();
    throw new ApiError(response.status, json?.error?.code ?? 'UNKNOWN',
      json?.error?.message ?? '요청을 처리하지 못했어요');
  }
  return json.data as T;
}

/** 로그인 토큰을 붙여 파일을 받아 브라우저 다운로드로 저장 (CSV 등) */
async function download(path: string, filename: string): Promise<void> {
  const token = session.token();
  const response = await fetch(path, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!response.ok) throw new ApiError(response.status, 'DOWNLOAD_FAILED', '파일을 받지 못했어요');
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export const api = {
  download,
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
};

/** 서버 메시지 "코드 설명: 상세"에서 사람이 읽을 부분 */
export function errorText(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return '네트워크 오류가 발생했어요';
}
