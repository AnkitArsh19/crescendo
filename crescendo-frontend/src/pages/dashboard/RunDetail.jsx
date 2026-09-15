import { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  HiOutlineArrowLeft,
  HiOutlineClock,
  HiOutlineCheckCircle,
  HiOutlineXCircle,
  HiOutlineRefresh,
  HiOutlineExclamationCircle,
  HiOutlineChevronDown,
  HiOutlineChevronUp,
  HiOutlineChevronRight,
  HiOutlineBan,
  HiOutlineLightningBolt,
  HiOutlineCog,
  HiOutlineMinusCircle,
  HiOutlineDownload,
  HiOutlineClipboardCopy,
  HiCheck,
  HiOutlineCode,
} from 'react-icons/hi';
import useLogbookStore from '../../store/logbookStore';
import { useWorkflowList, useWorkflowDetail } from '../../hooks/useWorkflows';
import { getCachedApps } from '../../api/appCatalogCache';
import { downloadFile } from '../../utils/download';
import AgentTimelineView from './AgentTimelineView';
import './RunDetail.css';

function formatDateTime(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString();
}

function formatDuration(start, end, isRunning = false) {
  if (!start) return '—';
  const endMs = end ? new Date(end).getTime() : (isRunning ? Date.now() : null);
  if (!endMs) return '—';
  const ms = Math.max(0, endMs - new Date(start).getTime());
  if (ms < 1000) return `${ms}ms`;
  const secs = (ms / 1000).toFixed(1);
  if (secs < 60) return `${secs}s`;
  const mins = Math.floor(ms / 60000);
  const remSecs = Math.floor((ms % 60000) / 1000);
  return `${mins}m ${remSecs}s`;
}

const statusConfig = {
  PENDING:  { icon: <HiOutlineClock />,        className: 'pending',  label: 'Pending' },
  RUNNING:  { icon: <HiOutlineRefresh />,      className: 'running',  label: 'Running' },
  SUCCESS:  { icon: <HiOutlineCheckCircle />,  className: 'success',  label: 'Success' },
  FAILED:   { icon: <HiOutlineXCircle />,      className: 'failed',   label: 'Failed' },
  SKIPPED:  { icon: <HiOutlineMinusCircle />,  className: 'skipped',  label: 'Skipped' },
};

export default function RunDetail() {
  const { workflowId, runId } = useParams();
  const navigate = useNavigate();

  const {
    runDetail, isLoadingDetail, detailError,
    fetchRunDetail, clearRunDetail, cancelRun, retryRun,
  } = useLogbookStore();

  const { data: workflows = [] } = useWorkflowList();
  const { data: workflowDetail } = useWorkflowDetail(workflowId);
  const [catalogApps, setCatalogApps] = useState([]);
  const [expandedStep, setExpandedStep] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [retrying, setRetrying] = useState(false);
  const [copiedKey, setCopiedKey] = useState(null);
  const [showTriggerPayload, setShowTriggerPayload] = useState(false);

  useEffect(() => {
    fetchRunDetail(workflowId, runId);
    return () => clearRunDetail();
  }, [workflowId, runId, fetchRunDetail, clearRunDetail]);

  // Auto-refresh when run is currently running or pending
  useEffect(() => {
    if (runDetail && (runDetail.status === 'PENDING' || runDetail.status === 'RUNNING')) {
      const timer = setInterval(() => {
        fetchRunDetail(workflowId, runId);
      }, 2000);
      return () => clearInterval(timer);
    }
  }, [runDetail?.status, workflowId, runId, fetchRunDetail]);

  useEffect(() => {
    getCachedApps().then(setCatalogApps).catch(() => {});
  }, []);

  const run = runDetail;
  const workflowName = workflows.find((w) => w.id === workflowId)?.name || workflowDetail?.name || 'Workflow';
  const sc = run ? (statusConfig[run.status] || statusConfig.PENDING) : null;

  // Auto-expand the failed step on load if there's an error so the user immediately sees what failed
  useEffect(() => {
    if (run?.stepRuns && run.stepRuns.length > 0 && expandedStep === null) {
      const failed = run.stepRuns.find((s) => s.status === 'FAILED');
      if (failed) {
        setExpandedStep(failed.id);
      }
    }
  }, [run, expandedStep]);

  // Index workflow step definitions by their UUID for rich metadata display
  const stepsById = useMemo(() => {
    const map = new Map();
    (workflowDetail?.steps || []).forEach((s, idx) => {
      map.set(s.id, { ...s, index: idx + 1 });
    });
    return map;
  }, [workflowDetail]);

  // Format and safely unescape nested JSON payloads, while ensuring sensitive keys are masked
  const formatJsonPayload = (data) => {
    if (data === undefined || data === null) return '';
    if (typeof data !== 'object') {
      if (typeof data === 'string') {
        const trimmed = data.trim();
        if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
          try {
            return formatJsonPayload(JSON.parse(trimmed));
          } catch {
            return data;
          }
        }
      }
      return String(data);
    }

    const sensitiveRegex = /^(password|passwd|secret|token|api_?key|access_?key|private_?key|client_?secret|authorization|auth_?header|credential|private)$/i;

    function deepProcess(key, val, depth = 0) {
      if (depth > 6) return val;
      if (key && sensitiveRegex.test(key)) {
        return '[REDACTED]';
      }
      if (typeof val === 'string') {
        const trimmed = val.trim();
        if (trimmed.length < 100000 && ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']')))) {
          try {
            const parsed = JSON.parse(trimmed);
            return deepProcess(key, parsed, depth + 1);
          } catch {
            // retain original string
          }
        }
        return val;
      }
      if (Array.isArray(val)) {
        return val.slice(0, 500).map((item) => deepProcess(null, item, depth + 1));
      }
      if (val !== null && typeof val === 'object') {
        const copy = {};
        const entries = Object.entries(val).slice(0, 500);
        for (const [k, v] of entries) {
          copy[k] = deepProcess(k, v, depth + 1);
        }
        return copy;
      }
      return val;
    }

    try {
      const sanitized = deepProcess(null, data, 0);
      return JSON.stringify(sanitized, null, 2);
    } catch {
      return JSON.stringify(data, null, 2);
    }
  };

  // Helper to extract clean human-readable step metadata with deduplication
  const getStepMeta = (stepRun, index) => {
    const stepDef = stepsById.get(stepRun.stepId);
    const appKey = stepDef?.appKey || stepRun.inputData?._appKey || '';
    const appInfo = catalogApps.find((a) => a.appKey?.toLowerCase() === appKey?.toLowerCase());
    const appName = appInfo?.name || (appKey ? appKey.charAt(0).toUpperCase() + appKey.slice(1) : '');
    const isTrigger = stepDef?.type === 'TRIGGER' || stepDef?.stepType === 'TRIGGER' || stepRun?.isTrigger === true;
    const rawName = stepDef?.name || stepDef?.actionKey || (isTrigger ? 'Trigger' : 'Action');

    // Deduplicate app name if rawName already starts with it (e.g. "Spotify • Search Spotify" with appName "Spotify")
    let cleanActionName = rawName;
    if (appName && cleanActionName.toLowerCase().startsWith(appName.toLowerCase())) {
      cleanActionName = cleanActionName.slice(appName.length).replace(/^[\s•\-:]+/, '').trim();
    }
    const stepName = appName
      ? (cleanActionName ? `${appName} • ${cleanActionName}` : appName)
      : (cleanActionName || `Step ${index + 1}`);
    const iconUrl = appInfo?.logoUrl || null;

    return {
      stepIndex: stepDef?.order ?? (index + 1),
      stepName,
      rawName: cleanActionName || rawName,
      appName: appName || (isTrigger ? 'Trigger' : 'Action'),
      isTrigger,
      iconUrl,
      appKey,
      stepId: stepRun.stepId,
    };
  };

  const handleCopy = (key, data) => {
    try {
      navigator.clipboard.writeText(typeof data === 'string' ? data : JSON.stringify(data, null, 2));
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(null), 1800);
    } catch {
      // ignore
    }
  };

  const handleCancel = async () => {
    setCancelling(true);
    try {
      await cancelRun(workflowId, runId);
    } catch {
      // handled in store
    } finally {
      setCancelling(false);
    }
  };

  const handleRetry = async () => {
    setRetrying(true);
    try {
      await retryRun(workflowId, runId);
      await fetchRunDetail(workflowId, runId);
    } catch {
      // handled in store
    } finally {
      setRetrying(false);
    }
  };

  const canCancel = run && (run.status === 'PENDING' || run.status === 'RUNNING');

  // Unified list of steps for display: includes trigger (GitHub) and unexecuted downstream steps
  const displaySteps = useMemo(() => {
    if (!run) return [];
    const stepRuns = run.stepRuns || [];
    const stepRunsByStepId = new Map(stepRuns.map((sr) => [sr.stepId, sr]));
    const workflowSteps = workflowDetail?.steps || [];

    if (workflowSteps.length === 0) {
      return stepRuns;
    }

    const triggerDef = workflowSteps.find((s) => s.type === 'TRIGGER' || s.stepType === 'TRIGGER');
    const result = [];
    const seen = new Set();

    if (triggerDef) {
      seen.add(triggerDef.id);
      const existingSr = stepRunsByStepId.get(triggerDef.id);
      result.push(
        existingSr || {
          id: `trigger-${triggerDef.id}`,
          stepId: triggerDef.id,
          status: 'SUCCESS',
          inputData: {},
          outputData: run.triggerData || {},
          errorMessage: null,
          createdAt: run.createdAt,
          completedAt: run.createdAt,
          isTrigger: true,
        }
      );
    }

    // Sort actions by step order / definition order
    const sortedWorkflowSteps = [...workflowSteps].sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
    sortedWorkflowSteps.forEach((stepDef) => {
      if (seen.has(stepDef.id)) return;
      seen.add(stepDef.id);

      const existingSr = stepRunsByStepId.get(stepDef.id);
      if (existingSr) {
        result.push(existingSr);
      } else {
        result.push({
          id: `unexecuted-${stepDef.id}`,
          stepId: stepDef.id,
          status: 'SKIPPED',
          inputData: {},
          outputData: {},
          errorMessage: 'Step was not executed because a previous step failed.',
          createdAt: null,
          completedAt: null,
          isUnexecuted: true,
        });
      }
    });

    return result;
  }, [run, workflowDetail]);

  // Group workflow steps into topological stages (supports both linear chains & multi-branch DAGs)
  const pipelineStages = useMemo(() => {
    if (!run) return [];

    const workflowSteps = workflowDetail?.steps || [];
    const edges = workflowDetail?.edges || [];
    const stepRunsByStepId = new Map();

    (run.stepRuns || []).forEach((sr, idx) => {
      stepRunsByStepId.set(sr.stepId, { ...sr, runIndex: idx });
    });

    // If workflow definition is missing, fall back to simple sequential map
    if (workflowSteps.length === 0) {
      return (run.stepRuns || []).map((step, idx) => ({
        stageIndex: idx,
        isBranchStage: false,
        nodes: [{
          stepRun: step,
          stepDef: stepsById.get(step.stepId),
          branchLabel: null,
          isExecuted: true,
          index: idx + 1,
        }],
      }));
    }

    const triggerDef = workflowSteps.find((s) => s.type === 'TRIGGER' || s.stepType === 'TRIGGER');
    if (triggerDef && !stepRunsByStepId.has(triggerDef.id)) {
      stepRunsByStepId.set(triggerDef.id, {
        id: `trigger-${triggerDef.id}`,
        stepId: triggerDef.id,
        status: 'SUCCESS',
        inputData: {},
        outputData: run.triggerData || {},
        errorMessage: null,
        createdAt: run.createdAt,
        completedAt: run.createdAt,
        isTrigger: true,
      });
    }

    const childrenMap = new Map();
    const parentsMap = new Map();
    const handleMap = new Map();

    workflowSteps.forEach((s) => {
      childrenMap.set(s.id, []);
      parentsMap.set(s.id, []);
    });

    edges.forEach((e) => {
      if (childrenMap.has(e.sourceStepId)) {
        childrenMap.get(e.sourceStepId).push(e.targetStepId);
      }
      if (parentsMap.has(e.targetStepId)) {
        parentsMap.get(e.targetStepId).push(e.sourceStepId);
      }
      if (e.sourceHandle) {
        handleMap.set(`${e.sourceStepId}->${e.targetStepId}`, e.sourceHandle);
      }
    });

    // Roots are nodes with no parents (typically the trigger)
    const roots = workflowSteps.filter((s) => (parentsMap.get(s.id) || []).length === 0);
    const depthMap = new Map();
    const queue = roots.map((r) => r.id);
    roots.forEach((r) => depthMap.set(r.id, 0));

    let iterations = 0;
    while (queue.length > 0 && iterations < workflowSteps.length * 4) {
      iterations++;
      const current = queue.shift();
      const currDepth = depthMap.get(current) || 0;
      const children = childrenMap.get(current) || [];
      for (const childId of children) {
        const nextDepth = currDepth + 1;
        if (!depthMap.has(childId) || nextDepth > depthMap.get(childId)) {
          depthMap.set(childId, nextDepth);
          queue.push(childId);
        }
      }
    }

    // Group steps by their topological depth
    const stagesByDepth = new Map();
    workflowSteps.forEach((step, sIdx) => {
      const depth = depthMap.get(step.id) ?? (step.order != null ? Math.floor(step.order) : sIdx);
      if (!stagesByDepth.has(depth)) {
        stagesByDepth.set(depth, []);
      }

      const sr = stepRunsByStepId.get(step.id);
      const parentIds = parentsMap.get(step.id) || [];
      const branchLabels = parentIds
        .map((pId) => handleMap.get(`${pId}->${step.id}`))
        .filter(Boolean);

      const branchLabel = branchLabels.length > 0 ? branchLabels.join(' / ') : null;

      stagesByDepth.get(depth).push({
        stepRun: sr || {
          id: `unexecuted-${step.id}`,
          stepId: step.id,
          status: 'SKIPPED',
          createdAt: null,
          completedAt: null,
        },
        stepDef: step,
        branchLabel,
        isExecuted: Boolean(sr && sr.status !== 'SKIPPED'),
        index: step.order != null ? Math.round(step.order) : sIdx + 1,
      });
    });

    const sortedDepths = Array.from(stagesByDepth.keys()).sort((a, b) => a - b);
    return sortedDepths.map((depth, idx) => {
      const nodes = stagesByDepth.get(depth);
      return {
        stageIndex: idx,
        isBranchStage: nodes.length > 1,
        nodes,
      };
    });
  }, [run, workflowDetail, stepsById]);

  const isBranchedWorkflow = pipelineStages.some((stage) => stage.isBranchStage);
  const triggerStep = displaySteps.find((s) => s.isTrigger);
  const triggerMeta = triggerStep ? getStepMeta(triggerStep, 0) : null;
  const failedStep = run?.stepRuns?.find((s) => s.status === 'FAILED');
  const failedMeta = failedStep ? getStepMeta(failedStep, 0) : null;
  const totalSteps = displaySteps.length || (run?.stepRuns?.length || 0);
  const succeededSteps = displaySteps.filter((s) => s.status === 'SUCCESS').length;

  return (
    <div className="rd-page">
      {/* Back button */}
      <button
        className="rd-back-btn"
        onClick={() => navigate(workflowId ? `/dashboard/history/${workflowId}` : '/dashboard/history')}
        title="Return to Run History"
        aria-label="Return to Run History"
      >
        <HiOutlineArrowLeft /> Run History
      </button>

      {/* Error */}
      {detailError && (
        <div className="rd-error">
          <HiOutlineExclamationCircle />
          {detailError}
        </div>
      )}

      {/* Loading */}
      {isLoadingDetail && (
        <div className="rd-loading">
          <div className="rd-loading-card rd-skeleton" />
          <div className="rd-loading-steps rd-skeleton" />
        </div>
      )}

      {/* Content */}
      {run && !isLoadingDetail && (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
        >
          {/* Run header card */}
          <div className="rd-header-card">
            <div className="rd-header-top">
              <div className="rd-header-left">
                <div className="rd-header-icon">
                  <HiOutlineLightningBolt />
                </div>
                <div>
                  <h1 className="rd-run-title">{workflowName}</h1>
                  <p className="rd-run-id">Execution Run #{run.id.substring(0, 8)}</p>
                </div>
              </div>
              <div className="rd-header-actions">
                <span className={`rd-status-badge ${sc.className}`}>
                  {sc.icon} {sc.label}
                </span>
                {run.triggerData && Object.keys(run.triggerData).length > 0 && (
                  <button
                    type="button"
                    className="rd-trigger-nav-btn"
                    onClick={() => {
                      setShowTriggerPayload(true);
                      setTimeout(() => {
                        document.getElementById('rd-trigger-payload')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                      }, 50);
                    }}
                    title="View initial webhook trigger payload"
                    aria-label="View Trigger Payload"
                  >
                    <HiOutlineCode /> Trigger Payload
                  </button>
                )}
                <button
                  className="rd-export-btn"
                  onClick={() => {
                    downloadFile(`crescendo-run-${run.id.substring(0, 8)}.json`, formatJsonPayload(run));
                  }}
                  title="Export execution run log as JSON"
                  aria-label="Export Run Log"
                >
                  <HiOutlineDownload /> Export Log
                </button>
                {run.status === 'FAILED' && (
                  <button
                    className="rd-retry-btn"
                    onClick={handleRetry}
                    disabled={retrying}
                    title="Retry execution from the failed step"
                    aria-label="Retry Run"
                  >
                    <HiOutlineRefresh className={retrying ? 'spin' : ''} />
                    {retrying ? 'Retrying...' : 'Retry Run'}
                  </button>
                )}
                {canCancel && (
                  <button
                    className="rd-cancel-btn"
                    onClick={handleCancel}
                    disabled={cancelling}
                    title="Cancel active execution"
                    aria-label="Cancel Run"
                  >
                    <HiOutlineBan />
                    {cancelling ? 'Cancelling...' : 'Cancel Run'}
                  </button>
                )}
              </div>
            </div>

            {/* Meta row - Executive statistics cards */}
            <div className="rd-meta-grid">
              <div className="rd-meta-card">
                <div className="rd-meta-card-header">
                  <HiOutlineClock className="rd-meta-card-icon" />
                  <span className="rd-meta-label">Duration</span>
                </div>
                <span className="rd-meta-value duration">{formatDuration(run.createdAt, run.completedAt, run.status === 'RUNNING')}</span>
              </div>

              <div className="rd-meta-card">
                <div className="rd-meta-card-header">
                  <HiOutlineCheckCircle className="rd-meta-card-icon" />
                  <span className="rd-meta-label">Steps Status</span>
                </div>
                <span className="rd-meta-value">
                  {succeededSteps} of {totalSteps} {totalSteps === 1 ? 'step' : 'steps'} succeeded
                </span>
              </div>

              <div className="rd-meta-card">
                <div className="rd-meta-card-header">
                  <span className="rd-meta-label">Started</span>
                </div>
                <span className="rd-meta-value">{formatDateTime(run.createdAt)}</span>
              </div>

              <div className="rd-meta-card">
                <div className="rd-meta-card-header">
                  <span className="rd-meta-label">Completed</span>
                </div>
                <span className="rd-meta-value">{formatDateTime(run.completedAt)}</span>
              </div>

              {triggerMeta && (
                <div className="rd-meta-card rd-meta-card-trigger">
                  <div className="rd-meta-card-header">
                    <HiOutlineLightningBolt className="rd-meta-card-icon" />
                    <span className="rd-meta-label">Trigger</span>
                  </div>
                  <div className="rd-meta-trigger-row">
                    {triggerMeta.iconUrl && (
                      <img src={triggerMeta.iconUrl} alt="" className="rd-meta-trigger-icon app-logo-img" />
                    )}
                    <span className="rd-meta-value">{triggerMeta.stepName}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Overall failure banner with exact step identification */}
            {run.status === 'FAILED' && (
              <div className="rd-error-msg">
                <HiOutlineExclamationCircle className="rd-error-msg-icon" />
                <div className="rd-error-msg-content">
                  <div className="rd-error-msg-title">
                    Execution failed {failedStep ? `at Step ${failedMeta?.stepIndex || ''}: ${failedMeta?.stepName || ''}` : ''}
                  </div>
                  <div className="rd-error-msg-desc">
                    {failedStep?.errorMessage && failedStep.errorMessage !== 'One or more steps failed'
                      ? failedStep.errorMessage
                      : run.errorMessage && run.errorMessage !== 'One or more steps failed'
                        ? run.errorMessage
                        : 'One or more steps encountered an error during workflow execution.'}
                  </div>
                </div>
                {failedStep && (
                  <button
                    type="button"
                    className="rd-error-jump-btn"
                    onClick={() => {
                      setExpandedStep(failedStep.id);
                      setTimeout(() => {
                        document.getElementById(`step-run-${failedStep.id}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                      }, 50);
                    }}
                  >
                    View Error Details →
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Visual Execution Flow Pipeline Map */}
          {pipelineStages.length > 0 && (
            <div className="rd-flow-map-card">
              <div className="rd-flow-map-header">
                <div className="rd-flow-map-title-group">
                  <span className="rd-flow-map-title">Execution Pipeline</span>
                  {isBranchedWorkflow && (
                    <span className="rd-flow-badge-branched">Branched Flow</span>
                  )}
                  <span className="rd-flow-map-summary">
                    {displaySteps.filter((s) => s.status === 'SUCCESS').length} of {displaySteps.length} steps succeeded
                  </span>
                </div>
                <span className="rd-flow-map-hint">Click a step to view details</span>
              </div>

              <div className="rd-flow-track">
                {pipelineStages.map((stage, stageIdx) => (
                  <div key={stage.stageIndex} className="rd-flow-stage-wrapper">
                    <div className={`rd-flow-stage ${stage.isBranchStage ? 'is-branch-column' : ''}`}>
                      {stage.nodes.map((nodeItem) => {
                        const step = nodeItem.stepRun;
                        const meta = getStepMeta(step, nodeItem.index - 1);
                        const stepSc = statusConfig[step.status] || (nodeItem.isExecuted ? statusConfig.PENDING : statusConfig.SKIPPED);
                        const isExpanded = expandedStep === step.id;
                        const isFailed = step.status === 'FAILED';
                        const isSkipped = !nodeItem.isExecuted || step.status === 'SKIPPED';

                        return (
                          <button
                            key={step.id}
                            type="button"
                            className={`rd-flow-node ${stepSc.className} ${isExpanded ? 'active' : ''} ${isFailed ? 'failed' : ''} ${isSkipped ? 'is-skipped' : ''}`}
                            onClick={() => {
                              setExpandedStep(step.id);
                              const el = document.getElementById(`step-run-${step.id}`);
                              if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            }}
                            title={nodeItem.isExecuted
                              ? `Step ${nodeItem.index}: ${meta.stepName} (${stepSc.label}) — Click to inspect`
                              : `${meta.stepName} (Not executed on this run)`}
                          >
                            {nodeItem.branchLabel && (
                              <span className="rd-flow-branch-tag" title={`Branch: ${nodeItem.branchLabel}`}>
                                {nodeItem.branchLabel.toUpperCase()}
                              </span>
                            )}
                            <div className="rd-flow-node-top">
                              <span className="rd-flow-node-index">{nodeItem.index}</span>
                              <span className={`rd-flow-node-badge ${stepSc.className}`}>
                                {stepSc.icon}
                              </span>
                            </div>
                            <div className="rd-flow-node-icon-row">
                              {meta.iconUrl ? (
                                <img
                                  src={meta.iconUrl}
                                  alt={meta.appName}
                                  className="rd-flow-app-logo app-logo-img"
                                  loading="lazy"
                                  onError={(e) => { e.target.style.display = 'none'; }}
                                />
                              ) : meta.isTrigger ? (
                                <HiOutlineLightningBolt className="rd-flow-fallback-icon" />
                              ) : (
                                <HiOutlineCog className="rd-flow-fallback-icon" />
                              )}
                              <span className="rd-flow-node-app-name">{meta.appName}</span>
                            </div>
                            <div className="rd-flow-node-label" title={meta.rawName}>
                              {meta.rawName}
                            </div>
                            <div className="rd-flow-node-footer">
                              <span className={`rd-flow-status-pill ${stepSc.className}`}>
                                {nodeItem.isExecuted ? stepSc.label : 'Not Run'}
                              </span>
                              <span className="rd-flow-time">
                                {nodeItem.isExecuted ? formatDuration(step.createdAt, step.completedAt) : '—'}
                              </span>
                            </div>
                          </button>
                        );
                      })}
                    </div>

                    {stageIdx < pipelineStages.length - 1 && (
                      <div className="rd-flow-arrow-wrap">
                        <span className="rd-flow-arrow-line" />
                        <HiOutlineChevronRight className="rd-flow-arrow-head" />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Step runs list */}
          <div className="rd-steps-section">
            <h2 className="rd-steps-title">Step Runs</h2>

            {(!displaySteps || displaySteps.length === 0) && (
              <div className="rd-steps-empty">
                <HiOutlineClock />
                <span>No step runs recorded yet.</span>
              </div>
            )}

            {displaySteps && displaySteps.length > 0 && (
              <div className="rd-steps-list">
                {displaySteps.map((step, i) => {
                  const meta = getStepMeta(step, i);
                  const stepSc = statusConfig[step.status] || statusConfig.PENDING;
                  const isExpanded = expandedStep === step.id;
                  const isFailed = step.status === 'FAILED';

                  return (
                    <motion.div
                      id={`step-run-${step.id}`}
                      key={step.id}
                      className={`rd-step-card ${stepSc.className} ${isExpanded ? 'is-expanded' : ''} ${isFailed ? 'is-failed' : ''}`}
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.04, duration: 0.35 }}
                    >
                      <div
                        className="rd-step-header"
                        onClick={() => setExpandedStep(isExpanded ? null : step.id)}
                        title={isExpanded ? 'Collapse step details' : 'Expand step details'}
                        role="button"
                        tabIndex={0}
                      >
                        <div className="rd-step-left">
                          <span className="rd-step-index">{i + 1}</span>
                          {meta.iconUrl ? (
                            <img
                              src={meta.iconUrl}
                              alt={meta.appName}
                              className="rd-step-app-logo app-logo-img"
                              loading="lazy"
                              onError={(e) => { e.target.style.display = 'none'; }}
                            />
                          ) : meta.isTrigger ? (
                            <span className="rd-step-icon-badge trigger"><HiOutlineLightningBolt /></span>
                          ) : (
                            <span className="rd-step-icon-badge action"><HiOutlineCog /></span>
                          )}
                          <div className="rd-step-title-group">
                            <div className="rd-step-main-row">
                              <span className="rd-step-display-name">{meta.stepName}</span>
                              <span className={`rd-step-kind-badge ${meta.isTrigger ? 'trigger' : 'action'}`}>
                                {meta.isTrigger ? 'TRIGGER' : 'ACTION'}
                              </span>
                            </div>
                            <span className="rd-step-id-sub">
                              Step ID: {step.stepId.substring(0, 8)}…
                            </span>
                          </div>
                        </div>
                        <div className="rd-step-right">
                          <span className={`rd-status-badge sm ${stepSc.className}`}>
                            {stepSc.icon} {stepSc.label}
                          </span>
                          <span className="rd-step-duration">
                            {formatDuration(step.createdAt, step.completedAt)}
                          </span>
                          {isExpanded ? <HiOutlineChevronUp /> : <HiOutlineChevronDown />}
                        </div>
                      </div>

                      {isExpanded && (
                        <motion.div
                          className="rd-step-body"
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: 'auto' }}
                          transition={{ duration: 0.2 }}
                        >
                          {/* High-visibility Error / Notice Banner */}
                          {step.errorMessage && (
                            <div className={`rd-step-error-banner ${step.status === 'SKIPPED' ? 'skipped' : ''}`}>
                              <HiOutlineExclamationCircle className="rd-error-banner-icon" />
                              <div className="rd-error-banner-body">
                                <span className="rd-error-banner-title">
                                  {step.status === 'SKIPPED' ? 'Step Skipped' : 'Step Execution Failed'}
                                </span>
                                <span className="rd-error-banner-text">{step.errorMessage}</span>
                              </div>
                            </div>
                          )}

                          <div className="rd-step-data-grid">
                            {step.inputData && Object.keys(step.inputData).length > 0 && (
                              <div>
                                <div className="rd-section-header-bar">
                                  <p className="rd-section-label">Input Payload</p>
                                  <button
                                    className="rd-small-action-btn"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleCopy(`input-${step.id}`, formatJsonPayload(step.inputData));
                                    }}
                                    title="Copy input JSON"
                                  >
                                    {copiedKey === `input-${step.id}` ? <><HiCheck /> Copied</> : <><HiOutlineClipboardCopy /> Copy</>}
                                  </button>
                                </div>
                                <pre className="rd-json-block">
                                  {formatJsonPayload(step.inputData)}
                                </pre>
                              </div>
                            )}

                            {step.outputData && Object.keys(step.outputData).length > 0 && (
                              <div>
                                {Array.isArray(step.outputData.timeline) && step.outputData.timeline.length > 0 && (
                                  <AgentTimelineView
                                    timeline={step.outputData.timeline}
                                    iterations={step.outputData.iterations}
                                    tokensUsed={step.outputData.tokensUsed}
                                    status={step.outputData.status || (step.status === 'SUCCESS' ? 'COMPLETED' : 'FAILED')}
                                  />
                                )}

                                <div className="rd-section-header-bar">
                                  <p className="rd-section-label">
                                    {step.isTrigger
                                      ? 'Trigger Event Payload'
                                      : Array.isArray(step.outputData.timeline) && step.outputData.timeline.length > 0
                                        ? 'Raw Output Payload'
                                        : 'Output Payload'}
                                  </p>
                                  <div className="rd-action-group">
                                    <button
                                      className="rd-small-action-btn"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        handleCopy(`output-${step.id}`, formatJsonPayload(step.outputData));
                                      }}
                                      title="Copy output JSON"
                                    >
                                      {copiedKey === `output-${step.id}` ? <><HiCheck /> Copied</> : <><HiOutlineClipboardCopy /> Copy</>}
                                    </button>
                                    <button
                                      className="rd-download-btn"
                                      title="Download output payload as JSON"
                                      aria-label="Download output JSON"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        downloadFile(`step-${i + 1}-output.json`, formatJsonPayload(step.outputData));
                                      }}
                                    >
                                      <HiOutlineDownload /> Download
                                    </button>
                                  </div>
                                </div>
                                <pre className="rd-json-block">
                                  {formatJsonPayload(step.outputData)}
                                </pre>
                              </div>
                            )}
                          </div>

                          <div className="rd-step-footer-meta">
                            <div className="rd-step-times">
                              <span>Started: {formatDateTime(step.createdAt)}</span>
                              <span>Completed: {formatDateTime(step.completedAt)}</span>
                            </div>
                            <span className="rd-step-full-id">
                              Full ID: {step.stepId}
                            </span>
                          </div>
                        </motion.div>
                      )}
                    </motion.div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Collapsible Trigger Payload at bottom */}
          {run.triggerData && Object.keys(run.triggerData).length > 0 && (
            <div className="rd-trigger-bottom-card" id="rd-trigger-payload">
              <div
                className="rd-trigger-bottom-header"
                onClick={() => setShowTriggerPayload((prev) => !prev)}
                role="button"
                tabIndex={0}
                aria-expanded={showTriggerPayload}
              >
                <div className="rd-trigger-bottom-title-group">
                  <span className="rd-trigger-icon-wrap">
                    <HiOutlineCode />
                  </span>
                  <div>
                    <h3 className="rd-trigger-bottom-title">Initial Trigger Payload</h3>
                    <p className="rd-trigger-bottom-sub">
                      Raw incoming webhook / event data that started this workflow execution
                    </p>
                  </div>
                </div>
                <div className="rd-trigger-bottom-actions">
                  <button
                    type="button"
                    className="rd-small-action-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleCopy('triggerData', formatJsonPayload(run.triggerData));
                    }}
                    title="Copy trigger payload JSON"
                  >
                    {copiedKey === 'triggerData' ? <><HiCheck /> Copied</> : <><HiOutlineClipboardCopy /> Copy JSON</>}
                  </button>
                  <button
                    type="button"
                    className="rd-trigger-toggle-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      setShowTriggerPayload((prev) => !prev);
                    }}
                  >
                    {showTriggerPayload ? <><HiOutlineChevronUp /> Hide Payload</> : <><HiOutlineChevronDown /> View Payload</>}
                  </button>
                </div>
              </div>

              {showTriggerPayload && (
                <div className="rd-trigger-payload-content">
                  <pre className="rd-json-block">{formatJsonPayload(run.triggerData)}</pre>
                </div>
              )}
            </div>
          )}
        </motion.div>
      )}
    </div>
  );
}

