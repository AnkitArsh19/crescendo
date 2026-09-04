import { useEffect } from 'react';
import useAuthStore from '../store/authStore';
import useNotificationStore from '../store/notificationStore';

const eventUrl = `${import.meta.env.VITE_API_URL || 'https://api.crescendo.run'}/notifications/events`;

export default function useNotificationStream() {
  const isGuest = useAuthStore((state) => state.isGuest);
  const accessToken = useAuthStore((state) => state.accessToken);
  const addRealtimeNotification = useNotificationStore((state) => state.addRealtimeNotification);
  const fetchUnreadCount = useNotificationStore((state) => state.fetchUnreadCount);

  useEffect(() => {
    if (isGuest || !accessToken) return undefined;

    // Hydrate initial unread count on login/mount
    fetchUnreadCount();

    const source = new EventSource(
      `${eventUrl}?access_token=${encodeURIComponent(accessToken)}`,
      { withCredentials: true }
    );

    const onNotification = (event) => {
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
    };

    source.addEventListener('notification', onNotification);

    return () => {
      source.removeEventListener('notification', onNotification);
      source.close();
    };
  }, [accessToken, isGuest, addRealtimeNotification, fetchUnreadCount]);
}
