import api from './axios';

export const metricsApi = {
  get: (days = 30) => api.get(`/settings/email-metrics?days=${days}`).then(r => r.data),
};
