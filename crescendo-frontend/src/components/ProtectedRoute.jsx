import { Navigate, Outlet } from 'react-router-dom';
import useAuthStore from '../store/authStore';

export default function ProtectedRoute() {
  const { isAuthenticated, isLoading, isGuest } = useAuthStore();

  if (isLoading) {
    return (
      <div style={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-primary)', color: 'var(--text-secondary)' }}>
        Loading session...
      </div>
    );
  }

  // Allow guests or authenticated users through
  if (!isAuthenticated && !isGuest) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

