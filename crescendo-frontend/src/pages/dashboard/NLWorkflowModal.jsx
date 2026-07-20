import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { HiX, HiOutlineLightningBolt, HiOutlineSparkles } from 'react-icons/hi';
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
 * the backend proxies it to the Python AI service. Once generated,
 * it immediately creates the workflow and redirects to the canvas.
 */
export default function NLWorkflowModal({ onClose }) {
    const navigate = useNavigate();
    const addToast = useToastStore(state => state.addToast);
    const toastSuccess = (msg) => addToast(msg, 'success');
    const toastError = (msg) => addToast(msg, 'error');

    const [prompt, setPrompt] = useState('');
    const [generating, setGenerating] = useState(false);
    const [error, setError] = useState(null);
    const [unavailable, setUnavailable] = useState(false);

    // ── Generate & Create Workflow from AI ────────────────────────────────────
    async function handleGenerate() {
        if (!prompt.trim()) return;
        setGenerating(true);
        setError(null);
        setUnavailable(false);

        try {
            // 1. Fetch draft from AI
            const data = await aiApi.createWorkflowDraft(prompt.trim());
            const spec = data.workflow_spec;
            if (!spec) {
                throw new Error("No workflow generated. Try rephrasing your prompt.");
            }

            // 2. Create the empty workflow
            const created = await workflowApi.create({ name: spec.workflow_name || 'AI Generated Workflow' });

            // 3. Format and save the steps
            const graphSteps = [];

            // Trigger
            if (spec.trigger) {
                const clientId = crypto.randomUUID();
                graphSteps.push({
                    clientId,
                    type: 'TRIGGER',
                    name: spec.trigger.trigger_type,
                    actionKey: spec.trigger.trigger_type,
                    appKey: spec.trigger.app_name,
                    parentStepId: null,
                    configuration: spec.trigger.config || {}
                });
            }

            // Actions
            if (spec.actions && spec.actions.length > 0) {
                for (let i = 0; i < spec.actions.length; i++) {
                    const action = spec.actions[i];
                    const clientId = crypto.randomUUID();
                    graphSteps.push({
                        clientId,
                        type: 'ACTION',
                        name: action.action_type,
                        actionKey: action.action_type,
                        appKey: action.app_name,
                        parentStepId: null, // Linear steps do not use parentStepId in this engine
                        configuration: action.config || {}
                    });
                }
            }

            if (graphSteps.length > 0) {
                await workflowApi.updateGraph(created.id, {
                    revision: created.revision,
                    steps: graphSteps,
                    deletedStepIds: []
                });
            }

            toastSuccess(`"${created.name}" created — configure the steps on the canvas.`);
            onClose();
            navigate(`/dashboard/workflows/${created.id}`);

        } catch (err) {
            if (err.response?.status === 503) {
                setUnavailable(true);
            } else {
                const msg = err.response?.data?.message || err.message || 'Failed to generate workflow. Please try again.';
                setError(msg);
                toastError(msg);
            }
        } finally {
            setGenerating(false);
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

                {/* Footer actions */}
                <div className="nlwf-actions">
                    <button className="nlwf-cancel-btn" onClick={onClose} type="button">
                        Cancel
                    </button>

                    <button
                        className="nlwf-generate-btn"
                        onClick={handleGenerate}
                        disabled={!prompt.trim() || generating}
                        type="button"
                    >
                        {generating ? (
                            <>
                                <span className="nlwf-spinner" />
                                Generating & Creating…
                            </>
                        ) : (
                            <>
                                <HiOutlineLightningBolt />
                                Generate
                            </>
                        )}
                    </button>
                </div>

            </div>
        </div>
    );
}
