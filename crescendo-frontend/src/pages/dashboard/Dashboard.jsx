import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
    HiPlus,
    HiArrowRight,
    HiOutlineLightningBolt,
    HiOutlineMail,
    HiOutlineDatabase,
    HiOutlineBell,
    HiOutlineGlobe,
    HiOutlineBookOpen,
    HiOutlineCode,
    HiOutlineClock,
    HiOutlineDocumentText,
    HiOutlineSun,
} from 'react-icons/hi';
import { SiGithub, SiGooglesheets, SiDiscord, SiGooglecalendar } from 'react-icons/si';
import { FaSlack } from 'react-icons/fa';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import useAuthStore from '../../store/authStore';
import useConnectionStore from '../../store/connectionStore';
import useToastStore from '../../store/toastStore';
import { useCreateWorkflow, useWorkflowList } from '../../hooks/useWorkflows';
import { workflowClient } from '../../api/workflowClient';
import { allRunsApi } from '../../api/logbookApi';
import './Dashboard.css';

const starters = [
    {
        icon: <HiOutlineBell />,
        name: 'Pull request heads-up',
        apps: 'GitHub -> Slack',
        desc: 'Send your team a review reminder when a new pull request opens.',
        steps: [
            { name: 'New pull request', type: 'TRIGGER', appKey: 'github', actionKey: 'new-pr', configuration: {} },
            { name: 'Post review reminder', type: 'ACTION', appKey: 'slack', actionKey: 'sendMessage', configuration: { text: 'A new pull request is ready for review.' } },
        ],
    },
    {
        icon: <HiOutlineDatabase />,
        name: 'Campus event RSVP',
        apps: 'Webhook -> Google Sheets',
        desc: 'Collect RSVP payloads and prepare them for a connected Google Sheet.',
        steps: [
            { name: 'Receive RSVP', type: 'TRIGGER', appKey: 'crescendo-webhook', actionKey: 'incoming', configuration: { method: 'POST', urlPattern: '/campus-rsvp' } },
            { name: 'Append RSVP row', type: 'ACTION', appKey: 'google-sheets', actionKey: 'appendRow', configuration: {} },
        ],
    },
    {
        icon: <HiOutlineSun />,
        name: 'Daily weather check',
        apps: 'Schedule -> Weather',
        desc: 'Run a weekday morning weather check; swap in your own city before activating.',
        steps: [
            { name: 'Weekday morning', type: 'TRIGGER', appKey: 'schedule', actionKey: 'cron', configuration: { cronExpression: '0 0 8 * * MON-FRI' } },
            { name: 'Get local weather', type: 'ACTION', appKey: 'weather', actionKey: 'get-weather', configuration: { city: 'Bengaluru', units: 'metric' } },
        ],
    },
    {
        icon: <HiOutlineGlobe />,
        name: 'Webhook relay',
        apps: 'Webhook -> HTTP / API',
        desc: 'Receive an event at Crescendo and forward it to the API endpoint you choose.',
        steps: [
            { name: 'Receive webhook', type: 'TRIGGER', appKey: 'crescendo-webhook', actionKey: 'incoming', configuration: { method: 'POST', urlPattern: '/relay' } },
            { name: 'Forward request', type: 'ACTION', appKey: 'http', actionKey: 'request', configuration: { authentication: 'none', method: 'POST', url: 'https://example.com/webhooks/crescendo', sendBody: true, bodyType: 'json', specifyBody: 'json', bodyParameters: {} } },
        ],
    },
    {
        icon: <HiOutlineMail />,
        name: 'Daily quote drop',
        apps: 'Schedule -> Random Quotes -> Discord',
        desc: 'A small morale boost for a study group or dev community. Choose a Discord channel before enabling it.',
        steps: [
            { name: 'Morning schedule', type: 'TRIGGER', appKey: 'schedule', actionKey: 'cron', configuration: { cronExpression: '0 30 8 * * *' } },
            { name: 'Get a motivational quote', type: 'ACTION', appKey: 'quotes', actionKey: 'get-by-category', configuration: { category: 'motivational' } },
            { name: 'Send to Discord', type: 'ACTION', appKey: 'discord', actionKey: 'sendMessage', configuration: { content: 'Your daily quote is ready. Configure this message with the quote output.' } },
        ],
    },
];

const featuredApps = [
    { icon: <FaSlack />, name: 'Slack', appKey: 'slack' },
    { icon: <SiGithub />, name: 'GitHub', appKey: 'github' },
    { icon: <SiGooglesheets />, name: 'Google Sheets', appKey: 'google-sheets' },
    { icon: <SiDiscord />, name: 'Discord', appKey: 'discord' },
    { icon: <SiGooglecalendar />, name: 'Google Calendar', appKey: 'google-calendar' },
];

const resources = [
    { icon: <HiOutlineBookOpen />, title: 'Getting started', desc: 'Set up your first workflow and learn the core concepts.', to: '/docs' },
    { icon: <HiOutlineCode />, title: 'Public API', desc: 'Explore the OpenAPI-backed workflow and integration endpoints.', to: '/docs/api/workflows' },
    { icon: <HiOutlineDocumentText />, title: 'Authentication', desc: 'Understand API keys, OAuth clients, scopes, and rate limits.', to: '/docs/authentication' },
];

const fadeIn = {
    hidden: { opacity: 0, y: 16 },
    visible: (index) => ({ opacity: 1, y: 0, transition: { delay: index * 0.06, duration: 0.5, ease: [0.22, 1, 0.36, 1] } }),
};

function formatRelative(dateStr) {
    if (!dateStr) return null;
    const minutes = Math.floor((Date.now() - new Date(dateStr).getTime()) / 60000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
}

const greetingSets = {
    late: [
        { label: 'Still up?', prompt: 'Build it once, then let Crescendo carry the repeat work.' },
        { label: 'Night owl mode', prompt: 'Set one helpful thing in motion before you call it a day.' },
        { label: 'Quiet hours', prompt: 'The best automations keep working after you log off.' },
    ],
    morning: [
        { label: 'Good morning', prompt: 'What can we take off your plate before the day gets busy?' },
        { label: 'Fresh start', prompt: 'Give future-you fewer tabs and fewer repetitive clicks.' },
        { label: 'Rise and automate', prompt: 'Turn today’s first recurring task into a flow.' },
        { label: 'Morning momentum', prompt: 'A tiny workflow can make the rest of the day feel lighter.' },
    ],
    afternoon: [
        { label: 'Good afternoon', prompt: 'Turn the next repetitive task into a flow.' },
        { label: 'In the flow', prompt: 'Keep the good work moving; hand the busywork to Crescendo.' },
        { label: 'Momentum looks good', prompt: 'One connected app can save more time than another coffee.' },
        { label: 'Hello again', prompt: 'There is probably one task here that does not need your attention twice.' },
    ],
    evening: [
        { label: 'Good evening', prompt: 'Clear a little busywork before you sign off.' },
        { label: 'Wind-down win', prompt: 'Set up tomorrow so you can start ahead.' },
        { label: 'Evening reset', prompt: 'Let a workflow handle the follow-up while you recharge.' },
        { label: 'One more useful thing', prompt: 'A quick automation now can make tomorrow calmer.' },
    ],
    night: [
        { label: 'Good night', prompt: 'Set up tomorrow so it can run while you rest.' },
        { label: 'After-hours ideas', prompt: 'Great workflows do not need anyone awake to keep moving.' },
        { label: 'Time to ship less busywork', prompt: 'Put the repeatable part on autopilot before bed.' },
    ],
};

function getGreeting(hour, name, variation) {
    const period = hour < 5 ? 'late' : hour < 12 ? 'morning' : hour < 17 ? 'afternoon' : hour < 22 ? 'evening' : 'night';
    const entries = greetingSets[period];
    const dateSeed = new Date().toDateString();
    const seed = `${dateSeed}-${name}-${period}-${variation}`.split('').reduce((total, char) => total + char.charCodeAt(0), 0);
    return entries[seed % entries.length];
}

export default function Dashboard() {
    const navigate = useNavigate();
    const { data: workflows = [] } = useWorkflowList();
    const createWorkflow = useCreateWorkflow();
    const { user, isGuest } = useAuthStore();
    const { connections, fetchConnections } = useConnectionStore();
    const [stats, setStats] = useState(null);
    const [currentHour, setCurrentHour] = useState(() => new Date().getHours());
    const [creatingTemplate, setCreatingTemplate] = useState(null);
    const recent = workflows.slice(0, 5);
    const displayName = isGuest ? 'there' : (user?.username || user?.email?.split('@')[0] || 'there');
    const greeting = getGreeting(currentHour, displayName, 0);
    const connectedAppKeys = useMemo(() => new Set(connections.map((connection) => connection.appKey)), [connections]);

    useEffect(() => {
        if (!isGuest) {
            fetchConnections();
            allRunsApi.stats().then(setStats).catch(() => setStats(null));
        }
    }, [fetchConnections, isGuest]);

    useEffect(() => {
        const timer = window.setInterval(() => setCurrentHour(new Date().getHours()), 60_000);
        return () => window.clearInterval(timer);
    }, []);

    const handleUseStarter = async (starter) => {
        setCreatingTemplate(starter.name);
        try {
            const workflow = await createWorkflow.mutateAsync({
                name: starter.name,
                description: starter.desc,
            });
            for (const step of starter.steps) {
                await workflowClient.steps.add(workflow.id, step);
            }
            useToastStore.getState().addToast('Starter added as a draft. Finish its setup before activating it.', 'success');
            navigate(`/dashboard/workflows/${workflow.id}`);
        } catch (error) {
            useToastStore.getState().addToast(error.response?.data?.message || 'Could not create this starter workflow.', 'error');
        } finally {
            setCreatingTemplate(null);
        }
    };

    const statsData = stats ? [
        { name: 'Success', value: stats.success },
        { name: 'Failed', value: stats.failed },
        { name: 'Running', value: stats.running },
        { name: 'Pending', value: stats.pending },
    ].filter((item) => item.value > 0) : [];
    const chartColors = ['var(--text-primary)', 'var(--text-secondary)', 'var(--border-color)', 'var(--bg-card-hover)'];

    return (
        <div className="dash-home">
            <motion.section className="dash-hero" custom={0} variants={fadeIn} initial="hidden" animate="visible">
                <div className="dash-hero-text">
                    <p className="dash-eyebrow">Your Crescendo workspace</p>
                    <div className="dash-greeting-line">
                        <h1>{greeting.label}, {displayName}.</h1>
                    </div>
                    <p>{greeting.prompt}</p>
                </div>
                <div className="dash-hero-actions">
                    <Link to="/dashboard/workflows/new" className="dash-hero-cta"><HiPlus /> Create workflow</Link>
                    <Link to="/dashboard/connections" className="dash-hero-secondary">Connect an app</Link>
                </div>
            </motion.section>

            {stats && stats.total > 0 && (
                <section className="dash-section">
                    <div className="dash-section-head"><span className="dash-section-title">Execution overview</span><Link to="/dashboard/history" className="dash-section-link">View history <HiArrowRight /></Link></div>
                    <div className="dash-stats-grid">
                        <motion.div className="dash-stat-card" custom={1} variants={fadeIn} initial="hidden" animate="visible"><div className="dash-stat-val">{stats.total.toLocaleString()}</div><div className="dash-stat-label">Total runs</div></motion.div>
                        <motion.div className="dash-stat-card" custom={2} variants={fadeIn} initial="hidden" animate="visible"><div className="dash-stat-val">{stats.success.toLocaleString()}</div><div className="dash-stat-label">Successful</div></motion.div>
                        <motion.div className="dash-stat-card" custom={3} variants={fadeIn} initial="hidden" animate="visible"><div className="dash-stat-val">{stats.failed.toLocaleString()}</div><div className="dash-stat-label">Failed</div></motion.div>
                        <motion.div className="dash-stat-chart" custom={4} variants={fadeIn} initial="hidden" animate="visible">
                            <ResponsiveContainer width="100%" height={120}><PieChart><Pie data={statsData} cx="50%" cy="50%" innerRadius={30} outerRadius={50} paddingAngle={2} dataKey="value" stroke="var(--bg-card)" strokeWidth={2} animationDuration={1500}>{statsData.map((entry, index) => <Cell key={entry.name} fill={chartColors[index % chartColors.length]} />)}</Pie><Tooltip contentStyle={{ background: 'rgba(10,10,10,0.7)', backdropFilter: 'blur(8px)', border: '1px solid var(--border-color)', borderRadius: '8px' }} itemStyle={{ color: 'var(--text-primary)' }} /></PieChart></ResponsiveContainer>
                        </motion.div>
                    </div>
                </section>
            )}

            <section className="dash-section">
                <div className="dash-section-head"><div><span className="dash-section-title">Starter workflows</span><span className="dash-section-note">Saved as drafts; connect apps and complete any fields before activation.</span></div><Link to="/dashboard/workflows/new" className="dash-section-link">Start from scratch <HiArrowRight /></Link></div>
                <div className="dash-templates-grid">
                    {starters.map((starter, index) => (
                        <motion.button className="dash-template-card" type="button" key={starter.name} custom={index} variants={fadeIn} initial="hidden" animate="visible" onClick={() => handleUseStarter(starter)} disabled={creatingTemplate !== null}>
                            <div className="dash-template-top"><div className="dash-template-icon">{starter.icon}</div><div className="dash-template-info"><div className="dash-template-name">{starter.name}</div><div className="dash-template-apps">{starter.apps}</div></div></div>
                            <div className="dash-template-desc">{starter.desc}</div>
                            <span className="dash-template-action">{creatingTemplate === starter.name ? 'Adding starter...' : 'Use starter'} <HiArrowRight /></span>
                        </motion.button>
                    ))}
                </div>
            </section>

            <section className="dash-section">
                <div className="dash-section-head"><span className="dash-section-title">Connect your apps</span><Link to="/dashboard/connections" className="dash-section-link">Browse integrations <HiArrowRight /></Link></div>
                <div className="dash-apps-row">
                    {featuredApps.map((app) => {
                        const isConnected = connectedAppKeys.has(app.appKey);
                        return <Link className="dash-app-chip" key={app.appKey} to={`/dashboard/connections?connect=${app.appKey}`}><span className="dash-app-chip-icon">{app.icon}</span><div><div className="dash-app-chip-name">{app.name}</div><div className={`dash-app-chip-status ${isConnected ? 'connected' : ''}`}>{isConnected ? 'Connected' : 'Connect'}</div></div></Link>;
                    })}
                    <Link className="dash-app-add" to="/dashboard/connections" aria-label="Browse all integrations"><HiPlus /></Link>
                </div>
            </section>

            <section className="dash-section">
                <div className="dash-section-head"><span className="dash-section-title">Learn and explore</span></div>
                <div className="dash-resources-grid">
                    {resources.map((resource, index) => <motion.div key={resource.title} custom={index + 8} variants={fadeIn} initial="hidden" animate="visible"><Link to={resource.to} className="dash-resource-card"><div className="dash-resource-icon">{resource.icon}</div><div className="dash-resource-title">{resource.title}</div><div className="dash-resource-desc">{resource.desc}</div></Link></motion.div>)}
                </div>
            </section>

            <section className="dash-section">
                <div className="dash-section-head"><span className="dash-section-title">Recent workflows</span><Link to="/dashboard/workflows" className="dash-section-link">View all <HiArrowRight /></Link></div>
                {recent.length === 0 ? (
                    <div className="dash-empty-state"><HiOutlineLightningBolt /><span>No workflows yet. Pick a starter above or <Link to="/dashboard/workflows/new">create one from scratch</Link>.</span></div>
                ) : (
                    <div className="dash-recent-list">
                        {recent.map((workflow, index) => <motion.div key={workflow.id} custom={index + 11} variants={fadeIn} initial="hidden" animate="visible"><Link to={`/dashboard/workflows/${workflow.id}`} className="dash-recent-row"><div className="dash-recent-icon"><HiOutlineLightningBolt /></div><div className="dash-recent-info"><div className="dash-recent-name">{workflow.name}</div><div className="dash-recent-meta">{workflow.lastRunAt ? `Last run ${formatRelative(workflow.lastRunAt)}` : `Created ${formatRelative(workflow.createdAt)}`}</div></div><span className={`dash-recent-status ${workflow.isActive ? 'active' : 'draft'}`}>{workflow.isActive ? 'Active' : 'Draft'}</span></Link></motion.div>)}
                    </div>
                )}
            </section>
        </div>
    );
}
