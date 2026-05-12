import axios from 'axios';
import useAuthStore from '../store/authStore';

let refreshPromise = null;

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://api.crescendo.run';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Needed if you want to send HttpOnly cookies like refresh_token
});

// Request Interceptor: Attach access token
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle 401s and automatic token refresh
api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // If it's a 401 and we haven't already tried to refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Don't intercept calls to the auth/refresh or login endpoints themselves
      if (originalRequest.url.includes('/auth/refresh') || originalRequest.url.includes('/auth/login')) {
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      // Guest users have no session — don't attempt refresh, just reject
      if (useAuthStore.getState().isGuest) {
        return Promise.reject(error);
      }

      try {
        const authStore = useAuthStore.getState();
        if (!refreshPromise) {
          // Single-flight refresh to avoid race conditions where two simultaneous 401s
          // rotate refresh tokens and make one request fail with "Invalid or expired refresh token".
          refreshPromise = axios.post(`${API_BASE_URL}/auth/refresh`, {}, {
            withCredentials: true
          })
            .then((response) => {
              const { accessToken, accessExpiresAt, refreshToken, refreshExpiresAt } = response.data;
              authStore.setTokens(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);
              return accessToken;
            })
            .finally(() => {
              refreshPromise = null;
            });
        }

        const accessToken = await refreshPromise;

        // Update the header on the original request and retry it
        originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed (e.g. cookie expired). Log out the user (only if not guest).
        const state = useAuthStore.getState();
        if (!state.isGuest) state.logout(true);
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;

