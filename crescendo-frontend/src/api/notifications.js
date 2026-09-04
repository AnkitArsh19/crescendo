import api from './axios';

export const getNotifications = async ({ filter = 'all', page = 0, size = 20 } = {}) => {
  const response = await api.get('/notifications', {
    params: { filter, page, size },
  });
  return response.data;
};

export const getUnreadCount = async () => {
  const response = await api.get('/notifications/unread-count');
  return response.data?.count ?? 0;
};

export const markNotificationsRead = async (ids) => {
  const response = await api.post('/notifications/mark-read', { ids });
  return response.data;
};

export const markAllNotificationsRead = async () => {
  const response = await api.post('/notifications/mark-all-read');
  return response.data;
};

export const deleteNotification = async (id) => {
  const response = await api.delete(`/notifications/${id}`);
  return response.data;
};

export const getNotificationPreferences = async () => {
  const response = await api.get('/notifications/preferences');
  return response.data;
};

export const updateNotificationPreference = async (type, { enabled }) => {
  const response = await api.put(`/notifications/preferences/${type}`, {
    enabled,
  });
  return response.data;
};

export const getWorkflowNotificationSetting = async (workflowId) => {
  const response = await api.get(`/notifications/workflow-settings/${workflowId}`);
  return response.data;
};

export const updateWorkflowNotificationSetting = async (workflowId, notifyMode) => {
  const response = await api.put(`/notifications/workflow-settings/${workflowId}`, {
    notifyMode,
  });
  return response.data;
};
