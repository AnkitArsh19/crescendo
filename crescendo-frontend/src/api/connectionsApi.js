import api from './axios';

// ─────────────────────────────────────────────────────────────────────────────
// Connections API  (/connections)
// ─────────────────────────────────────────────────────────────────────────────

export const connectionsApi = {
  list: () =>
    api.get('/connections').then((r) => r.data),

  get: (id) =>
    api.get(`/connections/${id}`).then((r) => r.data),

  create: (data) =>
    api.post('/connections', data).then((r) => r.data),

  update: (id, data) =>
    api.patch(`/connections/${id}`, data),

  delete: (id) =>
    api.delete(`/connections/${id}`),

  test: (id) =>
    api.post(`/connections/${id}/test`).then((r) => r.data),
};

