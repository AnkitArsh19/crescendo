import { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate, useOutletContext, useParams, useBeforeUnload } from 'react-router-dom';
import { workflowApi, stepApi, resourceApi, stepTestApi } from '../../api/workflowApi';
import { appCatalogApi } from '../../api/appCatalogApi';
import { connectionsApi } from '../../api/connectionsApi';
import useWorkflowStore from '../../store/workflowStore';
import useToastStore from '../../store/toastStore';
import ConfigPanelBody from './ConfigPanelBody';
import AppBrowserModal from './nodes/AppBrowserModal';
import { parseConfigSchema, toPersistedConfig, stepsToGraph, makeEdge } from '../../workflow/workflowGraphSerializer';
import { createDraftStore } from '../../workflow/workflowDraftStore';
import { createSaveCoordinator } from '../../workflow/workflowSaveCoordinator';
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
        data: { label: 'Select Trigger', stepIndex: 1, _vertical: vertical },
    },
    {
        id: '2',
        type: 'action',
        position: { x: vertical ? 250 : 450, y: vertical ? 280 : 200 },
        data: { label: 'Select Action', stepIndex: 2, _vertical: vertical },
    },
];



const makeDefaultEdge = (vertical) => makeEdge('1', '2', vertical);

/* Hardcoded app/trigger/action lists removed — all data comes from appCatalogApi and appDetailsByKey */

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
    const [isDirty, setIsDirty] = useState(false);
    const autosaveTimerRef = useRef(null);
    const lastAutosaveSignatureRef = useRef('');

    // ── Undo history ──
    // Each entry: { nodes, edges }. Max 50 entries.
    const historyRef = useRef([]);
    const historyIndexRef = useRef(-1);
    const [canUndo, setCanUndo] = useState(false);
    // Prevent history pushes during undo/redo restores
    const isRestoringRef = useRef(false);

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
        if (!appKey || stateRefs.current.appDetailsByKey[appKey]) return;
        try {
            const detail = await appCatalogApi.get(appKey);
            setAppDetailsByKey((prev) => ({ ...prev, [appKey]: detail }));
        } catch {
            // Non-fatal; UI will show fallback labels.
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const stateRefs = useRef({ nodes, workflowId, workflowName, catalogApps, appDetailsByKey });
    useEffect(() => {
        stateRefs.current = { nodes, workflowId, workflowName, catalogApps, appDetailsByKey };
    }, [nodes, workflowId, workflowName, catalogApps, appDetailsByKey]);

    const draftRef = useRef(null);
    if (!draftRef.current) draftRef.current = createDraftStore();

    const saveCoordinatorRef = useRef(null);
    useEffect(() => {
        saveCoordinatorRef.current = createSaveCoordinator({
            getNodes: () => stateRefs.current.nodes,
            getWorkflowId: () => stateRefs.current.workflowId,
            getWorkflowName: () => stateRefs.current.workflowName,
            getCatalogApps: () => stateRefs.current.catalogApps,
            getAppDetailsByKey: () => stateRefs.current.appDetailsByKey,
            draft: draftRef.current,
            onWorkflowCreated: (id) => setWorkflowId(id),
            onNodeSaved: (nodeId, backendId) => {
                setNodes((nds) => nds.map((n) => n.id === nodeId ? { ...n, data: { ...n.data, _backendId: backendId } } : n));
            },
            onSaveStart: () => { setIsSaving(true); setSaveError(null); },
            onSaveSuccess: (time) => { setIsSaving(false); setSavedAt(time); setIsDirty(false); },
            onSaveError: (msg) => { setIsSaving(false); setSaveError(msg); },
            onDirtyChange: (dirty) => setIsDirty(dirty),
            onAppDetailLoaded: (key, detail) => setAppDetailsByKey((prev) => ({ ...prev, [key]: detail }))
        });
        return () => saveCoordinatorRef.current?.destroy();
    }, [setNodes]);

    const handleSave = useCallback(async () => {
        return await saveCoordinatorRef.current?.saveManual();
    }, []);

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
                    const { nodes: loadedNodes, edges: loadedEdges } = stepsToGraph(steps, vertical);
                    setNodes(loadedNodes);
                    setEdges(loadedEdges);

                    // Bump nodeId to avoid collisions
                    nodeId = Math.max(nodeId, loadedNodes.length + 10);

                    // Pre-fetch app details for every step so the config panel
                    // can render trigger/action options and configSchema immediately
                    // without waiting for the user to open the panel first.
                    const uniqueAppKeys = [...new Set(steps.map((s) => s.appKey).filter(Boolean))];
                    await Promise.all(uniqueAppKeys.map((appKey) => ensureAppDetail(appKey)));


                } else {
                    // Workflow exists but has no steps yet — show default placeholder nodes
                    setNodes(makeDefaultNodes(vertical));
                    setEdges([makeDefaultEdge(vertical)]);
                }
            } catch {
                // If loading fails completely, show default nodes
            } finally {
                setIsLoadingWorkflow(false);
            }
        })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [routeWorkflowId]);

    // Handle naming modal — just set name and close. Workflow is created on first save.
    // IMPORTANT: do NOT call workflowApi.create() here — it would create a duplicate
    // workflow if autosave fires before the state update propagates.
    const handleCreateWorkflow = () => {
        const name = nameInput.trim() || 'Untitled';
        setWorkflowName(name);
        setShowNamingModal(false);
    };

    const handleCancelCreate = () => {
        navigate('/dashboard');
    };

    // ── Undo history helpers ──
    const pushHistory = useCallback((currentNodes, currentEdges) => {
        if (isRestoringRef.current) return;
        const snapshot = {
            nodes: JSON.parse(JSON.stringify(currentNodes)),
            edges: JSON.parse(JSON.stringify(currentEdges)),
        };
        // Truncate redo stack
        const newHistory = historyRef.current.slice(0, historyIndexRef.current + 1);
        newHistory.push(snapshot);
        // Limit to 50 entries
        if (newHistory.length > 50) newHistory.shift();
        historyRef.current = newHistory;
        historyIndexRef.current = newHistory.length - 1;
        setCanUndo(historyIndexRef.current > 0);
        setIsDirty(true);
        saveCoordinatorRef.current?.saveAuto();
    }, []);

    const handleUndo = useCallback(() => {
        if (historyIndexRef.current <= 0) return;
        historyIndexRef.current -= 1;
        const snapshot = historyRef.current[historyIndexRef.current];
        if (!snapshot) return;
        isRestoringRef.current = true;
        setNodes(snapshot.nodes);
        setEdges(snapshot.edges);
        setCanUndo(historyIndexRef.current > 0);
        // Re-sync configNode if it still exists
        setConfigNode((prev) => {
            if (!prev) return null;
            const restored = snapshot.nodes.find(n => n.id === prev.id);
            return restored || null;
        });
        setTimeout(() => { isRestoringRef.current = false; }, 0);
        useToastStore.getState().addToast('Undone', 'info', 1500);
    }, [setNodes, setEdges]);

    // ── Keyboard shortcut: Ctrl/Cmd+Z → undo ──
    useEffect(() => {
        const handler = (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
                e.preventDefault();
                handleUndo();
            }
        };
        window.addEventListener('keydown', handler);
        return () => window.removeEventListener('keydown', handler);
    }, [handleUndo]);

    const historyTimeoutRef = useRef(null);

    // ── Update node data from config panel (defined before handleSave to avoid TDZ) ──
    const updateNodeData = useCallback(
        (nodeId, newData, { skipHistory = false } = {}) => {
            setNodes((nds) => {
                const updated = nds.map((n) =>
                    n.id === nodeId ? { ...n, data: { ...n.data, ...newData } } : n
                );
                draftRef.current?.markChanged(nodeId);
                if (!skipHistory) {
                    if (historyTimeoutRef.current) clearTimeout(historyTimeoutRef.current);
                    historyTimeoutRef.current = setTimeout(() => {
                        pushHistory(updated, edges);
                    }, 500);
                }
                return updated;
            });
            setConfigNode((prev) =>
                prev && prev.id === nodeId ? { ...prev, data: { ...prev.data, ...newData } } : prev
            );
        },
        [setNodes, pushHistory, edges]
    );

    // ── Save workflow + steps to backend ──


    const { activateWorkflow: storeActivateWorkflow } = useWorkflowStore();

    const handleRunWorkflow = useCallback(async () => {
        if (isRunning) return;
        setIsRunning(true);
        setSaveError(null);

        // Pre-validate: check trigger-first model before even trying to save
        if (nodes.length === 0) {
            useToastStore.getState().addToast('Add at least one trigger and one action before running.', 'error');
            setIsRunning(false);
            return;
        }
        if (nodes[0].type !== 'trigger') {
            useToastStore.getState().addToast('The first step must be a trigger.', 'error');
            setIsRunning(false);
            return;
        }
        const triggerCount = nodes.filter(n => n.type === 'trigger').length;
        if (triggerCount > 1) {
            useToastStore.getState().addToast('Only one trigger is allowed per workflow. Remove duplicate triggers.', 'error');
            setIsRunning(false);
            return;
        }
        if (nodes.length < 2) {
            useToastStore.getState().addToast('Add at least one action step after the trigger.', 'error');
            setIsRunning(false);
            return;
        }

        try {
            const id = await saveCoordinatorRef.current?.saveManual();
            if (!id) return;

            // Use the Zustand store method so in-memory state stays in sync
            // when the user navigates back to the Workflows list page.
            await storeActivateWorkflow(id);
            useToastStore.getState().addToast('Workflow activated successfully!', 'success');
            setSavedAt(Date.now());
        } catch (err) {
            const msg = err.response?.data?.message || 'Activation failed';
            setSaveError(msg);
            useToastStore.getState().addToast(msg, 'error');
        } finally {
            setIsRunning(false);
        }
    }, [isRunning, handleSave, storeActivateWorkflow, nodes]);



    // Handle editable name
    const handleNameSubmit = () => {
        setEditingName(false);
        if (!workflowName.trim()) setWorkflowName('Untitled');
    };

    // Connect edges
    const onConnect = useCallback(
        (params) =>
            setEdges((eds) => {
                const newEdges = addEdge(
                    {
                        ...params,
                        sourceHandle: params.sourceHandle || 'out',
                        targetHandle: params.targetHandle || 'in',
                        type: vertical ? 'smoothstep' : 'default',
                        animated: true,
                        style: { stroke: 'var(--border-secondary)', strokeWidth: 2 },
                        markerEnd: { type: MarkerType.ArrowClosed, width: 16, height: 16, color: 'var(--text-tertiary)' },
                    },
                    eds
                );
                pushHistory(nodes, newEdges);
                return newEdges;
            }),
        [setEdges, vertical, pushHistory, nodes]
    );

    // Add a blank action node — auto-connects to last node when called without position
    const addActionNode = useCallback(
        (position) => {
            const id = String(nodeId++);
            let lastNodeId = null;
            let updatedNodes = null;
            setNodes((nds) => {
                // Find the last node in the chain to auto-position and auto-connect
                const lastNode = nds.length > 0 ? nds[nds.length - 1] : null;
                lastNodeId = lastNode?.id || null;
                const newNode = {
                    id,
                    type: 'action',
                    position: position || {
                        // In vertical mode, keep same X as other nodes (250) for straight edges
                        x: vertical ? 250 : (lastNode ? lastNode.position.x + 330 : 250),
                        y: lastNode ? lastNode.position.y + (vertical ? 220 : 0) : 200,
                    },
                    data: { label: 'New Action', stepIndex: nds.length + 1, _vertical: vertical },
                };
                updatedNodes = [...nds, newNode];
                return updatedNodes;
            });
            // Auto-connect to the last node if no explicit position was given
            if (!position && lastNodeId) {
                setEdges((eds) => {
                    const newEdges = [...eds, makeEdge(lastNodeId, id, vertical)];
                    if (updatedNodes) pushHistory(updatedNodes, newEdges);
                    return newEdges;
                });
            }
            return id;
        },
        [setNodes, setEdges, vertical, pushHistory]
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
                makeEdge(sourceId, newId, vertical),
            ]);
        },
        [nodes, addActionNode, setEdges, vertical]
    );

    // Clear trigger node — reset its data without removing the node
    const clearTriggerNode = useCallback(
        (id) => {
            setNodes((nds) => {
                const updated = nds.map((n) =>
                    n.id === id
                        ? {
                            ...n,
                            data: {
                                label: 'Select Trigger',
                                stepIndex: n.data.stepIndex,
                                _vertical: n.data._vertical,
                                // All config cleared
                                appKey: undefined, appName: undefined, iconUrl: undefined,
                                connectionId: null, actionKey: '', triggerKey: '',
                                triggerType: '', triggerName: '', actionName: '',
                                configuration: {}, _backendId: n.data._backendId,
                            },
                        }
                        : n
                );
                draftRef.current?.markChanged(id);
                pushHistory(updated, edges);
                return updated;
            });
            setConfigNode(null);
            useToastStore.getState().addToast('Trigger cleared. Reconfigure it to continue.', 'info');
        },
        [setNodes, edges, pushHistory]
    );

    // Delete node (action nodes only — trigger nodes use clearTriggerNode)
    const deleteNode = useCallback(
        (id) => {
            const nodeToDelete = nodes.find(n => n.id === id);
            // Guard: never hard-delete the trigger node — clear it instead
            if (nodeToDelete?.type === 'trigger') {
                clearTriggerNode(id);
                return;
            }
            if (nodeToDelete?.data?._backendId) {
                draftRef.current?.markDeleted(nodeToDelete.data._backendId);
            }
            setNodes((nds) => {
                const remaining = nds.filter((n) => n.id !== id);
                if (remaining.length === 0) {
                    const defaults = makeDefaultNodes(vertical);
                    pushHistory(defaults, [makeDefaultEdge(vertical)]);
                    return defaults;
                }
                const reindexed = remaining.map((n, idx) => ({
                    ...n,
                    data: { ...n.data, stepIndex: idx + 1, _vertical: vertical },
                }));
                pushHistory(reindexed, edges.filter((e) => e.source !== id && e.target !== id));
                return reindexed;
            });
            setEdges((eds) => eds.filter((e) => e.source !== id && e.target !== id));
            if (configNode?.id === id) setConfigNode(null);
            setContextMenu(null);
        },
        [setNodes, setEdges, configNode, vertical, nodes, edges, pushHistory, clearTriggerNode]
    );

    // Node click → open config panel (but NOT if clicking a handle)
    const onNodeClick = useCallback(
        (event, node) => {
            // Don't open config panel if clicking on a handle
            const target = event.target;
            if (target.closest('.react-flow__handle')) return;
            setConfigNode(node);
            setContextMenu(null);
        },
        []
    );

    // Double-click on node → also open config panel (same behavior)
    const onNodeDoubleClick = useCallback(
        (event, node) => {
            const target = event.target;
            if (target.closest('.react-flow__handle')) return;
            setConfigNode(node);
            setContextMenu(null);
        },
        []
    );

    // Edge click → select the edge (allows deletion with backspace/delete)
    const onEdgeClick = useCallback(
        (event, edge) => {
            event.stopPropagation();
            setContextMenu(null);
        },
        []
    );

    // Edge double-click → delete the edge
    const onEdgeDoubleClick = useCallback(
        (event, edge) => {
            event.stopPropagation();
            setEdges((eds) => eds.filter((e) => e.id !== edge.id));
        },
        [setEdges]
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
        // Rearrange existing nodes and update _vertical in data
        // In vertical mode all nodes share x=250 so edges go straight down
        setNodes((nds) => {
            const rearranged = nds.map((n, i) => ({
                ...n,
                position: {
                    x: nextVertical ? 250 : 120 + i * 330,
                    y: nextVertical ? 60 + i * 220 : 200,
                },
                data: { ...n.data, _vertical: nextVertical },
            }));
            return rearranged;
        });
        // Update Edge types — smoothstep for vertical (orthogonal), default (bezier) for horizontal
        setEdges((eds) =>
            eds.map((e) => ({
                ...e,
                type: nextVertical ? 'smoothstep' : 'default',
                sourceHandle: 'out',
                targetHandle: 'in',
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

    // ── Initial history snapshot after load ──
    useEffect(() => {
        if (isLoadingWorkflow || historyRef.current.length > 0) return;
        // Push first snapshot once nodes are ready
        if (nodes.length > 0) {
            pushHistory(nodes, edges);
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isLoadingWorkflow]);

    // ── Warn on browser tab close if unsaved ──
    useBeforeUnload(
        useCallback((event) => {
            if (isDirty) {
                event.preventDefault();
                // Attempt a fire-and-forget background save using keepalive
                saveCoordinatorRef.current?.saveExit({ keepalive: true });
            }
        }, [isDirty])
    );

    // ── Save on component unmount (SPA navigation away) ──
    const isDirtyRef = useRef(isDirty);
    useEffect(() => { isDirtyRef.current = isDirty; }, [isDirty]);
    useEffect(() => {
        return () => {
            if (isDirtyRef.current) {
                // Fire-and-forget save on unmount
                saveCoordinatorRef.current?.saveExit();
            }
        };
    }, []);

    // Close context menu on outside click
    useEffect(() => {
        const handler = () => setContextMenu(null);
        if (contextMenu) window.addEventListener('click', handler);
        return () => window.removeEventListener('click', handler);
    }, [contextMenu]);

    // ── OAuth popup postMessage listener ──
    // Handles the 'oauth-connected' message sent by the OAuth callback page.
    // Kept here (not in ConfigPanelBody) because `connections` state lives here.
    useEffect(() => {
        const handler = (event) => {
            if (event.data?.type !== 'oauth-connected') return;
            const addToast = useToastStore.getState().addToast;
            const isReconnect = event.data.reconnect;
            addToast(
                isReconnect
                    ? `Reconnected: ${event.data.connectionName || event.data.appKey}`
                    : `Connected: ${event.data.connectionName || event.data.appKey}`,
                'success'
            );
            // Refresh connections list so the dropdown updates immediately
            connectionsApi.list().then((conns) => {
                setConnections(Array.isArray(conns) ? conns : []);
                // Auto-select the newly created connection on the current config node
                if (!isReconnect && event.data.connectionId && configNode) {
                    updateNodeData(configNode.id, {
                        connectionId: event.data.connectionId,
                        account: event.data.connectionId,
                        accountName: event.data.connectionName || '',
                    });
                }
            }).catch(() => {});
        };
        window.addEventListener('message', handler);
        return () => window.removeEventListener('message', handler);
    }, [configNode, updateNodeData]);

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
    const selectedTriggerOptions = Array.isArray(selectedAppDetail?.triggers) ? selectedAppDetail.triggers : [];
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
                    <button
                        className="canvas-tb-btn"
                        title="Undo (Ctrl+Z)"
                        onClick={handleUndo}
                        disabled={!canUndo}
                        data-tooltip="Undo"
                    >
                        <HiOutlineReply />
                    </button>
                    <div className="canvas-tb-divider" />
                    <button
                        className="canvas-tb-btn"
                        title="Add Action Step"
                        data-tooltip="Add Step"
                        onClick={() => addActionNode()}
                    >
                        <HiPlus />
                    </button>
                    <button
                        className="canvas-tb-btn"
                        title={vertical ? 'Switch to Horizontal Layout' : 'Switch to Vertical Layout'}
                        data-tooltip={vertical ? 'Horizontal' : 'Vertical'}
                        onClick={toggleOrientation}
                    >
                        {vertical ? <HiOutlineSwitchHorizontal /> : <HiOutlineSwitchVertical />}
                    </button>
                    <div className="canvas-tb-divider" />
                    <button
                        className="canvas-tb-btn"
                        title="Save workflow (Ctrl+S)"
                        data-tooltip="Save"
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
                        title="Save and run this workflow"
                        data-tooltip="Run"
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
                    onNodeDragStop={() => pushHistory(nodes, edges)}
                    onNodeClick={onNodeClick}
                    onNodeDoubleClick={onNodeDoubleClick}
                    onNodeContextMenu={onNodeContextMenu}
                    onEdgeClick={onEdgeClick}
                    onEdgeDoubleClick={onEdgeDoubleClick}
                    onPaneClick={onPaneClick}
                    onDragOver={onDragOver}
                    onDrop={onDrop}
                    nodeTypes={nodeTypes}
                    defaultViewport={{ x: 200, y: 150, zoom: 0.75 }}
                    snapToGrid
                    snapGrid={[16, 16]}
                    proOptions={{ hideAttribution: true }}
                    defaultEdgeOptions={{ animated: true }}
                    connectionMode="loose"
                    connectionRadius={30}
                    deleteKeyCode={['Backspace', 'Delete']}
                    edgesReconnectable
                >
                    <Background variant={BackgroundVariant.Dots} gap={18} size={1.6} color="var(--dot-color-bright)" />
                    <Controls showInteractive={false} position="bottom-left">
                        <button
                            className="react-flow__controls-button"
                            onClick={handleSave}
                            title="Save Workflow"
                            disabled={isSaving}
                            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                        >
                            {isSaving ? <span className="canvas-spinner" style={{ width: 14, height: 14, borderWidth: 2 }} /> : <HiOutlineSave />}
                        </button>
                    </Controls>
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
                                    onClear={() => clearTriggerNode(configNode.id)}
                                    onSaveAndClose={() => {
                                        // Save current step data to backend (silent partial save) then close panel
                                        saveCoordinatorRef.current?.saveAuto();
                                        setConfigNode(null);
                                    }}
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
