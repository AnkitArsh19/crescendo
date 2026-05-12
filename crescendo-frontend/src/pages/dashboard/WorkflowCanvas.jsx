import { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { workflowApi, stepApi, resourceApi, stepTestApi } from '../../api/workflowApi';
import { appCatalogApi } from '../../api/appCatalogApi';
import { connectionsApi } from '../../api/connectionsApi';
import useWorkflowStore from '../../store/workflowStore';
import ConfigPanelBody from './ConfigPanelBody';
import AppBrowserModal from './nodes/AppBrowserModal';
import {
    ReactFlow,
    Background,
    Controls,
    addEdge,
    useNodesState,
    useEdgesState,
    MarkerType,
    BackgroundVariant,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { motion, AnimatePresence } from 'framer-motion';
import {
    HiOutlineSave,
    HiOutlinePlay,
    HiOutlineReply,
    HiOutlinePencil,
    HiCheck,
    HiPlus,
    HiX,
    HiOutlineTrash,
    HiOutlineDuplicate,
    HiOutlineSwitchVertical,
    HiOutlineSwitchHorizontal,
    HiSun,
    HiMoon,
    HiMenuAlt2,
} from 'react-icons/hi';
import WorkflowNode from './nodes/WorkflowNode';
import './WorkflowCanvas.css';

const nodeTypes = { trigger: WorkflowNode, action: WorkflowNode };

const makeDefaultNodes = (vertical) => [
    {
        id: '1',
        type: 'trigger',
        position: { x: vertical ? 250 : 120, y: vertical ? 60 : 200 },
        data: { label: 'Select Trigger', stepIndex: 1 },
    },
    {
        id: '2',
        type: 'action',
        position: { x: vertical ? 250 : 450, y: vertical ? 280 : 200 },
        data: { label: 'Select Action', stepIndex: 2 },
    },
];

const makeDefaultEdge = (vertical) => ({
    id: 'e1-2',
    source: '1',
    target: '2',
    type: vertical ? 'straight' : 'default',
    animated: true,
    style: { stroke: 'var(--border-secondary)', strokeWidth: 2 },
    markerEnd: { type: MarkerType.ArrowClosed, width: 16, height: 16, color: 'var(--text-tertiary)' },
});

/* Hardcoded app/trigger/action lists removed — all data comes from appCatalogApi and appDetailsByKey */

/**
 * Normalizes configSchema from either the new structured List<Map> format
 * or the legacy Map<String, String> hint format.
 */
function parseConfigSchema(configSchema) {
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
            dependsOn: Array.isArray(field.dependsOn) ? field.dependsOn : [],
            options: Array.isArray(field.options) ? field.options : [],
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
            return { key, label: key, required, type, helpText: text, placeholder: '', resourceType: null, dependsOn: [], options: [] };
        });
    }
    return [];
}

function toPersistedConfig(schemaFields, config) {
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
            // If already an array, ensure it's clean
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

let nodeId = 3;

export default function WorkflowCanvas() {
    const navigate = useNavigate();
    const { workflowId: routeWorkflowId } = useParams();
    const { toggleTheme, theme, collapsed, setCollapsed } = useOutletContext();

    // ── Naming modal (only for new workflows) ──
    const [showNamingModal, setShowNamingModal] = useState(!routeWorkflowId);
    const [nameInput, setNameInput] = useState('');

    // ── Persisted workflow id (set after first save) ──
    const [workflowId, setWorkflowId] = useState(routeWorkflowId || null);

    // ── Save state ──
    const [isSaving, setIsSaving] = useState(false);
    const [isRunning, setIsRunning] = useState(false);
    const [savedAt, setSavedAt] = useState(null);
    const [saveError, setSaveError] = useState(null);
    const autosaveTimerRef = useRef(null);
    const lastAutosaveSignatureRef = useRef('');

    // ── Canvas state ──
    const [workflowName, setWorkflowName] = useState('');
    const [editingName, setEditingName] = useState(false);
    const [vertical, setVertical] = useState(false);
    const [nodes, setNodes, onNodesChange] = useNodesState(routeWorkflowId ? [] : makeDefaultNodes(false));
    const [edges, setEdges, onEdgesChange] = useEdgesState(routeWorkflowId ? [] : [makeDefaultEdge(false)]);
    const [reactFlowInstance, setReactFlowInstance] = useState(null);
    const reactFlowWrapper = useRef(null);

    // ── Catalog + connections for real app/account/trigger/action selection ──
    const [catalogApps, setCatalogApps] = useState([]);
    const [connections, setConnections] = useState([]);
    const [appDetailsByKey, setAppDetailsByKey] = useState({});

    // ── Node config panel ──
    const [configNode, setConfigNode] = useState(null);

    // ── App browser modal ──
    const [showAppBrowser, setShowAppBrowser] = useState(false);
    const [appBrowserTarget, setAppBrowserTarget] = useState(null); // node id or 'new'

    // ── Right-click context menu ──
    const [contextMenu, setContextMenu] = useState(null);

    const ensureAppDetail = useCallback(async (appKey) => {
        if (!appKey || appDetailsByKey[appKey]) return;
        try {
            const detail = await appCatalogApi.get(appKey);
            setAppDetailsByKey((prev) => ({ ...prev, [appKey]: detail }));
        } catch {
            // Non-fatal; UI will show fallback labels.
        }
    }, [appDetailsByKey]);

    const getTriggerDefinitionsForApp = useCallback((appKey) => {
        const detail = appDetailsByKey[appKey];
        if (!detail) return [];
        const triggers = Array.isArray(detail.triggers) ? detail.triggers : [];
        if (triggers.length > 0) return triggers;
        const actions = Array.isArray(detail.actions) ? detail.actions : [];
        // Fallback: allow action-based trigger selection when app has no explicit trigger metadata.
        return actions.map((a) => ({
            triggerKey: a.actionKey,
            name: `${a.name} (manual trigger)`,
            description: 'No native trigger metadata yet; uses selected action key for manual/test runs',
            configSchema: a.configSchema || {},
        }));
    }, [appDetailsByKey]);

    const getActionDefinitionsForApp = useCallback((appKey) => {
        const detail = appDetailsByKey[appKey];
        return Array.isArray(detail?.actions) ? detail.actions : [];
    }, [appDetailsByKey]);

    const getNodeOperationKey = useCallback((node) => {
        return node.type === 'trigger'
            ? (node.data?.triggerKey || node.data?.actionKey)
            : node.data?.actionKey;
    }, []);

    const isNodeConfiguredForSave = useCallback((node, index) => {
        if (index === 0 && node.type !== 'trigger') return false;
        if (index > 0 && node.type !== 'action') return false;

        const appKey = node.data?.appKey;
        if (!appKey) return false;

        const opKey = getNodeOperationKey(node);
        if (!opKey) return false;

        const appInfo = catalogApps.find((a) => a.appKey === appKey);
        const needsAuth = appInfo?.authType !== 'NONE';
        if (needsAuth && !node.data?.connectionId) return false;

        const defs = node.type === 'trigger'
            ? getTriggerDefinitionsForApp(appKey)
            : getActionDefinitionsForApp(appKey);
        const def = defs.find((d) => (d.triggerKey || d.actionKey) === opKey);
        const schemaFields = parseConfigSchema(def?.configSchema || {});
        const currentConfig = node.data?.configuration || {};

        return schemaFields.every((field) => {
            if (!field.required) return true;
            const value = currentConfig[field.key];
            return value != null && String(value).trim() !== '';
        });
    }, [catalogApps, getActionDefinitionsForApp, getNodeOperationKey, getTriggerDefinitionsForApp]);

    const getConfiguredPrefixCount = useCallback((allNodes) => {
        let count = 0;
        for (let i = 0; i < allNodes.length; i++) {
            if (!isNodeConfiguredForSave(allNodes[i], i)) break;
            count += 1;
        }
        return count;
    }, [isNodeConfiguredForSave]);

    useEffect(() => {
        Promise.all([appCatalogApi.list(), connectionsApi.list()])
            .then(([apps, conns]) => {
                setCatalogApps(Array.isArray(apps) ? apps : []);
                setConnections(Array.isArray(conns) ? conns : []);
            })
            .catch(() => {
                setCatalogApps([]);
                setConnections([]);
            });
    }, []);

    useEffect(() => {
        if (configNode?.data?.appKey) {
            ensureAppDetail(configNode.data.appKey);
        }
    }, [configNode, ensureAppDetail]);

    // ── Loading guard for edit mode ──
    const [isLoadingWorkflow, setIsLoadingWorkflow] = useState(!!routeWorkflowId);

    // Load existing workflow if editing
    useEffect(() => {
        if (!routeWorkflowId) return;
        setIsLoadingWorkflow(true);

        (async () => {
            try {
                // Fetch workflow metadata
                const wf = await workflowApi.get(routeWorkflowId);
                setWorkflowName(wf.name);

                // Always fetch steps from the dedicated API for reliability
                let steps = [];
                try {
                    steps = await stepApi.list(routeWorkflowId);
                } catch {
                    // Fallback to embedded steps
                    steps = Array.isArray(wf.steps) ? wf.steps : [];
                }

                if (steps.length > 0) {
                    const sortedSteps = [...steps].sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
                    const loaded = sortedSteps.map((s, idx) => ({
                        id: s.id,
                        type: s.type === 'TRIGGER' ? 'trigger' : 'action',
                        position: { x: 120 + idx * 330, y: 200 },
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
                        },
                    }));
                    setNodes(loaded);

                    // Bump nodeId to avoid collisions
                    nodeId = Math.max(nodeId, loaded.length + 10);

                    // Load app details for all steps
                    const uniqueAppKeys = [...new Set(sortedSteps.map((s) => s.appKey).filter(Boolean))];
                    uniqueAppKeys.forEach((appKey) => ensureAppDetail(appKey));

                    if (loaded.length > 1) {
                        const loadedEdges = loaded.slice(0, -1).map((n, idx) => ({
                            id: `e${n.id}-${loaded[idx + 1].id}`,
                            source: n.id,
                            target: loaded[idx + 1].id,
                            type: vertical ? 'straight' : 'default',
                            animated: true,
                            style: { stroke: 'var(--border-secondary)', strokeWidth: 2 },
                            markerEnd: { type: MarkerType.ArrowClosed, width: 16, height: 16, color: 'var(--text-tertiary)' },
                        }));
                        setEdges(loadedEdges);
                    }
                }
            } catch {
                // If loading fails completely, show default nodes
            } finally {
                setIsLoadingWorkflow(false);
            }
        })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [routeWorkflowId]);

    // Handle naming modal — create workflow in backend on confirm
    const handleCreateWorkflow = async () => {
        const name = nameInput.trim() || 'Untitled';
        setWorkflowName(name);
        setShowNamingModal(false);
        // Eagerly create in backend so we have an ID for steps
        try {
            const wf = await workflowApi.create({ name, description: '' });
            setWorkflowId(wf.id);
        } catch {
            // Non-fatal — save will create it anyway
        }
    };

    const handleCancelCreate = () => {
        navigate('/dashboard');
    };

    // Update node data from config panel (defined before handleSave to avoid TDZ)
    const updateNodeData = useCallback(
        (nodeId, newData) => {
            setNodes((nds) =>
                nds.map((n) =>
                    n.id === nodeId ? { ...n, data: { ...n.data, ...newData } } : n
                )
            );
            setConfigNode((prev) =>
                prev && prev.id === nodeId ? { ...prev, data: { ...prev.data, ...newData } } : prev
            );
        },
        [setNodes]
    );

    // ── Save workflow + steps to backend ──
    const handleSave = useCallback(async (options = {}) => {
        const { mode = 'strict' } = options;
        if (isSaving) return;
        setIsSaving(true);
        if (mode === 'strict') setSaveError(null);
        try {
            const normalizedWorkflowName = (workflowName || '').trim() || 'Untitled';
            const configuredPrefixCount = mode === 'partial'
                ? getConfiguredPrefixCount(nodes)
                : nodes.length;
            const nodesToSave = mode === 'partial'
                ? nodes.slice(0, configuredPrefixCount)
                : nodes;

            if (mode === 'strict') {
                if (nodes.length === 0) {
                    setSaveError('Add at least one trigger and one action.');
                    return null;
                }

                // Enforce trigger-first model: first step must be trigger; all others actions.
                for (let idx = 0; idx < nodes.length; idx++) {
                    const node = nodes[idx];
                    if (idx === 0 && node.type !== 'trigger') {
                        setSaveError('The first step must be a trigger.');
                        return null;
                    }
                    if (idx > 0 && node.type !== 'action') {
                        setSaveError('Only the first step can be a trigger.');
                        return null;
                    }
                }
            } else if (nodesToSave.length === 0 && !workflowId) {
                return null;
            }

            // Resolve missing app details before validation.
            for (const node of nodesToSave) {
                if (node.data?.appKey && !appDetailsByKey[node.data.appKey]) {
                    try {
                        const detail = await appCatalogApi.get(node.data.appKey);
                        setAppDetailsByKey((prev) => ({ ...prev, [node.data.appKey]: detail }));
                    } catch {
                        if (mode === 'strict') {
                            setSaveError(`Failed to load app metadata for ${node.data.appKey}`);
                            return null;
                        }
                    }
                }
            }

            let id = workflowId;

            // Keep local UI in sync with normalized persisted name.
            if (normalizedWorkflowName !== workflowName) {
                setWorkflowName(normalizedWorkflowName);
            }

            // 1. Create or update the workflow record
            if (!id) {
                if (nodesToSave.length === 0) return null;
                const wf = await workflowApi.create({
                    name: normalizedWorkflowName,
                    description: '',
                });
                id = wf.id;
                setWorkflowId(id);
            } else if (normalizedWorkflowName !== workflowName) {
                await workflowApi.update(id, { name: normalizedWorkflowName });
            } else if (mode === 'partial' && nodesToSave.length === 0) {
                return id;
            }

            // 2. Upsert each node as a step in order
            for (let idx = 0; idx < nodesToSave.length; idx++) {
                const node = nodesToSave[idx];
                const stepType = node.type === 'trigger' ? 'TRIGGER' : 'ACTION';

                if (!node.data?.appKey) {
                    if (mode === 'strict') {
                        setSaveError(`Step ${idx + 1}: select an app.`);
                        return null;
                    }
                    continue;
                }
                // Only require connectionId for apps that need auth
                const appInfo = catalogApps.find((a) => a.appKey === node.data.appKey);
                const needsAuth = appInfo?.authType !== 'NONE';
                if (needsAuth && !node.data?.connectionId) {
                    if (mode === 'strict') {
                        setSaveError(`Step ${idx + 1}: select a connected account.`);
                        return null;
                    }
                    continue;
                }

                const defs = stepType === 'TRIGGER'
                    ? getTriggerDefinitionsForApp(node.data.appKey)
                    : getActionDefinitionsForApp(node.data.appKey);
                const opKey = stepType === 'TRIGGER'
                    ? (node.data.triggerKey || node.data.actionKey)
                    : node.data.actionKey;

                if (!opKey) {
                    if (mode === 'strict') {
                        setSaveError(`Step ${idx + 1}: select a ${stepType === 'TRIGGER' ? 'trigger event' : 'action'}.`);
                        return null;
                    }
                    continue;
                }

                const def = defs.find((d) => (d.triggerKey || d.actionKey) === opKey);
                const schemaFields = parseConfigSchema(def?.configSchema || {});
                const currentConfig = node.data.configuration || {};
                let missingRequired = false;
                for (const field of schemaFields) {
                    if (!field.required) continue;
                    const value = currentConfig[field.key];
                    if (value == null || String(value).trim() === '') {
                        if (mode === 'strict') {
                            setSaveError(`Step ${idx + 1}: '${field.key}' is required.`);
                            return null;
                        }
                        missingRequired = true;
                        break;
                    }
                }
                if (missingRequired) continue;

                const persistedConfig = toPersistedConfig(schemaFields, currentConfig);
                const payload = {
                    name: node.data.label || (stepType === 'TRIGGER' ? 'Trigger' : 'Action'),
                    type: stepType,
                    actionKey: opKey,
                    appKey: node.data.appKey,
                    connectionId: node.data.connectionId,
                    configuration: persistedConfig,
                };

                if (node.data._backendId) {
                    await stepApi.update(id, node.data._backendId, payload);
                } else {
                    const saved = await stepApi.add(id, payload);
                    updateNodeData(node.id, { _backendId: saved.id });
                }
            }

            if (id) {
                try {
                    const existingSteps = await stepApi.list(id);
                    const currentStepIds = new Set(
                        nodesToSave.map((node) => node.data?._backendId).filter(Boolean)
                    );
                    const staleSteps = Array.isArray(existingSteps)
                        ? existingSteps.filter((step) => !currentStepIds.has(step.id))
                        : [];
                    for (const staleStep of staleSteps) {
                        await stepApi.delete(id, staleStep.id);
                    }
                } catch (cleanupErr) {
                    if (mode === 'strict') {
                        setSaveError(cleanupErr.response?.data?.message || 'Failed to reconcile workflow steps');
                        return null;
                    }
                }
            }

            setSavedAt(Date.now());
            return id;
        } catch (err) {
            if (mode === 'strict') {
                setSaveError(err.response?.data?.message || 'Save failed');
            }
            return null;
        } finally {
            setIsSaving(false);
        }
    }, [
        isSaving,
        workflowId,
        workflowName,
        nodes,
        updateNodeData,
        catalogApps,
        appDetailsByKey,
        getActionDefinitionsForApp,
        getTriggerDefinitionsForApp,
        getConfiguredPrefixCount,
    ]);

    const { activateWorkflow: storeActivateWorkflow } = useWorkflowStore();

    const handleRunWorkflow = useCallback(async () => {
        if (isRunning) return;
        setIsRunning(true);
        setSaveError(null);
        try {
            const id = await handleSave({ mode: 'strict', reason: 'run' });
            if (!id) return;

            // Use the Zustand store method so in-memory state stays in sync
            // when the user navigates back to the Workflows list page.
            await storeActivateWorkflow(id);
            setSavedAt(Date.now());
        } catch (err) {
            setSaveError(err.response?.data?.message || 'Activation failed');
        } finally {
            setIsRunning(false);
        }
    }, [isRunning, handleSave, storeActivateWorkflow]);

    const autosaveSnapshot = useMemo(() => {
        const prefixCount = getConfiguredPrefixCount(nodes);
        const snapshotNodes = nodes.slice(0, prefixCount).map((node) => ({
            id: node.id,
            type: node.type,
            appKey: node.data?.appKey || '',
            actionKey: node.data?.actionKey || '',
            triggerKey: node.data?.triggerKey || '',
            connectionId: node.data?.connectionId || '',
            configuration: node.data?.configuration || {},
            label: node.data?.label || '',
            stepLabel: node.data?.stepLabel || '',
        }));
        return {
            prefixCount,
            signature: JSON.stringify({
                workflowId: workflowId || 'new',
                workflowName: (workflowName || '').trim() || 'Untitled',
                nodes: snapshotNodes,
            }),
        };
    }, [nodes, workflowId, workflowName, getConfiguredPrefixCount]);

    useEffect(() => {
        if (autosaveTimerRef.current) clearTimeout(autosaveTimerRef.current);
        if (isLoadingWorkflow) return; // Don't autosave while loading existing workflow
        if (autosaveSnapshot.prefixCount === 0 && !workflowId) return;
        if (autosaveSnapshot.signature === lastAutosaveSignatureRef.current) return;
        lastAutosaveSignatureRef.current = autosaveSnapshot.signature;

        autosaveTimerRef.current = setTimeout(() => {
            handleSave({ mode: 'partial', reason: 'autosave' });
        }, 800);

        return () => {
            if (autosaveTimerRef.current) clearTimeout(autosaveTimerRef.current);
        };
    }, [autosaveSnapshot, workflowId, handleSave, isLoadingWorkflow]);

    // Handle editable name
    const handleNameSubmit = () => {
        setEditingName(false);
        if (!workflowName.trim()) setWorkflowName('Untitled');
    };

    // Connect edges
    const onConnect = useCallback(
        (params) =>
            setEdges((eds) =>
                addEdge(
                    {
                        ...params,
                        type: vertical ? 'straight' : 'default',
                        animated: true,
                        style: { stroke: 'var(--border-secondary)', strokeWidth: 2 },
                        markerEnd: { type: MarkerType.ArrowClosed, width: 16, height: 16, color: 'var(--text-tertiary)' },
                    },
                    eds
                )
            ),
        [setEdges, vertical]
    );

    // Add a blank action node — auto-connects to last node when called without position
    const addActionNode = useCallback(
        (position) => {
            const id = String(nodeId++);
            let lastNodeId = null;
            setNodes((nds) => {
                // Find the last node in the chain to auto-position and auto-connect
                const lastNode = nds.length > 0 ? nds[nds.length - 1] : null;
                lastNodeId = lastNode?.id || null;
                const newNode = {
                    id,
                    type: 'action',
                    position: position || {
                        x: lastNode ? lastNode.position.x + (vertical ? 0 : 330) : 250,
                        y: lastNode ? lastNode.position.y + (vertical ? 220 : 0) : 200,
                    },
                    data: { label: 'New Action', stepIndex: nds.length + 1 },
                };
                return [...nds, newNode];
            });
            // Auto-connect to the last node if no explicit position was given
            if (!position && lastNodeId) {
                setEdges((eds) => [
                    ...eds,
                    {
                        id: `e${lastNodeId}-${id}`,
                        source: lastNodeId,
                        target: id,
                        type: vertical ? 'straight' : 'default',
                        animated: true,
                        style: { stroke: 'var(--border-secondary)', strokeWidth: 2 },
                        markerEnd: { type: MarkerType.ArrowClosed, width: 16, height: 16, color: 'var(--text-tertiary)' },
                    },
                ]);
            }
            return id;
        },
        [setNodes, setEdges, vertical]
    );

    // Add branch from a node
    const addBranch = useCallback(
        (sourceId) => {
            const sourceNode = nodes.find((n) => n.id === sourceId);
            if (!sourceNode) return;
            const newId = addActionNode({
                x: sourceNode.position.x + (vertical ? 220 : 0),
                y: sourceNode.position.y + (vertical ? 200 : 180),
            });
            setEdges((eds) => [
                ...eds,
                {
                    id: `e${sourceId}-${newId}`,
                    source: sourceId,
                    target: newId,
                    type: vertical ? 'straight' : 'default',
                    animated: true,
                    style: { stroke: 'var(--border-secondary)', strokeWidth: 2 },
                    markerEnd: { type: MarkerType.ArrowClosed, width: 16, height: 16, color: 'var(--text-tertiary)' },
                },
            ]);
        },
        [nodes, addActionNode, setEdges, vertical]
    );

    // Delete node
    const deleteNode = useCallback(
        (id) => {
            setNodes((nds) => {
                const remaining = nds.filter((n) => n.id !== id);
                if (remaining.length === 0) {
                    // No nodes left — return fresh defaults
                    return makeDefaultNodes(vertical);
                }
                // Ensure the first node is always type trigger
                return remaining.map((n, idx) => ({
                    ...n,
                    type: idx === 0 ? 'trigger' : n.type,
                    data: { ...n.data, stepIndex: idx + 1 },
                }));
            });
            setEdges((eds) => eds.filter((e) => e.source !== id && e.target !== id));
            if (configNode?.id === id) setConfigNode(null);
            setContextMenu(null);
        },
        [setNodes, setEdges, configNode, vertical]
    );

    // Node click → open config panel
    const onNodeClick = useCallback(
        (_, node) => {
            setConfigNode(node);
            setContextMenu(null);
        },
        []
    );

    // Right-click on node
    const onNodeContextMenu = useCallback(
        (event, node) => {
            event.preventDefault();
            setContextMenu({
                x: event.clientX,
                y: event.clientY,
                nodeId: node.id,
                nodeType: node.type,
            });
        },
        []
    );

    // Click on pane → close everything
    const onPaneClick = useCallback(() => {
        setContextMenu(null);
    }, []);

    // Orientation toggle
    const toggleOrientation = useCallback(() => {
        const nextVertical = !vertical;
        setVertical(nextVertical);
        // Rearrange existing nodes
        setNodes((nds) =>
            nds.map((n, i) => ({
                ...n,
                position: {
                    x: nextVertical ? 250 : 120 + i * 330,
                    y: nextVertical ? 60 + i * 220 : 200,
                },
            }))
        );
        // Update Edge types
        setEdges((eds) =>
            eds.map((e) => ({
                ...e,
                type: nextVertical ? 'straight' : 'default',
            }))
        );
    }, [vertical, setNodes, setEdges]);

    // Drag-and-drop blank action from Add button
    const onDragOver = useCallback((e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
    }, []);

    const onDrop = useCallback(
        (e) => {
            e.preventDefault();
            const raw = e.dataTransfer.getData('application/reactflow');
            if (!raw) return;
            const bounds = reactFlowWrapper.current.getBoundingClientRect();
            const position = reactFlowInstance.screenToFlowPosition({
                x: e.clientX - bounds.left,
                y: e.clientY - bounds.top,
            });
            addActionNode(position);
        },
        [reactFlowInstance, addActionNode]
    );

    // Close context menu on outside click
    useEffect(() => {
        const handler = () => setContextMenu(null);
        if (contextMenu) window.addEventListener('click', handler);
        return () => window.removeEventListener('click', handler);
    }, [contextMenu]);

    // ──────────────────────────────
    // RENDER
    // ──────────────────────────────

    const connectedAppOptions = Array.from(
        connections.reduce((acc, conn) => {
            if (!conn?.appKey) return acc;
            if (!acc.has(conn.appKey)) {
                const appMeta = catalogApps.find((a) => a.appKey === conn.appKey);
                acc.set(conn.appKey, {
                    appKey: conn.appKey,
                    name: appMeta?.name || conn.appKey,
                });
            }
            return acc;
        }, new Map()).values()
    );

    const selectedAppKey = configNode?.data?.appKey || '';
    const selectedAppDetail = selectedAppKey ? appDetailsByKey[selectedAppKey] : null;
    const selectedConnections = selectedAppKey
        ? connections.filter((c) => c.appKey === selectedAppKey)
        : [];
    const selectedTriggerOptions = selectedAppKey ? getTriggerDefinitionsForApp(selectedAppKey) : [];
    const selectedActionOptions = Array.isArray(selectedAppDetail?.actions) ? selectedAppDetail.actions : [];

    // Naming modal
    if (showNamingModal) {
        return (
            <div className="canvas-naming-overlay">
                <motion.div
                    className="canvas-naming-modal"
                    initial={{ opacity: 0, scale: 0.95, y: 10 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    transition={{ duration: 0.25 }}
                >
                    <button className="canvas-naming-close" onClick={handleCancelCreate}>
                        <HiX />
                    </button>
                    <h2 className="canvas-naming-title">Create New Workflow</h2>
                    <p className="canvas-naming-desc">
                        Give your workflow a name, or continue with the default.
                    </p>
                    <input
                        className="canvas-naming-input"
                        placeholder="e.g. Email Onboarding, Data Sync…"
                        value={nameInput}
                        onChange={(e) => setNameInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleCreateWorkflow()}
                        autoFocus
                    />
                    <div className="canvas-naming-actions">
                        <button className="canvas-naming-btn secondary" onClick={handleCancelCreate}>
                            Cancel
                        </button>
                        <button className="canvas-naming-btn primary" onClick={handleCreateWorkflow}>
                            Create Workflow
                        </button>
                    </div>
                </motion.div>
            </div>
        );
    }

    if (isLoadingWorkflow) {
        return (
            <div className="canvas-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                <div style={{ textAlign: 'center', color: 'var(--text-secondary)' }}>
                    <div style={{ fontSize: '1.1rem', fontWeight: 500, marginBottom: 8 }}>Loading workflow…</div>
                    <div style={{ fontSize: '0.85rem', opacity: 0.6 }}>Fetching steps and configuration</div>
                </div>
            </div>
        );
    }

    return (
        <div className="canvas-page">
            {/* ── Top bar ── */}
            <div className="canvas-topbar">
                <div className="canvas-topbar-left">
                    <button
                        className="canvas-topbar-icon-btn"
                        onClick={() => setCollapsed(!collapsed)}
                        title="Toggle sidebar"
                    >
                        <HiMenuAlt2 />
                    </button>
                    {editingName ? (
                        <div className="canvas-name-edit">
                            <input
                                className="canvas-name-input"
                                value={workflowName}
                                onChange={(e) => setWorkflowName(e.target.value)}
                                onBlur={handleNameSubmit}
                                onKeyDown={(e) => e.key === 'Enter' && handleNameSubmit()}
                                autoFocus
                            />
                            <button className="canvas-name-save" onClick={handleNameSubmit}>
                                <HiCheck />
                            </button>
                        </div>
                    ) : (
                        <div className="canvas-name-display" onClick={() => setEditingName(true)}>
                            <span className="canvas-name-text">{workflowName}</span>
                            <HiOutlinePencil className="canvas-name-edit-icon" />
                        </div>
                    )}
                </div>

                <div className="canvas-topbar-center">
                    <button className="canvas-tb-btn" title="Undo"><HiOutlineReply /></button>
                    <div className="canvas-tb-divider" />
                    <button className="canvas-tb-btn" title="Add Action" onClick={() => addActionNode()}>
                        <HiPlus />
                    </button>
                    <button
                        className="canvas-tb-btn"
                        title={vertical ? 'Switch to Horizontal' : 'Switch to Vertical'}
                        onClick={toggleOrientation}
                    >
                        {vertical ? <HiOutlineSwitchHorizontal /> : <HiOutlineSwitchVertical />}
                    </button>
                    <div className="canvas-tb-divider" />
                    <button
                        className="canvas-tb-btn"
                        title="Save"
                        onClick={handleSave}
                        disabled={isSaving}
                    >
                        {isSaving ? <span className="canvas-spinner" /> : <HiOutlineSave />}
                        {savedAt && !isSaving && !saveError ? ' Saved' : ''}
                    </button>
                    {saveError && (
                        <span className="canvas-save-error" title={saveError}>⚠</span>
                    )}
                    <button
                        className="canvas-tb-btn canvas-tb-btn-primary"
                        title="Run"
                        onClick={handleRunWorkflow}
                        disabled={isRunning || isSaving}
                    >
                        {isRunning ? <span className="canvas-spinner" /> : <HiOutlinePlay />} Run
                    </button>
                </div>

                <div className="canvas-topbar-right">
                    <button className="canvas-topbar-icon-btn" onClick={toggleTheme} title="Toggle theme">
                        {theme === 'dark' ? <HiSun /> : <HiMoon />}
                    </button>
                </div>
            </div>

            {/* ── Canvas ── */}
            <div ref={reactFlowWrapper} className="canvas-flow-wrap">
                <ReactFlow
                    nodes={nodes}
                    edges={edges}
                    onNodesChange={onNodesChange}
                    onEdgesChange={onEdgesChange}
                    onConnect={onConnect}
                    onInit={setReactFlowInstance}
                    onNodeClick={onNodeClick}
                    onNodeContextMenu={onNodeContextMenu}
                    onPaneClick={onPaneClick}
                    onDragOver={onDragOver}
                    onDrop={onDrop}
                    nodeTypes={nodeTypes}
                    defaultViewport={{ x: 200, y: 150, zoom: 0.75 }}
                    snapToGrid
                    snapGrid={[16, 16]}
                    proOptions={{ hideAttribution: true }}
                    defaultEdgeOptions={{ animated: true }}
                >
                    <Background variant={BackgroundVariant.Dots} gap={18} size={1.6} color="var(--dot-color-bright)" />
                    <Controls showInteractive={false} position="bottom-left" />
                </ReactFlow>

                {/* ── Right-click context menu ── */}
                <AnimatePresence>
                    {contextMenu && (
                        <motion.div
                            className="canvas-context-menu"
                            style={{ top: contextMenu.y - 60, left: contextMenu.x }}
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.9 }}
                            transition={{ duration: 0.12 }}
                            onClick={(e) => e.stopPropagation()}
                        >
                            <button
                                className="canvas-ctx-item"
                                onClick={() => { addBranch(contextMenu.nodeId); setContextMenu(null); }}
                            >
                                <HiPlus /> Add Branch
                            </button>
                            <button
                                className="canvas-ctx-item"
                                onClick={() => { /* duplicate logic */ setContextMenu(null); }}
                            >
                                <HiOutlineDuplicate /> Duplicate
                            </button>
                            <div className="canvas-ctx-divider" />
                            <button
                                className="canvas-ctx-item danger"
                                onClick={() => deleteNode(contextMenu.nodeId)}
                            >
                                <HiOutlineTrash /> Delete
                            </button>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>

            {/* ── Node Config Modal (Zapier-style centered overlay) ── */}
            {createPortal(
                <AnimatePresence>
                    {configNode && (
                        <>
                            <motion.div
                                className="canvas-config-backdrop"
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                exit={{ opacity: 0 }}
                                transition={{ duration: 0.2 }}
                                onClick={() => setConfigNode(null)}
                            />
                            <motion.div
                                className="canvas-config-modal"
                                initial={{ opacity: 0, y: 30 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0, y: 30 }}
                                transition={{ type: 'spring', damping: 28, stiffness: 350 }}
                            >
                                <ConfigPanelBody
                                    configNode={configNode}
                                    updateNodeData={updateNodeData}
                                    connectedAppOptions={connectedAppOptions}
                                    selectedConnections={selectedConnections}
                                    selectedTriggerOptions={selectedTriggerOptions}
                                    selectedActionOptions={selectedActionOptions}
                                    ensureAppDetail={ensureAppDetail}
                                    appDetailsByKey={appDetailsByKey}
                                    catalogApps={catalogApps}
                                    allNodes={nodes}
                                    allConnections={connections}
                                    onOpenAppBrowser={() => {
                                        setAppBrowserTarget(configNode.id);
                                        setShowAppBrowser(true);
                                    }}
                                    onClose={() => setConfigNode(null)}
                                    onDelete={() => deleteNode(configNode.id)}
                                    onNavigate={(dir) => {
                                        const idx = nodes.findIndex(n => n.id === configNode.id);
                                        if (dir === 'prev' && idx > 0) setConfigNode(nodes[idx - 1]);
                                        if (dir === 'next' && idx < nodes.length - 1) setConfigNode(nodes[idx + 1]);
                                    }}
                                    nodeCount={nodes.length}
                                    nodeIndex={nodes.findIndex(n => n.id === configNode.id)}
                                />
                            </motion.div>
                        </>
                    )}
                </AnimatePresence>,
                document.body
            )}

            {/* ── App Browser Modal ── */}
            <AnimatePresence>
                {showAppBrowser && (
                    <AppBrowserModal
                        apps={catalogApps}
                        connections={connections}
                        title={appBrowserTarget === 'new' ? 'Choose an App' : 'Select App'}
                        onSelect={async (app) => {
                            setShowAppBrowser(false);
                            if (appBrowserTarget === 'new') {
                                // Add a new action node with the selected app
                                const newId = addActionNode();
                                updateNodeData(newId, {
                                    appKey: app.appKey,
                                    app: app.appKey,
                                    appName: app.name,
                                    iconUrl: app.iconUrl || null,
                                    label: app.name,
                                });
                                await ensureAppDetail(app.appKey);
                            } else if (appBrowserTarget && configNode) {
                                // Update existing node
                                updateNodeData(appBrowserTarget, {
                                    appKey: app.appKey,
                                    app: app.appKey,
                                    appName: app.name,
                                    iconUrl: app.iconUrl || null,
                                    label: app.name,
                                    connectionId: null,
                                    actionKey: '',
                                    triggerKey: '',
                                    triggerType: '',
                                    action: '',
                                    configuration: {},
                                });
                                await ensureAppDetail(app.appKey);
                            }
                            setAppBrowserTarget(null);
                        }}
                        onClose={() => {
                            setShowAppBrowser(false);
                            setAppBrowserTarget(null);
                        }}
                    />
                )}
            </AnimatePresence>
        </div>
    );
}
