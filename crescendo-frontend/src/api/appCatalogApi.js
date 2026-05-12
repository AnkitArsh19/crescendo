import api from './axios';

// ─────────────────────────────────────────────────────────────────────────────
// App Catalog API  (/apps)
// ─────────────────────────────────────────────────────────────────────────────

export const appCatalogApi = {
  list: () =>
    api.get('/apps').then((r) => r.data),

  get: (appKey) =>
    api.get(`/apps/${appKey}`).then((r) => r.data),

  /**
   * Initiates an OAuth connection flow for a provider.
   * Returns { authorizationUrl } — the frontend opens this URL.
   * @param {string} providerKey
   * @param {string} [connectionId] — pass existing ID to reconnect instead of creating new
   */
  getOAuthUrl: (providerKey, connectionId) => {
    const params = connectionId ? `?connectionId=${connectionId}` : '';
    return api.get(`/connections/oauth/${providerKey}/authorize${params}`).then((r) => r.data);
  },
};
