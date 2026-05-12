import api from './axios';

// ─────────────────────────────────────────────────────────────────────────────
// Workflow Run API  (/workflows/{workflowId}/runs)
// ─────────────────────────────────────────────────────────────────────────────

export const workflowRunApi = {
  start: (workflowId, triggerData) =>
    api.post(`/workflows/${workflowId}/runs`, { triggerData }).then((r) => r.data),

  list: (workflowId, page = 0, size = 20) =>
    api.get(`/workflows/${workflowId}/runs`, { params: { page, size } }).then((r) => r.data),

  get: (workflowId, runId) =>
    api.get(`/workflows/${workflowId}/runs/${runId}`).then((r) => r.data),

  cancel: (workflowId, runId) =>
    api.post(`/workflows/${workflowId}/runs/${runId}/cancel`),

  stats: (workflowId) =>
    api.get(`/workflows/${workflowId}/runs/stats`).then((r) => r.data),
};

// ─────────────────────────────────────────────────────────────────────────────
// All Runs API  (/runs) — cross-workflow
// ─────────────────────────────────────────────────────────────────────────────

export const allRunsApi = {
  list: (page = 0, size = 20) =>
    api.get('/runs', { params: { page, size } }).then((r) => r.data),
};

// ─────────────────────────────────────────────────────────────────────────────
// Step Run API  (/workflows/{workflowId}/runs/{runId}/steps)
// ─────────────────────────────────────────────────────────────────────────────

export const stepRunApi = {
  list: (workflowId, runId) =>
    api.get(`/workflows/${workflowId}/runs/${runId}/steps`).then((r) => r.data),

  get: (workflowId, runId, stepRunId) =>
    api.get(`/workflows/${workflowId}/runs/${runId}/steps/${stepRunId}`).then((r) => r.data),
};
