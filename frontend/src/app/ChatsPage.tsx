import { Link } from 'react-router-dom';
import { ChatRoomSummary } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime, day } from '../format';
import { useApi } from '../useApi';

/** 내 단체방 목록 (INF-04). 매칭이 확정되면 테이블마다 하나씩 자동으로 생긴다 */
export default function ChatsPage() {
  const rooms = useApi<ChatRoomSummary[]>('/api/app/chat-rooms');
  if (!rooms.data) return rooms.error ? <ErrorBox message={rooms.error} /> : <Loading />;

  return (
    <div>
      <h1 className="h1">단체방</h1>
      <p className="muted">매칭이 확정되면 같은 식탁 멤버와의 단체방이 자동으로 열려요.</p>
      {rooms.data.length === 0 ? (
        <div className="card center" style={{ marginTop: 16 }}>
          <span style={{ color: 'var(--brand)' }}><Icon name="chat" size={36} /></span>
          <p className="muted">아직 열린 단체방이 없어요</p>
        </div>
      ) : (
        <div className="card" style={{ marginTop: 16, padding: '4px 16px' }}>
          {rooms.data.map((room) => (
            <Link key={room.roomId} to={`/app/chats/${room.roomId}`} className="list-item">
              <div className="avatar" style={{ width: 46, height: 46, background: 'var(--brand)', borderWidth: 0 }}>
                <Icon name="users" />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div className="spread">
                  <b>{room.title}</b>
                  <span className="small muted">{day(room.eventDate)}</span>
                </div>
                <div className="small muted" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {room.lastMessage ?? `${room.memberCount}명`}
                </div>
                {room.lastMessageAt && <div className="small muted">{dateTime(room.lastMessageAt)}</div>}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
