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
   * Returns { authorizationUrl } — the frontend opens this URL in a popup.
   *
   * @param {string} providerKey
   * @param {object} [opts]
   * @param {string} [opts.connectionId]       — existing connection ID for reconnect flows
   * @param {string} [opts.customClientId]     — user-provided OAuth client ID (optional)
   * @param {string} [opts.customClientSecret] — user-provided OAuth client secret (optional)
   * @param {string} [opts.customScopes]       — space-separated scopes (optional)
   *
   * If customClientId + customClientSecret are provided, the backend implicitly
   * creates/updates a UserOAuthApp record so it is reused on future connections —
   * no separate Settings page trip required.
   */
  getOAuthUrl: (providerKey, opts = {}) => {
    const { connectionId, customClientId, customClientSecret, customScopes } = opts;
    const body = {
      ...(connectionId         ? { connectionId }         : {}),
      ...(customClientId       ? { customClientId }       : {}),
      ...(customClientSecret   ? { customClientSecret }   : {}),
      ...(customScopes         ? { customScopes }         : {}),
    };
    return api
      .post(`/connections/oauth/${providerKey}/authorize`, body)
      .then((r) => r.data);
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// OAuth App Settings API  (/settings/oauth-apps)
// Manage per-user custom OAuth app registrations (client_id / client_secret).
// Settings page = view / rotate / delete what was implicitly created in the
// connection flow (or explicitly added here).
// ─────────────────────────────────────────────────────────────────────────────

export const oauthAppsApi = {
  /** List all configured custom OAuth apps (secrets are never returned). */
  list: () =>
    api.get('/settings/oauth-apps').then((r) => r.data),

  /**
   * Save (create or update) a custom OAuth app for a provider.
   * @param {{ providerKey: string, clientId: string, clientSecret: string, scopes?: string }} req
   */
  save: (req) =>
    api.post('/settings/oauth-apps', req).then((r) => r.data),

  /**
   * Delete the custom OAuth app config for a specific provider.
   * @param {string} providerKey
   */
  delete: (providerKey) =>
    api.delete(`/settings/oauth-apps/${providerKey}`).then((r) => r.data),
};
