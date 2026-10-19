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

// ---------- 매칭 이후 ----------

export interface MyTable {
  tableId: number;
  eventDate: string;
  startTime: string;
  region: string;
  size: number;
  minAge: number;
  maxAge: number;
  maleCount: number;
  femaleCount: number;
  score: number;
  commonInterests: string[];
  reasons: string[];
}

export interface MyFailure {
  eventDate: string;
  reason: string;
  outcome: 'CARRIED_OVER' | 'ALTERNATIVE_OFFERED';
  outcomeDescription: string;
  nextEventDate: string | null;
}

export interface MyMatchResult {
  applicationId: number;
  matchStatus: string;
  message: string;
  table: MyTable | null;
  failure: MyFailure | null;
}

export interface MyVenue {
  assigned: boolean;
  message: string;
  name: string | null;
  address: string | null;
  mapUrl: string | null;
  gatherAt: string | null;
  reservationName: string | null;
  priceGuide: string | null;
}

export type AttendanceStatus =
  | 'SCHEDULED' | 'ATTENDED' | 'CANCELED_EARLY' | 'CANCELED_LATE' | 'NO_SHOW_CANDIDATE' | 'NO_SHOW';

export interface AttendanceView {
  attendanceId: number;
  applicationId: number;
  tableId: number;
  status: AttendanceStatus;
  statusLabel: string;
  checkedInAt: string | null;
  canceledAt: string | null;
  reviewedAt: string | null;
}

export interface ChatRoomSummary {
  roomId: number;
  title: string;
  eventDate: string;
  memberCount: number;
  lastMessage: string | null;
  lastMessageAt: string | null;
}

export interface ChatMember { seatNo: number; alias: string; intro: string; me: boolean }

export interface ChatMessage {
  messageId: number;
  kind: 'WELCOME' | 'PLACE' | 'CURATION' | 'POLICY' | 'REMINDER' | 'CONTENT' | 'CHAT';
  system: boolean;
  senderAlias: string | null;
  content: string;
  at: string;
  mine: boolean;
}

export interface ChatRoom {
  roomId: number;
  title: string;
  eventDate: string;
  myAlias: string;
  members: ChatMember[];
  messages: ChatMessage[];
}

export interface TableContent {
  topics: { question: string; basis: string }[];
  icebreakers: { name: string; howTo: string; basis: string }[];
}

export interface NotificationItem {
  notificationId: number;
  type: string;
  title: string;
  body: string;
  applicationId: number | null;
  at: string;
  read: boolean;
}

export interface MyNotifications { unreadCount: number; items: NotificationItem[] }

export type ResolutionChoice = 'MOVE_SCHEDULE' | 'KEEP_TICKET' | 'REFUND';

export interface Alternatives {
  applicationId: number;
  failure: MyFailure | null;
  slots: {
    slotId: number;
    eventDate: string;
    eventStartsAt: string;
    applyDeadline: string;
    recommendedRegions: { code: string; label: string; waitingCount: number; mine: boolean }[];
  }[];
  choices: { choice: ResolutionChoice; label: string; description: string; available: boolean; unavailableReason: string | null }[];
}

export interface ResolutionResult {
  applicationId: number;
  choice: ResolutionChoice;
  movedToEventDate: string | null;
  refund: PaymentResult | null;
}

export interface FeedbackAnswers {
  tableMood: number;
  conversation: number;
  venue: number;
  rejoinIntent: number;
  recommendScore: number;
  comment: string | null;
  wantAgainSeats: number[];
  avoidSeats: number[];
}

export interface FeedbackForm {
  applicationId: number;
  submittable: boolean;
  unavailableReason: string | null;
  submitted: boolean;
  others: { seatNo: number; alias: string; intro: string }[];
  mine: FeedbackAnswers | null;
}

export interface MyHistory {
  history: {
    applicationId: number;
    eventDate: string;
    region: string;
    tableSize: number;
    attendance: AttendanceStatus;
    attendanceLabel: string;
    feedbackSubmitted: boolean;
  }[];
  recommendations: { slotId: number; eventDate: string; applyDeadline: string; region: string; reason: string }[];
}

export const REPORT_REASONS = [
  { code: 'HARASSMENT', label: '불쾌한 신체 접촉·성희롱' },
  { code: 'OFFENSIVE_LANGUAGE', label: '욕설·비하 발언' },
  { code: 'UNWANTED_CONTACT', label: '원치 않는 연락·만남 강요' },
  { code: 'SOLICITATION', label: '영업·종교·다단계 권유' },
  { code: 'OTHER', label: '기타' },
];
