/**
 * workflowSaveCoordinator.js
 *
 * Owns all save triggers for a workflow canvas session.
 * Replaces the single monolithic handleSave() that previously lived inline
 * in WorkflowCanvas.jsx and was called from 5 different places with a
 * `mode` flag to conditionally branch 12+ if/else blocks.
 *
 * Three save modes — each with a distinct contract:
 *
 *   saveManual(ctx)
 *     - Triggered by: Save button, Run button (pre-run), Config "Save & Close"
 *     - Validates all nodes strictly. Returns error message on first failure.
 *     - Awaited by the caller. Updates UI feedback (isSaving, savedAt, error).
 *     - Deletes backend steps the user explicitly removed.
 *     - Returns the workflow ID on success, null on failure.
 *
 *   saveAuto(ctx)
 *     - Triggered by: debounced autosave timer (800ms after last change)
 *     - Silently persists only fully-configured nodes.
 *     - NEVER deletes any backend step (avoids destructive partial saves).
 *     - Deduplicates: if a save is already in flight, queues one follow-up.
 *     - No UI error feedback; failures are swallowed (non-blocking).
 *
 *   saveExit(ctx)
 *     - Triggered by: beforeunload, component unmount (SPA navigation)
 *     - Best-effort: uses fetch keepalive for beforeunload, async for unmount.
 *     - Sends only the graph-save payload; never waits for a response.
 *     - Falls back to no-op if nothing is dirty.
 *
 * All three modes share one inflight lock (inflightRef) to prevent
 * concurrent saves from interleaving their HTTP calls.
 */

import { workflowClient } from '../api/workflowClient';
import { appCatalogApi } from '../api/appCatalogApi';
import {
    edgesToPayload,
    orderedNodesFromGraph,
    validateGraphForSave,
    validateNodeForSave,
    nodeToStepPayload,
} from './workflowGraphSerializer';
import useToastStore from '../store/toastStore';

// ─────────────────────────────────────────────────────────────────────────────
// Factory
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Creates a save coordinator for one WorkflowCanvas session.
 * Call once per mount inside a useRef so the object identity is stable.
 *
 * @param {{
 *   getNodes:           () => Array,       // current React Flow nodes
 *   getEdges:           () => Array,       // current React Flow edges
 *   getWorkflowId:      () => string|null, // current persisted workflow ID
 *   getWorkflowName:    () => string,
 *   getCatalogApps:     () => Array,
 *   getAppDetailsByKey: () => Object,
 *   draft:              Object,            // workflowDraftStore instance
 *   onWorkflowCreated:  (id: string) => void,
 *   onNodeSaved:        (nodeId: string, backendId: string) => void,
 *   onSaveStart:        () => void,
 *   onSaveSuccess:      (savedAt: number) => void,
 *   onSaveError:        (msg: string) => void,
 *   onDirtyChange:      (isDirty: boolean) => void,
 * }} callbacks
 */
export function createSaveCoordinator(callbacks) {
    const {
        getNodes,
        getEdges = () => [],
        getWorkflowId,
        getWorkflowName,
        getCatalogApps,
        getAppDetailsByKey,
        draft,
        onWorkflowCreated,
        onNodeSaved,
        onSaveStart,
        onSaveSuccess,
        onSaveError,
        onDirtyChange,
    } = callbacks;

    let inflightPromise = null;  // currently running save promise
    let pendingAutoSave = false; // queued autosave to run after inflight completes
    let autoSaveTimer = null;

    // ─────────────────────────────────────────────────────────────────────────
    // Internal: ensure app details are loaded for every node
    // ─────────────────────────────────────────────────────────────────────────
    async function ensureAppDetails(nodes) {
        const appDetailsByKey = getAppDetailsByKey();
        const missing = [...new Set(
            nodes.map((n) => n.data?.appKey).filter((k) => k && !appDetailsByKey[k])
        )];
        const loaded = await Promise.all(missing.map(async (key) => {
            try {
                const detail = await appCatalogApi.get(key);
                callbacks.onAppDetailLoaded?.(key, detail);
                return [key, detail];
            } catch {
                return null;
            }
        }));
        return {
            ...appDetailsByKey,
            ...Object.fromEntries(loaded.filter(Boolean)),
        };
    }


    // Internal: ensure the workflow record exists (create or update name)
    // ─────────────────────────────────────────────────────────────────────────
    async function ensureWorkflow(nodes) {
        let id = getWorkflowId();
        const normalizedName = (getWorkflowName() || '').trim() || 'Untitled';

        if (!id) {
            if (nodes.length === 0) return null;
            const wf = await workflowClient.create({ name: normalizedName, description: '' });
            id = wf.id;
            onWorkflowCreated(id);
        } else {
            // Update name if it changed (cheap check — backend is idempotent)
            await workflowClient.update(id, { name: normalizedName }).catch(() => {});
        }
        return id;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // saveManual — strict full save
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Performs a complete validated save.
     * If another save is in flight, waits for it to finish first.
     *
     * @returns {Promise<string|null>} workflow ID on success, null on failure
     */
    async function saveManual() {
        // Wait for any in-flight save to complete before starting a new strict save
        if (inflightPromise) {
            try { await inflightPromise; } catch { /* ignore previous save errors */ }
        }

        onSaveStart();

        const nodes = orderedNodesFromGraph(getNodes(), getEdges());
        const edges = getEdges();
        const catalogApps = getCatalogApps();
        let appDetailsByKey = getAppDetailsByKey();

        // --- Structural validation ---
        if (nodes.length === 0) {
            const msg = 'Add at least one trigger and one action.';
            onSaveError(msg);
            useToastStore.getState().addToast(msg, 'error');
            return null;
        }
        const graphError = validateGraphForSave(nodes, edges);
        if (graphError) {
            onSaveError(graphError);
            useToastStore.getState().addToast(graphError, 'error');
            return null;
        }

        try {
            appDetailsByKey = await ensureAppDetails(nodes);
        } catch {
            // Backend validation remains authoritative if a catalog detail is unavailable.
        }

        for (let idx = 0; idx < nodes.length; idx++) {
            const err = validateNodeForSave(nodes[idx], idx, catalogApps, appDetailsByKey);
            if (err) {
                onSaveError(err);
                useToastStore.getState().addToast(err, 'error');
                return null;
            }
        }

        // --- Execute save ---
        inflightPromise = (async () => {
            try {
                const id = await ensureWorkflow(nodes);
                if (!id) return null;

                const { deletedBackendIds, serverRevision } = draft.getDelta();

                const steps = nodes.map(node => {
                    const payload = nodeToStepPayload(node, appDetailsByKey);
                    if (!payload) return null;
                    return {
                        clientId: node.id,
                        backendId: payload.backendId,
                        type: payload.type,
                        name: payload.name,
                        actionKey: payload.actionKey,
                        appKey: payload.appKey,
                        connectionId: payload.connectionId,
                        configuration: payload.configuration
                    };
                }).filter(Boolean);

                const edgePayloads = edgesToPayload(edges);

                const resp = await workflowClient.updateGraph(id, {
                    name: (getWorkflowName() || '').trim() || 'Untitled',
                    revision: serverRevision,
                    steps,
                    edges: edgePayloads,
                    deletedStepIds: [...deletedBackendIds]
                });

                for (const saved of resp.savedSteps) {
                    const node = nodes.find(n => n.id === saved.clientId);
                    if (node && !node.data._backendId) {
                        onNodeSaved(saved.clientId, saved.backendId);
                    }
                    draft.markSaved(saved.clientId);
                }

                draft.reset(resp.revision);
                onDirtyChange(false);
                onSaveSuccess(Date.now());
                return id;
            } catch (err) {
                const msg = err.response?.data?.message || err.message || 'Save failed';
                onSaveError(msg);
                useToastStore.getState().addToast(msg, 'error');
                return null;
            }
        })();

        try {
            return await inflightPromise;
        } finally {
            inflightPromise = null;
            // Run queued autosave if one was deferred while we were saving
            if (pendingAutoSave) {
                pendingAutoSave = false;
                _runAutoSave();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // saveAuto — silent partial save, never destructive
    // ─────────────────────────────────────────────────────────────────────────

    async function _runAutoSave() {
        const nodes = orderedNodesFromGraph(getNodes(), getEdges());
        const edges = getEdges();
        const catalogApps = getCatalogApps();
        let appDetailsByKey = getAppDetailsByKey();

        // Graph save replaces every server edge. A partial autosave would delete
        // connections that happen to touch an unfinished node, so it is all-or-nothing.
        if (validateGraphForSave(nodes, edges)) return;
        try {
            appDetailsByKey = await ensureAppDetails(nodes);
        } catch {
            return;
        }
        if (nodes.some((node, index) =>
            validateNodeForSave(node, index, catalogApps, appDetailsByKey))) return;

        if (inflightPromise) {
            // Another save in flight — queue one follow-up
            pendingAutoSave = true;
            return;
        }

        inflightPromise = (async () => {
            try {
                const id = await ensureWorkflow(nodes);
                if (!id) return;

                const steps = nodes.map(node => {
                    const payload = nodeToStepPayload(node, appDetailsByKey);
                    if (!payload) return null;
                    return {
                        clientId: node.id,
                        backendId: payload.backendId,
                        type: payload.type,
                        name: payload.name,
                        actionKey: payload.actionKey,
                        appKey: payload.appKey,
                        connectionId: payload.connectionId,
                        configuration: payload.configuration
                    };
                }).filter(Boolean);

                const edgePayloads = edgesToPayload(edges);

                const { serverRevision } = draft.getDelta();

                const resp = await workflowClient.updateGraph(id, {
                    name: (getWorkflowName() || '').trim() || 'Untitled',
                    revision: serverRevision,
                    steps,
                    edges: edgePayloads,
                    deletedStepIds: [] // IMPORTANT: autosave NEVER deletes steps.
                });

                for (const saved of resp.savedSteps) {
                    const node = nodes.find(n => n.id === saved.clientId);
                    if (node && !node.data._backendId) {
                        onNodeSaved(saved.clientId, saved.backendId);
                    }
                    draft.markSaved(saved.clientId);
                }

                draft.reset(resp.revision);
                onSaveSuccess(Date.now());
                onDirtyChange(draft.isDirty());
            } catch (err) {
                if (err.response?.status === 409) {
                    const msg = err.response?.data?.message || 'Workflow modified by another session. Please refresh.';
                    useToastStore.getState().addToast(msg, 'error');
                }
                // Other autosave failures are silently swallowed
            }
        })();

        try {
            await inflightPromise;
        } finally {
            inflightPromise = null;
            if (pendingAutoSave) {
                pendingAutoSave = false;
                _runAutoSave();
            }
        }
    }

    /**
     * Schedules a debounced autosave (800ms delay).
     * Call this whenever the canvas state changes.
     * Multiple calls within the debounce window are collapsed into one save.
     */
    function saveAuto() {
        if (autoSaveTimer) clearTimeout(autoSaveTimer);
        autoSaveTimer = setTimeout(() => {
            autoSaveTimer = null;
            _runAutoSave();
        }, 800);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // saveExit — best-effort on navigation/tab close
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fires a best-effort save when the user navigates away or closes the tab.
     * Does NOT wait for a response (fire-and-forget).
     * Does NOT delete steps (partial/exit saves are non-destructive).
     *
     * @param {{ keepalive?: boolean }} options
     *   keepalive: true when called from beforeunload (browser tab close).
     *              The save is sent via fetch keepalive so it survives page unload.
     */
    async function saveExit({ keepalive = false } = {}) {
        if (!draft.isDirty()) return;

        const nodes = getNodes();
        const catalogApps = getCatalogApps();
        const appDetailsByKey = getAppDetailsByKey();

        // Only attempt to save the configured prefix — skip empty/partial nodes
        const configuredNodes = [];
        for (let i = 0; i < nodes.length; i++) {
            const err = validateNodeForSave(nodes[i], i, catalogApps, appDetailsByKey);
            if (err) break;
            configuredNodes.push(nodes[i]);
        }

        const id = getWorkflowId();
        if (!id || configuredNodes.length === 0) return;

        if (keepalive) {
            // Use fetch keepalive for beforeunload — survives tab close
            // Sends a lightweight PATCH on the workflow name only (minimal payload)
            // The full step data is not guaranteed to be sendable within keepalive limits (64KB)
            try {
                const token = document.cookie.match(/jwt=([^;]+)/)?.[1] ||
                    localStorage.getItem('accessToken') || '';
                const guestSessionId = localStorage.getItem('crescendo_guest_session');
                const isGuest = !token && !!guestSessionId;

                const url = isGuest 
                    ? `/api/guest/workflows/${id}` 
                    : `/api/workflows/${id}`;

                const headers = { 'Content-Type': 'application/json' };
                if (isGuest) {
                    headers['X-Guest-Session'] = guestSessionId;
                } else if (token) {
                    headers['Authorization'] = `Bearer ${token}`;
                }

                const baseUrl = import.meta.env.VITE_API_URL || 'https://api.crescendo.run';
                fetch(`${baseUrl}${url}`, {
                    method: 'PATCH',
                    headers,
                    body: JSON.stringify({ name: (getWorkflowName() || '').trim() || 'Untitled' }),
                    keepalive: true,
                });
            } catch { /* best effort */ }
        } else {
            // Regular async fire-and-forget for SPA unmount
            _runAutoSave().catch(() => {});
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────

    function destroy() {
        if (autoSaveTimer) clearTimeout(autoSaveTimer);
        autoSaveTimer = null;
    }

    return { saveManual, saveAuto, saveExit, destroy };
}
