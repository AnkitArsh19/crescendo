import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  HiOutlineCheckCircle,
  HiOutlineXCircle,
  HiOutlineBan,
  HiOutlineSparkles,
  HiOutlineExclamationCircle,
  HiOutlineShieldCheck,
  HiOutlineExclamation,
  HiOutlineLockClosed,
  HiOutlineLockOpen,
  HiOutlineLink,
  HiOutlineInformationCircle,
  HiOutlineTrash,
  HiOutlineCheck,
} from 'react-icons/hi';
import useNotificationStore from '../store/notificationStore';

function formatRelativeTime(dateString) {
  if (!dateString) return '';
  const now = new Date();
  const date = new Date(dateString);
  const diffInSec = Math.floor((now - date) / 1000);

  if (diffInSec < 60) return 'Just now';
  const diffInMin = Math.floor(diffInSec / 60);
  if (diffInMin < 60) return `${diffInMin}m ago`;
  const diffInHours = Math.floor(diffInMin / 60);
  if (diffInHours < 24) return `${diffInHours}h ago`;
  const diffInDays = Math.floor(diffInHours / 24);
  if (diffInDays < 7) return `${diffInDays}d ago`;
  return date.toLocaleDateString();
}

function getTypeConfig(type) {
  switch (type) {
    case 'WORKFLOW_RUN_SUCCESS':
      return {
        icon: <HiOutlineCheckCircle />,
        colorClass: 'notif-type-success',
        badge: 'Success',
      };
    case 'WORKFLOW_RUN_FAILED':
      return {
        icon: <HiOutlineXCircle />,
        colorClass: 'notif-type-error',
        badge: 'Failed',
      };
    case 'WORKFLOW_RUN_CANCELLED':
      return {
        icon: <HiOutlineBan />,
        colorClass: 'notif-type-warning',
        badge: 'Cancelled',
      };
    case 'AI_WORKFLOW_GENERATED':
      return {
        icon: <HiOutlineSparkles />,
        colorClass: 'notif-type-ai',
        badge: 'AI Draft',
      };
    case 'AI_WORKFLOW_GENERATION_FAILED':
      return {
        icon: <HiOutlineExclamationCircle />,
        colorClass: 'notif-type-error',
        badge: 'AI Error',
      };
    case 'LOGIN_NEW_DEVICE':
      return {
        icon: <HiOutlineShieldCheck />,
        colorClass: 'notif-type-info',
        badge: 'Security',
      };
    case 'LOGIN_SUSPICIOUS':
      return {
        icon: <HiOutlineExclamation />,
        colorClass: 'notif-type-error',
        badge: 'Alert',
      };
    case 'MFA_ENABLED':
      return {
        icon: <HiOutlineLockClosed />,
        colorClass: 'notif-type-success',
        badge: 'MFA',
      };
    case 'MFA_DISABLED':
      return {
        icon: <HiOutlineLockOpen />,
        colorClass: 'notif-type-warning',
        badge: 'MFA',
      };
    case 'CONNECTION_TOKEN_EXPIRED':
      return {
        icon: <HiOutlineLink />,
        colorClass: 'notif-type-warning',
        badge: 'Connection',
      };
    case 'CONNECTION_RECONNECTED':
      return {
        icon: <HiOutlineLink />,
        colorClass: 'notif-type-success',
        badge: 'Connected',
      };
    case 'SYSTEM_ANNOUNCEMENT':
    default:
      return {
        icon: <HiOutlineInformationCircle />,
        colorClass: 'notif-type-info',
        badge: 'System',
      };
  }
}

export default function NotificationItem({ notification }) {
  const navigate = useNavigate();
  const markAsRead = useNotificationStore((state) => state.markAsRead);
  const deleteNotificationItem = useNotificationStore((state) => state.deleteNotificationItem);
  const closeDrawer = useNotificationStore((state) => state.closeDrawer);

  const { id, type, title, body, metadata, isRead, createdAt } = notification;
  const config = getTypeConfig(type);

  const handleClick = () => {
    if (!isRead) {
      markAsRead([id]);
    }

    if (metadata?.workflowId) {
      closeDrawer();
      navigate('/dashboard/history');
    } else if (metadata?.connectionId || type?.startsWith('CONNECTION_')) {
      closeDrawer();
      navigate('/dashboard/connections');
    } else if (type?.startsWith('LOGIN_') || type?.startsWith('MFA_')) {
      closeDrawer();
      navigate('/dashboard/settings/security');
    } else if (type?.startsWith('AI_')) {
      closeDrawer();
      navigate('/dashboard/workflows');
    }
  };

  const handleMarkRead = (e) => {
    e.stopPropagation();
    markAsRead([id]);
  };

  const handleDelete = (e) => {
    e.stopPropagation();
    deleteNotificationItem(id);
  };

  return (
    <div
      className={`notif-item ${!isRead ? 'unread' : ''}`}
      onClick={handleClick}
      role="button"
      tabIndex={0}
    >
      <div className={`notif-item-icon ${config.colorClass}`}>
        {config.icon}
      </div>

      <div className="notif-item-content">
        <div className="notif-item-header">
          <span className="notif-item-title">{title}</span>
          <span className="notif-item-time">{formatRelativeTime(createdAt)}</span>
        </div>

        {body && <p className="notif-item-body">{body}</p>}
      </div>

      <div className="notif-item-actions">
        {!isRead && (
          <button
            className="notif-action-btn"
            onClick={handleMarkRead}
            title="Mark as read"
            aria-label="Mark as read"
          >
            <HiOutlineCheck />
          </button>
        )}
        <button
          className="notif-action-btn notif-action-delete"
          onClick={handleDelete}
          title="Delete"
          aria-label="Delete notification"
        >
          <HiOutlineTrash />
        </button>
        {!isRead && <span className="notif-unread-dot" />}
      </div>
    </div>
  );
}
