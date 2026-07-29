import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
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

    // Multi-turn clarification state
    const [sessionId, setSessionId] = useState(null);
    const [clarifyingQuestions, setClarifyingQuestions] = useState([]);
    const [suggestedOptions, setSuggestedOptions] = useState([]);
    const [selectedOption, setSelectedOption] = useState('');

    // Default fallback options if AI asks open-ended questions without explicit list
    const DEFAULT_APP_OPTIONS = [
        { label: 'Slack (Post Message / Notification)', value: 'Slack' },
        { label: 'Gmail (Send Email / Watch Inbox)', value: 'Gmail' },
        { label: 'GitHub (Repository / PR / Issues)', value: 'GitHub' },
        { label: 'Notion (Create Page / Database Row)', value: 'Notion' },
        { label: 'Discord (Webhook / Channel Message)', value: 'Discord' },
        { label: 'Google Sheets (Add Row / Update)', value: 'Google Sheets' },
        { label: 'Webhook (HTTP Trigger / Action)', value: 'Webhook' }
    ];

    // ── Generate & Create Workflow from AI ────────────────────────────────────
    async function handleGenerate(overridePrompt = null) {
        const textToSubmit = overridePrompt || selectedOption || prompt;
        if (!textToSubmit.trim()) return;

        setGenerating(true);
        setError(null);
        setUnavailable(false);

        try {
            // 1. Fetch draft from AI (passing active sessionId if in multi-turn mode)
            const data = await aiApi.createWorkflowDraft(textToSubmit.trim(), {}, sessionId);

            if (data.session_id) {
                setSessionId(data.session_id);
            }

            // Check if AI requires clarification / options
            if (!data.workflow_spec && (data.clarifying_questions?.length > 0 || data.suggested_options?.length > 0)) {
                setClarifyingQuestions(data.clarifying_questions || []);
                const optionsList = data.suggested_options && data.suggested_options.length > 0
                    ? data.suggested_options
                    : DEFAULT_APP_OPTIONS;
                setSuggestedOptions(optionsList);
                if (optionsList.length > 0) {
                    setSelectedOption(optionsList[0].value || optionsList[0].label);
                }
                return;
            }

            const spec = data.workflow_spec;
            if (!spec) {
                throw new Error("No workflow generated. Try rephrasing your prompt or selecting an option below.");
            }

            // Reset clarification state on success
            setClarifyingQuestions([]);
            setSuggestedOptions([]);
            setSelectedOption('');

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
                    name: spec.trigger.trigger_key || spec.trigger.app_key || 'Trigger',
                    actionKey: spec.trigger.trigger_key || spec.trigger.app_key,
                    appKey: spec.trigger.app_key,
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
                        name: action.action_key || action.app_key || 'Action',
                        actionKey: action.action_key || action.app_key,
                        appKey: action.app_key,
                        parentStepId: null,
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
            <motion.div
                className="nlwf-modal"
                role="dialog"
                aria-modal="true"
                aria-label="Build workflow with AI"
                initial={{ opacity: 0, scale: 0.65, y: 40 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.75, y: 20 }}
                transition={{ type: "spring", stiffness: 380, damping: 18, mass: 0.8 }}
                onClick={(e) => e.stopPropagation()}
            >
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

                {/* Clarification / Dropdown Selector Block */}
                {clarifyingQuestions.length > 0 && (
                    <div className="nlwf-clarification-box" style={{
                        marginTop: '1rem',
                        padding: '1rem',
                        background: 'rgba(255, 255, 255, 0.03)',
                        border: '1px solid rgba(255, 255, 255, 0.12)',
                        borderRadius: '8px'
                    }}>
                        <div style={{ fontWeight: 600, marginBottom: '0.5rem', color: '#ffffff', fontSize: '0.9rem' }}>
                            Clarification Required
                        </div>
                        {clarifyingQuestions.map((q, idx) => (
                            <p key={idx} style={{ fontSize: '0.85rem', color: '#cccccc', marginBottom: '0.5rem' }}>{q}</p>
                        ))}

                        <label style={{ display: 'block', fontSize: '0.8rem', color: '#888888', marginBottom: '0.3rem' }}>
                            Select Target Option
                        </label>
                        <select
                            value={selectedOption}
                            onChange={(e) => setSelectedOption(e.target.value)}
                            style={{
                                width: '100%',
                                padding: '0.5rem 0.75rem',
                                background: '#121212',
                                color: '#ffffff',
                                border: '1px solid rgba(255, 255, 255, 0.18)',
                                borderRadius: '6px',
                                fontSize: '0.85rem',
                                outline: 'none'
                            }}
                        >
                            {suggestedOptions.map((opt, i) => {
                                const val = typeof opt === 'string' ? opt : (opt.value || opt.label);
                                const lbl = typeof opt === 'string' ? opt : (opt.label || opt.value);
                                return <option key={i} value={val}>{lbl}</option>;
                            })}
                        </select>

                        <button
                            type="button"
                            onClick={() => handleGenerate(selectedOption)}
                            style={{
                                marginTop: '0.85rem',
                                padding: '0.45rem 0.9rem',
                                background: '#ffffff',
                                color: '#000000',
                                border: 'none',
                                borderRadius: '6px',
                                cursor: 'pointer',
                                fontSize: '0.85rem',
                                fontWeight: 600,
                                transition: 'opacity 0.2s ease'
                            }}
                        >
                            Continue with Selected Option
                        </button>
                    </div>
                )}

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
            </motion.div>
        </div>
    );
}
