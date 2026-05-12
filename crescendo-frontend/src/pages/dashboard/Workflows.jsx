import { useEffect, useState, useCallback } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    HiPlus,
    HiOutlineLightningBolt,
    HiOutlineTrash,
    HiOutlinePencil,
    HiOutlinePlay,
    HiOutlinePause,
    HiOutlineClock,
    HiOutlineDotsVertical,
    HiOutlineExclamationCircle,
    HiOutlineShare,
    HiOutlineCheck,
    HiOutlineClipboardCopy,
} from 'react-icons/hi';
import useWorkflowStore from '../../store/workflowStore';
import './Workflows.css';

const fadeIn = {
    hidden: { opacity: 0, y: 16 },
    visible: (i) => ({
        opacity: 1, y: 0,
        transition: { delay: i * 0.05, duration: 0.45, ease: [0.22, 1, 0.36, 1] },
    }),
};

function formatRelative(dateStr) {
    if (!dateStr) return null;
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
}

function generateShareLink(ids) {
    const encoded = btoa(ids.join(','));
    const origin = window.location.origin;
    return `${origin}/shared#ids=${encoded}`;
}

export default function Workflows() {
    const navigate = useNavigate();
    const location = useLocation();
    const {
        workflows, isLoading, error,
        fetchWorkflows, deleteWorkflow, activateWorkflow, deactivateWorkflow,
        bulkActivate, bulkDeactivate,
    } = useWorkflowStore();

    const [menuOpen, setMenuOpen] = useState(null);
    const [deleting, setDeleting] = useState(null);
    const [toggling, setToggling] = useState(null);

    // Multi-select state
    const [selectMode, setSelectMode] = useState(false);
    const [selected, setSelected] = useState(new Set());
    const [bulkAction, setBulkAction] = useState(null); // 'activating' | 'deactivating'
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        fetchWorkflows();
    }, [fetchWorkflows, location.key]);

    // Exit select mode resets selection
    const exitSelectMode = useCallback(() => {
        setSelectMode(false);
        setSelected(new Set());
    }, []);

    const toggleSelect = (id) => {
        setSelected((prev) => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    };

    const selectAll = () => {
        if (selected.size === workflows.length) {
            setSelected(new Set());
        } else {
            setSelected(new Set(workflows.map((w) => w.id)));
        }
    };

    const handleToggleActive = async (wf) => {
        setToggling(wf.id);
        try {
            if (wf.isActive) await deactivateWorkflow(wf.id);
            else await activateWorkflow(wf.id);
        } catch { /* handled by store */ } finally {
            setToggling(null);
        }
    };

    const handleDelete = async (id) => {
        setDeleting(id);
        try { await deleteWorkflow(id); } finally {
            setDeleting(null);
            setMenuOpen(null);
        }
    };

    const handleBulkActivate = async () => {
        setBulkAction('activating');
        try {
            await bulkActivate([...selected]);
        } finally {
            setBulkAction(null);
        }
    };

    const handleBulkDeactivate = async () => {
        setBulkAction('deactivating');
        try {
            await bulkDeactivate([...selected]);
        } finally {
            setBulkAction(null);
        }
    };

    const handleShare = (ids) => {
        const link = generateShareLink(ids);
        navigator.clipboard.writeText(link).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        });
    };

    const handleShareSelected = () => handleShare([...selected]);
    const handleShareSingle = (id) => handleShare([id]);

    // Close menu on outside click
    useEffect(() => {
        const close = () => setMenuOpen(null);
        if (menuOpen) window.addEventListener('click', close);
        return () => window.removeEventListener('click', close);
    }, [menuOpen]);

    return (
        <div className="wf-page">
            {/* Header */}
            <div className="wf-header">
                <div>
                    <h1 className="wf-title">Workflows</h1>
                    <p className="wf-subtitle">
                        {workflows.length === 0 && !isLoading
                            ? 'No workflows yet. Create your first one.'
                            : `${workflows.length} workflow${workflows.length !== 1 ? 's' : ''}`}
                    </p>
                </div>
                <div className="wf-header-actions">
                    {workflows.length > 0 && (
                        <button
                            className={`wf-select-toggle ${selectMode ? 'active' : ''}`}
                            onClick={() => selectMode ? exitSelectMode() : setSelectMode(true)}
                        >
                            {selectMode ? 'Cancel' : 'Select'}
                        </button>
                    )}
                    <Link to="/dashboard/workflows/new" className="wf-create-btn">
                        <HiPlus /> New Workflow
                    </Link>
                </div>
            </div>

            {/* Select-all bar */}
            <AnimatePresence>
                {selectMode && workflows.length > 0 && (
                    <motion.div
                        className="wf-select-bar"
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                    >
                        <label className="wf-select-all" onClick={selectAll}>
                            <span className={`wf-checkbox ${selected.size === workflows.length ? 'checked' : ''}`}>
                                {selected.size === workflows.length && <HiOutlineCheck />}
                            </span>
                            Select all ({selected.size}/{workflows.length})
                        </label>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* Error */}
            {error && (
                <div className="wf-error">
                    <HiOutlineExclamationCircle />
                    {error}
                </div>
            )}

            {/* Loading */}
            {isLoading && (
                <div className="wf-grid">
                    {[1, 2, 3].map((i) => (
                        <div key={i} className="wf-card wf-card-skeleton" />
                    ))}
                </div>
            )}

            {/* Empty */}
            {!isLoading && workflows.length === 0 && !error && (
                <motion.div
                    className="wf-empty"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5 }}
                >
                    <div className="wf-empty-icon"><HiOutlineLightningBolt /></div>
                    <h3>No workflows yet</h3>
                    <p>Build your first automation to get started.</p>
                    <Link to="/dashboard/workflows/new" className="wf-create-btn">
                        <HiPlus /> Create Workflow
                    </Link>
                </motion.div>
            )}

            {/* Workflow cards */}
            {!isLoading && workflows.length > 0 && (
                <div className="wf-grid">
                    {workflows.map((wf, i) => (
                        <motion.div
                            key={wf.id}
                            className={`wf-card ${selectMode && selected.has(wf.id) ? 'wf-card-selected' : ''}`}
                            custom={i}
                            variants={fadeIn}
                            initial="hidden"
                            animate="visible"
                            onClick={selectMode
                                ? () => toggleSelect(wf.id)
                                : () => navigate(`/dashboard/workflows/${wf.id}`)
                            }
                            style={{ cursor: 'pointer' }}
                        >
                            {/* Checkbox overlay in select mode */}
                            {selectMode && (
                                <span className={`wf-card-checkbox ${selected.has(wf.id) ? 'checked' : ''}`}>
                                    {selected.has(wf.id) && <HiOutlineCheck />}
                                </span>
                            )}

                            {/* Card top */}
                            <div className="wf-card-top">
                                <div className="wf-card-icon">
                                    <HiOutlineLightningBolt />
                                </div>
                                <div className="wf-card-meta">
                                    <div className="wf-card-name">{wf.name}</div>
                                    {wf.description && (
                                        <div className="wf-card-desc">{wf.description}</div>
                                    )}
                                </div>
                                {/* Three-dot menu (hidden in select mode) */}
                                {!selectMode && (
                                    <div
                                        className="wf-card-menu-wrap"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setMenuOpen(menuOpen === wf.id ? null : wf.id);
                                        }}
                                    >
                                        <button className="wf-card-menu-btn">
                                            <HiOutlineDotsVertical />
                                        </button>
                                        <AnimatePresence>
                                            {menuOpen === wf.id && (
                                                <motion.div
                                                    className="wf-dropdown"
                                                    initial={{ opacity: 0, scale: 0.92 }}
                                                    animate={{ opacity: 1, scale: 1 }}
                                                    exit={{ opacity: 0, scale: 0.92 }}
                                                    transition={{ duration: 0.12 }}
                                                    onClick={(e) => e.stopPropagation()}
                                                >
                                                    <button
                                                        className="wf-dropdown-item"
                                                        onClick={() => {
                                                            setMenuOpen(null);
                                                            handleShareSingle(wf.id);
                                                        }}
                                                    >
                                                        <HiOutlineShare /> Share
                                                    </button>
                                                    <button
                                                        className="wf-dropdown-item danger"
                                                        onClick={() => handleDelete(wf.id)}
                                                        disabled={deleting === wf.id}
                                                    >
                                                        <HiOutlineTrash />
                                                        {deleting === wf.id ? 'Deleting…' : 'Delete'}
                                                    </button>
                                                </motion.div>
                                            )}
                                        </AnimatePresence>
                                    </div>
                                )}
                            </div>

                            {/* Stats row */}
                            <div className="wf-card-stats">
                                <span className="wf-stat">
                                    <HiOutlineLightningBolt />
                                    {wf.stepCount} step{wf.stepCount !== 1 ? 's' : ''}
                                </span>
                                {wf.lastRunAt && (
                                    <span className="wf-stat">
                                        <HiOutlineClock />
                                        {formatRelative(wf.lastRunAt)}
                                    </span>
                                )}
                            </div>

                            {/* Footer */}
                            <div className="wf-card-footer">
                                <span className={`wf-status-badge ${wf.isActive ? 'active' : 'inactive'}`}>
                                    {wf.isActive ? 'Active' : 'Inactive'}
                                </span>
                                {!selectMode && (
                                    <button
                                        className={`wf-toggle-btn ${wf.isActive ? 'pause' : 'play'}`}
                                        onClick={(e) => { e.stopPropagation(); handleToggleActive(wf); }}
                                        disabled={toggling === wf.id}
                                        title={wf.isActive ? 'Deactivate' : 'Activate'}
                                    >
                                        {toggling === wf.id ? (
                                            <span className="wf-spinner" />
                                        ) : wf.isActive ? (
                                            <><HiOutlinePause /> Deactivate</>
                                        ) : (
                                            <><HiOutlinePlay /> Activate</>
                                        )}
                                    </button>
                                )}
                            </div>
                        </motion.div>
                    ))}
                </div>
            )}

            {/* Floating bulk-action bar */}
            <AnimatePresence>
                {selectMode && selected.size > 0 && (
                    <motion.div
                        className="wf-bulk-bar"
                        initial={{ opacity: 0, y: 40 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: 40 }}
                        transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
                    >
                        <span className="wf-bulk-count">{selected.size} selected</span>
                        <div className="wf-bulk-actions">
                            <button
                                className="wf-bulk-btn activate"
                                onClick={handleBulkActivate}
                                disabled={!!bulkAction}
                            >
                                {bulkAction === 'activating' ? <span className="wf-spinner" /> : <HiOutlinePlay />}
                                Activate All
                            </button>
                            <button
                                className="wf-bulk-btn deactivate"
                                onClick={handleBulkDeactivate}
                                disabled={!!bulkAction}
                            >
                                {bulkAction === 'deactivating' ? <span className="wf-spinner" /> : <HiOutlinePause />}
                                Deactivate All
                            </button>
                            <button
                                className="wf-bulk-btn share"
                                onClick={handleShareSelected}
                            >
                                <HiOutlineShare />
                                Share
                            </button>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* Copied toast */}
            <AnimatePresence>
                {copied && (
                    <motion.div
                        className="wf-toast"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: 20 }}
                    >
                        <HiOutlineClipboardCopy /> Share link copied to clipboard!
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
