import api from './axios';

// ─────────────────────────────────────────────────────────────────────────────
// Webhook API  (/workflows/{workflowId}/webhooks)
// ─────────────────────────────────────────────────────────────────────────────

export const webhookApi = {
  list: (workflowId) =>
    api.get(`/workflows/${workflowId}/webhooks`).then((r) => r.data),

  get: (workflowId, webhookId) =>
    api.get(`/workflows/${workflowId}/webhooks/${webhookId}`).then((r) => r.data),

  toggle: (workflowId, webhookId) =>
    api.post(`/workflows/${workflowId}/webhooks/${webhookId}/toggle`).then((r) => r.data),

  regenerate: (workflowId, webhookId) =>
    api.post(`/workflows/${workflowId}/webhooks/${webhookId}/regenerate`).then((r) => r.data),
};
