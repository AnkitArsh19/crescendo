/**
 * workflowGraphSerializer.js
 *
 * Single source of truth for:
 *   - Config schema parsing  (parseConfigSchema)
 *   - Config type coercion   (toPersistedConfig)
 *   - Node → step DTO        (nodeToStepPayload)
 *   - Step DTO → React node  (stepsToNodes)
 *
 * Previously these were duplicated across WorkflowCanvas.jsx and
 * ConfigPanelBody.jsx. They now live here so any change is made once.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Schema Parsing
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Normalises configSchema from either the new structured List<Map> format
 * returned by the backend, or the legacy Map<String,String> hint format.
 *
 * @param {Array|Object} configSchema
 * @returns {Array<{key, label, type, required, helpText, placeholder, resourceType, dependsOn, options}>}
 */
export function parseConfigSchema(configSchema) {
    if (!configSchema) return [];

    // New structured format: configSchema is an Array of field objects
    if (Array.isArray(configSchema)) {
        return configSchema.map((field) => ({
            key: field.key,
            label: field.label || field.key,
            type: field.type || 'text',
            required: field.required === true,
            helpText: field.helpText || '',
            placeholder: field.placeholder || '',
            resourceType: field.resourceType || null,
            dependsOn: Array.isArray(field.dependsOn) ? field.dependsOn : (typeof field.dependsOn === 'string' ? [field.dependsOn] : []),
            options: Array.isArray(field.options) ? field.options : [],
            accept: field.accept || null,
            maxSizeMB: field.maxSizeMB || null,
        }));
    }

    // Legacy format: configSchema is { key: "hint string" }
    if (typeof configSchema === 'object') {
        return Object.entries(configSchema).map(([key, hint]) => {
            const text = String(hint || '');
            const lower = text.toLowerCase();
            const required = lower.includes('required');
            let type = 'text';
            if (lower.includes('array')) type = 'array';
            else if (lower.includes('number')) type = 'number';
            else if (lower.includes('boolean')) type = 'boolean';
            else if (lower.includes('json') || lower.includes('object')) type = 'json';
            return {
                key, label: key, required, type, helpText: text,
                placeholder: '', resourceType: null, dependsOn: [], options: [],
                accept: null, maxSizeMB: null,
            };
        });
    }

    return [];
}

// ─────────────────────────────────────────────────────────────────────────────
// Config Type Coercion
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Coerces raw string values from form inputs into their schema-declared types
 * before sending to the backend.
 *
 * @param {Array} schemaFields  — output of parseConfigSchema
 * @param {Object} config       — raw form values
 * @returns {Object}            — config with values cast to correct types
 */
export function toPersistedConfig(schemaFields, config) {
    const output = { ...(config || {}) };
    for (const field of schemaFields) {
        const raw = output[field.key];
        if (raw == null || raw === '') continue;
        if (field.type === 'number') {
            const n = Number(raw);
            output[field.key] = Number.isNaN(n) ? raw : n;
        } else if (field.type === 'boolean') {
            if (typeof raw === 'string') output[field.key] = raw === 'true';
        } else if (field.type === 'array' || field.type === 'multi_select_tags') {
            if (typeof raw === 'string') {
                output[field.key] = raw.split(',').map((s) => s.trim()).filter(Boolean);
            }
            if (Array.isArray(raw)) {
                output[field.key] = raw.filter(Boolean);
            }
        } else if (field.type === 'json') {
            if (typeof raw === 'string') {
                try { output[field.key] = JSON.parse(raw); } catch { /* keep as-is */ }
            }
        }
    }
    return output;
}

// ─────────────────────────────────────────────────────────────────────────────
// Node → Step DTO
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts a single React Flow node into a backend step payload.
 * Returns null if the node is not fully configured (used to skip partial nodes).
 *
 * @param {Object} node           — React Flow node
 * @param {Object} appDetailsByKey — map of appKey → app detail (triggers/actions)
 * @returns {{ clientId, backendId, type, name, appKey, actionKey, connectionId, configuration } | null}
 */
export function nodeToStepPayload(node, appDetailsByKey) {
    const stepType = node.type === 'trigger' ? 'TRIGGER' : 'ACTION';
    const appKey = node.data?.appKey;
    if (!appKey) return null;

    const opKey = stepType === 'TRIGGER'
        ? (node.data.triggerKey || node.data.actionKey)
        : node.data.actionKey;
    if (!opKey) return null;

    const detail = appDetailsByKey[appKey];
    const defs = stepType === 'TRIGGER'
        ? (Array.isArray(detail?.triggers) ? detail.triggers : [])
        : (Array.isArray(detail?.actions) ? detail.actions : []);
    const def = defs.find((d) => (d.triggerKey || d.actionKey) === opKey);
    const schemaFields = parseConfigSchema(def?.configSchema || {});
    const configuration = toPersistedConfig(schemaFields, node.data.configuration || {});

    return {
        clientId: node.id,
        backendId: node.data._backendId || null,
        type: stepType,
        name: node.data.label || (stepType === 'TRIGGER' ? 'Trigger' : 'Action'),
        appKey,
        actionKey: opKey,
        connectionId: node.data.connectionId || null,
        configuration,
    };
}

export function orderedNodesFromGraph(nodes, edges) {
    const root = nodes.find((n) => n.type === 'trigger') || nodes[0];
    if (!root) return [];

    const byId = new Map(nodes.map((n) => [n.id, n]));
    const children = new Map();
    const incoming = new Map();
    for (const edge of edges || []) {
        if (!byId.has(edge.source) || !byId.has(edge.target)) continue;
        if (!children.has(edge.source)) children.set(edge.source, []);
        children.get(edge.source).push(edge.target);
        incoming.set(edge.target, (incoming.get(edge.target) || 0) + 1);
    }

    const compareNodes = (leftId, rightId) => {
        const left = byId.get(leftId);
        const right = byId.get(rightId);
        return ((left?.position.y || 0) - (right?.position.y || 0))
            || ((left?.position.x || 0) - (right?.position.x || 0))
            || String(leftId).localeCompare(String(rightId));
    };

    // Only the trigger-reachable component is execution ordered. Orphans are
    // appended as layout-only nodes; the engine deliberately skips them.
    const reachable = new Set();
    const markReachable = (id) => {
        if (reachable.has(id)) return;
        reachable.add(id);
        for (const childId of children.get(id) || []) markReachable(childId);
    };
    markReachable(root.id);

    const reachableIncoming = new Map();
    for (const id of reachable) reachableIncoming.set(id, 0);
    for (const edge of edges || []) {
        if (reachable.has(edge.source) && reachable.has(edge.target)) {
            reachableIncoming.set(edge.target, (reachableIncoming.get(edge.target) || 0) + 1);
        }
    }

    const queue = [...reachable]
        .filter((id) => id === root.id || (reachableIncoming.get(id) || 0) === 0)
        .sort(compareNodes);
    const ordered = [];
    const emitted = new Set();
    while (queue.length > 0) {
        const id = queue.shift();
        if (emitted.has(id)) continue;
        emitted.add(id);
        const node = byId.get(id);
        if (node) ordered.push(node);

        for (const childId of (children.get(id) || []).sort(compareNodes)) {
            if (!reachable.has(childId)) continue;
            const nextIncoming = (reachableIncoming.get(childId) || 0) - 1;
            reachableIncoming.set(childId, nextIncoming);
            if (nextIncoming === 0) {
                queue.push(childId);
                queue.sort(compareNodes);
            }
        }
    }

    // A cyclic graph is invalid and blocked elsewhere. Still return every node
    // deterministically so the canvas remains renderable while a user repairs it.
    for (const id of [...reachable].sort(compareNodes)) {
        if (!emitted.has(id) && byId.has(id)) ordered.push(byId.get(id));
    }
    for (const node of nodes.filter((n) => !reachable.has(n.id)).sort((a, b) =>
        (a.position.x - b.position.x) || (a.position.y - b.position.y) || String(a.id).localeCompare(String(b.id)))) {
        ordered.push(node);
    }
    return ordered;
}

/**
 * Converts React Flow edges to backend EdgeRequest format.
 * Client IDs are intentional: one graph-save request may create nodes and
 * edges together, and the backend resolves them after step upserts.
 *
 * @param {Array} edges — React Flow edges
 * @returns {Array<{clientSourceId, clientTargetId, sourceHandle, targetHandle}>}
 */
export function edgesToPayload(edges) {
    return (edges || []).map((e) => ({
        clientSourceId: e.source,
        clientTargetId: e.target,
        sourceHandle: e.sourceHandle || null,
        targetHandle: e.targetHandle || null,
    }));
}

export function validateGraphForSave(nodes, edges) {
    const triggerNodes = nodes.filter((n) => n.type === 'trigger');
    if (triggerNodes.length !== 1) return 'Workflow must have exactly one trigger.';
    const trigger = triggerNodes[0];
    if (nodes.length < 2) return 'Add at least one action after the trigger.';

    const ids = new Set(nodes.map((n) => n.id));
    const adjacency = new Map();
    const incoming = new Map();
    const pairs = new Set();
    for (const edge of edges || []) {
        if (!ids.has(edge.source) || !ids.has(edge.target)) return 'Workflow has a broken connection.';
        if (edge.source === edge.target) return 'A step cannot connect to itself.';
        const target = nodes.find((n) => n.id === edge.target);
        if (target?.type === 'trigger') return 'Nothing can connect into the trigger.';
        const pair = `${edge.source}:${edge.target}`;
        if (pairs.has(pair)) return 'This connection already exists.';
        pairs.add(pair);
        // DAG: multiple incoming edges allowed (merge points)
        if (!adjacency.has(edge.source)) adjacency.set(edge.source, []);
        adjacency.get(edge.source).push(edge.target);
        incoming.set(edge.target, (incoming.get(edge.target) || 0) + 1);
    }

    if ((incoming.get(trigger.id) || 0) > 0) {
        return 'The trigger must be the root step.';
    }

    // Every non-trigger node must be reachable from the trigger
    const reachable = new Set();
    const dfsReach = (id) => {
        if (reachable.has(id)) return;
        reachable.add(id);
        for (const child of adjacency.get(id) || []) dfsReach(child);
    };
    dfsReach(trigger.id);
    if (![...reachable].some((id) => id !== trigger.id)) {
        return 'Connect at least one action to the trigger.';
    }

    // Cycle detection covers orphan components too. The backend rejects cycles,
    // but doing it here makes the correction immediate on the canvas.
    const visiting = new Set();
    const visited = new Set();
    const visit = (id) => {
        if (visiting.has(id)) return true;
        if (visited.has(id)) return false;
        visiting.add(id);
        for (const child of adjacency.get(id) || []) {
            if (visit(child)) return true;
        }
        visiting.delete(id);
        visited.add(id);
        return false;
    };
    if (nodes.some((node) => visit(node.id))) return 'Workflow connections cannot contain a cycle.';
    return null;
}

// graphInfoForNode removed — topology is now sent as explicit edges[].
// The backend no longer uses parentStepId / branchKey.

/**
 * Validates a node as fully configured for a strict save.
 * Returns an error string if invalid, or null if valid.
 *
 * @param {Object} node
 * @param {number} index          — 0-based position in the node list
 * @param {Array}  catalogApps    — list from /apps
 * @param {Object} appDetailsByKey
 * @returns {string|null}
 */
export function validateNodeForSave(node, index, catalogApps, appDetailsByKey) {
    if (index === 0 && node.type !== 'trigger')
        return 'The first step must be a trigger.';
    if (index > 0 && node.type === 'trigger')
        return 'Only the first step can be a trigger.';

    if (!node.data?.appKey)
        return `Step ${index + 1}: select an app.`;

    const appInfo = catalogApps.find((a) => a.appKey === node.data.appKey);
    const needsAuth = appInfo?.authType !== 'NONE';
    if (needsAuth && !node.data?.connectionId)
        return `Step ${index + 1}: select a connected account.`;

    const stepType = node.type === 'trigger' ? 'TRIGGER' : 'ACTION';
    const opKey = stepType === 'TRIGGER'
        ? (node.data.triggerKey || node.data.actionKey)
        : node.data.actionKey;
    if (!opKey)
        return `Step ${index + 1}: select a ${stepType === 'TRIGGER' ? 'trigger event' : 'action'}.`;

    const detail = appDetailsByKey[node.data.appKey];
    const defs = stepType === 'TRIGGER'
        ? (Array.isArray(detail?.triggers) ? detail.triggers : [])
        : (Array.isArray(detail?.actions) ? detail.actions : []);
    const def = defs.find((d) => (d.triggerKey || d.actionKey) === opKey);
    const schemaFields = parseConfigSchema(def?.configSchema || {});
    const config = node.data.configuration || {};
    for (const field of schemaFields) {
        if (!field.required) continue;
        const value = config[field.key];
        if (value == null || String(value).trim() === '')
            return `Step ${index + 1}: '${field.label || field.key}' is required.`;
    }

    return null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Step DTO → React Flow Nodes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts an ordered array of backend step DTOs into React Flow nodes.
 * Also returns the edges connecting them in sequence.
 *
 * @param {Array}   steps      — sorted step responses from backend
 * @param {boolean} vertical   — layout orientation
 * @returns {{ nodes: Array, edges: Array }}
 */
export function stepsToGraph(steps, backendEdges = [], vertical = false) {
    const sorted = [...steps].sort((a, b) => (a.order ?? 0) - (b.order ?? 0));

    const nodes = sorted.map((s, idx) => ({
        id: s.id,
        type: s.type === 'TRIGGER' ? 'trigger' : 'action',
        position: vertical
            ? { x: 250, y: 60 + idx * 220 }
            : { x: 120 + idx * 330, y: 200 },
        data: {
            stepIndex: idx + 1,
            label: s.name,
            appKey: s.appKey,
            app: s.appKey,
            appName: s.appKey,
            actionKey: s.actionKey,
            action: s.actionKey,
            actionName: s.type !== 'TRIGGER' ? s.name : undefined,
            triggerKey: s.type === 'TRIGGER' ? s.actionKey : undefined,
            triggerName: s.type === 'TRIGGER' ? s.name : undefined,
            operationKey: s.actionKey,
            connectionId: s.connectionId || null,
            configuration: s.configuration,
            _backendId: s.id,
            _vertical: vertical,
        },
    }));

    // Build edges from backend edge data (preferred), fall back to linear chain
    let edges;
    if (backendEdges && backendEdges.length > 0) {
        const nodeIds = new Set(nodes.map((n) => n.id));
        edges = backendEdges
            .filter((e) => nodeIds.has(e.sourceStepId) && nodeIds.has(e.targetStepId))
            .map((e) => makeEdge(e.sourceStepId, e.targetStepId, e.sourceHandle, e.targetHandle));
    } else {
        // Fallback: linear chain (no edges returned by backend yet)
        edges = nodes.slice(0, -1).map((n, idx) => makeEdge(n.id, nodes[idx + 1].id));
    }

    return { nodes, edges };
}

/**
 * Creates a single React Flow edge between two node IDs.
 */
export function makeEdge(sourceId, targetId, sourceHandle, targetHandle) {
    return {
        id: `e${sourceId}-${targetId}`,
        source: sourceId,
        target: targetId,
        sourceHandle: sourceHandle || 'out',
        targetHandle: targetHandle || 'in',
        type: 'deleteable',
        animated: false,
        style: { stroke: 'var(--border-secondary)', strokeWidth: 2 },
        markerEnd: {
            type: 'arrowclosed',
            width: 16,
            height: 16,
            color: 'var(--text-tertiary)',
        },
    };
}
