import { FormEvent, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { ChatMember, ChatRoom, REPORT_REASONS, TableContent } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorBox, Field, Loading, SingleChips } from '../components/ui';
import { dateTime, day } from '../format';
import { useApi } from '../useApi';
import { avatarStyle } from './MatchPage';

const POLL_MS = 5000;

const NOTICE_LABEL: Record<string, string> = {
  WELCOME: '단체방 안내',
  PLACE: '시간·장소',
  CURATION: '오늘의 식탁',
  POLICY: '취소·노쇼 정책',
  REMINDER: '내일 만나요',
  CONTENT: '대화 주제',
};

type Tab = 'chat' | 'content' | 'members';

/** 단체방: 시스템 공지(INF-05·07, CON-03) + 대화, 몇 초마다 새로 불러온다. 대화 주제 (CON-01·02), 신고·차단 (SAF-01) */
export default function ChatRoomPage() {
  const { roomId } = useParams();
  const room = useApi<ChatRoom>(`/api/app/chat-rooms/${roomId}`);
  const [tab, setTab] = useState<Tab>('chat');
  const [text, setText] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [reportTarget, setReportTarget] = useState<ChatMember | null>(null);
  const bottom = useRef<HTMLDivElement>(null);
  const count = room.data?.messages.length ?? 0;

  useEffect(() => {
    const timer = window.setInterval(() => {
      api.get<ChatRoom>(`/api/app/chat-rooms/${roomId}`).then(room.setData).catch(() => undefined);
    }, POLL_MS);
    return () => window.clearInterval(timer);
  }, [roomId, room.setData]);

  useEffect(() => {
    if (tab === 'chat') bottom.current?.scrollIntoView({ block: 'end' });
  }, [count, tab]);

  if (!room.data) return room.error ? <ErrorBox message={room.error} /> : <Loading />;
  const r = room.data;

  const send = async (event: FormEvent) => {
    event.preventDefault();
    if (!text.trim()) return;
    try {
      room.setData(await api.post<ChatRoom>(`/api/app/chat-rooms/${roomId}/messages`, { content: text.trim() }));
      setText('');
      setError(null);
    } catch (e) {
      setError(errorText(e));
    }
  };

  return (
    <div>
      <div className="spread" style={{ marginBottom: 12 }}>
        <div>
          <p className="h2" style={{ margin: 0 }}>{r.title}</p>
          <p className="small muted" style={{ margin: 0 }}>{day(r.eventDate)} · {r.members.length}명 · 나는 {r.myAlias}</p>
        </div>
      </div>
      <div className="tabs">
        <button className={tab === 'chat' ? 'on' : ''} onClick={() => setTab('chat')}>대화</button>
        <button className={tab === 'content' ? 'on' : ''} onClick={() => setTab('content')}>대화 주제</button>
        <button className={tab === 'members' ? 'on' : ''} onClick={() => setTab('members')}>멤버</button>
      </div>
      <ErrorBox message={error} />

      {tab === 'chat' && (
        <>
          <div className="chat">
            {r.messages.map((m) =>
              m.system ? (
                <div key={m.messageId} className="msg-system">
                  <div className="label">{NOTICE_LABEL[m.kind] ?? '안내'}</div>
                  {m.content}
                </div>
              ) : (
                <div key={m.messageId} className={`msg ${m.mine ? 'mine' : ''}`}>
                  {!m.mine && <div className="who">{m.senderAlias}</div>}
                  <div className="bubble">{m.content}</div>
                  <div className="at">{dateTime(m.at).slice(-5)}</div>
                </div>
              ),
            )}
            <div ref={bottom} />
          </div>
          <form className="composer" onSubmit={send}>
            <input className="input" value={text} maxLength={500} placeholder="메시지를 입력하세요"
              onChange={(e) => setText(e.target.value)} />
            <button className="icon-btn" disabled={!text.trim()} aria-label="보내기"><Icon name="send" size={20} /></button>
          </form>
        </>
      )}

      {tab === 'content' && <ContentTab roomId={roomId!} />}

      {tab === 'members' && (
        <div className="card" style={{ padding: '4px 16px' }}>
          {r.members.map((m, i) => (
            <div key={m.seatNo} className="list-item">
              <div className="avatar" style={{ ...avatarStyle(i), width: 44, height: 44, borderWidth: 2 }}>{m.alias.slice(0, 1)}</div>
              <div style={{ flex: 1 }}>
                <b>{m.alias}{m.me && ' (나)'}</b>
                <div className="small muted">{m.intro}</div>
              </div>
              {!m.me && (
                <button className="btn btn-ghost btn-sm" onClick={() => setReportTarget(m)} aria-label="신고·차단">
                  <Icon name="flag" size={16} />
                </button>
              )}
            </div>
          ))}
          <p className="small muted">실명·연락처는 공유되지 않아요. 불편한 일이 있으면 신고해 주세요. 상대에게는 알려지지 않아요.</p>
        </div>
      )}

      {reportTarget && (
        <ReportSheet roomId={roomId!} target={reportTarget} onClose={() => setReportTarget(null)} />
      )}
    </div>
  );
}

function ContentTab({ roomId }: { roomId: string }) {
  const content = useApi<TableContent>(`/api/app/chat-rooms/${roomId}/content`);
  if (!content.data) return content.error ? <ErrorBox message={content.error} /> : <Loading />;
  return (
    <div>
      <p className="section-title" style={{ marginTop: 4 }}>우리 식탁 맞춤 대화 주제</p>
      {content.data.topics.map((topic, i) => (
        <div key={topic.question} className="card">
          <p style={{ margin: 0, fontWeight: 700 }}>{i + 1}. {topic.question}</p>
          <p className="small muted" style={{ margin: '4px 0 0' }}>{topic.basis}</p>
        </div>
      ))}
      <p className="section-title">아이스브레이킹</p>
      {content.data.icebreakers.map((game) => (
        <div key={game.name} className="card">
          <p className="h2">{game.name}</p>
          <p style={{ margin: 0, fontSize: 14 }}>{game.howTo}</p>
          <p className="small muted" style={{ margin: '6px 0 0' }}>{game.basis}</p>
        </div>
      ))}
    </div>
  );
}

function ReportSheet({ roomId, target, onClose }: { roomId: string; target: ChatMember; onClose: () => void }) {
  const [mode, setMode] = useState<'report' | 'block'>('report');
  const [reason, setReason] = useState<string | null>(null);
  const [detail, setDetail] = useState('');
  const [block, setBlock] = useState(true);
  const [done, setDone] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    try {
      if (mode === 'report') {
        const receipt = await api.post<{ urgent: boolean; blocked: boolean }>(`/api/app/chat-rooms/${roomId}/reports`,
          { seatNo: target.seatNo, reason, detail, block });
        setDone(receipt.urgent
          ? '긴급 신고로 접수했어요. 운영자가 바로 확인할게요.'
          : `신고를 접수했어요.${receipt.blocked ? ' 다음 매칭부터 같은 식탁에 배정되지 않아요.' : ''}`);
      } else {
        await api.post(`/api/app/chat-rooms/${roomId}/blocks`, { seatNo: target.seatNo });
        setDone('차단했어요. 다음 매칭부터 같은 식탁에 배정되지 않아요.');
      }
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="sheet-backdrop" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <p className="h2"><span className="icon-inline"><Icon name="shield" size={20} /></span>{target.alias}님 신고·차단</p>
        {done ? (
          <>
            <div className="alert ok">{done}</div>
            <button className="btn btn-primary btn-block" onClick={onClose}>닫기</button>
          </>
        ) : (
          <>
            <div className="tabs">
              <button className={mode === 'report' ? 'on' : ''} onClick={() => setMode('report')}>신고</button>
              <button className={mode === 'block' ? 'on' : ''} onClick={() => setMode('block')}>차단만</button>
            </div>
            {mode === 'report' ? (
              <>
                <Field label="사유">
                  <SingleChips options={REPORT_REASONS} value={reason} onChange={setReason} />
                </Field>
                <Field label="내용 (선택)">
                  <textarea className="input" rows={3} maxLength={1000} value={detail} onChange={(e) => setDetail(e.target.value)} />
                </Field>
                <label className="row small" style={{ marginBottom: 14 }}>
                  <input type="checkbox" checked={block} onChange={(e) => setBlock(e.target.checked)} />
                  다음 매칭부터 이 분과 같은 식탁에 배정하지 않기
                </label>
              </>
            ) : (
              <p className="muted">신고 없이 차단만 해요. 상대에게는 알려지지 않고, 다음 매칭부터 같은 식탁에 배정되지 않아요.</p>
            )}
            <ErrorBox message={error} />
            <div className="stack">
              <button className="btn btn-danger btn-block" disabled={busy || (mode === 'report' && !reason)} onClick={submit}>
                {mode === 'report' ? '신고하기' : '차단하기'}
              </button>
              <button className="btn btn-ghost btn-block" onClick={onClose}>취소</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
