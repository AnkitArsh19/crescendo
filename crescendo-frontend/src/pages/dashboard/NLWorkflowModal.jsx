import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { HiX, HiOutlineLightningBolt, HiOutlineSparkles, HiOutlineCheckCircle } from 'react-icons/hi';
import { aiApi } from '../../api/aiApi';
import { workflowApi } from '../../api/workflowApi';
import useToastStore from '../../store/toastStore';
import './NLWorkflowModal.css';

const EXAMPLES = [
    'Send a Slack message when a GitHub PR is merged',
    'Add a row in Google Sheets when a Typeform is submitted',
    'Email me when a new contact is added',
    'Post to Discord when a new workflow run fails',
];

/**
 * NLWorkflowModal — Natural Language Workflow Builder
 *
 * Opens from the Workflows page. User types a plain-English description,
 * the backend proxies it to the Python AI service, and the returned draft
 * is previewed before being saved as a real workflow.
 */
export default function NLWorkflowModal({ onClose }) {
    const navigate = useNavigate();
    const { success: toastSuccess, error: toastError } = useToastStore();

    const [prompt, setPrompt] = useState('');
    const [draft, setDraft] = useState(null);       // { name, steps: [...] }
    const [generating, setGenerating] = useState(false);
    const [creating, setCreating] = useState(false);
    const [error, setError] = useState(null);
    const [unavailable, setUnavailable] = useState(false);

    // ── Generate draft from AI ───────────────────────────────────────────────
    async function handleGenerate() {
        if (!prompt.trim()) return;
        setGenerating(true);
        setError(null);
        setUnavailable(false);
        setDraft(null);

        try {
            const data = await aiApi.createWorkflowDraft(prompt.trim());
            setDraft(data.workflow);
        } catch (err) {
            if (err.response?.status === 503) {
                setUnavailable(true);
            } else {
                const msg = err.response?.data?.message || 'Failed to generate workflow. Please try again.';
                setError(msg);
                toastError(msg);
            }
        } finally {
            setGenerating(false);
        }
    }

    // ── Create workflow from draft ───────────────────────────────────────────
    async function handleCreate() {
        if (!draft) return;
        setCreating(true);
        try {
            // 1. Create the empty workflow
            const created = await workflowApi.create({ name: draft.name });

            // 2. Format and save the steps
            if (draft.steps && draft.steps.length > 0) {
                const graphSteps = [];
                let parentStepId = null;

                for (let i = 0; i < draft.steps.length; i++) {
                    const step = draft.steps[i];
                    const clientId = crypto.randomUUID();
                    graphSteps.push({
                        clientId,
                        type: i === 0 ? 'TRIGGER' : 'ACTION',
                        name: step.label || step.actionKey,
                        actionKey: step.actionKey,
                        appKey: step.appKey,
                        parentStepId,
                        configuration: step.config || {}
                    });
                    parentStepId = clientId;
                }

                await workflowApi.updateGraph(created.id, {
                    revision: null,
                    steps: graphSteps,
                    deletedStepIds: []
                });
            }

            toastSuccess(`"${draft.name}" created — configure the steps on the canvas.`);
            onClose();
            navigate(`/dashboard/workflows/${created.id}`);
        } catch (err) {
            const msg = err.response?.data?.message || 'Failed to create workflow.';
            toastError(msg);
        } finally {
            setCreating(false);
        }
    }

    // ── Close on overlay click ───────────────────────────────────────────────
    function handleOverlayClick(e) {
        if (e.target === e.currentTarget) onClose();
    }

    // ── Keyboard: Ctrl/Cmd+Enter to generate ────────────────────────────────
    function handleKeyDown(e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') handleGenerate();
    }

    return (
        <div className="nlwf-overlay" onClick={handleOverlayClick}>
            <div className="nlwf-modal" role="dialog" aria-modal="true" aria-label="Build workflow with AI">

                {/* Header */}
                <div className="nlwf-header">
                    <div className="nlwf-header-text">
                        <h2>
                            <HiOutlineSparkles />
                            Build with AI
                        </h2>
                        <p>Describe what you want to automate in plain English.</p>
                    </div>
                    <button className="nlwf-close" onClick={onClose} aria-label="Close">
                        <HiX />
                    </button>
                </div>

                {/* Prompt */}
                <div>
                    <span className="nlwf-prompt-label">What should this workflow do?</span>
                    <textarea
                        className="nlwf-textarea"
                        placeholder="e.g. Send a Slack message when a GitHub PR is merged"
                        value={prompt}
                        onChange={(e) => setPrompt(e.target.value)}
                        onKeyDown={handleKeyDown}
                        rows={4}
                        autoFocus
                    />
                    {/* Example chips */}
                    <div className="nlwf-examples">
                        {EXAMPLES.map((ex) => (
                            <button
                                key={ex}
                                className="nlwf-example-chip"
                                onClick={() => setPrompt(ex)}
                                type="button"
                            >
                                {ex}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Unavailable notice */}
                {unavailable && (
                    <div className="nlwf-unavailable">
                        ⚠️ The AI service is not configured yet. Ask your team to set up the Python microservice.
                    </div>
                )}

                {/* Error */}
                {error && !unavailable && (
                    <div className="nlwf-error">
                        {error}
                    </div>
                )}

                {/* Draft preview */}
                {draft && (
                    <div className="nlwf-draft">
                        <div className="nlwf-draft-title">
                            <HiOutlineCheckCircle style={{ color: '#22c55e' }} />
                            {draft.name}
                        </div>
                        <div className="nlwf-draft-steps">
                            {(draft.steps || []).map((step, i) => (
                                <div key={i} className="nlwf-step-row">
                                    <span className="nlwf-step-num">{i + 1}</span>
                                    <span className="nlwf-step-label">{step.label || step.actionKey}</span>
                                    {step.appKey && (
                                        <span className="nlwf-step-app">· {step.appKey}</span>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Footer actions */}
                <div className="nlwf-actions">
                    <button className="nlwf-cancel-btn" onClick={onClose} type="button">
                        Cancel
                    </button>

                    {!draft ? (
                        <button
                            className="nlwf-generate-btn"
                            onClick={handleGenerate}
                            disabled={!prompt.trim() || generating}
                            type="button"
                        >
                            {generating ? (
                                <>
                                    <span className="nlwf-spinner" />
                                    Generating…
                                </>
                            ) : (
                                <>
                                    <HiOutlineLightningBolt />
                                    Generate
                                </>
                            )}
                        </button>
                    ) : (
                        <>
                            <button
                                className="nlwf-cancel-btn"
                                onClick={() => setDraft(null)}
                                type="button"
                            >
                                ← Retry
                            </button>
                            <button
                                className="nlwf-create-btn"
                                onClick={handleCreate}
                                disabled={creating}
                                type="button"
                            >
                                {creating ? (
                                    <>
                                        <span className="nlwf-spinner" style={{ borderTopColor: '#fff' }} />
                                        Creating…
                                    </>
                                ) : (
                                    <>
                                        <HiOutlineCheckCircle />
                                        Create Workflow
                                    </>
                                )}
                            </button>
                        </>
                    )}
                </div>

            </div>
        </div>
    );
}
