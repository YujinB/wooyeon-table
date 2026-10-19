const WEEK = ['일', '월', '화', '수', '목', '금', '토'];

/** "2026-10-22" → "10/22(목)" */
export function day(date: string): string {
  const d = new Date(`${date.slice(0, 10)}T00:00:00`);
  return `${d.getMonth() + 1}/${d.getDate()}(${WEEK[d.getDay()]})`;
}

/** "2026-10-21T00:00:00" → "10/21(수) 00:00" */
export function dateTime(value: string): string {
  return `${day(value)} ${value.slice(11, 16)}`;
}

/** "19:30:00" → "19:30" */
export function time(value: string): string {
  return value.slice(0, 5);
}

export function won(amount: number): string {
  return `${amount.toLocaleString('ko-KR')}원`;
}

const TONE: Record<string, string> = {
  PAYMENT_PENDING: 'warn',
  PAYMENT_FAILED: 'danger',
  MATCH_WAITING: '',
  MATCH_CONFIRMED: 'ok',
  MATCH_FAILED: 'danger',
  TICKET_KEPT: 'gray',
  REFUNDED: 'gray',
  CANCELED: 'gray',
};

export function statusTone(status: string): string {
  return TONE[status] ?? '';
}

/** 입력 중인 휴대전화 번호를 010-1234-5678 꼴로 (숫자만 남기고 최대 11자리) */
export function formatPhone(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.length < 4) return digits;
  if (digits.length < 8) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  if (digits.length === 10) return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`;
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
}
