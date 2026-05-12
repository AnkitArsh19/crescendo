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
};

// ─────────────────────────────────────────────────────────────────────────────
// Email Service — Domain Management  (/settings/domains)
// ─────────────────────────────────────────────────────────────────────────────

export const domainsApi = {
  list: () =>
    api.get('/settings/domains').then((r) => r.data),

  get: (id) =>
    api.get(`/settings/domains/${id}`).then((r) => r.data),

  add: (data) =>
    api.post('/settings/domains', data).then((r) => r.data),

  verify: (id) =>
    api.post(`/settings/domains/${id}/verify`).then((r) => r.data),

  delete: (id) =>
    api.delete(`/settings/domains/${id}`),
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
};
