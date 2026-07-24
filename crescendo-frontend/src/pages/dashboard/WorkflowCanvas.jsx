/* eslint-disable react-hooks/refs */
/* eslint-disable react-hooks/set-state-in-effect */
/* eslint-disable react-hooks/rules-of-hooks */
import { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate, useOutletContext, useParams, useBeforeUnload } from 'react-router-dom';
import { appCatalogApi } from '../../api/appCatalogApi';
import { connectionsApi } from '../../api/connectionsApi';
import { workflowClient } from '../../api/workflowClient';
import { useActivateWorkflow, useSaveWorkflowGraph, useWorkflowDetail } from '../../hooks/useWorkflows';
import useToastStore from '../../store/toastStore';
import ConfigPanelBody from './ConfigPanelBody';
import AppBrowserModal from './nodes/AppBrowserModal';
import {
    stepsToGraph,
    makeEdge,
    orderedNodesFromGraph,
    validateGraphForSave,
    resolveNodeType,
} from '../../workflow/workflowGraphSerializer';
import { createDraftStore } from '../../workflow/workflowDraftStore';
import { createSaveCoordinator } from '../../workflow/workflowSaveCoordinator';
import {
    ReactFlow,
    Background,
    Controls,
    MiniMap,
    useNodesState,
    useEdgesState,
    MarkerType,
    BackgroundVariant,
    ConnectionLineType,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
// eslint-disable-next-line no-unused-vars
import { motion, AnimatePresence } from 'framer-motion';
import {
    HiOutlineSave,
    HiOutlinePlay,
    HiOutlineReply,
    HiRefresh,
    HiOutlinePencil,
    HiCheck,
    HiPlus,
    HiX,
    HiOutlineTrash,
    HiOutlineDuplicate,
    HiOutlineSwitchVertical,
    HiOutlineSwitchHorizontal,
    HiOutlineShare,
    HiOutlineArrowsExpand,
    HiSun,
    HiMoon,
    HiMenuAlt2,
} from 'react-icons/hi';
import WorkflowNode from './nodes/WorkflowNode';
import BranchNode from './nodes/BranchNode';
import DeleteableEdge from './nodes/DeleteableEdge';
import './WorkflowCanvas.css';

// 'branch' handles logic:if and logic:switch — these nodes render named output ports
// ('true'/'false' for If; 'output_0'…'output_N' for Switch) so the execution engine
// can route edges by sourceHandle. All other nodes use the generic WorkflowNode.
const nodeTypes = { trigger: WorkflowNode, action: WorkflowNode, branch: BranchNode };
// edgeTypes registered after removeEdge is defined (passed via data.onDelete)

const NODE_GAP_X = 330;
const NODE_GAP_Y = 210;
const BRANCH_GAP_X = 260;
const BRANCH_GAP_Y = 150;

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



const makeDefaultEdge = () => makeEdge('1', '2');

const layoutGraph = (inputNodes, inputEdges, vertical) => {
    if (inputNodes.length === 0) return [];

    const byId = new Map(inputNodes.map((node) => [node.id, node]));
    const children = new Map();
    for (const edge of inputEdges || []) {
        if (!byId.has(edge.source) || !byId.has(edge.target)) continue;
        children.set(edge.source, [...(children.get(edge.source) || []), edge.target]);
    }

    const compareByCurrentPosition = (leftId, rightId) => {
        const left = byId.get(leftId);
        const right = byId.get(rightId);
        const leftAxis = vertical ? left?.position.x : left?.position.y;
        const rightAxis = vertical ? right?.position.x : right?.position.y;
        return ((leftAxis || 0) - (rightAxis || 0)) || String(leftId).localeCompare(String(rightId));
    };
    const trigger = inputNodes.find((node) => node.type === 'trigger');
    const reachable = new Set();
    const markReachable = (id) => {
        if (reachable.has(id)) return;
        reachable.add(id);
        for (const childId of children.get(id) || []) markReachable(childId);
    };
    if (trigger) markReachable(trigger.id);

    // Longest-path layers give every merge a stable column/row after all of
    // its parents, unlike tree recursion which visits a merge more than once.
    const degree = new Map();
    const depth = new Map();
    for (const id of reachable) degree.set(id, 0);
    for (const edge of inputEdges || []) {
        if (reachable.has(edge.source) && reachable.has(edge.target)) {
            degree.set(edge.target, (degree.get(edge.target) || 0) + 1);
        }
    }
    if (trigger) depth.set(trigger.id, 0);
    const queue = [...reachable]
        .filter((id) => id === trigger?.id || (degree.get(id) || 0) === 0)
        .sort(compareByCurrentPosition);
    while (queue.length > 0) {
        const id = queue.shift();
        const currentDepth = depth.get(id) || 0;
        for (const childId of (children.get(id) || []).sort(compareByCurrentPosition)) {
            if (!reachable.has(childId)) continue;
            depth.set(childId, Math.max(depth.get(childId) || 0, currentDepth + 1));
            const remaining = (degree.get(childId) || 0) - 1;
            degree.set(childId, remaining);
            if (remaining === 0) {
                queue.push(childId);
                queue.sort(compareByCurrentPosition);
            }
        }
    }

    const layers = new Map();
    for (const id of reachable) {
        const nodeDepth = depth.get(id) || 0;
        layers.set(nodeDepth, [...(layers.get(nodeDepth) || []), id]);
    }
    for (const ids of layers.values()) ids.sort(compareByCurrentPosition);

    const maxDepth = Math.max(0, ...layers.keys());
    const orphanIds = inputNodes.filter((node) => !reachable.has(node.id))
        .map((node) => node.id)
        .sort(compareByCurrentPosition);
    const positions = new Map();
    for (const [nodeDepth, ids] of layers) {
        ids.forEach((id, lane) => {
            positions.set(id, vertical
                ? { x: 250 + lane * BRANCH_GAP_X, y: 60 + nodeDepth * NODE_GAP_Y }
                : { x: 120 + nodeDepth * NODE_GAP_X, y: 160 + lane * BRANCH_GAP_Y });
        });
    }
    orphanIds.forEach((id, lane) => {
        positions.set(id, vertical
            ? { x: 250 + lane * BRANCH_GAP_X, y: 60 + (maxDepth + 1) * NODE_GAP_Y }
            : { x: 120 + (maxDepth + 1) * NODE_GAP_X, y: 160 + lane * BRANCH_GAP_Y });
    });

    return inputNodes.map((node) => ({
        ...node,
        position: positions.get(node.id) || node.position,
        data: { ...node.data, _vertical: vertical },
    }));
};

const reindexNodes = (inputNodes, inputEdges) => {
    const orderedIds = orderedNodesFromGraph(inputNodes, inputEdges).map((n) => n.id);
    const indexById = new Map(orderedIds.map((id, idx) => [id, idx + 1]));
    // Compute reachable set from trigger for orphan detection
    const triggerNode = inputNodes.find((n) => n.type === 'trigger');
    const reachable = new Set();
    if (triggerNode) {
        const adj = new Map();
        for (const e of inputEdges || []) {
            if (!adj.has(e.source)) adj.set(e.source, []);
            adj.get(e.source).push(e.target);
        }
        const dfs = (id) => {
            if (reachable.has(id)) return;
            reachable.add(id);
            for (const child of adj.get(id) || []) dfs(child);
        };
        dfs(triggerNode.id);
    }
    return inputNodes.map((node) => ({
        ...node,
        data: {
            ...node.data,
            stepIndex: indexById.get(node.id) || node.data.stepIndex,
            isOrphaned: node.type !== 'trigger' && !reachable.has(node.id),
        },
    }));
};

/* Hardcoded app/trigger/action lists removed — all data comes from appCatalogApi and appDetailsByKey */

let nodeId = 3;

export default function WorkflowCanvas() {
    const navigate = useNavigate();
    const { workflowId: routeWorkflowId } = useParams();
    const { toggleTheme, theme, collapsed, setCollapsed } = useOutletContext();
    const { data: loadedWorkflow, isLoading: isLoadingDetail, isError: isDetailError } = useWorkflowDetail(routeWorkflowId);
    const saveWorkflowGraph = useSaveWorkflowGraph();
    const activateWorkflow = useActivateWorkflow();
    const loadedWorkflowIdRef = useRef(null);
    const saveGraphRef = useRef(null);
    // eslint-disable-next-line react-hooks/refs
    saveGraphRef.current = (id, data) => saveWorkflowGraph.mutateAsync({ id, data });

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

    // ── Undo history ──
    // Each entry: { nodes, edges }. Max 50 entries.
    const historyRef = useRef([]);
    const historyIndexRef = useRef(-1);
    const [canUndo, setCanUndo] = useState(false);
    const [canRedo, setCanRedo] = useState(false);
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
    const [lastSelectedNodeId, setLastSelectedNodeId] = useState(null);
    const [showSplitMenu, setShowSplitMenu] = useState(false);

    const ensureAppDetail = useCallback(async (appKey) => {
        if (!appKey || stateRefs.current.appDetailsByKey[appKey]) return;
        try {
            const detail = await appCatalogApi.get(appKey);
            setAppDetailsByKey((prev) => ({ ...prev, [appKey]: detail }));
        } catch {
            // Non-fatal; UI will show fallback labels.
        }
    }, []);

    const stateRefs = useRef({ nodes, edges, workflowId, workflowName, catalogApps, appDetailsByKey });
    useEffect(() => {
        // eslint-disable-next-line react-hooks/immutability
        stateRefs.current = { nodes, edges, workflowId, workflowName, catalogApps, appDetailsByKey };
    }, [nodes, edges, workflowId, workflowName, catalogApps, appDetailsByKey]);

    // `_isNew` is a short-lived presentation flag, never workflow data.
    useEffect(() => {
        if (!nodes.some((node) => node.data?._isNew)) return undefined;
        const timer = setTimeout(() => {
            setNodes((current) => current.map((node) => {
                if (!node.data?._isNew) return node;
                const { _isNew, ...data } = node.data;
                return { ...node, data };
            }));
        }, 260);
        return () => clearTimeout(timer);
    }, [nodes, setNodes]);

    const draftRef = useRef(null);
    // eslint-disable-next-line react-hooks/refs
    if (!draftRef.current) draftRef.current = createDraftStore();

    const saveCoordinatorRef = useRef(null);
    useEffect(() => {
        saveCoordinatorRef.current = createSaveCoordinator({
            getNodes: () => stateRefs.current.nodes,
            getEdges: () => stateRefs.current.edges,
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
            saveGraph: (id, data) => saveGraphRef.current(id, data),
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
        if (!loadedWorkflow) {
            setIsLoadingWorkflow(isLoadingDetail);
            if (isDetailError) setIsLoadingWorkflow(false);
            return;
        }
        // A focus/SSE revalidation can update the query while the canvas is dirty.
        // Keep the editor stable; the fresh detail is used on the next navigation.
        if (loadedWorkflowIdRef.current === routeWorkflowId) return;
        loadedWorkflowIdRef.current = routeWorkflowId;
        setIsLoadingWorkflow(true);

        (async () => {
            try {
                // Detail comes from React Query. The separate step endpoint remains
                // the canonical source for canvas node data.
                const wf = loadedWorkflow;
                setWorkflowName(wf.name);

                // Initialize draft store with current revision to enable OCC
                draftRef.current?.reset(wf.revision);

                // Always fetch steps from the dedicated API for reliability
                let steps = [];
                try {
                    steps = await workflowClient.steps.list(routeWorkflowId);
                } catch {
                    // Fallback to embedded steps if dedicated API fails
                    steps = Array.isArray(wf.steps) ? wf.steps : [];
                }

                if (steps.length > 0) {
                    // Use backend edges for accurate DAG topology (supports branching + merging)
                    const backendEdges = Array.isArray(wf.edges) ? wf.edges : [];
                    const { nodes: loadedNodes, edges: loadedEdges } = stepsToGraph(steps, backendEdges, vertical);
                    setNodes(layoutGraph(reindexNodes(loadedNodes, loadedEdges), loadedEdges, vertical));
                    setEdges(loadedEdges);

                    // Bump nodeId counter to avoid collisions with loaded node IDs
                    nodeId = Math.max(nodeId, loadedNodes.length + 10);

                    // Pre-fetch app details so the config panel renders immediately
                    const uniqueAppKeys = [...new Set(steps.map((s) => s.appKey).filter(Boolean))];
                    await Promise.all(uniqueAppKeys.map((appKey) => ensureAppDetail(appKey)));
                } else {
                    // Workflow exists but has no steps yet - show default placeholder nodes
                    setNodes(makeDefaultNodes(vertical));
                    setEdges([makeDefaultEdge()]);
                }
            } catch {
                // If loading fails completely, show default nodes so canvas is not blank
            } finally {
                setIsLoadingWorkflow(false);
            }
        })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [routeWorkflowId, loadedWorkflow, isLoadingDetail, isDetailError]);

    // Handle naming modal — just set name and close. Workflow is created on first save.
    // IMPORTANT: do NOT create here — an autosave could otherwise create a duplicate
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
        const current = historyRef.current[historyIndexRef.current];
        if (current && JSON.stringify(current) === JSON.stringify(snapshot)) return;
        // Truncate redo stack
        const newHistory = historyRef.current.slice(0, historyIndexRef.current + 1);
        newHistory.push(snapshot);
        // Limit to 50 entries
        if (newHistory.length > 50) newHistory.shift();
        historyRef.current = newHistory;
        historyIndexRef.current = newHistory.length - 1;
        setCanUndo(historyIndexRef.current > 0);
        setCanRedo(false);
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
        setCanRedo(historyIndexRef.current < historyRef.current.length - 1);
        // Re-sync configNode if it still exists
        setConfigNode((prev) => {
            if (!prev) return null;
            const restored = snapshot.nodes.find(n => n.id === prev.id);
            return restored || null;
        });
        setTimeout(() => { isRestoringRef.current = false; }, 0);
        useToastStore.getState().addToast('Undone', 'info', 1500);
    }, [setNodes, setEdges]);

    const handleRedo = useCallback(() => {
        if (historyIndexRef.current >= historyRef.current.length - 1) return;
        historyIndexRef.current += 1;
        const snapshot = historyRef.current[historyIndexRef.current];
        if (!snapshot) return;
        isRestoringRef.current = true;
        setNodes(snapshot.nodes);
        setEdges(snapshot.edges);
        setCanUndo(historyIndexRef.current > 0);
        setCanRedo(historyIndexRef.current < historyRef.current.length - 1);
        setConfigNode((prev) => {
            if (!prev) return null;
            return snapshot.nodes.find((node) => node.id === prev.id) || null;
        });
        setTimeout(() => { isRestoringRef.current = false; }, 0);
        useToastStore.getState().addToast('Redone', 'info', 1500);
    }, [setNodes, setEdges]);


    const fitWorkflow = useCallback(() => {
        reactFlowInstance?.fitView({
            padding: 0.24,
            minZoom: 0.35,
            maxZoom: 1.1,
            duration: 280,
        });
    }, [reactFlowInstance]);

    // Auto-fit once after workflow finishes loading
    const hasFittedRef = useRef(false);
    useEffect(() => {
        if (!isLoadingWorkflow && !hasFittedRef.current && reactFlowInstance && nodes.length > 0) {
            hasFittedRef.current = true;
            setTimeout(() => fitWorkflow(), 120);
        }
    }, [isLoadingWorkflow, reactFlowInstance, nodes.length, fitWorkflow]);

    // ── Keyboard shortcuts ──
    useEffect(() => {
        const handler = (e) => {
            // Skip shortcuts when user is typing in an input/textarea
            if (['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target?.tagName)) return;
            // Ctrl/Cmd+Z → undo
            if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
                e.preventDefault();
                handleUndo();
                return;
            }
            if ((e.ctrlKey || e.metaKey) && (e.key === 'y' || (e.key === 'z' && e.shiftKey))) {
                e.preventDefault();
                handleRedo();
                return;
            }
            // Ctrl/Cmd+S → save
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                e.preventDefault();
                handleSave();
                return;
            }
            // F → fit workflow to view
            if (e.key === 'f' || e.key === 'F') {
                e.preventDefault();
                fitWorkflow();
                return;
            }
        };
        window.addEventListener('keydown', handler);
        return () => window.removeEventListener('keydown', handler);
    }, [handleUndo, handleRedo, handleSave, fitWorkflow]);
    // ── Save workflow + steps to backend ──


    const handleRunWorkflow = useCallback(async () => {
        if (isRunning) return;
        setIsRunning(true);
        setSaveError(null);

        const graphError = validateGraphForSave(nodes, edges);
        if (graphError) {
            useToastStore.getState().addToast(graphError, 'error');
            setIsRunning(false);
            return;
        }

        try {
            const id = await saveCoordinatorRef.current?.saveManual();
            if (!id) return;

            await activateWorkflow.mutateAsync(id);
            useToastStore.getState().addToast('Workflow activated successfully!', 'success');
            setSavedAt(Date.now());
        } catch (err) {
            const msg = err.response?.data?.message || 'Activation failed';
            setSaveError(msg);
            useToastStore.getState().addToast(msg, 'error');
        } finally {
            setIsRunning(false);
        }
    }, [isRunning, activateWorkflow, nodes, edges]);



    // Handle editable name
    const handleNameSubmit = () => {
        setEditingName(false);
        if (!workflowName.trim()) setWorkflowName('Untitled');
    };

    const wouldCreateCycle = useCallback((sourceId, targetId, currentEdges) => {
        const adjacency = new Map();
        for (const edge of currentEdges) {
            if (!adjacency.has(edge.source)) adjacency.set(edge.source, []);
            adjacency.get(edge.source).push(edge.target);
        }
        if (!adjacency.has(sourceId)) adjacency.set(sourceId, []);
        adjacency.get(sourceId).push(targetId);
        const visit = (id, seen = new Set()) => {
            if (id === sourceId && seen.size > 0) return true;
            if (seen.has(id)) return false;
            seen.add(id);
            return (adjacency.get(id) || []).some((next) => visit(next, new Set(seen)));
        };
        return visit(targetId);
    }, []);

    const isValidConnection = useCallback((connection) => {
        if (!connection?.source || !connection?.target) return false;
        if (connection.source === connection.target) return false;
        if (connection.targetHandle && connection.targetHandle !== 'in') return false;
        const { nodes: curNodes, edges: curEdges } = stateRefs.current;
        const source = curNodes.find((n) => n.id === connection.source);
        const target = curNodes.find((n) => n.id === connection.target);
        // Never connect into the trigger
        if (!source || !target || target.type === 'trigger') return false;
        const sourceHandle = connection.sourceHandle || 'out';
        if (source.type === 'branch') {
            const outputCount = Math.max(2, Number(source.data?.configuration?.numberOutputs || 4));
            const allowed = source.data?.actionKey === 'logic:if'
                ? ['true', 'false']
                : Array.from({ length: outputCount }, (_, index) => `output_${index}`);
            if (!allowed.includes(sourceHandle)) return false;
        } else if (sourceHandle !== 'out') return false;
        // No duplicate edges (same source→target pair)
        if (curEdges.some((e) => e.source === connection.source && e.target === connection.target)) return false;
        // DAG: multiple incoming allowed (merge points) — only prevent cycles
        return !wouldCreateCycle(connection.source, connection.target, curEdges);
    }, [wouldCreateCycle]);

    // Connect edges — use stateRefs to avoid stale closure on nodes/edges
    const onConnect = useCallback(
        (params) => {
            if (!isValidConnection(params)) {
                useToastStore.getState().addToast(
                    'Connection rejected — cannot connect into trigger or create a cycle.',
                    'error'
                );
                return;
            }
            const curNodes = stateRefs.current.nodes;
            const curEdges = stateRefs.current.edges;
            const newEdges = [...curEdges, makeEdge(params.source, params.target, params.sourceHandle, params.targetHandle)];
            const reindexed = reindexNodes(curNodes, newEdges);
            const laidOut = layoutGraph(reindexed, newEdges, vertical);
            setNodes(laidOut);
            setEdges(newEdges);
            pushHistory(laidOut, newEdges);
        },
        [setEdges, setNodes, vertical, pushHistory, isValidConnection]
    );

    const commitGraph = useCallback((nextNodes, nextEdges, { relayout = true } = {}) => {
        const validIds = new Set(nextNodes.map((node) => node.id));
        const seenPairs = new Set();
        const cleanEdges = nextEdges
            .filter((edge) => validIds.has(edge.source) && validIds.has(edge.target) && edge.source !== edge.target)
            .filter((edge) => {
                const key = `${edge.source}:${edge.target}`;
                if (seenPairs.has(key)) return false;
                seenPairs.add(key);
                return true;
            })
            .map((edge) => makeEdge(edge.source, edge.target, edge.sourceHandle, edge.targetHandle));
        const reindexed = reindexNodes(nextNodes, cleanEdges);
        const laidOut = relayout ? layoutGraph(reindexed, cleanEdges, vertical) : reindexed;
        setNodes(laidOut);
        setEdges(cleanEdges);
        pushHistory(laidOut, cleanEdges);
        saveCoordinatorRef.current?.saveAuto();
        return { nodes: laidOut, edges: cleanEdges };
    }, [setNodes, setEdges, pushHistory, vertical]);

    const removeEdge = useCallback((edgeId) => {
        const nextEdges = edges.filter((e) => e.id !== edgeId);
        commitGraph(nodes, nextEdges);
    }, [commitGraph, nodes, edges]);

    const insertActionOnEdge = useCallback((edgeId) => {
        const edge = edges.find((candidate) => candidate.id === edgeId);
        if (!edge) return;
        const source = nodes.find((node) => node.id === edge.source);
        const target = nodes.find((node) => node.id === edge.target);
        const id = String(nodeId++);
        const inserted = {
            id,
            type: 'action',
            position: {
                x: ((source?.position.x || 0) + (target?.position.x || 0)) / 2,
                y: ((source?.position.y || 0) + (target?.position.y || 0)) / 2,
            },
            data: {
                label: 'New Action',
                stepIndex: nodes.length + 1,
                _vertical: vertical,
                _isNew: true,
            },
        };
        const retained = edges.filter((candidate) => candidate.id !== edgeId);
        commitGraph(
            [...nodes, inserted],
            [
                ...retained,
                makeEdge(edge.source, id, edge.sourceHandle, 'in'),
                makeEdge(id, edge.target, 'out', edge.targetHandle),
            ],
        );
        setConfigNode(inserted);
    }, [nodes, edges, vertical, commitGraph]);

    // Keep custom edge callbacks fresh without recreating every edge component.
    const removeEdgeRef = useRef(null);
    const insertEdgeRef = useRef(null);
    removeEdgeRef.current = removeEdge;
    insertEdgeRef.current = insertActionOnEdge;
    const edgeTypes = useMemo(() => ({
        // eslint-disable-next-line react-hooks/refs
        deleteable: (props) => (
            <DeleteableEdge
                {...props}
                data={{
                    ...props.data,
                    onDelete: (id) => removeEdgeRef.current(id),
                    onInsert: (id) => insertEdgeRef.current(id),
                }}
            />
        ),
    }), []);

    // Add a blank action node — auto-connects to selected/context node, otherwise the last graph node.
    const addActionNode = useCallback(
        (position, sourceId = null, { openConfig = false } = {}) => {
            const id = String(nodeId++);
            const ordered = orderedNodesFromGraph(nodes, edges);
            const fallbackSource = ordered[ordered.length - 1]?.id || nodes[nodes.length - 1]?.id || null;
            const parentId = sourceId || configNode?.id || fallbackSource;
            const parent = nodes.find((n) => n.id === parentId) || ordered[ordered.length - 1] || null;
            const newNode = {
                id,
                type: 'action',
                position: position || {
                    x: parent ? parent.position.x + (vertical ? 0 : NODE_GAP_X) : 250,
                    y: parent ? parent.position.y + (vertical ? NODE_GAP_Y : 0) : 200,
                },
                data: {
                    label: 'New Action',
                    stepIndex: nodes.length + 1,
                    _vertical: vertical,
                    _isNew: true,
                },
            };
            const nextNodes = [...nodes, newNode];
            const nextEdges = parentId ? [...edges, makeEdge(parentId, id)] : edges;
            commitGraph(nextNodes, nextEdges, { relayout: !position });
            if (openConfig) setConfigNode(newNode);
            return id;
        },
        [nodes, edges, vertical, configNode, commitGraph]
    );

    // Add branch from a node
    const addBranch = useCallback(
        (sourceId) => {
            const sourceNode = stateRefs.current.nodes.find((n) => n.id === sourceId);
            if (!sourceNode) return;
            addActionNode(null, sourceId, { openConfig: true });
        },
        [addActionNode]
    );

    const splitNode = useCallback((sourceId, requestedCount) => {
        const count = Math.max(2, Math.min(4, Number(requestedCount) || 2));
        const source = nodes.find((node) => node.id === sourceId);
        if (!source) return;

        const splitNodes = Array.from({ length: count }, (_, index) => {
            const lane = index - ((count - 1) / 2);
            const id = String(nodeId++);
            return {
                id,
                type: 'action',
                position: vertical
                    ? { x: source.position.x + lane * BRANCH_GAP_X, y: source.position.y + NODE_GAP_Y }
                    : { x: source.position.x + NODE_GAP_X, y: source.position.y + lane * BRANCH_GAP_Y },
                data: {
                    label: 'New Action',
                    stepIndex: nodes.length + index + 1,
                    _vertical: vertical,
                    _isNew: true,
                },
            };
        });
        const splitEdges = splitNodes.map((node) => makeEdge(sourceId, node.id));
        commitGraph([...nodes, ...splitNodes], [...edges, ...splitEdges]);
        useToastStore.getState().addToast(`Created ${count} branches.`, 'success', 1800);
    }, [nodes, edges, vertical, commitGraph]);

    // Duplicate an action node (trigger cannot be duplicated)
    const duplicateNode = useCallback(
        (id) => {
            const original = stateRefs.current.nodes.find((n) => n.id === id);
            if (!original || original.type === 'trigger') {
                useToastStore.getState().addToast('Only action nodes can be duplicated.', 'info');
                return;
            }
            const newId = String(nodeId++);
            const curNodes = stateRefs.current.nodes;
            const curEdges = stateRefs.current.edges;
            const clone = {
                ...original,
                id: newId,
                position: {
                    x: original.position.x + (vertical ? 0 : 40),
                    y: original.position.y + (vertical ? 40 : 40),
                },
                data: {
                    ...original.data,
                    _backendId: undefined,
                    stepIndex: curNodes.length + 1,
                    _isNew: true,
                },
                selected: false,
            };
            const nextNodes = [...curNodes, clone];
            // A duplicate is a new branch from the original, preserving its own
            // configuration without silently replacing any existing route.
            const nextEdges = [...curEdges, makeEdge(id, newId)];
            commitGraph(nextNodes, nextEdges);
            useToastStore.getState().addToast('Node duplicated.', 'success', 1500);
        },
        [vertical, commitGraph]
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
            const incoming = edges.filter((e) => e.target === id);
            const outgoing = edges.filter((e) => e.source === id);
            const remaining = nodes.filter((n) => n.id !== id);
            if (remaining.length === 0) {
                commitGraph(makeDefaultNodes(vertical), [makeDefaultEdge()]);
            } else {
                const baseEdges = edges.filter((e) => e.source !== id && e.target !== id);
                // Preserve the DAG when deleting an intermediate/merge node:
                // every parent remains connected to every downstream child.
                const reconnected = incoming.flatMap((parent) => outgoing
                    .filter((child) => parent.source !== child.target)
                    .filter((child) => !baseEdges.some((existing) =>
                        existing.source === parent.source && existing.target === child.target))
                    .map((child) => makeEdge(parent.source, child.target)));
                commitGraph(remaining, [...baseEdges, ...reconnected]);
            }
            if (configNode?.id === id) setConfigNode(null);
            if (lastSelectedNodeId === id) setLastSelectedNodeId(null);
            setContextMenu(null);
        },
        [configNode, lastSelectedNodeId, vertical, nodes, edges, clearTriggerNode, commitGraph]
    );

    // Node click → open config panel (but NOT if clicking a handle)
    const onNodeClick = useCallback(
        (event, node) => {
            // Don't open config panel if clicking on a handle
            const target = event.target;
            if (target.closest('.react-flow__handle')) return;
            setLastSelectedNodeId(node.id);
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
            setLastSelectedNodeId(node.id);
            setConfigNode(node);
            setContextMenu(null);
        },
        []
    );

    const onNodeDragStop = useCallback((event, draggedNode) => {
        const curEdges = stateRefs.current.edges;
        const movedNodes = stateRefs.current.nodes.map((node) => (
            node.id === draggedNode.id
                ? { ...node, position: draggedNode.position }
                : node
        ));
        const reindexed = reindexNodes(movedNodes, curEdges);
        setNodes(reindexed);
        pushHistory(reindexed, curEdges);
    }, [setNodes, pushHistory]);

    // Edge click → select the edge (allows deletion with backspace/delete)
    const onEdgeClick = useCallback(
        (event) => {
            event.stopPropagation();
            setContextMenu(null);
        },
        []
    );

    // Edge double-click → delete the edge
    const onEdgeDoubleClick = useCallback(
        (event, edge) => {
            event.stopPropagation();
            removeEdge(edge.id);
        },
        [removeEdge]
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
            setLastSelectedNodeId(node.id);
        },
        []
    );

    // Click on pane → close everything
    const onPaneClick = useCallback(() => {
        setContextMenu(null);
        setShowSplitMenu(false);
    }, []);

    // Orientation toggle
    const toggleOrientation = useCallback(() => {
        const nextVertical = !vertical;
        setVertical(nextVertical);
        const nextEdges = edges.map((edge) => makeEdge(edge.source, edge.target, edge.sourceHandle, edge.targetHandle));
        const nextNodes = layoutGraph(nodes, nextEdges, nextVertical);
        setNodes(nextNodes);
        setEdges(nextEdges);
        pushHistory(nextNodes, nextEdges);
    }, [vertical, nodes, edges, setNodes, setEdges, pushHistory]);



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
                    <button
                        className="canvas-tb-btn"
                        title="Redo (Ctrl+Shift+Z)"
                        onClick={handleRedo}
                        disabled={!canRedo}
                        data-tooltip="Redo"
                    >
                        <HiRefresh style={{ transform: 'scaleX(-1)' }} />
                    </button>
                    <div className="canvas-tb-divider" />
                    <button
                        className="canvas-tb-btn"
                        title="Add Action Step"
                        data-tooltip="Add Step"
                        onClick={() => addActionNode(null, null, { openConfig: true })}
                    >
                        <HiPlus />
                    </button>
                    <div className="canvas-split-control">
                        <button
                            className="canvas-tb-btn"
                            title={lastSelectedNodeId ? 'Split selected step' : 'Select a step to split'}
                            data-tooltip="Split"
                            onClick={() => setShowSplitMenu((open) => !open)}
                            disabled={!lastSelectedNodeId}
                        >
                            <HiOutlineShare />
                        </button>
                        <AnimatePresence>
                            {showSplitMenu && lastSelectedNodeId && (
                                <motion.div
                                    className="canvas-toolbar-split-menu"
                                    initial={{ opacity: 0, y: -4, scale: 0.96 }}
                                    animate={{ opacity: 1, y: 0, scale: 1 }}
                                    exit={{ opacity: 0, y: -4, scale: 0.96 }}
                                    transition={{ duration: 0.14 }}
                                >
                                    {[2, 3, 4].map((count) => (
                                        <button
                                            key={count}
                                            className="canvas-toolbar-split-btn"
                                            title={`Create ${count} branches`}
                                            onClick={() => {
                                                splitNode(lastSelectedNodeId, count);
                                                setShowSplitMenu(false);
                                            }}
                                        >
                                            {count}
                                        </button>
                                    ))}
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>
                    <button
                        className="canvas-tb-btn"
                        title={vertical ? 'Switch to Horizontal Layout' : 'Switch to Vertical Layout'}
                        data-tooltip={vertical ? 'Horizontal' : 'Vertical'}
                        onClick={toggleOrientation}
                    >
                        {vertical ? <HiOutlineSwitchHorizontal /> : <HiOutlineSwitchVertical />}
                    </button>
                    <button
                        className="canvas-tb-btn"
                        title="Fit workflow to canvas (F)"
                        data-tooltip="Fit view"
                        onClick={fitWorkflow}
                    >
                        <HiOutlineArrowsExpand />
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
                    onNodeDragStop={onNodeDragStop}
                    onNodeClick={onNodeClick}
                    onNodeDoubleClick={onNodeDoubleClick}
                    onNodeContextMenu={onNodeContextMenu}
                    onEdgeClick={onEdgeClick}
                    onEdgeDoubleClick={onEdgeDoubleClick}
                    onPaneClick={onPaneClick}
                    onDragOver={onDragOver}
                    onDrop={onDrop}
                    nodeTypes={nodeTypes}
                    edgeTypes={edgeTypes}
                    defaultViewport={{ x: 200, y: 150, zoom: 0.75 }}
                    snapToGrid
                    snapGrid={[16, 16]}
                    proOptions={{ hideAttribution: true }}
                    defaultEdgeOptions={{
                        animated: false,
                        type: 'deleteable',
                        markerEnd: { type: MarkerType.ArrowClosed, width: 16, height: 16, color: 'var(--border-hover)' },
                        style: { stroke: 'var(--border-secondary)', strokeWidth: 1.5 },
                    }}
                    connectionMode="strict"
                    connectionLineType={ConnectionLineType.SmoothStep}
                    isValidConnection={isValidConnection}
                    connectionRadius={40}
                    deleteKeyCode={null}
                    edgesReconnectable={false}
                >
                    <Background variant={BackgroundVariant.Dots} gap={18} size={1.6} color="var(--dot-color-bright)" />
                    <MiniMap
                        position="bottom-right"
                        pannable
                        zoomable
                        nodeStrokeWidth={2}
                        nodeColor="var(--bg-elevated)"
                        maskColor="rgba(15, 23, 42, 0.18)"
                    />
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
                                <HiPlus /> Add next step
                            </button>
                            <div className="canvas-ctx-split">
                                <span className="canvas-ctx-split-label"><HiOutlineShare /> Split into</span>
                                <div className="canvas-ctx-split-options" role="group" aria-label="Number of branches">
                                    {[2, 3, 4].map((count) => (
                                        <button
                                            key={count}
                                            className="canvas-ctx-split-btn"
                                            onClick={() => { splitNode(contextMenu.nodeId, count); setContextMenu(null); }}
                                            title={`Create ${count} branches`}
                                        >
                                            {count}
                                        </button>
                                    ))}
                                </div>
                            </div>
                            <button
                                className="canvas-ctx-item"
                                onClick={() => { duplicateNode(contextMenu.nodeId); setContextMenu(null); }}
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
                        onSelect={async (app, credentialSource) => {
                            setShowAppBrowser(false);
                            if (appBrowserTarget === 'new') {
                                // Add a new action node with the selected app
                                const newId = addActionNode();
                                updateNodeData(newId, {
                                    appKey: app.appKey,
                                    app: app.appKey,
                                    appName: app.name,
                                    iconUrl: app.logoUrl || `/icons/${app.appKey}.svg`,
                                    label: app.name,
                                    credentialSource: credentialSource || 'PERSONAL',
                                });
                                await ensureAppDetail(app.appKey);
                            } else if (appBrowserTarget && configNode) {
                                // Update existing node
                                updateNodeData(appBrowserTarget, {
                                    appKey: app.appKey,
                                    app: app.appKey,
                                    appName: app.name,
                                    iconUrl: app.logoUrl || `/icons/${app.appKey}.svg`,
                                    label: app.name,
                                    connectionId: null,
                                    credentialSource: credentialSource || 'PERSONAL',
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
