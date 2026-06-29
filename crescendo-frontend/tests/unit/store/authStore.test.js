import { describe, it, expect, beforeEach, vi } from 'vitest';
import useAuthStore from '../../../src/store/authStore';
import api from '../../../src/api/axios';
import * as deviceFingerprint from '../../../src/utils/deviceFingerprint';

// Mock api and deviceFingerprint
vi.mock('../../../src/api/axios', () => {
  return {
    default: {
      post: vi.fn(),
      get: vi.fn(),
      delete: vi.fn(),
    }
  };
});

vi.mock('../../../src/utils/deviceFingerprint', () => ({
  getDeviceMetadata: vi.fn(() => ({ deviceId: 'test-id', deviceLabel: 'test-label' }))
}));

// Mock global fetch for checkAuth
global.fetch = vi.fn();

describe('authStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      isGuest: false,
      accessToken: null,
      accessExpiresAt: null,
      refreshToken: null,
      refreshExpiresAt: null,
    });
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('login', () => {
    it('handles successful login and sets state', async () => {
      api.post.mockResolvedValueOnce({
        status: 200,
        data: {
          accessToken: 'access-token-123',
          accessExpiresAt: '2026-01-01T00:00:00Z',
          id: 'user-1',
          email: 'test@example.com'
        }
      });

      const result = await useAuthStore.getState().login('test@example.com', 'password123');
      
      expect(result).toEqual({ success: true, mfaRequired: false });
      
      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(true);
      expect(state.accessToken).toBe('access-token-123');
      expect(state.user).toEqual({ id: 'user-1', email: 'test@example.com' });
      expect(api.post).toHaveBeenCalledWith('/auth/login', {
        email: 'test@example.com',
        password: 'password123',
        rememberMe: false,
        deviceId: 'test-id',
        deviceLabel: 'test-label'
      });
    });

    it('handles MFA required response', async () => {
      api.post.mockResolvedValueOnce({
        status: 202,
        data: { message: 'MFA Required' }
      });

      const result = await useAuthStore.getState().login('test@example.com', 'password123');
      
      expect(result).toEqual({ success: true, mfaRequired: true });
      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(false);
      expect(state.accessToken).toBeNull();
    });

    it('handles login failure', async () => {
      api.post.mockRejectedValueOnce({
        response: { status: 401 }
      });

      await expect(useAuthStore.getState().login('test@example.com', 'wrong')).rejects.toThrow('Invalid email or password');
    });
  });

  describe('verifyMfa', () => {
    it('handles successful MFA verification', async () => {
      api.post.mockResolvedValueOnce({
        data: { success: true, accessToken: 'access-token-456' }
      });
      api.get.mockResolvedValueOnce({
        data: { id: 'user-1', email: 'test@example.com' }
      });

      const result = await useAuthStore.getState().verifyMfa('test@example.com', '123456');
      
      expect(result).toEqual({ success: true });
      const state = useAuthStore.getState();
      expect(state.accessToken).toBe('access-token-456');
      expect(state.isAuthenticated).toBe(true);
      expect(state.user).toEqual({ id: 'user-1', email: 'test@example.com' });
    });
  });

  describe('logout', () => {
    it('calls api and clears state', async () => {
      useAuthStore.setState({ isAuthenticated: true, user: { id: '1' } });
      api.post.mockResolvedValueOnce({});

      await useAuthStore.getState().logout();
      
      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
      expect(api.post).toHaveBeenCalledWith('/auth/logout');
    });

    it('can force local logout without calling api', async () => {
      useAuthStore.setState({ isAuthenticated: true, user: { id: '1' } });

      await useAuthStore.getState().logout(true);
      
      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
      expect(api.post).not.toHaveBeenCalled();
    });
  });

  describe('Guest Mode', () => {
    it('enters guest mode and generates session ID', () => {
      useAuthStore.getState().enterGuestMode();
      
      const state = useAuthStore.getState();
      expect(state.isGuest).toBe(true);
      expect(state.isAuthenticated).toBe(false);
      
      const sessionId = localStorage.getItem('crescendo_guest_session');
      expect(sessionId).toBeDefined();
    });
  });
});
