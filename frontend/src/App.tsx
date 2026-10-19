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
        <Route path="chats" element={<ChatsPage />} />
        <Route path="chats/:roomId" element={<ChatRoomPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  );
}
