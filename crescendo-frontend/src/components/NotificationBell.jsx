import React from 'react';
import { HiOutlineBell } from 'react-icons/hi';
import useNotificationStore from '../store/notificationStore';
import './NotificationBell.css';

export default function NotificationBell() {
  const unreadCount = useNotificationStore((state) => state.unreadCount);
  const toggleDrawer = useNotificationStore((state) => state.toggleDrawer);

  const displayCount = unreadCount > 99 ? '99+' : unreadCount;

  return (
    <button
      className="notif-bell-btn"
      onClick={toggleDrawer}
      title={unreadCount > 0 ? `Notifications (${unreadCount} unread)` : 'Notifications'}
      aria-label={unreadCount > 0 ? `Notifications (${unreadCount} unread)` : 'Notifications'}
    >
      <HiOutlineBell />
      {unreadCount > 0 && <span className="notif-badge">{displayCount}</span>}
    </button>
  );
}
