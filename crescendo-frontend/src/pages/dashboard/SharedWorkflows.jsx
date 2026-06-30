import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    HiOutlineLightningBolt,
    HiOutlineCheck,
    HiOutlineChevronRight,
    HiOutlineX,
    HiOutlinePuzzle,
} from 'react-icons/hi';
import { workflowApi } from '../../api/workflowApi';
import './SharedWorkflows.css';

export default function SharedWorkflows() {
    const navigate = useNavigate();

    const [workflows, setWorkflows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [importing, setImporting] = useState(false);
    const [imported, setImported] = useState([]); // list of imported workflow names

    const { shareId } = useParams();

    useEffect(() => {
        if (shareId) {
            workflowApi.getSharedTemplate(shareId)
                .then((data) => {
                    const parsedData = typeof data === 'string' ? JSON.parse(data) : data;
                    if (!parsedData || parsedData.length === 0) {
                        setError('No workflows found in this share link.');
                    } else {
                        setWorkflows(parsedData);
                    }
                })
                .catch(() => setError('Share link expired or invalid.'))
                .finally(() => setLoading(false));
            return;
        }

        setError('Invalid share link — no workflow data found.');
        setLoading(false);
    }, [shareId]);

    const currentWorkflow = workflows[currentIndex];
    const isLast = currentIndex >= workflows.length - 1;

    const handleImport = async () => {
        if (!currentWorkflow) return;
        setImporting(true);
        try {
            await workflowApi.importWorkflow({
                name: currentWorkflow.name,
                description: currentWorkflow.description,
                steps: currentWorkflow.steps?.map((s) => ({
                    name: s.name,
                    type: s.type,
                    actionKey: s.actionKey,
                    appKey: s.appKey,
                    order: s.order,
                    configuration: s.configuration,
                })) || [],
            });
            setImported((prev) => [...prev, currentWorkflow.name]);
            if (isLast) {
                setTimeout(() => navigate('/dashboard/workflows'), 800);
            } else {
                setCurrentIndex((i) => i + 1);
            }
        } catch {
            // Could add error toast here
        } finally {
            setImporting(false);
        }
    };

    const handleSkip = () => {
        if (isLast) {
            navigate('/dashboard/workflows');
        } else {
            setCurrentIndex((i) => i + 1);
        }
    };

    if (loading) {
        return (
            <div className="sw-page">
                <div className="sw-loading">
                    <div className="wf-spinner" style={{ width: 24, height: 24 }} />
                    <p>Loading shared workflows…</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="sw-page">
                <div className="sw-error-state">
                    <HiOutlineX style={{ fontSize: 28, color: '#ef4444' }} />
                    <h3>{error}</h3>
                    <button className="sw-back-btn" onClick={() => navigate('/dashboard/workflows')}>
                        Go to Workflows
                    </button>
                </div>
            </div>
        );
    }

    if (!currentWorkflow) {
        return (
            <div className="sw-page">
                <div className="sw-done-state">
                    <HiOutlineCheck style={{ fontSize: 32, color: '#22c55e' }} />
                    <h3>All done!</h3>
                    <p>{imported.length} workflow{imported.length !== 1 ? 's' : ''} imported.</p>
                    <button className="sw-back-btn" onClick={() => navigate('/dashboard/workflows')}>
                        Go to Workflows
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="sw-page">
            {/* Progress */}
            <div className="sw-progress">
                <div className="sw-progress-text">
                    Workflow {currentIndex + 1} of {workflows.length}
                </div>
                <div className="sw-progress-bar">
                    <motion.div
                        className="sw-progress-fill"
                        animate={{ width: `${((currentIndex + 1) / workflows.length) * 100}%` }}
                        transition={{ duration: 0.3 }}
                    />
                </div>
            </div>

            {/* Workflow card */}
            <AnimatePresence mode="wait">
                <motion.div
                    key={currentWorkflow.id}
                    className="sw-card"
                    initial={{ opacity: 0, x: 40 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -40 }}
                    transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
                >
                    <div className="sw-card-header">
                        <div className="sw-card-icon">
                            <HiOutlineLightningBolt />
                        </div>
                        <div>
                            <h2 className="sw-card-name">{currentWorkflow.name}</h2>
                            {currentWorkflow.description && (
                                <p className="sw-card-desc">{currentWorkflow.description}</p>
                            )}
                        </div>
                    </div>

                    {/* Steps list */}
                    <div className="sw-steps-section">
                        <h4 className="sw-steps-title">
                            Steps ({currentWorkflow.steps?.length || 0})
                        </h4>
                        {currentWorkflow.steps && currentWorkflow.steps.length > 0 ? (
                            <div className="sw-steps-list">
                                {currentWorkflow.steps
                                    .sort((a, b) => (a.order || 0) - (b.order || 0))
                                    .map((step, idx) => (
                                    <div className="sw-step" key={idx}>
                                        <div className="sw-step-number">{idx + 1}</div>
                                        <div className="sw-step-info">
                                            <span className="sw-step-name">{step.name}</span>
                                            <span className="sw-step-meta">
                                                <span className="sw-step-type">{step.type}</span>
                                                <span className="sw-step-app">{step.appKey}</span>
                                                <HiOutlineChevronRight style={{ fontSize: 10, opacity: 0.4 }} />
                                                <span className="sw-step-action">{step.actionKey}</span>
                                            </span>
                                        </div>
                                        <div className="sw-step-icon">
                                            <HiOutlinePuzzle />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="sw-no-steps">No steps in this workflow.</p>
                        )}
                    </div>

                    {/* Import notice */}
                    <div className="sw-notice">
                        <HiOutlineLightningBolt />
                        Steps requiring connections (OAuth) will need to be connected after import.
                    </div>
                </motion.div>
            </AnimatePresence>

            {/* Actions */}
            <div className="sw-actions">
                <button className="sw-skip-btn" onClick={handleSkip} disabled={importing}>
                    {isLast ? 'Finish' : 'Skip'}
                </button>
                <button className="sw-import-btn" onClick={handleImport} disabled={importing}>
                    {importing ? (
                        <><span className="wf-spinner" /> Importing…</>
                    ) : (
                        <><HiOutlineCheck /> Add to my workflows</>
                    )}
                </button>
            </div>

            {/* Imported count */}
            {imported.length > 0 && (
                <div className="sw-imported-count">
                    ✓ {imported.length} imported so far
                </div>
            )}
        </div>
    );
}
