import { create } from 'zustand';
import {
  getNotifications,
  getUnreadCount,
  markNotificationsRead,
  markAllNotificationsRead,
  deleteNotification,
} from '../api/notifications';

const useNotificationStore = create((set, get) => ({
  notifications: [],
  unreadCount: 0,
  isDrawerOpen: false,
  filter: 'all', // 'all' | 'unread'
  page: 0,
  hasMore: false,
  isLoading: false,

  openDrawer: () => {
    set({ isDrawerOpen: true });
    get().fetchNotifications();
    get().fetchUnreadCount();
  },

  closeDrawer: () => set({ isDrawerOpen: false }),

  toggleDrawer: () => {
    const next = !get().isDrawerOpen;
    set({ isDrawerOpen: next });
    if (next) {
      get().fetchNotifications();
      get().fetchUnreadCount();
    }
  },

  setFilter: (filter) => {
    set({ filter, page: 0, notifications: [], hasMore: false });
    get().fetchNotifications(0, filter);
  },

  fetchNotifications: async (pageToFetch = 0, currentFilter) => {
    const filter = currentFilter ?? get().filter;
    set({ isLoading: true });
    try {
      const data = await getNotifications({ filter, page: pageToFetch, size: 20 });
      const newItems = data.content || [];
      set({
        notifications: pageToFetch === 0 ? newItems : [...get().notifications, ...newItems],
        page: pageToFetch,
        hasMore: !data.last,
        isLoading: false,
      });
    } catch {
      set({ isLoading: false });
    }
  },

  fetchMore: async () => {
    if (get().isLoading || !get().hasMore) return;
    const nextPage = get().page + 1;
    await get().fetchNotifications(nextPage);
  },

  fetchUnreadCount: async () => {
    try {
      const count = await getUnreadCount();
      set({ unreadCount: count });
    } catch {
      // ignore
    }
  },

  addRealtimeNotification: (notif) => {
    if (!notif) return;
    const { filter, notifications, unreadCount } = get();
    const exists = notifications.some((n) => n.id === notif.id);
    if (!exists) {
      const shouldInclude = filter === 'all' || (filter === 'unread' && !notif.isRead);
      set({
        notifications: shouldInclude ? [notif, ...notifications] : notifications,
        unreadCount: unreadCount + 1,
      });
    }
  },

  markAsRead: async (ids) => {
    if (!ids || ids.length === 0) return;
    try {
      await markNotificationsRead(ids);
      const setIds = new Set(ids);
      let markedCount = 0;
      const updated = get().notifications.map((n) => {
        if (setIds.has(n.id)) {
          if (!n.isRead) markedCount++;
          return { ...n, isRead: true };
        }
        return n;
      });
      set({
        notifications: updated,
        unreadCount: Math.max(0, get().unreadCount - markedCount),
      });
    } catch {
      // ignore
    }
  },

  markAllAsRead: async () => {
    try {
      await markAllNotificationsRead();
      const updated = get().notifications.map((n) => ({ ...n, isRead: true }));
      set({
        notifications: updated,
        unreadCount: 0,
      });
    } catch {
      // ignore
    }
  },

  deleteNotificationItem: async (id) => {
    try {
      await deleteNotification(id);
      const target = get().notifications.find((n) => n.id === id);
      const wasUnread = target && !target.isRead;
      set({
        notifications: get().notifications.filter((n) => n.id !== id),
        unreadCount: wasUnread ? Math.max(0, get().unreadCount - 1) : get().unreadCount,
      });
    } catch {
      // ignore
    }
  },
}));

export default useNotificationStore;
