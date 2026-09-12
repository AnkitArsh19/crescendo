import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { redirectToDesktopHandoff, getBrowserBaseUrl } from '../../../src/utils/desktopAuth';
import api from '../../../src/api/axios';
import useAuthStore from '../../../src/store/authStore';

vi.mock('../../../src/api/axios', () => ({
  default: {
    post: vi.fn(),
  },
}));

describe('desktopAuth', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
    useAuthStore.setState({
      accessToken: 'test-jwt-access-token',
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe('getBrowserBaseUrl', () => {
    it('returns default production URL when no env var set', () => {
      expect(getBrowserBaseUrl()).toBe('https://app.crescendo.run');
    });
  });

  describe('redirectToDesktopHandoff', () => {
    it('issues a one-time code and navigates to /open-app with code query param', async () => {
      const navigate = vi.fn();
      api.post.mockResolvedValueOnce({
        data: { code: 'one-time-handoff-code-xyz' },
      });

      sessionStorage.setItem('crescendo_from_desktop', 'true');
      document.cookie = 'crescendo_from_desktop=true; path=/';

      await redirectToDesktopHandoff(navigate);

      // Verify POST call was made to /auth/desktop-handoff/issue
      expect(api.post).toHaveBeenCalledWith(
        '/auth/desktop-handoff/issue',
        expect.objectContaining({
          deviceId: expect.any(String),
          deviceLabel: expect.any(String),
        }),
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: 'Bearer test-jwt-access-token',
          }),
        })
      );

      // Verify cookies/sessionStorage cleared
      expect(sessionStorage.getItem('crescendo_from_desktop')).toBeNull();

      // Verify navigate called with code param only (no tokens in URL)
      expect(navigate).toHaveBeenCalledWith(
        '/open-app?code=one-time-handoff-code-xyz',
        { replace: true }
      );
    });

    it('falls back gracefully to /open-app if issuing code fails', async () => {
      const navigate = vi.fn();
      api.post.mockRejectedValueOnce(new Error('Network error'));

      await redirectToDesktopHandoff(navigate);

      expect(navigate).toHaveBeenCalledWith('/open-app', { replace: true });
    });
  });
});
