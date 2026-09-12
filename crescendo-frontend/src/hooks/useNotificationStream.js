import { useEffect } from 'react';
import useAuthStore from '../store/authStore';
import useNotificationStore from '../store/notificationStore';
import api from '../api/axios';

const eventUrl = `${import.meta.env.VITE_API_URL || 'https://api.crescendo.run'}/notifications/events`;

function isTokenExpired(token) {
  if (!token) return true;
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return false;
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(window.atob(base64));
    return payload.exp ? Date.now() >= (payload.exp - 5) * 1000 : false;
  } catch {
    return false;
  }
}

export default function useNotificationStream() {
  const isGuest = useAuthStore((state) => state.isGuest);
  const accessToken = useAuthStore((state) => state.accessToken);
  const addRealtimeNotification = useNotificationStore((state) => state.addRealtimeNotification);
  const fetchUnreadCount = useNotificationStore((state) => state.fetchUnreadCount);

  useEffect(() => {
    if (isGuest || !accessToken) return undefined;

    // Hydrate initial unread count on login/mount
    fetchUnreadCount();

    // If token is already expired, proactively refresh it via axios instead of opening dead stream
    if (isTokenExpired(accessToken)) {
      api.get('/users/me').catch(() => {});
      return undefined;
    }

    let isSubscribed = true;
    let retryTimer = null;
    let source = null;

    const connect = () => {
      if (!isSubscribed) return;

      // Do NOT set withCredentials: true since token is in the query string and
      // cross-origin cookies trigger Edge Tracking Prevention warnings.
      source = new EventSource(`${eventUrl}?access_token=${encodeURIComponent(accessToken)}`);

      source.addEventListener('notification', (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data) {
            addRealtimeNotification(data);

            // Show browser notification if tab is in background and permission granted
            if (
              typeof window !== 'undefined' &&
              document.visibilityState === 'hidden' &&
              'Notification' in window &&
              Notification.permission === 'granted'
            ) {
              new Notification(data.title || 'Crescendo Notification', {
                body: data.body || '',
                icon: '/favicon.ico',
              });
            }
          }
        } catch (err) {
          console.warn('Failed to parse incoming notification event:', err);
        }
      });

      source.onerror = () => {
        // Immediately close the dead source to abort browser's 3-second retry loop
        if (source) {
          source.close();
          source = null;
        }

        if (!isSubscribed) return;

        // Trigger token refresh via axios interceptor if the token was expired
        api.get('/users/me').catch(() => {});

        // Backoff reconnect after 10s if the token was not updated by refresh
        retryTimer = setTimeout(() => {
          if (isSubscribed) connect();
        }, 10000);
      };
    };

    connect();

    return () => {
      isSubscribed = false;
      if (retryTimer) clearTimeout(retryTimer);
      if (source) source.close();
    };
  }, [accessToken, isGuest, addRealtimeNotification, fetchUnreadCount]);
}

