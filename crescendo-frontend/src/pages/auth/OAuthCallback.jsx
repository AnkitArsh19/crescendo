import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import useAuthStore from '../../store/authStore';

/**
 * Handles the redirect from the backend after a successful OAuth login.
 * The backend redirects to: /oauth/callback#access_token=...&expires_at=...
 * This page reads the token from the URL hash, stores it, fetches the user profile,
 * and redirects to the dashboard.
 */
export default function OAuthCallback() {
  const navigate = useNavigate();
  const checkAuth = useAuthStore((state) => state.checkAuth);

  useEffect(() => {
    const hash = window.location.hash.substring(1); // remove leading #
    const params = new URLSearchParams(hash);
    const accessToken = params.get('access_token');
    const expiresAt = params.get('expires_at');

    if (accessToken) {
      // Store the access token in zustand
      useAuthStore.setState({
        accessToken,
        accessExpiresAt: expiresAt,
      });

      // Now fetch user profile using the token, then redirect
      checkAuth().then(() => {
        navigate('/dashboard', { replace: true, state: { justLoggedIn: true } });
      });
    } else {
      // No token — something went wrong, go back to login
      navigate('/login?error=oauth_failed', { replace: true });
    }
  }, [navigate, checkAuth]);

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh',
      color: 'var(--text-secondary)',
      fontSize: '1.1rem'
    }}>
      Completing sign-in...
    </div>
  );
}
