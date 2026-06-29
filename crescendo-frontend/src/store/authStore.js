import { create } from 'zustand';
import api from '../api/axios';
import { getDeviceMetadata } from '../utils/deviceFingerprint';

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
    set({
      accessToken,
      accessExpiresAt,
      refreshToken: refreshExpiresAt ? refreshToken : undefined, 
    });
  },

  // Called on app mount to restore session from the token (if stored or fetched).
  // Proactively refreshes the access token before hitting /users/me to avoid
  // a 401 console error on every page load.
  checkAuth: async () => {
    // Guest mode: no session to restore, just stop loading
    if (get().isGuest) {
      set({ isLoading: false });
      return;
    }
    try {
      // If we don't have an access token, try refreshing first (HttpOnly cookie).
      // This avoids the 401 console error from hitting /users/me with no token.
      if (!get().accessToken) {
        const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://api.crescendo.run';
        const refreshResp = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
          body: '{}',
        });
        if (!refreshResp.ok) {
          // No valid refresh token — user is not logged in
          set({ user: null, isAuthenticated: false, isLoading: false, accessToken: null });
          return;
        }
        const tokens = await refreshResp.json();
        set({ accessToken: tokens.accessToken, accessExpiresAt: tokens.accessExpiresAt });
      }
      // Now we have a token — fetch user profile
      const response = await api.get('/users/me');
      set({ user: response.data, isAuthenticated: true, isLoading: false });
    } catch {
      // Any failure means no valid session
      set({ user: null, isAuthenticated: false, isLoading: false, accessToken: null });
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

      // Complete success
      const { accessToken, accessExpiresAt } = response.data;
      const { refreshToken: _rt, refreshExpiresAt: _re, message: _m, accessToken: _at, accessExpiresAt: _ae, ...userData } = response.data;
      
      set({ 
        user: userData, 
        isAuthenticated: true, 
        accessToken, 
        accessExpiresAt 
      });

      return { success: true, mfaRequired: false };
    } catch (error) {
      if (error.response?.status === 401) {
          throw new Error('Invalid email or password');
      }
      throw new Error(error.response?.data?.message || 'Failed to log in');
    }
  },

  verifyMfa: async (email, code) => {
    try {
      // POST /mfa/challenge expects { email, code } to identify the user and verify the TOTP code.
      const response = await api.post('/mfa/challenge', { email, code });
      
      if (response.data.success) {
         const { accessToken } = response.data;
         const { refreshToken: _rt } = response.data;
         
         set({ accessToken });
         // Fetch user details immediately after solving challenge
         const userResp = await api.get('/users/me');
         set({ user: userResp.data, isAuthenticated: true });
         return { success: true };
      } else {
         throw new Error('Invalid 2FA code');
      }
    } catch (error) {
      throw new Error(error.response?.data?.message || error.message || 'Failed to verify 2FA');
    }
  },

  register: async (email, username, password) => {
    try {
      const { deviceId, deviceLabel } = getDeviceMetadata();
      const response = await api.post('/auth/register', { email, username, password, deviceId, deviceLabel });
      
      // Response includes tokens
      const { accessToken, accessExpiresAt } = response.data;
      const { refreshToken: _rt, refreshExpiresAt: _re, message: _m, accessToken: _at, accessExpiresAt: _ae, ...userData } = response.data;
      
      set({ 
        user: userData, 
        isAuthenticated: true, 
        accessToken, 
        accessExpiresAt 
      });
      return { success: true };
    } catch (error) {
      if (error.response?.status === 409) {
          throw new Error('Email or username already taken');
      }
      throw new Error(error.response?.data?.message || 'Failed to register');
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
  },
}));

export default useAuthStore;
