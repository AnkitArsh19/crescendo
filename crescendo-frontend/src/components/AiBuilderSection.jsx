import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    HiOutlineSparkles,
    HiOutlineArrowRight,
    HiOutlineMail,
    HiOutlineFolder,
    HiOutlineChat,
    HiOutlineCode,
    HiOutlineUserAdd,
    HiOutlineCalendar,
    HiOutlineTemplate
} from 'react-icons/hi';
import './AiBuilderSection.css';

const EXAMPLES = [
    {
        id: 'email_slack',
        tabLabel: 'Email & Storage',
        tabIcon: <HiOutlineMail />,
        prompt: "When a starred email with an invoice arrives in Gmail, save the attachment to Google Drive and message our finance team on Slack.",
        steps: [
            { app: "Gmail", action: "New Starred Email", role: "Trigger", icon: <HiOutlineMail /> },
            { app: "Google Drive", action: "Upload Attachment", role: "Action 1", icon: <HiOutlineFolder /> },
            { app: "Slack", action: "Send Channel Message", role: "Action 2", icon: <HiOutlineChat /> }
        ]
    },
    {
        id: 'dev_ops',
        tabLabel: 'Code & Summary',
        tabIcon: <HiOutlineCode />,
        prompt: "When a new Pull Request is opened on GitHub, summarize the code changes using OpenAI and alert the engineers on Discord.",
        steps: [
            { app: "GitHub", action: "Pull Request Opened", role: "Trigger", icon: <HiOutlineCode /> },
            { app: "OpenAI", action: "Summarize Code Diff", role: "Action 1", icon: <HiOutlineSparkles /> },
            { app: "Discord", action: "Post Team Update", role: "Action 2", icon: <HiOutlineChat /> }
        ]
    },
    {
        id: 'lead_sync',
        tabLabel: 'Customer Onboarding',
        tabIcon: <HiOutlineUserAdd />,
        prompt: "When a new customer signs up, create a lead record in HubSpot, schedule an intro call on Google Calendar, and send a welcome email.",
        steps: [
            { app: "HubSpot", action: "Create CRM Contact", role: "Trigger", icon: <HiOutlineUserAdd /> },
            { app: "Google Calendar", action: "Schedule Intro Invite", role: "Action 1", icon: <HiOutlineCalendar /> },
            { app: "CrescendoMail", action: "Send Welcome Email", role: "Action 2", icon: <HiOutlineTemplate /> }
        ]
    }
];

export default function AiBuilderSection() {
    const [activeIdx, setActiveIdx] = useState(0);
    const current = EXAMPLES[activeIdx];

    return (
        <section className="ai-builder" id="ai-builder">
            <div className="ai-builder-inner">
                <div className="ai-builder-header">
                    <motion.p
                        className="section-label"
                        initial={{ opacity: 0, y: 12 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: false, amount: 0.2 }}
                        transition={{ duration: 0.5 }}
                    >
                        AI Workflow Builder
                    </motion.p>
                    <motion.h2
                        className="section-title"
                        initial={{ opacity: 0, y: 20 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: false, amount: 0.2 }}
                        transition={{ duration: 0.6, delay: 0.08 }}
                    >
                        Describe what you need. Let AI <span className="font-serif" style={{ fontStyle: 'italic' }}>build the rest</span>
                    </motion.h2>
                    <motion.p
                        className="section-subtitle"
                        initial={{ opacity: 0, y: 20 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: false, amount: 0.2 }}
                        transition={{ duration: 0.6, delay: 0.16 }}
                    >
                        No coding or manual setup required. Just explain your automation in plain English, and Crescendo connects your favorite apps instantly.
                    </motion.p>
                </div>

                <div className="ai-showcase-container">
                    {/* ── Example Tab Buttons ── */}
                    <div className="ai-tabs-row">
                        {EXAMPLES.map((ex, idx) => {
                            const isCurrent = idx === activeIdx;
                            return (
                                <button
                                    key={ex.id}
                                    type="button"
                                    className={`ai-tab-button ${isCurrent ? 'active' : ''}`}
                                    onClick={() => setActiveIdx(idx)}
                                >
                                    <span style={{ display: 'flex', fontSize: '1.1rem' }}>{ex.tabIcon}</span>
                                    <span>{ex.tabLabel}</span>
                                </button>
                            );
                        })}
                    </div>

                    {/* ── Plain English Prompt Box ── */}
                    <AnimatePresence mode="wait">
                        <motion.div
                            key={`prompt-${current.id}`}
                            className="ai-prompt-box"
                            initial={{ opacity: 0, scale: 0.98, y: 10 }}
                            animate={{ opacity: 1, scale: 1, y: 0 }}
                            exit={{ opacity: 0, scale: 0.98, y: -10 }}
                            transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
                        >
                            <div className="ai-prompt-icon-box">
                                <HiOutlineSparkles />
                            </div>
                            <div className="ai-prompt-text-area">
                                <span className="ai-prompt-sublabel">Plain English Instruction</span>
                                <span className="ai-prompt-content">&ldquo;{current.prompt}&rdquo;</span>
                            </div>
                        </motion.div>
                    </AnimatePresence>

                    {/* ── Transition Divider ── */}
                    <div className="ai-output-divider">
                        <div className="ai-divider-line" />
                        <span>Generated App Integration</span>
                        <div className="ai-divider-line" />
                    </div>

                    {/* ── Generated Feature Cards ── */}
                    <div className="ai-cards-row">
                        <AnimatePresence mode="wait">
                            {current.steps.map((step, idx) => (
                                <div key={`${current.id}-${step.app}-${idx}`} className="ai-workflow-card-wrapper">
                                    <motion.div
                                        className="ai-feature-card"
                                        initial={{ opacity: 0, y: 28, scale: 0.96 }}
                                        animate={{ opacity: 1, y: 0, scale: 1 }}
                                        transition={{ type: "spring", stiffness: 380, damping: 22, delay: idx * 0.1 }}
                                        whileHover={{ y: -6, transition: { duration: 0.25, ease: 'easeOut' } }}
                                    >
                                        <div className="ai-card-corner" />
                                        
                                        <div className="ai-card-top-bar">
                                            <div className="ai-card-icon">{step.icon}</div>
                                            <span className={`ai-card-badge ${step.role.toLowerCase() === 'trigger' ? 'trigger' : ''}`}>
                                                {step.role}
                                            </span>
                                        </div>

                                        <div className="ai-card-details">
                                            <div className="ai-card-title">{step.app}</div>
                                            <div className="ai-card-desc">{step.action}</div>
                                        </div>
                                    </motion.div>

                                    {idx < current.steps.length - 1 && (
                                        <motion.div
                                            className="ai-card-connector"
                                            initial={{ opacity: 0, scale: 0.4 }}
                                            animate={{ opacity: 1, scale: 1 }}
                                            transition={{ duration: 0.25, delay: idx * 0.1 + 0.15 }}
                                        >
                                            <HiOutlineArrowRight />
                                        </motion.div>
                                    )}
                                </div>
                            ))}
                        </AnimatePresence>
                    </div>
                </div>
            </div>
        </section>
    );
}
