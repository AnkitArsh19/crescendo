import { create } from 'zustand';
import api from '../api/axios';
import { getDeviceMetadata } from '../utils/deviceFingerprint';

// Single-flight guard: if checkAuth() is already in-flight, all subsequent callers
// share the same promise instead of firing independent POST /auth/refresh requests.
// This is the fix for the concurrent mount problem (App.jsx + DesktopAuthEntry.jsx both
// calling checkAuth() within milliseconds, causing rotation-reuse detection to trigger).
let _checkAuthPromise = null;

const useAuthStore = create((set, get) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,
  isGuest: false,

  accessToken: null,
  accessExpiresAt: null,
  refreshToken: null,
  refreshExpiresAt: null,

  setTokens: (accessToken, accessExpiresAt, refreshToken, refreshExpiresAt) => {
    if (refreshToken) {
      try {
        localStorage.setItem('crescendo_refresh_token', refreshToken);
      } catch { /* ignore */ }
    }
    set({
      accessToken,
      accessExpiresAt,
      refreshToken: refreshToken || get().refreshToken,
      refreshExpiresAt: refreshExpiresAt || get().refreshExpiresAt,
    });
  },

  // Called on app mount to restore session from the token (if stored or fetched).
  // Proactively refreshes the access token before hitting /users/me to avoid
  // a 401 console error on every page load.
  // Single-flight: concurrent callers share the active promise so only one
  // POST /auth/refresh is sent regardless of how many components mount at once.
  checkAuth: async () => {
    if (_checkAuthPromise) return _checkAuthPromise;

    _checkAuthPromise = (async () => {
      // Guest mode: no session to restore, just stop loading
      if (get().isGuest) {
        set({ isLoading: false });
        return;
      }
      try {
        // If we don't have an access token, try refreshing first (HttpOnly cookie or stored refreshToken).
        // This avoids the 401 console error from hitting /users/me with no token.
        if (!get().accessToken) {
          const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://api.crescendo.run';
          let tokenToUse = get().refreshToken;
          if (!tokenToUse) {
            try {
              tokenToUse = localStorage.getItem('crescendo_refresh_token');
            } catch { /* ignore */ }
          }
          const refreshResp = await fetch(`${API_BASE_URL}/auth/refresh`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(tokenToUse ? { refreshToken: tokenToUse } : {}),
          });
          if (!refreshResp.ok) {
            try {
              localStorage.removeItem('crescendo_refresh_token');
            } catch { /* ignore */ }
            // No valid refresh token — user is not logged in
            set({ user: null, isAuthenticated: false, isLoading: false, accessToken: null, refreshToken: null });
            return;
          }
          const tokens = await refreshResp.json();
          if (tokens.refreshToken) {
            try {
              localStorage.setItem('crescendo_refresh_token', tokens.refreshToken);
            } catch { /* ignore */ }
          }
          // Persist rotated refresh token too (backend rotates on every refresh).
          // Without this DesktopAuthEntry cannot forward a valid refresh_token to the desktop app.
          set({
            accessToken: tokens.accessToken,
            accessExpiresAt: tokens.accessExpiresAt,
            refreshToken: tokens.refreshToken || tokenToUse || get().refreshToken,
            refreshExpiresAt: tokens.refreshExpiresAt || get().refreshExpiresAt,
          });
        }
        // Now we have a token — fetch user profile
        const response = await api.get('/users/me');
        set({ user: response.data, isAuthenticated: true, isLoading: false });
      } catch {
        // Any failure means no valid session
        set({ user: null, isAuthenticated: false, isLoading: false, accessToken: null });
      }
    })();

    try {
      await _checkAuthPromise;
    } finally {
      _checkAuthPromise = null;
    }
  },

  login: async (email, password, rememberMe = false) => {
    try {
      const { deviceId, deviceLabel } = getDeviceMetadata();
      const response = await api.post('/auth/login', { email, password, rememberMe, deviceId, deviceLabel });
      
      if (response.status === 202) {
        // MFA Required
        return { success: true, mfaRequired: true };
      }

      // Complete success - persist tokens to memory and localStorage
      const { accessToken, accessExpiresAt, refreshToken, refreshExpiresAt } = response.data;
      get().setTokens(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);

      try {
        const userResp = await api.get('/users/me');
        set({ 
          user: userResp.data, 
          isAuthenticated: true, 
        });
      } catch {
        const { refreshToken: _rt, refreshExpiresAt: _re, message: _m, accessToken: _at, accessExpiresAt: _ae, ...userData } = response.data;
        set({ 
          user: userData, 
          isAuthenticated: true, 
        });
      }

      return { success: true, mfaRequired: false };
    } catch (error) {
      if (error.response?.status === 401) {
          throw new Error('Invalid email or password', { cause: error });
      }
      throw new Error(error.response?.data?.message || 'Failed to log in', { cause: error });
    }
  },

  verifyMfa: async (email, code) => {
    try {
      const { deviceId, deviceLabel } = getDeviceMetadata();
      // POST /mfa/challenge expects { email, code } to identify the user and verify the TOTP code.
      const response = await api.post('/mfa/challenge', { email, code, deviceId, deviceLabel });
      
      if (response.data.success) {
         const { accessToken, accessExpiresAt, refreshToken, refreshExpiresAt } = response.data;
         get().setTokens(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);
         // Fetch user details immediately after solving challenge
         const userResp = await api.get('/users/me');
         set({ user: userResp.data, isAuthenticated: true });
         return { success: true };
      } else {
         throw new Error('Invalid 2FA code');
      }
    } catch (error) {
      throw new Error(error.response?.data?.message || error.message || 'Failed to verify 2FA', { cause: error });
    }
  },

  useBackupCode: async (email, backupCode) => {
    try {
      const { deviceId, deviceLabel } = getDeviceMetadata();
      const response = await api.post('/mfa/backup-code', { email, backupCode, deviceId, deviceLabel });
      if (response.data.success) {
        const { accessToken, accessExpiresAt, refreshToken, refreshExpiresAt } = response.data;
        get().setTokens(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);
        const userResp = await api.get('/users/me');
        set({ user: userResp.data, isAuthenticated: true });
        return { success: true, remaining: response.data.remaining };
      } else {
        throw new Error('Invalid backup code');
      }
    } catch (error) {
      throw new Error(error.response?.data?.message || error.message || 'Invalid backup code', { cause: error });
    }
  },

  register: async (email, username, password) => {
    try {
      const { deviceId, deviceLabel } = getDeviceMetadata();
      const response = await api.post('/auth/register', { email, username, password, deviceId, deviceLabel });
      
      // Response includes tokens - persist to memory and localStorage
      const { accessToken, accessExpiresAt, refreshToken, refreshExpiresAt } = response.data;
      get().setTokens(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);

      try {
        const userResp = await api.get('/users/me');
        set({ 
          user: userResp.data, 
          isAuthenticated: true, 
        });
      } catch {
        const { refreshToken: _rt, refreshExpiresAt: _re, message: _m, accessToken: _at, accessExpiresAt: _ae, ...userData } = response.data;
        set({ 
          user: userData, 
          isAuthenticated: true, 
        });
      }
      return { success: true };
    } catch (error) {
      if (error.response?.status === 409) {
          throw new Error('Email or username already taken', { cause: error });
      }
      throw new Error(error.response?.data?.message || 'Failed to register', { cause: error });
    }
  },

  logout: async (forceLocalOnly = false) => {
    if (!forceLocalOnly) {
      try {
        await api.post('/auth/logout');
      } catch (error) {
        console.error('Logout error against server', error);
      }
    }
    
    // Clear state
    set({
      user: null,
      isAuthenticated: false,
      isGuest: false,
      accessToken: null,
      accessExpiresAt: null,
      refreshToken: null,
    });
    localStorage.removeItem('crescendo_guest_session');
    try {
      localStorage.removeItem('crescendo_refresh_token');
    } catch { /* ignore */ }
  },

  // ── Guest Mode ──────────────────────────────────────────────────────
  enterGuestMode: () => {
    let sessionId = localStorage.getItem('crescendo_guest_session');
    if (!sessionId) {
      sessionId = crypto.randomUUID();
      localStorage.setItem('crescendo_guest_session', sessionId);
    }
    set({ isGuest: true, isAuthenticated: false, isLoading: false, user: null });
  },

  getGuestSessionId: () => {
    return localStorage.getItem('crescendo_guest_session');
  },

  exitGuestMode: () => {
    localStorage.removeItem('crescendo_guest_session');
    set({ isGuest: false });
  },

  // ── User account helpers ────────────────────────────────────────────
  refreshUser: async () => {
    try {
      const response = await api.get('/users/me');
      set({ user: response.data });
    } catch { /* silent */ }
  },

  deleteAccount: async () => {
    await api.delete('/users/me');
    set({
      user: null,
      isAuthenticated: false,
      accessToken: null,
      accessExpiresAt: null,
      refreshToken: null,
    });
    try {
      localStorage.removeItem('crescendo_refresh_token');
    } catch { /* ignore */ }
  },
}));

export default useAuthStore;
