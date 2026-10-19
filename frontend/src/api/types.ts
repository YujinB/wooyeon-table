// 백엔드 응답 타입 (필요한 필드만)

export interface Option { code: string; label: string }

export interface ProfileOptions {
  genders: Option[];
  interests: Option[];
  traits: Option[];
  preferredTraits: Option[];
  mealBudgets: Option[];
  regions: Option[];
}

export interface MyProfile {
  loginId: string;
  name: string;
  phone: string | null;
  birthYear: number | null;
  gender: string | null;
  job: string | null;
  mbti: string | null;
  interests: string[];
  traits: string[];
  preferredTraits: string[];
  mealBudgets: string[];
  foodRestriction: string | null;
  completed: boolean;
}

export interface OpenSlot {
  slotId: number;
  eventDate: string;
  startTime: string;
  applyDeadline: string;
  refundClosesAt: string;
  regions: string[];
}

export interface Schedule {
  eventDate: string;
  eventStartsAt: string;
  matchAnnounceAt: string;
  refundClosesAt: string;
  reminderAt: string;
}

export type ParticipantStatus =
  | 'PAYMENT_PENDING' | 'PAYMENT_FAILED' | 'MATCH_WAITING' | 'MATCH_CONFIRMED'
  | 'MATCH_FAILED' | 'TICKET_KEPT' | 'REFUNDED' | 'CANCELED';

export interface MyApplication {
  applicationId: number;
  ticketType: string;
  ticketLabel: string;
  price: number;
  regions: string[];
  eventDates: string[];
  confirmedEventDate: string | null;
  status: ParticipantStatus;
  statusLabel: string;
  statusMessage: string;
  applicationStatus: string;
  paymentStatus: string;
  paymentStatusLabel: string;
  matchStatus: string;
  schedule: Schedule | null;
  submittedAt: string;
}

export interface PaymentResult {
  applicationId: number;
  success: boolean;
  type: 'CHARGE' | 'MEMBERSHIP_USE' | 'TICKET_CREDIT_USE' | 'REFUND';
  amount: number;
  paymentStatus: string;
  failureReason: string | null;
  membershipExpiresOn: string | null;
}

export interface RefundPolicy { summary: string; rules: string[] }

export interface Eligibility { eligible: boolean; level: string | null; reason: string | null; until: string | null }

export interface LoginResult { token: string; userId: number; name: string; role: 'PARTICIPANT' | 'ADMIN' }

export const TICKETS = [
  { code: 'SINGLE', label: '1회권', price: 9900, note: '이번 한 번' },
  { code: 'MEMBERSHIP_1M', label: '멤버십 1개월', price: 35000, note: '기간 안 무제한' },
  { code: 'MEMBERSHIP_3M', label: '멤버십 3개월', price: 75000, note: '기간 안 무제한' },
  { code: 'MEMBERSHIP_6M', label: '멤버십 6개월', price: 120000, note: '기간 안 무제한' },
] as const;
