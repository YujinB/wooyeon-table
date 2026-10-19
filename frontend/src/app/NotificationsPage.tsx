import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { MyNotifications, NotificationItem } from '../api/types';
import { Icon } from '../components/Icon';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime } from '../format';
import { useApi } from '../useApi';

/** 알림을 누르면 관련 화면으로 */
function target(item: NotificationItem): string | null {
  if (!item.applicationId) return null;
  const base = `/app/applications/${item.applicationId}`;
  switch (item.type) {
    case 'MATCH_CONFIRMED':
    case 'EVENT_REMINDER':
      return `${base}/match`;
    case 'MATCH_FAILED':
      return `${base}/resolve`;
    case 'FEEDBACK_REQUESTED':
      return `${base}/feedback`;
    default:
      return base;
  }
}

/** 앱 내 알림함 (푸시 대신, INF-01·02) */
export default function NotificationsPage() {
  const navigate = useNavigate();
  const notifications = useApi<MyNotifications>('/api/app/notifications');
  if (!notifications.data) return notifications.error ? <ErrorBox message={notifications.error} /> : <Loading />;
  const { items, unreadCount } = notifications.data;

  const open = async (item: NotificationItem) => {
    if (!item.read) await api.post(`/api/app/notifications/${item.notificationId}/read`).catch(() => undefined);
    const to = target(item);
    if (to) navigate(to);
    else notifications.reload();
  };

  const readAll = async () => {
    await api.post('/api/app/notifications/read-all');
    notifications.reload();
  };

  return (
    <div>
      <div className="spread">
        <h1 className="h1">알림</h1>
        {unreadCount > 0 && <button className="btn btn-ghost btn-sm" onClick={readAll}>모두 읽음</button>}
      </div>
      {items.length === 0 ? (
        <div className="card center" style={{ marginTop: 16 }}>
          <span style={{ color: 'var(--brand)' }}><Icon name="bell" size={36} /></span>
          <p className="muted">아직 받은 알림이 없어요</p>
        </div>
      ) : (
        items.map((item) => (
          <div key={item.notificationId} className={`noti ${item.read ? 'read' : ''}`} onClick={() => open(item)}>
            <span className="dot" />
            <div style={{ flex: 1 }}>
              <b>{item.title}</b>
              <div className="body">{item.body}</div>
              <div className="small muted">{dateTime(item.at)}</div>
            </div>
          </div>
        ))
      )}
    </div>
  );
}
