import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlineX,
  HiOutlineCog,
  HiOutlineInbox,
} from 'react-icons/hi';
import useNotificationStore from '../store/notificationStore';
import NotificationItem from './NotificationItem';
import './NotificationDrawer.css';

export default function NotificationDrawer() {
  const navigate = useNavigate();
  const drawerRef = useRef(null);

  const {
    isDrawerOpen,
    closeDrawer,
    notifications,
    unreadCount,
    filter,
    setFilter,
    hasMore,
    isLoading,
    fetchMore,
    markAllAsRead,
  } = useNotificationStore();

  // Close drawer on Escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isDrawerOpen) {
        closeDrawer();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isDrawerOpen, closeDrawer]);

  const handleBackdropClick = (e) => {
    if (drawerRef.current && !drawerRef.current.contains(e.target)) {
      closeDrawer();
    }
  };

  return (
    <AnimatePresence>
      {isDrawerOpen && (
        <motion.div
          className="notif-drawer-backdrop"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
          onClick={handleBackdropClick}
        >
          <motion.div
            ref={drawerRef}
            className="notif-drawer-panel"
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 26, stiffness: 280 }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="notif-drawer-header">
              <div className="notif-header-top">
                <div className="notif-title-group">
                  <h3 className="notif-title">Inbox</h3>
                  {unreadCount > 0 && (
                    <span className="notif-count-pill">{unreadCount} new</span>
                  )}
                </div>

                <div className="notif-header-actions">
                  <button
                    className="notif-icon-btn"
                    onClick={() => {
                      closeDrawer();
                      navigate('/dashboard/settings/notifications');
                    }}
                    title="Notification Settings"
                    aria-label="Notification Settings"
                  >
                    <HiOutlineCog />
                  </button>
                  <button
                    className="notif-icon-btn"
                    onClick={closeDrawer}
                    title="Close"
                    aria-label="Close Inbox"
                  >
                    <HiOutlineX />
                  </button>
                </div>
              </div>

              <div className="notif-header-bar">
                <div className="notif-tabs">
                  <button
                    className={`notif-tab ${filter === 'all' ? 'active' : ''}`}
                    onClick={() => setFilter('all')}
                  >
                    All
                  </button>
                  <button
                    className={`notif-tab ${filter === 'unread' ? 'active' : ''}`}
                    onClick={() => setFilter('unread')}
                  >
                    Unread {unreadCount > 0 ? `(${unreadCount})` : ''}
                  </button>
                </div>

                {unreadCount > 0 && (
                  <button
                    className="notif-mark-all-btn"
                    onClick={markAllAsRead}
                  >
                    Mark all read
                  </button>
                )}
              </div>
            </div>

            {/* List */}
            <div className="notif-list-container">
              {notifications.length === 0 && !isLoading ? (
                <div className="notif-empty-state">
                  <div className="notif-empty-icon">
                    <HiOutlineInbox />
                  </div>
                  <h4 className="notif-empty-title">All caught up!</h4>
                  <p className="notif-empty-desc">
                    {filter === 'unread'
                      ? 'No unread notifications right now.'
                      : 'No notifications in your inbox yet.'}
                  </p>
                </div>
              ) : (
                notifications.map((notification) => (
                  <NotificationItem
                    key={notification.id}
                    notification={notification}
                  />
                ))
              )}

              {hasMore && (
                <button
                  className="notif-load-more-btn"
                  onClick={fetchMore}
                  disabled={isLoading}
                >
                  {isLoading ? 'Loading...' : 'Load older notifications'}
                </button>
              )}
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
