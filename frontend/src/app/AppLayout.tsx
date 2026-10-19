import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Icon } from '../components/Icon';
import { LogoMark } from '../components/Logo';

const TABS = [
  { to: '/app', label: '홈', icon: 'home', end: true },
  { to: '/app/apply', label: '신청', icon: 'dining', end: false },
  { to: '/app/chats', label: '단체방', icon: 'chat', end: false },
  { to: '/app/me', label: '내 정보', icon: 'user', end: false },
];

/** 모바일 폭 참가자 앱 틀: 상단 바 + 하단 탭 */
export default function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const isRoot = location.pathname === '/app';
  return (
    <div className="app-shell">
      <header className="app-header">
        {isRoot ? (
          <LogoMark size={26} />
        ) : (
          <button className="back" onClick={() => navigate(-1)} aria-label="뒤로">
            <Icon name="back" />
          </button>
        )}
        <div className="title">우연한 식탁</div>
        <NavLink to="/app/notifications" aria-label="알림" style={{ color: 'var(--brand-strong)', display: 'flex' }}>
          <Icon name="bell" />
        </NavLink>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <nav className="tabbar">
        {TABS.map((tab) => (
          <NavLink key={tab.to} to={tab.to} end={tab.end} className={({ isActive }) => (isActive ? 'active' : '')}>
            <span className="icon"><Icon name={tab.icon} /></span>
            {tab.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
