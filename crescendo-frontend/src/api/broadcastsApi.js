import api from './axios';

export const broadcastsApi = {
  list: () => api.get('/settings/broadcasts').then(r => r.data),
  get: (id) => api.get(`/settings/broadcasts/${id}`).then(r => r.data),
  create: (data) => api.post('/settings/broadcasts', data).then(r => r.data),
  send: (id) => api.post(`/settings/broadcasts/${id}/send`).then(r => r.data),
  delete: (id) => api.delete(`/settings/broadcasts/${id}`),
};
