import api from './axios';

// ─────────────────────────────────────────────────────────────────────────────
// Authenticated Workflow API  (/workflows)
// ─────────────────────────────────────────────────────────────────────────────

export const workflowApi = {
  list: () =>
    api.get('/workflows').then((r) => r.data),

  get: (id) =>
    api.get(`/workflows/${id}`).then((r) => r.data),

  create: (data) =>
    api.post('/workflows', data).then((r) => r.data),

  update: (id, data) =>
    api.patch(`/workflows/${id}`, data),

  updateGraph: (id, data) =>
    api.put(`/workflows/${id}/graph`, data).then((r) => r.data),

  delete: (id) =>
    api.delete(`/workflows/${id}`),

  activate: (id) =>
    api.post(`/workflows/${id}/activate`),

  deactivate: (id) =>
    api.post(`/workflows/${id}/deactivate`),

  bulkActivate: (ids) =>
    api.post('/workflows/bulk/activate', { ids }),

  bulkDeactivate: (ids) =>
    api.post('/workflows/bulk/deactivate', { ids }),

  getShared: (ids) =>
    api.get('/shared/workflows', { params: { ids: ids.join(',') } }).then((r) => r.data),

  importWorkflow: (data) =>
    api.post('/workflows/import', data).then((r) => r.data),

  run: (id, triggerData = {}) =>
    api.post(`/workflows/${id}/runs`, { triggerData }).then((r) => r.data),
};

// ─────────────────────────────────────────────────────────────────────────────
// Authenticated Step API  (/workflows/{workflowId}/steps)
// ─────────────────────────────────────────────────────────────────────────────

export const stepApi = {
  list: (workflowId) =>
    api.get(`/workflows/${workflowId}/steps`).then((r) => r.data),

  get: (workflowId, stepId) =>
    api.get(`/workflows/${workflowId}/steps/${stepId}`).then((r) => r.data),

  add: (workflowId, data) =>
    api.post(`/workflows/${workflowId}/steps`, data).then((r) => r.data),

  update: (workflowId, stepId, data) =>
    api.patch(`/workflows/${workflowId}/steps/${stepId}`, data),

  delete: (workflowId, stepId) =>
    api.delete(`/workflows/${workflowId}/steps/${stepId}`),

  reorder: (workflowId, stepId, newOrder) =>
    api.patch(`/workflows/${workflowId}/steps/${stepId}/order`, { newOrder }),

  addCondition: (workflowId, stepId, data) =>
    api.post(`/workflows/${workflowId}/steps/${stepId}/conditions`, data).then((r) => r.data),

  listConditions: (workflowId, stepId) =>
    api.get(`/workflows/${workflowId}/steps/${stepId}/conditions`).then((r) => r.data),

  deleteCondition: (workflowId, stepId, conditionId) =>
    api.delete(`/workflows/${workflowId}/steps/${stepId}/conditions/${conditionId}`),
};

// ─────────────────────────────────────────────────────────────────────────────
// Guest Workflow API  (/guest/workflows) — no JWT, X-Guest-Session header
// ─────────────────────────────────────────────────────────────────────────────

function guestHeaders(sessionId) {
  return { headers: { 'X-Guest-Session': sessionId } };
}

export const guestWorkflowApi = {
  list: (sessionId) =>
    api.get('/guest/workflows', guestHeaders(sessionId)).then((r) => r.data),

  get: (sessionId, id) =>
    api.get(`/guest/workflows/${id}`, guestHeaders(sessionId)).then((r) => r.data),

  create: (sessionId, data) =>
    api.post('/guest/workflows', data, guestHeaders(sessionId)).then((r) => r.data),

  update: (sessionId, id, data) =>
    api.patch(`/guest/workflows/${id}`, data, guestHeaders(sessionId)),

  updateGraph: (sessionId, id, data) =>
    api.put(`/guest/workflows/${id}/graph`, data, guestHeaders(sessionId)).then((r) => r.data),

  delete: (sessionId, id) =>
    api.delete(`/guest/workflows/${id}`, guestHeaders(sessionId)),
};

// ─────────────────────────────────────────────────────────────────────────────
// Guest Step API  (/guest/workflows/{workflowId}/steps)
// ─────────────────────────────────────────────────────────────────────────────

export const guestStepApi = {
  list: (sessionId, workflowId) =>
    api.get(`/guest/workflows/${workflowId}/steps`, guestHeaders(sessionId)).then((r) => r.data),

  add: (sessionId, workflowId, data) =>
    api.post(`/guest/workflows/${workflowId}/steps`, data, guestHeaders(sessionId)).then((r) => r.data),

  update: (sessionId, workflowId, stepId, data) =>
    api.patch(`/guest/workflows/${workflowId}/steps/${stepId}`, data, guestHeaders(sessionId)),

  delete: (sessionId, workflowId, stepId) =>
    api.delete(`/guest/workflows/${workflowId}/steps/${stepId}`, guestHeaders(sessionId)),

  reorder: (sessionId, workflowId, stepId, newOrder) =>
    api.patch(
      `/guest/workflows/${workflowId}/steps/${stepId}/order`,
      { newOrder },
      guestHeaders(sessionId)
    ),
};

// ─────────────────────────────────────────────────────────────────────────────
// Dynamic Resource Fetching  (/apps/{appKey}/resources/{resourceType})
// Powers cascading dropdowns in the step config panel.
// ─────────────────────────────────────────────────────────────────────────────

export const resourceApi = {
  /**
   * Fetch dynamic options for a dropdown field.
   * @param {string} appKey - e.g. "google-sheets"
   * @param {string} resourceType - e.g. "spreadsheets", "channels"
   * @param {string} connectionId - user's connection UUID
   * @param {Object} params - parent cascade params, e.g. { spreadsheetId: "abc" }
   */
  list: (appKey, resourceType, connectionId, params = {}) => {
    const query = new URLSearchParams({ connectionId, ...params }).toString();
    return api
      .get(`/apps/${appKey}/resources/${resourceType}?${query}`)
      .then((r) => r.data);
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// Step Test  (/workflows/steps/test)
// Test a single step with real credentials and config.
// ─────────────────────────────────────────────────────────────────────────────

export const stepTestApi = {
  test: (data) =>
    api.post('/workflows/steps/test', data).then((r) => r.data),
};
