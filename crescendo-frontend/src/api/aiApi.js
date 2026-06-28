import api from './axios';

// ─────────────────────────────────────────────────────────────────────────────
// AI API  (/ai)
// Proxied by Spring Boot to the Python AI microservice.
// Requires a logged-in user session or an API key with the ai:build scope.
// Returns 503 when the Python service is not yet configured.
// ─────────────────────────────────────────────────────────────────────────────

export const aiApi = {
  /**
   * Generate a workflow draft from a natural language prompt.
   * @param {string} prompt  - e.g. "Send a Slack message when a GitHub PR is merged"
   * @param {Object} context - optional metadata (e.g. available connections)
   * @returns {Promise<{workflow: {name: string, steps: Array}}>}
   */
  createWorkflowDraft: (prompt, context = {}) =>
    api.post('/ai/workflow-drafts', { prompt, context }).then((r) => r.data),
};
