import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
    HiPlus,
    HiArrowRight,
    HiOutlineLightningBolt,
    HiOutlineMail,
    HiOutlineDatabase,
    HiOutlineBell,
    HiOutlineCloud,
    HiOutlineGlobe,
    HiOutlineBookOpen,
    HiOutlineCode,
    HiOutlinePlay,
    HiOutlineClock,
    HiOutlineUserAdd,
    HiOutlineDocumentText,
} from 'react-icons/hi';
import { SiSlack, SiGmail, SiGithub, SiPostgresql, SiDiscord } from 'react-icons/si';
import useWorkflowStore from '../../store/workflowStore';
import './Dashboard.css';

const templates = [
    {
        icon: <HiOutlineMail />,
        name: 'Email Welcome Flow',
        apps: 'Webhook → Gmail',
        desc: 'Send a personalized welcome email when a new user signs up via your app.',
    },
    {
        icon: <HiOutlineDatabase />,
        name: 'Data Sync Pipeline',
        apps: 'PostgreSQL → MongoDB',
        desc: 'Keep your databases in sync with real-time bi-directional data flow.',
    },
    {
        icon: <HiOutlineBell />,
        name: 'Slack Notifications',
        apps: 'Webhook → Slack',
        desc: 'Route alerts from your monitoring tools directly to the right Slack channel.',
    },
    {
        icon: <HiOutlineDocumentText />,
        name: 'Invoice Generator',
        apps: 'GitHub → Slack',
        desc: 'Automatically generate and send invoices when a payment is received.',
    },
    {
        icon: <HiOutlineUserAdd />,
        name: 'User Onboarding',
        apps: 'Webhook → Multi-step',
        desc: 'Enrich user data, assign roles, and trigger onboarding sequences.',
    },
    {
        icon: <HiOutlineGlobe />,
        name: 'Custom Webhook',
        apps: 'Any → Any',
        desc: 'Start from a blank canvas and build your own custom automation workflow.',
    },
];

const apps = [
    { icon: <SiSlack />, name: 'Slack' },
    { icon: <SiGmail />, name: 'Gmail' },
    { icon: <SiGithub />, name: 'GitHub' },
    { icon: <SiPostgresql />, name: 'PostgreSQL' },
    { icon: <SiDiscord />, name: 'Discord' },
];

const resources = [
    {
        icon: <HiOutlineBookOpen />,
        title: 'Getting Started',
        desc: 'Learn the basics and create your first workflow in under 5 minutes.',
    },
    {
        icon: <HiOutlineCode />,
        title: 'API Reference',
        desc: 'Integrate Crescendo into your stack with our REST & webhook APIs.',
    },
    {
        icon: <HiOutlinePlay />,
        title: 'Video Tutorials',
        desc: 'Watch step-by-step guides for building advanced automations.',
    },
];

const fadeIn = {
    hidden: { opacity: 0, y: 16 },
    visible: (i) => ({
        opacity: 1, y: 0,
        transition: { delay: i * 0.06, duration: 0.5, ease: [0.22, 1, 0.36, 1] },
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

export default function Dashboard() {
    const { workflows, fetchWorkflows } = useWorkflowStore();
    const recent = workflows.slice(0, 5);

    useEffect(() => {
        fetchWorkflows();
    }, [fetchWorkflows]);

    return (
        <div className="dash-home">

            {/* ── Hero ── */}
            <motion.div
                className="dash-hero"
                custom={0}
                variants={fadeIn}
                initial="hidden"
                animate="visible"
            >
                <div className="dash-hero-text">
                    <h1>Build automations, your way.</h1>
                    <p>
                        Crescendo handles the complexity. Start with a template or build from scratch.
                    </p>
                </div>
                <Link to="/dashboard/workflows/new">
                    <button className="dash-hero-cta">
                        <HiPlus /> Create Workflow
                    </button>
                </Link>
            </motion.div>

            {/* ── Templates ── */}
            <div className="dash-section">
                <div className="dash-section-head">
                    <span className="dash-section-title">Start with a template</span>
                    <span className="dash-section-link">
                        View all <HiArrowRight />
                    </span>
                </div>
                <div className="dash-templates-grid">
                    {templates.map((t, i) => (
                        <motion.div
                            className="dash-template-card"
                            key={t.name}
                            custom={i}
                            variants={fadeIn}
                            initial="hidden"
                            animate="visible"
                        >
                            <div className="dash-template-top">
                                <div className="dash-template-icon">{t.icon}</div>
                                <div className="dash-template-info">
                                    <div className="dash-template-name">{t.name}</div>
                                    <div className="dash-template-apps">{t.apps}</div>
                                </div>
                            </div>
                            <div className="dash-template-desc">{t.desc}</div>
                            <span className="dash-template-action">
                                Use template <HiArrowRight />
                            </span>
                        </motion.div>
                    ))}
                </div>
            </div>

            {/* ── Connect Apps ── */}
            <div className="dash-section">
                <div className="dash-section-head">
                    <span className="dash-section-title">Connect your apps</span>
                    <span className="dash-section-link">
                        Browse integrations <HiArrowRight />
                    </span>
                </div>
                <div className="dash-apps-row">
                    {apps.map((app) => (
                        <motion.div
                            className="dash-app-chip"
                            key={app.name}
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                        >
                            <span className="dash-app-chip-icon">{app.icon}</span>
                            <div>
                                <div className="dash-app-chip-name">{app.name}</div>
                                <div className="dash-app-chip-status">Connect</div>
                            </div>
                        </motion.div>
                    ))}
                    <div className="dash-app-add">
                        <HiPlus />
                    </div>
                </div>
            </div>

            {/* ── Resources ── */}
            <div className="dash-section">
                <div className="dash-section-head">
                    <span className="dash-section-title">Learn & explore</span>
                </div>
                <div className="dash-resources-grid">
                    {resources.map((r, i) => (
                        <motion.div
                            className="dash-resource-card"
                            key={r.title}
                            custom={i + 8}
                            variants={fadeIn}
                            initial="hidden"
                            animate="visible"
                        >
                            <div className="dash-resource-icon">{r.icon}</div>
                            <div className="dash-resource-title">{r.title}</div>
                            <div className="dash-resource-desc">{r.desc}</div>
                        </motion.div>
                    ))}
                </div>
            </div>

            {/* ── Recent Workflows (real data) ── */}
            <div className="dash-section">
                <div className="dash-section-head">
                    <span className="dash-section-title">Recent workflows</span>
                    <Link to="/dashboard/workflows" className="dash-section-link">
                        View all <HiArrowRight />
                    </Link>
                </div>
                {recent.length === 0 ? (
                    <div style={{ padding: '16px 0', color: 'var(--text-secondary)', fontSize: '13px' }}>
                        No workflows yet. <Link to="/dashboard/workflows/new" style={{ color: 'var(--text-accent)' }}>Create one &rarr;</Link>
                    </div>
                ) : (
                <div className="dash-recent-list">
                    {recent.map((wf, i) => (
                        <motion.div
                            className="dash-recent-row"
                            key={wf.id}
                            custom={i + 11}
                            variants={fadeIn}
                            initial="hidden"
                            animate="visible"
                        >
                            <div className="dash-recent-icon">
                                <HiOutlineLightningBolt />
                            </div>
                            <div className="dash-recent-info">
                                <div className="dash-recent-name">{wf.name}</div>
                                <div className="dash-recent-meta">
                                    {wf.lastRunAt
                                        ? `Last run ${formatRelative(wf.lastRunAt)}`
                                        : `Created ${formatRelative(wf.createdAt)}`}
                                </div>
                            </div>
                            <span className={`dash-recent-status ${wf.isActive ? 'active' : 'draft'}`}>
                                {wf.isActive ? 'Active' : 'Inactive'}
                            </span>
                        </motion.div>
                    ))}
                </div>
                )}
            </div>
        </div>
    );
}
