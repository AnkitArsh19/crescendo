import api from './axios';

// ─────────────────────────────────────────────────────────────────────────────
// Email Service — API Key Management  (/settings/api-keys)
// ─────────────────────────────────────────────────────────────────────────────

export const apiKeysApi = {
  list: () =>
    api.get('/settings/api-keys').then((r) => r.data),

  get: (id) =>
    api.get(`/settings/api-keys/${id}`).then((r) => r.data),

  create: (data) =>
    api.post('/settings/api-keys', data).then((r) => r.data),

  revoke: (id) =>
    api.delete(`/settings/api-keys/${id}`),

  rotate: (id, data = { gracePeriodHours: 24 }) =>
    api.post(`/settings/api-keys/${id}/rotate`, data).then((r) => r.data),
};

// ─────────────────────────────────────────────────────────────────────────────
// Email Service — Domain Management  (/settings/domains)
// ─────────────────────────────────────────────────────────────────────────────

export const domainsApi = {
  list: () => api.get('/settings/domains').then(res => res.data),
  add: (data) => api.post('/settings/domains', data).then(res => res.data),
  verify: (id) => api.post(`/settings/domains/${id}/verify`).then(res => res.data),
  delete: (id) => api.delete(`/settings/domains/${id}`).then(res => res.data),
  getDomainConnectUrl: (id) => api.get(`/settings/domains/${id}/domain-connect`).then(res => res.data)
};

// ─────────────────────────────────────────────────────────────────────────────
// Email Service — Template Management  (/settings/templates)
// ─────────────────────────────────────────────────────────────────────────────

export const templatesApi = {
  list: () =>
    api.get('/settings/templates').then((r) => r.data),

  get: (id) =>
    api.get(`/settings/templates/${id}`).then((r) => r.data),

  create: (data) =>
    api.post('/settings/templates', data).then((r) => r.data),

  update: (id, data) =>
    api.patch(`/settings/templates/${id}`, data).then((r) => r.data),

  delete: (id) =>
    api.delete(`/settings/templates/${id}`),

  publish: (id) =>
    api.post(`/settings/templates/${id}/publish`).then((r) => r.data),

  testSend: (id, data) =>
    api.post(`/settings/templates/${id}/test-send`, data).then((r) => r.data),

  cloneFromBroadcast: (broadcastId) =>
    api.post(`/settings/templates/clone-from-broadcast/${broadcastId}`).then((r) => r.data),
};

// ─────────────────────────────────────────────────────────────────────────────
// Email Service — Email Sending & Logs  (/api/v1/emails)
// ─────────────────────────────────────────────────────────────────────────────

export const emailsApi = {
  list: () =>
    api.get('/api/v1/emails').then((r) => r.data),

  get: (id) =>
    api.get(`/api/v1/emails/${id}`).then((r) => r.data),

  send: (data) =>
    api.post('/api/v1/emails', data).then((r) => r.data),

  checkSpamScore: (data) =>
    api.post('/api/email/check-content', data).then((r) => r.data),
};
