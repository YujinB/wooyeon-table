import { useEffect, useState } from 'react';
import { NavLink, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api, session } from '../api/client';
import { Dashboard } from '../api/adminTypes';
import { Icon } from '../components/Icon';
import { LogoMark } from '../components/Logo';

export const EXCEPTIONS_CHANGED = 'admin:exceptions-changed';

/** 운영자 웹 틀: 왼쪽 메뉴 + 본문. 운영자 계정만 */
export default function AdminLayout() {
  const user = session.user();
  const navigate = useNavigate();
  const location = useLocation();
  const [openExceptions, setOpenExceptions] = useState(0);

  // 화면을 옮기거나 예외를 처리할 때마다 미해결 예외 수를 다시 센다
  useEffect(() => {
    if (user?.role !== 'ADMIN') return;
    const refresh = () => api.get<Dashboard>('/api/admin/dashboard')
      .then((d) => setOpenExceptions(d.exceptions.total)).catch(() => undefined);
    refresh();
    window.addEventListener(EXCEPTIONS_CHANGED, refresh);
    return () => window.removeEventListener(EXCEPTIONS_CHANGED, refresh);
  }, [location.pathname, user?.role]);

  if (!session.token() || user?.role !== 'ADMIN') return <Navigate to="/admin/login" replace />;

  const logout = () => {
    session.clear();
    navigate('/admin/login', { replace: true });
  };

  return (
    <div className="admin">
      <aside className="admin-nav">
        <div className="admin-brand"><LogoMark size={28} color="#fbe9d7" /> <span>우연한 식탁 운영</span></div>
        <NavLink to="/admin" end><Icon name="home" size={20} />대시보드</NavLink>
        <NavLink to="/admin/exceptions">
          <Icon name="alert" size={20} />예외 처리함
          {openExceptions > 0 && <span className="count">{openExceptions}</span>}
        </NavLink>
        <button className="admin-logout" onClick={logout}><Icon name="logout" size={20} />로그아웃</button>
      </aside>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
