import api from './axios';

export const suppressionsApi = {
  list: () => api.get('/settings/suppressions').then(r => r.data),
  add: (email) => api.post('/settings/suppressions', { email }).then(r => r.data),
  import: (formData) => api.post('/settings/suppressions/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data),
  remove: (id) => api.delete(`/settings/suppressions/${id}`),
};
