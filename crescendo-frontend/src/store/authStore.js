import { create } from 'zustand';
import api from '../api/axios';

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

  // Called on app mount to restore session from the token (if stored or fetched)
  // Usually the refresh token is in an HttpOnly cookie.
  checkAuth: async () => {
    // Guest mode: no session to restore, just stop loading
    if (get().isGuest) {
      set({ isLoading: false });
      return;
    }
    try {
      // Trying to fetch the user profile. If we have no access token, the interceptor will fail.
      // But we can try to hit /auth/refresh first proactively, or let the interceptor handle it.
      // Easiest is to just hit /users/me. If it works (or refreshes successfully), we're good.
      const response = await api.get('/users/me');
      set({ user: response.data, isAuthenticated: true, isLoading: false });
    } catch {
      // 401 means no valid session
      set({ user: null, isAuthenticated: false, isLoading: false, accessToken: null });
    }
  },

  login: async (email, password, rememberMe = false) => {
    try {
      const response = await api.post('/auth/login', { email, password, rememberMe });
      
      if (response.status === 202) {
        // MFA Required
        return { success: true, mfaRequired: true };
      }

      // Complete success
      const { accessToken, accessExpiresAt } = response.data;
      const { refreshToken: _rt, refreshExpiresAt: _re, message: _m, ...userData } = response.data;
      
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
      const response = await api.post('/auth/register', { email, username, password });
      
      // Response includes tokens
      const { accessToken, accessExpiresAt } = response.data;
      const { refreshToken: _rt, refreshExpiresAt: _re, message: _m, ...userData } = response.data;
      
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
