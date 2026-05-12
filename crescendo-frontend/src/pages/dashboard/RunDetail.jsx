import { useEffect, useState } from 'react';
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
  HiOutlineBan,
  HiOutlineLightningBolt,
  HiOutlineMinusCircle,
  HiOutlineDownload,
} from 'react-icons/hi';
import useLogbookStore from '../../store/logbookStore';
import useWorkflowStore from '../../store/workflowStore';
import './RunDetail.css';

function formatDateTime(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString();
}

function formatDuration(start, end) {
  if (!start || !end) return '—';
  const ms = new Date(end).getTime() - new Date(start).getTime();
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
    fetchRunDetail, clearRunDetail, cancelRun,
  } = useLogbookStore();

  const { workflows, fetchWorkflows } = useWorkflowStore();
  const [expandedStep, setExpandedStep] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    fetchRunDetail(workflowId, runId);
    fetchWorkflows();
    return () => clearRunDetail();
  }, [workflowId, runId, fetchRunDetail, clearRunDetail, fetchWorkflows]);

  const workflowName = workflows.find((w) => w.id === workflowId)?.name || 'Workflow';
  const run = runDetail;
  const sc = run ? (statusConfig[run.status] || statusConfig.PENDING) : null;

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

  const canCancel = run && (run.status === 'PENDING' || run.status === 'RUNNING');

  return (
    <div className="rd-page">
      {/* Back button */}
      <button className="rd-back-btn" onClick={() => navigate('/dashboard/history')}>
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
                  <p className="rd-run-id">Run {run.id.substring(0, 8)}…</p>
                </div>
              </div>
              <div className="rd-header-actions">
                <span className={`rd-status-badge ${sc.className}`}>
                  {sc.icon} {sc.label}
                </span>
                <button
                  className="rd-export-btn"
                  onClick={() => {
                    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(run, null, 2));
                    const downloadAnchorNode = document.createElement('a');
                    downloadAnchorNode.setAttribute("href", dataStr);
                    downloadAnchorNode.setAttribute("download", `crescendo-run-${run.id.substring(0, 8)}.json`);
                    document.body.appendChild(downloadAnchorNode);
                    downloadAnchorNode.click();
                    downloadAnchorNode.remove();
                  }}
                  title="Export Run Log"
                >
                  <HiOutlineDownload /> Export Log
                </button>
                {canCancel && (
                  <button
                    className="rd-cancel-btn"
                    onClick={handleCancel}
                    disabled={cancelling}
                  >
                    <HiOutlineBan />
                    {cancelling ? 'Cancelling…' : 'Cancel Run'}
                  </button>
                )}
              </div>
            </div>

            {/* Meta row */}
            <div className="rd-meta-grid">
              <div className="rd-meta-item">
                <span className="rd-meta-label">Started</span>
                <span className="rd-meta-value">{formatDateTime(run.createdAt)}</span>
              </div>
              <div className="rd-meta-item">
                <span className="rd-meta-label">Completed</span>
                <span className="rd-meta-value">{formatDateTime(run.completedAt)}</span>
              </div>
              <div className="rd-meta-item">
                <span className="rd-meta-label">Duration</span>
                <span className="rd-meta-value">{formatDuration(run.createdAt, run.completedAt)}</span>
              </div>
              <div className="rd-meta-item">
                <span className="rd-meta-label">Steps</span>
                <span className="rd-meta-value">{run.stepRuns?.length || 0}</span>
              </div>
            </div>

            {/* Error message */}
            {run.errorMessage && (
              <div className="rd-error-msg">
                <HiOutlineExclamationCircle />
                {run.errorMessage}
              </div>
            )}

            {/* Trigger data */}
            {run.triggerData && Object.keys(run.triggerData).length > 0 && (
              <div className="rd-trigger-section">
                <p className="rd-section-label">Trigger Data</p>
                <pre className="rd-json-block">{JSON.stringify(run.triggerData, null, 2)}</pre>
              </div>
            )}
          </div>

          {/* Step runs */}
          <div className="rd-steps-section">
            <h2 className="rd-steps-title">Step Runs</h2>

            {(!run.stepRuns || run.stepRuns.length === 0) && (
              <div className="rd-steps-empty">
                <HiOutlineClock />
                <span>No step runs recorded yet.</span>
              </div>
            )}

            {run.stepRuns && run.stepRuns.length > 0 && (
              <div className="rd-steps-list">
                {run.stepRuns.map((step, i) => {
                  const stepSc = statusConfig[step.status] || statusConfig.PENDING;
                  const isExpanded = expandedStep === step.id;

                  return (
                    <motion.div
                      key={step.id}
                      className={`rd-step-card ${stepSc.className}`}
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.04, duration: 0.35 }}
                    >
                      <div
                        className="rd-step-header"
                        onClick={() => setExpandedStep(isExpanded ? null : step.id)}
                      >
                        <div className="rd-step-left">
                          <span className="rd-step-index">{i + 1}</span>
                          <span className={`rd-step-status-dot ${stepSc.className}`} />
                          <span className="rd-step-id">
                            Step {step.stepId.substring(0, 8)}…
                          </span>
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
                          {step.errorMessage && (
                            <div className="rd-step-error">
                              <HiOutlineExclamationCircle />
                              {step.errorMessage}
                            </div>
                          )}

                          <div className="rd-step-data-grid">
                            {step.inputData && Object.keys(step.inputData).length > 0 && (
                              <div>
                                <p className="rd-section-label">Input</p>
                                <pre className="rd-json-block">
                                  {JSON.stringify(step.inputData, null, 2)}
                                </pre>
                              </div>
                            )}
                            {step.outputData && Object.keys(step.outputData).length > 0 && (
                              <div>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                  <p className="rd-section-label">Output</p>
                                  <button
                                    className="rd-download-btn"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      const json = JSON.stringify(step.outputData, null, 2);
                                      const blob = new Blob([json], { type: 'application/json' });
                                      const url = URL.createObjectURL(blob);
                                      const a = document.createElement('a');
                                      a.href = url;
                                      a.download = `step-${i + 1}-output.json`;
                                      document.body.appendChild(a);
                                      a.click();
                                      document.body.removeChild(a);
                                      URL.revokeObjectURL(url);
                                    }}
                                  >
                                    <HiOutlineDownload /> Download
                                  </button>
                                </div>
                                <pre className="rd-json-block">
                                  {JSON.stringify(step.outputData, null, 2)}
                                </pre>
                              </div>
                            )}
                          </div>

                          <div className="rd-step-times">
                            <span>Started: {formatDateTime(step.createdAt)}</span>
                            <span>Completed: {formatDateTime(step.completedAt)}</span>
                          </div>
                        </motion.div>
                      )}
                    </motion.div>
                  );
                })}
              </div>
            )}
          </div>
        </motion.div>
      )}
    </div>
  );
}
