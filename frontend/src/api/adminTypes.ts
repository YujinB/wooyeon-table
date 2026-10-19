// 운영자 API 응답 타입 (필요한 필드만)

export type SlotStatus = 'RECRUITING' | 'MATCHED' | 'COMPLETED';

export const SLOT_STATUS_LABEL: Record<SlotStatus, string> = {
  RECRUITING: '모집 중',
  MATCHED: '매칭 완료',
  COMPLETED: '행사 종료',
};

/** 마감 시각이 지났는데 아직 매칭을 돌리지 않은 회차는 "모집 중"이 아니라 마감 지남으로 보여 준다 */
export function slotStatusView(status: SlotStatus, matchAt: string): { label: string; tone: string } {
  if (status === 'RECRUITING' && new Date(matchAt).getTime() <= Date.now()) {
    return { label: '마감 지남 · 매칭 전', tone: 'warn' };
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
