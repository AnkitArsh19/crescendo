import { useEffect, useState } from 'react';
import { HiOutlineExclamationCircle } from 'react-icons/hi';
import { oauthAuthorizationApi } from '../../api/developerApi';
import useAuthStore from '../../store/authStore';
import './Auth.css';

export default function OAuthAuthorizePage() {
  const isGuest = useAuthStore((state) => state.isGuest);
  const [error, setError] = useState('');

  const continueAuthorization = async () => {
    setError('');
    if (isGuest) {
      setError('Sign in with a Crescendo account to authorize an application.');
      return;
    }
    try {
      const result = await oauthAuthorizationApi.createSession();
      window.location.replace(result.authorizationUrl);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The authorization request expired. Start again from the application.');
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    continueAuthorization();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <main className="oauth-bridge">
      <img className="oauth-bridge-logo" src="/logo.png" alt="Crescendo" />
      {error ? (
        <>
          <HiOutlineExclamationCircle className="oauth-bridge-icon" />
          <h1>Authorization paused</h1>
          <p>{error}</p>
          <button type="button" className="auth-btn" onClick={continueAuthorization}>Try again</button>
        </>
      ) : (
        <>
          <span className="oauth-bridge-spinner" aria-label="Loading" />
          <h1>Opening authorization</h1>
        </>
      )}
    </main>
  );
}
