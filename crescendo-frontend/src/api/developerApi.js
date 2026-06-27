import api from './axios';

export const developerAppsApi = {
  list: () => api.get('/settings/developer-apps').then((response) => response.data),
  get: (id) => api.get(`/settings/developer-apps/${id}`).then((response) => response.data),
  create: (request) =>
    api.post('/settings/developer-apps', request).then((response) => response.data),
  update: (id, request) =>
    api.patch(`/settings/developer-apps/${id}`, request).then((response) => response.data),
  rotateSecret: (id) =>
    api.post(`/settings/developer-apps/${id}/rotate-secret`).then((response) => response.data),
  deactivate: (id) => api.post(`/settings/developer-apps/${id}/deactivate`),
  delete: (id) => api.delete(`/settings/developer-apps/${id}`),
};

export const oauthAuthorizationApi = {
  createSession: () =>
    api.post('/oauth/session').then((response) => response.data),
};
