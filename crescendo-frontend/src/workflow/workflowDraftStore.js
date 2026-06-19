/**
 * workflowDraftStore.js
 *
 * Tracks the delta between the current canvas state and the last
 * successfully saved server state. Stored in module-level refs (not React
 * state) to avoid re-renders on every keystroke.
 *
 * The draft captures three distinct kinds of changes:
 *   - changedNodeIds   : nodes whose data was mutated since last save
 *   - deletedBackendIds: backend step UUIDs the user explicitly removed
 *   - serverRevision   : the updatedAt timestamp returned by the last save
 *     (used as an optimistic lock when the graph-save endpoint is active)
 *
 * Usage:
 *   import { createDraftStore } from '../workflow/workflowDraftStore';
 *   const draft = createDraftStore();         // once, inside the component
 *   draft.markChanged(nodeId);                // on every node data mutation
 *   draft.markDeleted(backendId);             // when user deletes a step
 *   const delta = draft.getDelta();           // read before save
 *   draft.reset(savedRevision);              // after successful save
 */

/**
 * Creates an isolated draft-tracking object.
 * Call once per WorkflowCanvas mount (use useRef to persist it).
 *
 * @returns {{
 *   markChanged: (nodeId: string) => void,
 *   markSaved:   (nodeId: string) => void,
 *   markDeleted: (backendId: string) => void,
 *   getDelta:    () => { changedNodeIds: Set<string>, deletedBackendIds: Set<string>, serverRevision: string|null },
 *   isDirty:     () => boolean,
 *   reset:       (newRevision?: string) => void,
 * }}
 */
export function createDraftStore() {
    let changedNodeIds = new Set();
    let deletedBackendIds = new Set();
    let serverRevision = null;

    return {
        /**
         * Mark a node as having unsaved changes.
         * Call whenever a node's data is updated by the user.
         */
        markChanged(nodeId) {
            changedNodeIds.add(nodeId);
        },

        /**
         * Unmark a node after it has been successfully saved.
         */
        markSaved(nodeId) {
            changedNodeIds.delete(nodeId);
        },

        /**
         * Record a user-intended deletion of a backend step.
         * This is separate from partial-save skipping — only call this when
         * the user explicitly removes a node from the canvas.
         *
         * @param {string} backendId — the step's UUID in the backend
         */
        markDeleted(backendId) {
            if (backendId) {
                deletedBackendIds.add(backendId);
            }
        },

        /**
         * Returns the full current delta.
         */
        getDelta() {
            return {
                changedNodeIds: new Set(changedNodeIds),
                deletedBackendIds: new Set(deletedBackendIds),
                serverRevision,
            };
        },

        /**
         * Returns true if there is anything that needs saving.
         */
        isDirty() {
            return changedNodeIds.size > 0 || deletedBackendIds.size > 0;
        },

        /**
         * Clears the delta after a successful save.
         * @param {string|null} newRevision — the updatedAt from the server response
         */
        reset(newRevision = null) {
            changedNodeIds = new Set();
            deletedBackendIds = new Set();
            if (newRevision) serverRevision = newRevision;
        },

        /**
         * Sets the server revision without clearing the delta.
         * Used after initial load to record the current server state.
         */
        setRevision(revision) {
            serverRevision = revision;
        },
    };
}
