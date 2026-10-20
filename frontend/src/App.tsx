import { Navigate, Route, Routes } from 'react-router-dom';
import { session } from './api/client';
import AppLayout from './app/AppLayout';
import LoginPage from './app/LoginPage';
import HomePage from './app/HomePage';
import ProfilePage from './app/ProfilePage';
import ApplyPage from './app/ApplyPage';
import PayPage from './app/PayPage';
import ApplicationPage from './app/ApplicationPage';
import MatchPage from './app/MatchPage';
import ChatsPage from './app/ChatsPage';
import ChatRoomPage from './app/ChatRoomPage';
import NotificationsPage from './app/NotificationsPage';
import ResolvePage from './app/ResolvePage';
import FeedbackPage from './app/FeedbackPage';
import MePage from './app/MePage';
import AdminLayout from './admin/AdminLayout';
import DashboardPage from './admin/DashboardPage';
import ExceptionsPage from './admin/ExceptionsPage';
import SlotPage from './admin/SlotPage';
import TablePage from './admin/TablePage';
import { NotificationsLogPage, PaymentsPage, ReportsPage, VenuesPage } from './admin/LookupPages';

function RequireParticipant({ children }: { children: JSX.Element }) {
  return session.token() ? children : <Navigate to="/app/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app" replace />} />
      <Route path="/app/login" element={<LoginPage />} />
      <Route
        path="/app"
        element={
          <RequireParticipant>
            <AppLayout />
          </RequireParticipant>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="apply" element={<ApplyPage />} />
        <Route path="applications/:id" element={<ApplicationPage />} />
        <Route path="applications/:id/pay" element={<PayPage />} />
        <Route path="applications/:id/match" element={<MatchPage />} />
        <Route path="applications/:id/resolve" element={<ResolvePage />} />
        <Route path="applications/:id/feedback" element={<FeedbackPage />} />
        <Route path="me" element={<MePage />} />
        <Route path="chats" element={<ChatsPage />} />
        <Route path="chats/:roomId" element={<ChatRoomPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
      </Route>
      <Route path="/admin/login" element={<LoginPage />} />
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="exceptions" element={<ExceptionsPage />} />
        <Route path="slots/:slotId" element={<SlotPage />} />
        <Route path="tables/:tableId" element={<TablePage />} />
        <Route path="venues" element={<VenuesPage />} />
        <Route path="payments" element={<PaymentsPage />} />
        <Route path="notifications" element={<NotificationsLogPage />} />
        <Route path="reports" element={<ReportsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  );
}
