import { appCatalogApi } from './appCatalogApi';

let cachedApps = null;
let pendingPromise = null;

/**
 * Returns cached app catalog list, or fetches from /apps if not yet cached.
 */
export async function getCachedApps() {
  if (cachedApps) return cachedApps;
  if (!pendingPromise) {
    pendingPromise = appCatalogApi
      .list()
      .then((apps) => {
        cachedApps = Array.isArray(apps) ? apps : [];
        return cachedApps;
      })
      .catch((err) => {
        console.warn('Failed to load app catalog cache:', err);
        return [];
      })
      .finally(() => {
        pendingPromise = null;
      });
  }
  return pendingPromise;
}

/**
 * Returns the synchronously available cached apps array (may be empty if still fetching).
 */
export function getCachedAppsSync() {
  return cachedApps || [];
}
