import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlineClock,
  HiOutlineCheckCircle,
  HiOutlineXCircle,
  HiOutlineRefresh,
  HiOutlineExclamationCircle,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlineFilter,
  HiOutlineLightningBolt,
} from 'react-icons/hi';
import useLogbookStore from '../../store/logbookStore';
import { useWorkflowList } from '../../hooks/useWorkflows';
import './History.css';

const fadeIn = {
  hidden: { opacity: 0, y: 16 },
  visible: (i) => ({
    opacity: 1, y: 0,
    transition: { delay: i * 0.05, duration: 0.45, ease: [0.22, 1, 0.36, 1] },
  }),
};

function formatRelative(dateStr) {
  if (!dateStr) return '—';
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString();
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
};

export default function History() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const currentPage = parseInt(searchParams.get('page') || '0', 10);
  const [statusFilter, setStatusFilter] = useState('ALL');

  const { runs, page, isLoading, error, fetchAllRuns } = useLogbookStore();
  const { data: workflows = [] } = useWorkflowList();

  useEffect(() => {
    fetchAllRuns(currentPage);
  }, [fetchAllRuns, currentPage]);

  const workflowNames = {};
  workflows.forEach((w) => { workflowNames[w.id] = w.name; });

  const filteredRuns = statusFilter === 'ALL'
    ? runs
    : runs.filter((r) => r.status === statusFilter);

  const goToPage = (p) => {
    setSearchParams({ page: p.toString() });
  };

  return (
    <div className="hist-page">
      {/* Header */}
      <div className="hist-header">
        <div>
          <h1 className="hist-title">Run History</h1>
          <p className="hist-subtitle">
            {page ? `${page.totalElements} total run${page.totalElements !== 1 ? 's' : ''}` : 'Loading...'}
          </p>
        </div>
        {/* Status filter */}
        <div className="hist-filters">
          <HiOutlineFilter className="hist-filter-icon" />
          {['ALL', 'PENDING', 'RUNNING', 'SUCCESS', 'FAILED'].map((s) => (
            <button
              key={s}
              className={`hist-filter-btn ${statusFilter === s ? 'active' : ''}`}
              onClick={() => setStatusFilter(s)}
            >
              {s === 'ALL' ? 'All' : statusConfig[s]?.label || s}
            </button>
          ))}
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="hist-error">
          <HiOutlineExclamationCircle />
          {error}
        </div>
      )}

      {/* Loading skeleton */}
      {isLoading && (
        <div className="hist-list">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="hist-row hist-row-skeleton" />
          ))}
        </div>
      )}

      {/* Active Workflows Section */}
      {!isLoading && workflows.length > 0 && (() => {
        const activeWfs = workflows.filter((w) => w.isActive);
        if (activeWfs.length === 0) return null;
        return (
          <div className="hist-active-section">
            <h2 className="hist-active-title">
              <span className="hist-active-dot" /> Active Workflows ({activeWfs.length})
            </h2>
            <div className="hist-active-list">
              {activeWfs.map((w) => (
                <div
                  key={w.id}
                  className="hist-active-card"
                  onClick={() => navigate(`/dashboard/workflows/${w.id}`)}
                >
                  <span className="hist-active-name">{w.name}</span>
                  <span className="hist-active-steps">{w.stepCount} step{w.stepCount !== 1 ? 's' : ''}</span>
                </div>
              ))}
            </div>
          </div>
        );
      })()}

      {/* Empty state */}
      {!isLoading && runs.length === 0 && !error && (
        <motion.div
          className="hist-empty"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
        >
          <div className="hist-empty-icon"><HiOutlineClock /></div>
          <h3>No runs yet</h3>
          <p>Activate a workflow and trigger it to see run history here.</p>
          <Link to="/dashboard/workflows" className="hist-link-btn">
            <HiOutlineLightningBolt /> Go to Workflows
          </Link>
        </motion.div>
      )}

      {/* Run list */}
      {!isLoading && filteredRuns.length > 0 && (
        <>
          <div className="hist-list">
            {/* Header row */}
            <div className="hist-row hist-row-header">
              <span className="hist-col hist-col-status">Status</span>
              <span className="hist-col hist-col-workflow">Workflow</span>
              <span className="hist-col hist-col-steps">Steps</span>
              <span className="hist-col hist-col-duration">Duration</span>
              <span className="hist-col hist-col-time">Started</span>
            </div>

            {filteredRuns.map((run, i) => {
              const sc = statusConfig[run.status] || statusConfig.PENDING;
              return (
                <motion.div
                  key={run.id}
                  className="hist-row hist-row-data"
                  custom={i}
                  variants={fadeIn}
                  initial="hidden"
                  animate="visible"
                  onClick={() => navigate(`/dashboard/history/${run.workflowId}/${run.id}`)}
                >
                  <span className={`hist-col hist-col-status`}>
                    <span className={`hist-status-badge ${sc.className}`}>
                      {sc.icon} {sc.label}
                    </span>
                  </span>
                  <span className="hist-col hist-col-workflow">
                    <span className="hist-wf-name">
                      {workflowNames[run.workflowId] || run.workflowId.substring(0, 8) + '…'}
                    </span>
                  </span>
                  <span className="hist-col hist-col-steps">
                    <span className="hist-step-count">
                      {run.completedSteps}/{run.totalSteps}
                      {run.failedSteps > 0 && (
                        <span className="hist-failed-count"> ({run.failedSteps} failed)</span>
                      )}
                    </span>
                    {run.totalSteps > 0 && (
                      <div className="hist-step-bar">
                        <div
                          className="hist-step-bar-fill"
                          style={{ width: `${Math.round(((run.completedSteps + run.failedSteps) / run.totalSteps) * 100)}%` }}
                        />
                      </div>
                    )}
                  </span>
                  <span className="hist-col hist-col-duration">
                    {formatDuration(run.createdAt, run.completedAt)}
                  </span>
                  <span className="hist-col hist-col-time">
                    {formatRelative(run.createdAt)}
                  </span>
                </motion.div>
              );
            })}
          </div>

          {/* Pagination */}
          {page && page.totalPages > 1 && (
            <div className="hist-pagination">
              <button
                className="hist-page-btn"
                disabled={currentPage === 0}
                onClick={() => goToPage(currentPage - 1)}
              >
                <HiOutlineChevronLeft /> Previous
              </button>
              <span className="hist-page-info">
                Page {currentPage + 1} of {page.totalPages}
              </span>
              <button
                className="hist-page-btn"
                disabled={currentPage + 1 >= page.totalPages}
                onClick={() => goToPage(currentPage + 1)}
              >
                Next <HiOutlineChevronRight />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
