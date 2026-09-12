import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import useAuthStore from '../../store/authStore';
import { redirectToDesktopHandoff } from '../../utils/desktopAuth';

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
      // Check if this flow originated from the desktop app (via param, sessionStorage, or cookie)
      const searchParams = new URLSearchParams(window.location.search);
      const cookies = typeof document !== 'undefined' ? document.cookie.split(';').map(c => c.trim()) : [];
      const hasDesktopCookie = cookies.some(c => c.startsWith('crescendo_from_desktop=true'));
      const fromDesktop = searchParams.get('from') === 'desktop' || 
                          sessionStorage.getItem('crescendo_from_desktop') === 'true' ||
                          hasDesktopCookie;

      // Maintain valid web session in zustand as well
      useAuthStore.setState({
        accessToken,
        accessExpiresAt: expiresAt,
      });

      if (fromDesktop) {
        checkAuth().catch(() => {});
        redirectToDesktopHandoff(navigate, accessToken);
        return;
      }

      // Store the access token in zustand for web browser
      useAuthStore.setState({
        accessToken,
        accessExpiresAt: expiresAt,
      });

      // Now fetch user profile using the token, then redirect to web dashboard
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
