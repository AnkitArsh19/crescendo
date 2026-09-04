import { useState, useRef, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { HiX, HiOutlineLightningBolt, HiOutlineSparkles, HiArrowSmRight } from 'react-icons/hi';
import { aiApi } from '../../api/aiApi';
import { workflowApi } from '../../api/workflowApi';
import useToastStore from '../../store/toastStore';
import './NLWorkflowModal.css';

const EXAMPLES = [
    'Send a Slack message when a GitHub commit happens, then post in Discord',
    'Add a row in Google Sheets when a Typeform is submitted',
    'Send a notification to Slack and an email when a GitHub PR is merged',
    'Post to Discord when a new workflow run fails',
];

export default function NLWorkflowModal({ onClose }) {
    const navigate = useNavigate();
    const addToast = useToastStore(state => state.addToast);
    const toastSuccess = (msg) => addToast(msg, 'success');
    const toastError = (msg) => addToast(msg, 'error');

    const [messages, setMessages] = useState([
        {
            id: 'welcome',
            role: 'assistant',
            text: 'Hi! Describe what you want to automate in plain English. I will draft the workflow, configure your triggers and actions, and connect the steps.',
            examples: EXAMPLES,
        }
    ]);
    const [inputText, setInputText] = useState('');
    const [generating, setGenerating] = useState(false);
    const [sessionId, setSessionId] = useState(null);
    const initialPromptRef = useRef('');

    // Multi-group clarification state (mapped by group name)
    const [selectedPills, setSelectedPills] = useState({});
    const [customPills, setCustomPills] = useState({});

    const chatStreamRef = useRef(null);
    const textareaRef = useRef(null);

    // Auto-scroll to bottom of chat
    useEffect(() => {
        if (chatStreamRef.current) {
            chatStreamRef.current.scrollTo({
                top: chatStreamRef.current.scrollHeight,
                behavior: 'smooth'
            });
        }
    }, [messages, generating]);

    // Focus input on load
    useEffect(() => {
        textareaRef.current?.focus();
    }, []);

    // ── Submit message to AI ──────────────────────────────────────────────────
    async function handleSend(promptText = null) {
        const raw = typeof promptText === 'string' ? promptText : inputText;
        const textToSubmit = String(raw || '').trim();
        if (!textToSubmit || generating) return;

        setInputText('');
        setGenerating(true);

        // Append user's message to chat
        const userMsgId = crypto.randomUUID();
        setMessages(prev => [...prev, { id: userMsgId, role: 'user', text: textToSubmit }]);

        // Retain cumulative context across clarification turns
        let promptPayload = textToSubmit;
        if (!initialPromptRef.current) {
            initialPromptRef.current = textToSubmit;
        } else if (sessionId) {
            promptPayload = `${initialPromptRef.current}. Details: ${textToSubmit}`;
        }

        try {
            const data = await aiApi.createWorkflowDraft(promptPayload, {}, sessionId);

            if (data.session_id) {
                setSessionId(data.session_id);
            }

            // Case A: AI requires clarification or options
            if (!data.workflow_spec && (data.clarifying_questions?.length > 0 || data.suggested_options?.length > 0)) {
                const optionsList = (data.suggested_options && data.suggested_options.length > 0)
                    ? data.suggested_options
                    : [];

                setMessages(prev => [
                    ...prev,
                    {
                        id: crypto.randomUUID(),
                        role: 'assistant',
                        text: 'I need a few details to configure this workflow accurately:',
                        questions: data.clarifying_questions || [],
                        options: optionsList,
                    }
                ]);
                return;
            }

            // Case B: Workflow spec returned
            const spec = data.workflow_spec;
            if (!spec) {
                throw new Error("Could not draft workflow. Please rephrase or provide more details.");
            }

            setMessages(prev => [
                ...prev,
                {
                    id: crypto.randomUUID(),
                    role: 'assistant',
                    text: data.explanation || 'Workflow generated! Creating your workflow on the canvas now...',
                    isSuccess: true,
                }
            ]);

            // Create empty workflow
            const created = await workflowApi.create({ name: spec.workflow_name || 'AI Generated Workflow' });

            // Format steps
            const graphSteps = [];
            if (spec.trigger) {
                graphSteps.push({
                    clientId: crypto.randomUUID(),
                    type: 'TRIGGER',
                    name: spec.trigger.trigger_key || spec.trigger.app_key || 'Trigger',
                    actionKey: spec.trigger.trigger_key || spec.trigger.app_key,
                    appKey: spec.trigger.app_key,
                    parentStepId: null,
                    configuration: spec.trigger.config || {}
                });
            }

            if (spec.actions && spec.actions.length > 0) {
                for (let i = 0; i < spec.actions.length; i++) {
                    const action = spec.actions[i];
                    graphSteps.push({
                        clientId: crypto.randomUUID(),
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

            const toastMsg = data.explanation
                ? `"${created.name}" created: ${data.explanation}`
                : `"${created.name}" created — configure the steps on the canvas.`;
            toastSuccess(toastMsg);

            // Small delay so user sees success message before redirect
            setTimeout(() => {
                onClose();
                navigate(`/dashboard/workflows/${created.id}`);
            }, 600);

        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'Failed to generate workflow. Please try again.';
            setMessages(prev => [
                ...prev,
                {
                    id: crypto.randomUUID(),
                    role: 'assistant',
                    text: msg,
                    isError: true,
                }
            ]);
            toastError(msg);
        } finally {
            setGenerating(false);
            textareaRef.current?.focus();
        }
    }

    // ── Multi-group clarification submission ──────────────────────────────────
    function handleApplyAllClarifications(groupedOptions) {
        const parts = [];
        Object.keys(groupedOptions).forEach(groupKey => {
            const custom = (customPills[groupKey] || '').trim();
            const pillVal = selectedPills[groupKey];
            if (custom) {
                parts.push(custom);
            } else if (pillVal) {
                parts.push(pillVal);
            }
        });

        if (parts.length > 0) {
            // Combine all clarifications in one cohesive sentence
            const combinedText = parts.join('. ');
            handleSend(combinedText);
        }
    }

    // Helper: group options array by group/appKey
    function getGroupedOptions(options = []) {
        const groups = {};
        options.forEach(opt => {
            const groupName = opt.group || (opt.appKey ? `${opt.appKey.toUpperCase()} Target` : 'Options');
            if (!groups[groupName]) groups[groupName] = [];
            groups[groupName].push(opt);
        });
        return groups;
    }

    return (
        <div className="nlwf-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <motion.div
                className="nlwf-modal"
                role="dialog"
                aria-modal="true"
                aria-label="Build workflow with AI"
                initial={{ opacity: 0, scale: 0.85, y: 30 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.85, y: 20 }}
                transition={{ type: "spring", stiffness: 380, damping: 22 }}
                onClick={(e) => e.stopPropagation()}
            >
                {/* ── Fixed Header ── */}
                <div className="nlwf-header">
                    <div className="nlwf-header-text">
                        <h2>
                            <HiOutlineSparkles style={{ color: 'var(--text-accent, #6366f1)' }} />
                            Build with AI
                        </h2>
                        <p>Describe your automation workflow in plain English.</p>
                    </div>
                    <button className="nlwf-close" onClick={onClose} aria-label="Close modal">
                        <HiX />
                    </button>
                </div>

                {/* ── Scrollable Chat Stream ── */}
                <div className="nlwf-chat-stream" ref={chatStreamRef}>
                    {messages.map((msg) => {
                        if (msg.role === 'user') {
                            return (
                                <div key={msg.id} className="nlwf-msg nlwf-msg-user">
                                    {msg.text}
                                </div>
                            );
                        }

                        // Assistant message
                        const groupedOptions = msg.options ? getGroupedOptions(msg.options) : {};
                        const hasGroups = Object.keys(groupedOptions).length > 0;

                        return (
                            <div key={msg.id} className="nlwf-msg nlwf-msg-assistant">
                                <div className="nlwf-ai-bubble" style={msg.isError ? { borderColor: '#ef4444' } : {}}>
                                    <div className="nlwf-ai-bubble-header">
                                        <HiOutlineSparkles />
                                        <span>Crescendo AI</span>
                                    </div>
                                    <div>{msg.text}</div>

                                    {/* Clickable prompt examples on welcome message */}
                                    {msg.examples && (
                                        <div className="nlwf-examples-grid">
                                            {msg.examples.map((ex) => (
                                                <button
                                                    key={ex}
                                                    type="button"
                                                    className="nlwf-example-chip-chat"
                                                    onClick={() => handleSend(ex)}
                                                >
                                                    <HiArrowSmRight style={{ color: 'var(--text-accent, #6366f1)', flexShrink: 0 }} />
                                                    <span>{ex}</span>
                                                </button>
                                            ))}
                                        </div>
                                    )}

                                    {/* Clarification questions and grouped options */}
                                    {msg.questions && msg.questions.length > 0 && (
                                        <div className="nlwf-clarification-card" style={{ marginTop: '12px' }}>
                                            <ul className="nlwf-clarification-questions-list">
                                                {msg.questions.map((q, idx) => (
                                                    <li key={idx}><strong>{q}</strong></li>
                                                ))}
                                            </ul>

                                            {hasGroups && (
                                                <div className="nlwf-group-container">
                                                    {Object.entries(groupedOptions).map(([groupKey, opts]) => (
                                                        <div key={groupKey} className="nlwf-group-box">
                                                            <div className="nlwf-group-header">
                                                                <span className="nlwf-group-name">{groupKey}</span>
                                                            </div>
                                                            <div className="nlwf-group-pills">
                                                                {opts.map((opt, i) => {
                                                                    const val = opt.value || opt.label;
                                                                    const isSelected = selectedPills[groupKey] === val;
                                                                    return (
                                                                        <button
                                                                            key={i}
                                                                            type="button"
                                                                            className={`nlwf-group-pill ${isSelected ? 'selected' : ''}`}
                                                                            onClick={() => {
                                                                                setSelectedPills(prev => ({
                                                                                    ...prev,
                                                                                    [groupKey]: isSelected ? '' : val
                                                                                }));
                                                                            }}
                                                                        >
                                                                            {opt.itemLabel || opt.label}
                                                                        </button>
                                                                    );
                                                                })}
                                                            </div>
                                                            <input
                                                                type="text"
                                                                className="nlwf-group-custom-input"
                                                                placeholder={`Or type custom for ${groupKey}...`}
                                                                value={customPills[groupKey] || ''}
                                                                onChange={(e) => {
                                                                    const val = e.target.value;
                                                                    setCustomPills(prev => ({ ...prev, [groupKey]: val }));
                                                                }}
                                                                onKeyDown={(e) => {
                                                                    if (e.key === 'Enter') {
                                                                        e.preventDefault();
                                                                        handleApplyAllClarifications(groupedOptions);
                                                                    }
                                                                }}
                                                            />
                                                        </div>
                                                    ))}

                                                    <button
                                                        type="button"
                                                        className="nlwf-apply-clarifications-btn"
                                                        onClick={() => handleApplyAllClarifications(groupedOptions)}
                                                    >
                                                        <HiOutlineLightningBolt />
                                                        <span>Apply Clarifications & Continue</span>
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })}

                    {/* Thinking / generating bubble */}
                    {generating && (
                        <div className="nlwf-msg nlwf-msg-assistant">
                            <div className="nlwf-typing-bubble">
                                <div className="nlwf-typing-dots">
                                    <div className="nlwf-typing-dot" />
                                    <div className="nlwf-typing-dot" />
                                    <div className="nlwf-typing-dot" />
                                </div>
                                <span>Designing workflow pipeline…</span>
                            </div>
                        </div>
                    )}
                </div>

                {/* ── Pinned Bottom Input Bar (Never covered by taskbar) ── */}
                <div className="nlwf-chat-footer">
                    <div className="nlwf-input-row">
                        <textarea
                            ref={textareaRef}
                            className="nlwf-chat-input"
                            rows={1}
                            placeholder="Type a workflow description or answer..."
                            value={inputText}
                            onChange={(e) => setInputText(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter' && !e.shiftKey) {
                                    e.preventDefault();
                                    handleSend();
                                }
                            }}
                        />
                        <button
                            type="button"
                            className="nlwf-send-btn"
                            onClick={() => handleSend()}
                            disabled={!inputText.trim() || generating}
                            title="Send prompt (Enter)"
                            aria-label="Send"
                        >
                            <HiOutlineLightningBolt />
                        </button>
                    </div>
                    <div className="nlwf-footer-hint-row">
                        <span className="nlwf-footer-hint">Press <strong>Enter ↵</strong> to send · Shift+Enter for newline</span>
                        <button type="button" className="nlwf-footer-cancel-btn" onClick={onClose}>Cancel</button>
                    </div>
                </div>
            </motion.div>
        </div>
    );
}
