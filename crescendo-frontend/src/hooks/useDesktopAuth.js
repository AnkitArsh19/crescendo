import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { isTauri } from '../utils/platform';
import { focusDesktopWindow, exchangeDesktopHandoffCode } from '../utils/desktopAuth';
import { getDeviceMetadata } from '../utils/deviceFingerprint';
import useAuthStore from '../store/authStore';
import useConnectionStore from '../store/connectionStore';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://api.crescendo.run';

// Re-export utility functions for convenience
export * from '../utils/desktopAuth';

// In-memory set + sessionStorage to prevent duplicate exchanges of single-use handoff codes
const processedHandoffCodes = new Set();
try {
  const cached = sessionStorage.getItem('crescendo_processed_handoff_codes');
  if (cached) {
    JSON.parse(cached).forEach((c) => processedHandoffCodes.add(c));
  }
} catch {
  // Ignore storage errors in restricted environments
}

function markCodeHandled(code) {
  if (!code) return;
  processedHandoffCodes.add(code);
  try {
    sessionStorage.setItem(
      'crescendo_processed_handoff_codes',
      JSON.stringify(Array.from(processedHandoffCodes).slice(-50))
    );
  } catch {}
}

// In-memory set + sessionStorage to prevent duplicate processing of connection callback URLs
const processedConnectionUrls = new Set();
try {
  const cached = sessionStorage.getItem('crescendo_processed_connection_urls');
  if (cached) {
    JSON.parse(cached).forEach((u) => processedConnectionUrls.add(u));
  }
} catch {}

function markConnectionUrlHandled(url) {
  if (!url) return;
  processedConnectionUrls.add(url);
  try {
    sessionStorage.setItem(
      'crescendo_processed_connection_urls',
      JSON.stringify(Array.from(processedConnectionUrls).slice(-50))
    );
  } catch {}
}

let initialColdStartChecked = false;

/**
 * Hook to listen for OS deep-link events (crescendo://...)
 * Handles incoming auth tokens and app connection responses.
 */
export default function useDesktopAuth() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isTauri()) return;

    let unlistenDeepLink = null;
    let unlistenEvent = null;

    const handleDeepLink = async (urlString, isColdStart = false) => {
      if (!urlString) return;

      // 1. Auth deep link: crescendo://auth/callback?code=...
      if (urlString.startsWith('crescendo://auth/callback')) {
        const url = new URL(urlString.replace('crescendo://', 'https://crescendo.desktop/'));
        const code = url.searchParams.get('code');
        const accessToken = url.searchParams.get('access_token') || url.searchParams.get('token');
        const refreshToken = url.searchParams.get('refresh_token');
        const expiresAt = url.searchParams.get('expires_at');

        if (code) {
          // If already processed this code, avoid duplicate exchange
          if (processedHandoffCodes.has(code)) {
            return;
          }

          // If cold-start check and user is already authenticated, ignore stale launch-arg code
          if (isColdStart && useAuthStore.getState().isAuthenticated) {
            markCodeHandled(code);
            return;
          }

          // Mark handled immediately to prevent concurrent duplicate calls (e.g. React StrictMode)
          markCodeHandled(code);

          try {
            await exchangeDesktopHandoffCode(code);
            navigate('/dashboard', { replace: true, state: { justLoggedIn: true } });
            return;
          } catch (err) {
            if (useAuthStore.getState().isAuthenticated) {
              console.debug('Stale desktop handoff code ignored:', err?.message || err);
            } else {
              console.error('Failed to exchange desktop handoff code:', err);
            }
          }
        } else if (accessToken) {
          // Legacy token handoff fallback
          useAuthStore.getState().setTokens(accessToken, expiresAt, refreshToken, null);
          await useAuthStore.getState().checkAuth();
          await focusDesktopWindow();
          window.dispatchEvent(new CustomEvent('crescendo-desktop-auth-complete'));
          navigate('/dashboard', { replace: true, state: { justLoggedIn: true } });
        }
      } 
      // 2. Connection deep link: crescendo://connections/callback?data=...
      else if (urlString.startsWith('crescendo://connections/callback')) {
        if (processedConnectionUrls.has(urlString)) {
          return;
        }
        markConnectionUrlHandled(urlString);

        let connectionData = null;
        try {
          const url = new URL(urlString.replace('crescendo://', 'https://crescendo.desktop/'));
          const rawData = url.searchParams.get('data');
          if (rawData) {
            const json = atob(rawData.replace(/-/g, '+').replace(/_/g, '/'));
            connectionData = JSON.parse(json);
          }
        } catch (e) {
          console.warn('Failed to parse desktop connection callback data:', e);
        }

        await focusDesktopWindow();

        // Refresh connection store so connections are immediately populated
        try {
          await useConnectionStore.getState().fetchConnections();
        } catch (err) {
          console.warn('Failed to refresh connections after desktop OAuth:', err);
        }

        window.dispatchEvent(new CustomEvent('crescendo-connection-updated', {
          detail: {
            url: urlString,
            data: connectionData,
            connectionId: connectionData?.connectionId,
            appKey: connectionData?.appKey,
            connectionName: connectionData?.connectionName,
            reconnect: connectionData?.reconnect,
          }
        }));

        // Only navigate to connections page if not currently editing a workflow on canvas
        // and only on active live connection arrival (not replayed cold-start)
        const currentPath = window.location.pathname;
        if (!isColdStart && !currentPath.includes('/dashboard/workflows') && currentPath !== '/dashboard/connections') {
          navigate('/dashboard/connections');
        }
      }
    };

    // Listen via @tauri-apps/plugin-deep-link
    import('@tauri-apps/plugin-deep-link')
      .then(async ({ onOpenUrl, register, isRegistered, getCurrent }) => {
        try {
          if (isRegistered && register) {
            const registered = await isRegistered('crescendo');
            if (!registered) {
              await register('crescendo');
            }
          }
        } catch (e) {
          console.warn('Deep link registration check:', e);
        }

        // Check if started via deep-link (cold start) — check only once per app instance
        if (!initialColdStartChecked) {
          initialColdStartChecked = true;
          try {
            if (getCurrent) {
              const initialUrls = await getCurrent();
              if (initialUrls && Array.isArray(initialUrls)) {
                for (const u of initialUrls) {
                  handleDeepLink(u, true);
                }
              }
            }
          } catch (e) {
            console.warn('Deep link initial check:', e);
          }
        }

        return onOpenUrl((urls) => {
          for (const rawUrl of urls) {
            try {
              handleDeepLink(rawUrl, false);
            } catch (err) {
              console.error('Error handling deep link URL:', rawUrl, err);
            }
          }
        });
      })
      .then((fn) => {
        unlistenDeepLink = fn;
      })
      .catch((err) => {
        console.warn('Failed to register deep-link listener:', err);
      });

    // Dual listener: listen to 'deep-link://new-url' directly from single-instance forwarder
    import('@tauri-apps/api/event')
      .then(async ({ listen }) => {
        return listen('deep-link://new-url', (event) => {
          const payload = event.payload;
          if (Array.isArray(payload)) {
            payload.forEach((u) => handleDeepLink(u, false));
          } else if (typeof payload === 'string') {
            handleDeepLink(payload, false);
          }
        });
      })
      .then((fn) => {
        unlistenEvent = fn;
      })
      .catch((err) => {
        console.warn('Failed to register direct event listener:', err);
      });

    return () => {
      if (unlistenDeepLink) unlistenDeepLink();
      if (unlistenEvent) unlistenEvent();
    };
  }, [navigate]);
}

