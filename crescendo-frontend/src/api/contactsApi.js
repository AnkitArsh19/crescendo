import api from './axios';

export const contactsApi = {
  list: () => api.get('/settings/contacts').then(r => r.data),
  get: (id) => api.get(`/settings/contacts/${id}`).then(r => r.data),
  create: (data) => api.post('/settings/contacts', data).then(r => r.data),
  update: (id, data) => api.patch(`/settings/contacts/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/settings/contacts/${id}`),
};
