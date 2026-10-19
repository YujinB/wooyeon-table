// 운영자 API 응답 타입 (필요한 필드만)

export type SlotStatus = 'RECRUITING' | 'MATCHED' | 'COMPLETED';

export const SLOT_STATUS_LABEL: Record<SlotStatus, string> = {
  RECRUITING: '모집 중',
  MATCHED: '매칭 완료',
  COMPLETED: '행사 종료',
};

/** 행사 시작 시각 (startTime이 없으면 19:30) */
export function slotStartsAt(eventDate: string, startTime = '19:30:00'): number {
  return new Date(`${eventDate}T${startTime}`).getTime();
}

/**
 * 저장된 상태에 시각을 더해 보여 준다: 마감이 지났는데 매칭 전이면 "마감 지남",
 * 행사일이 지났는데 종료 처리 전이면 "행사일 지남"
 */
export function slotStatusView(status: SlotStatus, matchAt: string, eventDate: string,
  startTime?: string): { label: string; tone: string } {
  if (status === 'RECRUITING' && new Date(matchAt).getTime() <= Date.now()) {
    return { label: '마감 지남 · 매칭 전', tone: 'warn' };
  }
  if (status === 'MATCHED' && slotStartsAt(eventDate, startTime) <= Date.now()) {
    return { label: '행사일 지남 · 종료 처리 전', tone: 'gray' };
  }
  return { label: SLOT_STATUS_LABEL[status], tone: { RECRUITING: '', MATCHED: 'ok', COMPLETED: 'gray' }[status] };
}

export interface SlotOverview {
  slotId: number;
  eventDate: string;
  startTime: string;
  matchAt: string;
  status: SlotStatus;
  applicants: number;
  paymentPending: number;
  waiting: number;
  confirmed: number;
  matchFailed: number;
  closed: number;
  tables: number;
}

export interface ExceptionCounts {
  venue: number;
  notification: number;
  noShow: number;
  reports: number;
  urgentReports: number;
  refund: number;
  total: number;
}

export interface Dashboard { slots: SlotOverview[]; exceptions: ExceptionCounts }

export type ExceptionType = 'REPORT' | 'VENUE' | 'REFUND' | 'NOTIFICATION' | 'NO_SHOW';

export interface ExceptionItem {
  type: ExceptionType;
  typeLabel: string;
  urgent: boolean;
  title: string;
  detail: string;
  refId: number;
  slotId: number | null;
  tableId: number | null;
  applicationId: number | null;
  at: string | null;
}

export interface MatchRunSummary {
  matchRunId: number;
  algorithmVersion: string;
  startedAt: string;
  finishedAt: string;
  candidateCount: number;
  assignedCount: number;
  tableCount: number;
  carriedOverCount: number;
  alternativeOfferedCount: number;
  reallocations: number;
  dissolvedTables: number;
  alreadyExecuted: boolean;
}

export interface TableSummary {
  tableId: number;
  tableNo: number;
  regionLabel: string;
  size: number;
  minAge: number;
  maxAge: number;
  maleCount: number;
  femaleCount: number;
  totalScore: number;
}

export interface FailureDetail {
  applicationId: number;
  userId: number;
  name: string;
  reasonDescription: string;
  relocationCount: number;
  outcome: 'CARRIED_OVER' | 'ALTERNATIVE_OFFERED';
  outcomeDescription: string;
  nextEventDate: string | null;
}

export interface SlotMatchingResult {
  slotId: number;
  eventDate: string;
  matchAt: string;
  slotStatus: SlotStatus;
  run: MatchRunSummary | null;
  appliedPolicy: string | null;
  tables: TableSummary[];
  failures: FailureDetail[];
}

export interface ScoreBreakdown {
  total: number;
  genderBalance: number;
  mbti: number;
  interest: number;
  preference: number;
  novelty: number;
}

export interface SeatDetail {
  applicationId: number;
  userId: number;
  name: string;
  age: number | null;
  gender: string;
  mbti: string | null;
  interests: string[];
  placementDescription: string;
  relocationCount: number;
  preferenceMet: boolean;
}

export interface TableDetail {
  tableId: number;
  slotId: number;
  eventDate: string;
  startTime: string;
  tableNo: number;
  regionLabel: string;
  confirmedAt: string;
  size: number;
  minAge: number;
  maxAge: number;
  maleCount: number;
  femaleCount: number;
  mbtiComposition: string[];
  commonInterests: string[];
  previouslyMetPairs: number;
  score: ScoreBreakdown;
  seats: SeatDetail[];
}

export interface VenueAssignment {
  tableId: number;
  status: 'ASSIGNED' | 'FAILED' | string;
  venue: { name: string; address: string; priceGuide: string; mapUrl: string } | null;
  reservationName: string | null;
  attemptedVenues: number;
  failureReason: string | null;
  decidedAt: string | null;
}

export interface AdminChatRoom {
  created: boolean;
  roomId: number | null;
  title: string | null;
  members: { alias: string; userId: number; applicationId: number; intro: string }[];
  messages: { kind: string; senderAlias: string | null; content: string; visibleAt: string; visible: boolean }[];
}

export interface SlotApplicant {
  applicationId: number;
  userId: number;
  name: string;
  ticketLabel: string;
  regions: string[];
  status: string;
  statusLabel: string;
  paymentStatusLabel: string;
  confirmedHere: boolean;
  submittedAt: string;
}

export interface AdminAttendance {
  attendanceId: number;
  userId: number;
  tableId: number;
  status: string;
  statusLabel: string;
  checkedInAt: string | null;
}

export interface FeedbackAverages {
  tableMood: number;
  conversation: number;
  venue: number;
  rejoinIntent: number;
  recommendScore: number;
}

export interface SlotFeedbackSummary {
  slotId: number;
  responses: number;
  averages: FeedbackAverages;
  nps: number;
  tables: { tableId: number; responses: number; averages: FeedbackAverages }[];
  comments: { tableId: number; name: string; tableMood: number; comment: string }[];
}
