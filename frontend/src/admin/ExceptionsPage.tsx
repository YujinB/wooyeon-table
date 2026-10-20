import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorText } from '../api/client';
import { ExceptionItem, ExceptionType } from '../api/adminTypes';
import { Icon } from '../components/Icon';
import { ErrorBox, Loading } from '../components/ui';
import { dateTime } from '../format';
import { useApi } from '../useApi';
import { EXCEPTIONS_CHANGED } from './AdminLayout';

const TYPE_ICON: Record<ExceptionType, string> = {
  REPORT: 'shield',
  VENUE: 'dining',
  REFUND: 'refund',
  NOTIFICATION: 'bell',
  NO_SHOW: 'clock',
};

interface Action { label: string; path: string; body?: unknown; tone?: 'danger' | 'ghost' }

/** 예외마다 운영자가 누를 수 있는 처리 */
function actionsOf(item: ExceptionItem): Action[] {
  switch (item.type) {
    case 'REPORT':
      return [
        { label: '경고', path: `/api/admin/reports/${item.refId}/action`, body: { action: 'WARNING' } },
        { label: '30일 제한', path: `/api/admin/reports/${item.refId}/action`, body: { action: 'RESTRICTED', days: 30 }, tone: 'danger' },
        { label: '영구 제한', path: `/api/admin/reports/${item.refId}/action`, body: { action: 'BANNED' }, tone: 'danger' },
        { label: '조치 없이 종결', path: `/api/admin/reports/${item.refId}/action`, body: {}, tone: 'ghost' },
      ];
    case 'VENUE':
      return [{ label: '다시 배정', path: `/api/admin/tables/${item.refId}/venue/retry` }];
    case 'REFUND':
      return [{ label: '다시 환불', path: `/api/admin/applications/${item.refId}/refund/retry` }];
    case 'NOTIFICATION':
      return [{ label: '다시 보내기', path: `/api/admin/notifications/${item.refId}/resend` }];
    case 'NO_SHOW':
      return [
        { label: '노쇼 확정', path: `/api/admin/attendances/${item.refId}/review?noShow=true`, tone: 'danger' },
        { label: '참석으로 정정', path: `/api/admin/attendances/${item.refId}/review?noShow=false`, tone: 'ghost' },
      ];
  }
}

/** 예외 처리함: 자동으로 해결되지 않은 건만 모아 바로 처리 (MAT-13, SAF-02, INF-02·08, PAY-03, FDB-01) */
export default function ExceptionsPage() {
  const exceptions = useApi<ExceptionItem[]>('/api/admin/exceptions');
  const [filter, setFilter] = useState<ExceptionType | 'ALL'>('ALL');
  const [busy, setBusy] = useState<string | null>(null);
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);

  if (!exceptions.data) return exceptions.error ? <ErrorBox message={exceptions.error} /> : <Loading />;
  const items = exceptions.data;
  const shown = filter === 'ALL' ? items : items.filter((i) => i.type === filter);
  const types = Array.from(new Set(items.map((i) => i.type)));

  const run = async (item: ExceptionItem, action: Action) => {
    const key = `${item.type}-${item.refId}`;
    setBusy(key);
    try {
      await api.post(action.path, action.body ?? {});
      setMessage({ ok: true, text: `${item.title} — ${action.label} 처리했어요` });
      await exceptions.reload();
      window.dispatchEvent(new Event(EXCEPTIONS_CHANGED));
    } catch (e) {
      setMessage({ ok: false, text: errorText(e) });
    } finally {
      setBusy(null);
    }
  };

  return (
    <div>
      <div className="admin-head">
        <div>
          <h1 className="h1">예외 처리함</h1>
          <p className="muted" style={{ margin: 0 }}>자동 처리에 실패했거나 운영자 판단이 필요한 건만 올라와요. 긴급 신고가 먼저 보여요.</p>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={exceptions.reload}>새로고침</button>
      </div>

      {message && <div className={`alert ${message.ok ? 'ok' : 'error'}`}>{message.text}</div>}

      <div className="filter">
        <button className={`chip ${filter === 'ALL' ? 'on' : ''}`} onClick={() => setFilter('ALL')}>전체 {items.length}</button>
        {types.map((type) => (
          <button key={type} className={`chip ${filter === type ? 'on' : ''}`} onClick={() => setFilter(type)}>
            {items.find((i) => i.type === type)!.typeLabel} {items.filter((i) => i.type === type).length}
          </button>
        ))}
      </div>

      {shown.length === 0 ? (
        <div className="card center">
          <span style={{ color: 'var(--ok)' }}><Icon name="check" size={36} /></span>
          <p className="muted">처리할 예외가 없어요. 모든 흐름이 자동으로 진행되고 있어요.</p>
        </div>
      ) : (
        shown.map((item) => (
          <div key={`${item.type}-${item.refId}`} className={`exception ${item.urgent ? 'urgent' : ''}`}>
            <span style={{ color: item.urgent ? 'var(--danger)' : 'var(--brand)', marginTop: 2 }}>
              <Icon name={TYPE_ICON[item.type]} />
            </span>
            <div className="body">
              <div className="row" style={{ flexWrap: 'wrap' }}>
                <span className={`pill ${item.urgent ? 'danger' : 'gray'}`}>{item.urgent ? '긴급 ' : ''}{item.typeLabel}</span>
                <b>{item.title}</b>
              </div>
              <div className="small" style={{ marginTop: 4 }}>{item.detail}</div>
              <div className="small muted" style={{ marginTop: 2 }}>
                {item.at && dateTime(item.at)}
                {item.tableId && <> · <Link to={`/admin/tables/${item.tableId}`} style={{ color: 'var(--brand)' }}>테이블 보기</Link></>}
              </div>
            </div>
            <div className="actions">
              {actionsOf(item).map((action) => (
                <button key={action.label} disabled={busy === `${item.type}-${item.refId}`}
                  className={`btn btn-sm ${action.tone === 'danger' ? 'btn-danger' : action.tone === 'ghost' ? 'btn-ghost' : 'btn-primary'}`}
                  onClick={() => run(item, action)}>
                  {action.label}
                </button>
              ))}
            </div>
          </div>
        ))
      )}
    </div>
  );
}
