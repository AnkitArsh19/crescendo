import { isTauri } from './platform';
import api from '../api/axios';
import useAuthStore from '../store/authStore';
import { getDeviceMetadata } from './deviceFingerprint';
import { appCatalogApi } from '../api/appCatalogApi';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://api.crescendo.run';

/**
 * Returns the base URL for the client's external browser.
 * Uses local dev server (http://localhost:5173) in dev or app.crescendo.run in production.
 */
export function getBrowserBaseUrl() {
  if (import.meta.env.VITE_BROWSER_URL) {
    return import.meta.env.VITE_BROWSER_URL.replace(/\/+$/, '');
  }
  return 'https://app.crescendo.run';
}

/**
 * Opens a URL in the native external browser (Chrome/Safari/Edge/Brave)
 * when running inside Tauri, or falls back to window.open in standard web mode.
 */
export async function openExternalBrowser(url) {
  if (isTauri()) {
    try {
      const { openUrl } = await import('@tauri-apps/plugin-opener');
      await openUrl(url);
      return;
    } catch (err) {
      console.warn('Tauri opener plugin failed, falling back to window.open:', err);
    }
  }
  window.open(url, '_blank');
}

/**
 * Initiates browser-based login or signup from the desktop app.
 * Opens the user's default browser with ?from=desktop parameter.
 */
export async function startBrowserLogin(mode = 'login') {
  const baseUrl = getBrowserBaseUrl();
  const url = `${baseUrl}/desktop-auth?mode=${mode}&streamlined=true`;
  await openExternalBrowser(url);
}

/**
 * Initiates browser-based OAuth authentication for desktop.
 * Opens the system browser pointing to the OAuth provider with the
 * crescendo:// deep-link redirect target.
 */
export async function startDesktopOAuth(provider = 'google') {
  const redirectUri = isTauri()
    ? encodeURIComponent('crescendo://auth/callback')
    : encodeURIComponent(`${window.location.origin}/oauth/callback`);

  const authUrl = `${API_BASE_URL}/oauth2/authorize/${provider}?redirect_uri=${redirectUri}`;
  await openExternalBrowser(authUrl);
}

/**
 * Initiates third-party service connection (Slack, Notion, Google Drive, etc.)
 * in the system browser with crescendo:// connection callback.
 */
export async function startDesktopConnection(appKey, opts = {}) {
  try {
    const { authorizationUrl } = await appCatalogApi.getOAuthUrl(appKey, opts);
    if (authorizationUrl) {
      await openExternalBrowser(authorizationUrl);
      return authorizationUrl;
    }
  } catch (err) {
    console.error(`Failed to initiate desktop connection for ${appKey}:`, err);
    throw err;
  }
}

/**
 * Brings the native desktop application window to the foreground and focuses it.
 */
export async function focusDesktopWindow() {
  if (!isTauri()) return;
  try {
    const { getCurrentWindow } = await import('@tauri-apps/api/window');
    const win = getCurrentWindow();
    await win.unminimize().catch(() => {});
    await win.setFocus().catch(() => {});
  } catch (err) {
    console.warn('Failed to focus Tauri window:', err);
  }
}

/**
 * Issues a single-use desktop handoff code via POST /auth/desktop-handoff/issue
 * and navigates to /open-app?code=...
 * 
 * Never puts raw access_token or refresh_token in URLs (RFC 8252 pattern).
 */
export async function redirectToDesktopHandoff(navigate, explicitAccessToken = null) {
  try {
    const token = explicitAccessToken || useAuthStore.getState().accessToken;
    const { deviceId, deviceLabel } = getDeviceMetadata();
    const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
    const resp = await api.post('/auth/desktop-handoff/issue', { deviceId, deviceLabel }, config);
    const { code } = resp.data;

    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem('crescendo_from_desktop');
    }
    if (typeof document !== 'undefined') {
      document.cookie = 'crescendo_from_desktop=; path=/; max-age=0';
    }
    navigate(`/open-app?code=${encodeURIComponent(code)}`, { replace: true });
  } catch (err) {
    console.error('Failed to issue desktop handoff code:', err);
    navigate('/open-app', { replace: true });
  }
}

/**
 * Returns the public browser URL used to initiate desktop login or signup.
 * Safe to copy/share because it contains zero tokens, secrets, or credentials.
 */
export function getBrowserLoginUrl(mode = 'login') {
  const baseUrl = getBrowserBaseUrl();
  return `${baseUrl}/desktop-auth?mode=${mode}&streamlined=true`;
}

/**
 * Exchanges a single-use desktop handoff code for access and refresh tokens.
 * Handles both plain codes and URLs pasted by the user.
 */
export async function exchangeDesktopHandoffCode(rawCode) {
  if (!rawCode) throw new Error('Sign-in code is required');
  let cleanCode = rawCode.trim();

  // Extract code parameter if a full URL was pasted
  if (cleanCode.includes('code=')) {
    try {
      const url = new URL(cleanCode.replace('crescendo://', 'https://crescendo.desktop/'));
      cleanCode = url.searchParams.get('code') || cleanCode;
    } catch {
      const match = cleanCode.match(/code=([^&]+)/);
      if (match) cleanCode = match[1];
    }
  }

  const { deviceId, deviceLabel } = getDeviceMetadata();
  const resp = await fetch(`${API_BASE_URL}/auth/desktop-handoff/exchange`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code: cleanCode, deviceId, deviceLabel }),
  });

  if (!resp.ok) {
    const errData = await resp.json().catch(() => ({}));
    throw new Error(errData.message || `Code exchange failed with status ${resp.status}`);
  }

  const tokens = await resp.json();
  useAuthStore.getState().setTokens(
    tokens.accessToken,
    tokens.accessExpiresAt,
    tokens.refreshToken,
    tokens.refreshExpiresAt
  );
  await useAuthStore.getState().checkAuth().catch(() => {});
  await focusDesktopWindow();
  window.dispatchEvent(new CustomEvent('crescendo-desktop-auth-complete'));
  return tokens;
}

