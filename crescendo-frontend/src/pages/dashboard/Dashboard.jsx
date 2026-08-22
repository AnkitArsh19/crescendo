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
    HiOutlineSparkles,
    HiOutlineChip,
} from 'react-icons/hi';
import { SiGithub, SiGooglesheets, SiDiscord, SiGooglecalendar, SiSpotify } from 'react-icons/si';
import { FaSlack } from 'react-icons/fa';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import useAuthStore from '../../store/authStore';
import useConnectionStore from '../../store/connectionStore';
import useToastStore from '../../store/toastStore';
import { useCreateWorkflow, useWorkflowList } from '../../hooks/useWorkflows';
import { workflowClient } from '../../api/workflowClient';
import { allRunsApi } from '../../api/logbookApi';
import './Dashboard.css';

const WORKFLOW_CATEGORIES = ['All', 'Developers', 'Students', 'AI & Automation', 'Productivity'];

const starters = [
    {
        icon: <SiGithub />,
        name: 'AI Pull Request Reviewer & Slack Alert',
        category: 'Developers',
        apps: 'GitHub -> AI Agent -> Condition -> Slack',
        desc: 'Audit new pull requests for security vulnerabilities with AI, then route high-risk alerts directly to Slack.',
        steps: [
            { name: 'New Pull Request', type: 'TRIGGER', appKey: 'github', actionKey: 'new-pr', configuration: {} },
            { name: 'AI Code & Security Audit', type: 'ACTION', appKey: 'agent', actionKey: 'ai_agent', configuration: { provider: 'gemini', model: 'gemini-2.5-flash', goal: 'Analyze the pull request diff for bugs, breaking changes, and security risks. Rate overall risk as LOW, MEDIUM, or HIGH.' } },
            { name: 'Check If High Risk', type: 'CONDITION', appKey: 'condition', actionKey: 'rule', configuration: { operator: 'CONTAINS', field: '{{step_2.finalAnswer}}', value: 'HIGH' } },
            { name: 'Dispatch Slack Alert', type: 'ACTION', appKey: 'slack', actionKey: 'sendMessage', configuration: { text: '🚨 High-risk Pull Request detected:\n{{step_2.finalAnswer}}' } },
        ],
    },
    {
        icon: <HiOutlineCode />,
        name: 'Daily LeetCode Challenge & AI Study Coach',
        category: 'Students',
        apps: 'Schedule -> LeetCode -> AI Agent -> Discord',
        desc: 'Fetch the daily LeetCode challenge every morning, generate progressive hints with AI, and post to your study group.',
        steps: [
            { name: 'Morning 8:00 AM Alarm', type: 'TRIGGER', appKey: 'schedule', actionKey: 'cron', configuration: { cronExpression: '0 0 8 * * *' } },
            { name: 'Fetch Daily LeetCode', type: 'ACTION', appKey: 'leetcode', actionKey: 'daily-problem', configuration: {} },
            { name: 'AI Problem Explainer & Hints', type: 'ACTION', appKey: 'agent', actionKey: 'ai_agent', configuration: { provider: 'gemini', model: 'gemini-2.5-flash', goal: 'Provide 2 progressive algorithmic hints and time/space complexity targets without spoiling the full solution code.' } },
            { name: 'Post to Discord Study Room', type: 'ACTION', appKey: 'discord', actionKey: 'sendMessage', configuration: { content: '🎯 Daily LeetCode Challenge is live! Study hints:\n{{step_3.finalAnswer}}' } },
        ],
    },
    {
        icon: <HiOutlineSparkles />,
        name: 'Tech Radar AI Digest to Notion',
        category: 'AI & Automation',
        apps: 'Schedule -> Hacker News -> AI Agent -> Notion',
        desc: 'Pulls top trending tech stories every weekday, synthesizes key takeaways using AI, and saves notes into Notion.',
        steps: [
            { name: 'Weekday 9:00 AM Cron', type: 'TRIGGER', appKey: 'schedule', actionKey: 'cron', configuration: { cronExpression: '0 0 9 * * MON-FRI' } },
            { name: 'Top Hacker News Stories', type: 'ACTION', appKey: 'hackernews', actionKey: 'top-stories', configuration: { limit: 5 } },
            { name: 'AI Executive Summary', type: 'ACTION', appKey: 'agent', actionKey: 'ai_agent', configuration: { provider: 'gemini', model: 'gemini-2.5-flash', goal: 'Extract the top 3 architectural and tech takeaways from these articles.' } },
            { name: 'Append to Notion Journal', type: 'ACTION', appKey: 'notion', actionKey: 'append-block', configuration: { content: '{{step_3.finalAnswer}}' } },
        ],
    },
    {
        icon: <HiOutlineChip />,
        name: 'Smart Customer Sentiment Triage',
        category: 'AI & Automation',
        apps: 'Webhook -> AI Agent -> Condition -> Google Sheets',
        desc: 'Classify inbound user feedback sentiment with AI. Log feedback to Sheets and immediately alert team on critical bugs.',
        steps: [
            { name: 'Receive Feedback Webhook', type: 'TRIGGER', appKey: 'crescendo-webhook', actionKey: 'incoming', configuration: { method: 'POST', urlPattern: '/feedback' } },
            { name: 'AI Sentiment & Severity Triage', type: 'ACTION', appKey: 'agent', actionKey: 'ai_agent', configuration: { provider: 'gemini', model: 'gemini-2.5-flash', goal: 'Classify user feedback into: POSITIVE, NEUTRAL, or CRITICAL_BUG.' } },
            { name: 'Log to Google Sheets', type: 'ACTION', appKey: 'google-sheets', actionKey: 'appendRow', configuration: { spreadsheetId: 'feedback_db' } },
            { name: 'Check If Critical Bug', type: 'CONDITION', appKey: 'condition', actionKey: 'rule', configuration: { operator: 'CONTAINS', field: '{{step_2.finalAnswer}}', value: 'CRITICAL_BUG' } },
            { name: 'Alert On-Call Slack Channel', type: 'ACTION', appKey: 'slack', actionKey: 'sendMessage', configuration: { text: '🔥 Critical Bug reported by user: {{step_1.message}}' } },
        ],
    },
    {
        icon: <HiOutlineDatabase />,
        name: 'Campus Event RSVP & Email Confirmation',
        category: 'Students',
        apps: 'Webhook -> Google Sheets -> Crescendo Mail',
        desc: 'Capture campus RSVP submissions, record attendee rows in Google Sheets, and auto-dispatch confirmation passes.',
        steps: [
            { name: 'Receive RSVP Form', type: 'TRIGGER', appKey: 'crescendo-webhook', actionKey: 'incoming', configuration: { method: 'POST', urlPattern: '/campus-rsvp' } },
            { name: 'Record Attendee in Sheets', type: 'ACTION', appKey: 'google-sheets', actionKey: 'appendRow', configuration: {} },
            { name: 'Send Confirmation Email', type: 'ACTION', appKey: 'crescendo-mail', actionKey: 'sendEmail', configuration: { subject: 'Confirmed: Your Hackathon Ticket!', htmlBody: '<p>Hi {{step_1.name}}, your registration for the Campus Hackathon is confirmed!</p>' } },
        ],
    },
    {
        icon: <HiOutlineGlobe />,
        name: 'Website Uptime Monitor & Alerting',
        category: 'Developers',
        apps: 'Schedule -> HTTP -> Condition -> Telegram',
        desc: 'Run automated health checks against your server every 15 minutes and trigger emergency Telegram alerts on downtime.',
        steps: [
            { name: '15-Minute Uptime Poller', type: 'TRIGGER', appKey: 'schedule', actionKey: 'cron', configuration: { cronExpression: '*/15 * * * *' } },
            { name: 'Ping Health Endpoint', type: 'ACTION', appKey: 'http', actionKey: 'request', configuration: { method: 'GET', url: 'https://api.my-app.com/health', authentication: 'none' } },
            { name: 'Check HTTP Status Code', type: 'CONDITION', appKey: 'condition', actionKey: 'rule', configuration: { operator: 'EQUALS', field: '{{step_2.status}}', value: '200' } },
            { name: 'Telegram Down Alert', type: 'ACTION', appKey: 'telegram', actionKey: 'sendMessage', configuration: { text: '⚠️ Service alert: API health endpoint returned non-200 status code.' } },
        ],
    },
    {
        icon: <SiGithub />,
        name: 'GitHub Issue Auto-Triage to Linear',
        category: 'Developers',
        apps: 'GitHub -> AI Agent -> Linear',
        desc: 'Analyze new GitHub issues with AI to classify component tags and automatically sync into Linear engineering backlogs.',
        steps: [
            { name: 'New GitHub Issue', type: 'TRIGGER', appKey: 'github', actionKey: 'new-issue', configuration: {} },
            { name: 'AI Severity & Domain Classifier', type: 'ACTION', appKey: 'agent', actionKey: 'ai_agent', configuration: { provider: 'gemini', model: 'gemini-2.5-flash', goal: 'Classify this issue into frontend, backend, or DevOps and assign priority (Low/Medium/High).' } },
            { name: 'Create Linear Task', type: 'ACTION', appKey: 'linear', actionKey: 'create-issue', configuration: { title: '{{step_1.title}}', description: '{{step_2.finalAnswer}}' } },
        ],
    },
    {
        icon: <HiOutlineSun />,
        name: 'Weather-Triggered Morning Notification',
        category: 'Productivity',
        apps: 'Schedule -> Weather -> Condition -> Slack',
        desc: 'Check morning forecasts and notify your team channel only when rain, snow, or extreme weather conditions occur.',
        steps: [
            { name: 'Weekday Morning 7:30 AM', type: 'TRIGGER', appKey: 'schedule', actionKey: 'cron', configuration: { cronExpression: '0 30 7 * * MON-FRI' } },
            { name: 'Check Local Weather', type: 'ACTION', appKey: 'weather', actionKey: 'get-weather', configuration: { city: 'Bengaluru', units: 'metric' } },
            { name: 'Check If Rain Forecasted', type: 'CONDITION', appKey: 'condition', actionKey: 'rule', configuration: { operator: 'CONTAINS', field: '{{step_2.condition}}', value: 'Rain' } },
            { name: 'Slack Rain Advisory', type: 'ACTION', appKey: 'slack', actionKey: 'sendMessage', configuration: { text: '🌧️ Rain advisory for today: {{step_2.condition}}, temperature: {{step_2.temperature}}°C. Don\'t forget your umbrella!' } },
        ],
    },
    {
        icon: <SiSpotify />,
        name: 'Weekly Spotify Music Discovery to Discord',
        category: 'Productivity',
        apps: 'Schedule -> Spotify -> Discord',
        desc: 'Fetch top new music releases every Friday afternoon and drop recommendations into your community Discord.',
        steps: [
            { name: 'Friday Afternoon 4:00 PM', type: 'TRIGGER', appKey: 'schedule', actionKey: 'cron', configuration: { cronExpression: '0 0 16 * * FRI' } },
            { name: 'Fetch New Music Releases', type: 'ACTION', appKey: 'spotify', actionKey: 'get-new-releases', configuration: { country: 'US', limit: 5 } },
            { name: 'Post Playlist to Discord', type: 'ACTION', appKey: 'discord', actionKey: 'sendMessage', configuration: { content: '🎵 Friday Vibes! Check out this week\'s new music releases to wrap up the sprint.' } },
        ],
    },
    {
        icon: <HiOutlineMail />,
        name: 'Daily Motivational Wisdom to Discord',
        category: 'Students',
        apps: 'Schedule -> Random Quotes -> Discord',
        desc: 'Broadcast daily motivational quotes and wisdom drops to student clubs or study groups.',
        steps: [
            { name: 'Morning 8:30 AM Cron', type: 'TRIGGER', appKey: 'schedule', actionKey: 'cron', configuration: { cronExpression: '0 30 8 * * *' } },
            { name: 'Get Motivational Quote', type: 'ACTION', appKey: 'quotes', actionKey: 'get-by-category', configuration: { category: 'motivational' } },
            { name: 'Send to Discord', type: 'ACTION', appKey: 'discord', actionKey: 'sendMessage', configuration: { content: '✨ Daily Inspiration:\n"{{step_2.quote}}" — {{step_2.author}}' } },
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
    const [selectedWfCategory, setSelectedWfCategory] = useState('All');
    const recent = workflows.slice(0, 5);
    const displayName = isGuest ? 'there' : (user?.username || user?.email?.split('@')[0] || 'there');
    const greeting = getGreeting(currentHour, displayName, 0);
    const connectedAppKeys = useMemo(() => new Set(connections.map((connection) => connection.appKey)), [connections]);

    const filteredStarters = useMemo(() => {
        if (selectedWfCategory === 'All') return starters;
        return starters.filter((s) => s.category === selectedWfCategory);
    }, [selectedWfCategory]);

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
                    <Link
                        to="/dashboard/workflows/new"
                        className="dash-hero-cta"
                        title="Create a new workflow from scratch"
                        aria-label="Create workflow"
                    >
                        <HiPlus /> Create workflow
                    </Link>
                    <Link
                        to="/dashboard/connections"
                        className="dash-hero-secondary"
                        title="Manage and connect your app integrations"
                        aria-label="Connect an app"
                    >
                        Connect an app
                    </Link>
                </div>
            </motion.section>

            {stats && stats.total > 0 && (
                <section className="dash-section">
                    <div className="dash-section-head">
                        <span className="dash-section-title">Execution overview</span>
                        <Link
                            to="/dashboard/history"
                            className="dash-section-link"
                            title="View all execution history and logs"
                            aria-label="View history"
                        >
                            View history <HiArrowRight />
                        </Link>
                    </div>
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
                <div className="dash-section-head" style={{ flexWrap: 'wrap', gap: 12 }}>
                    <div>
                        <span className="dash-section-title">Starter workflows</span>
                        <span className="dash-section-note">Saved as drafts; customize triggers, AI agents, and logic before activating.</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                        {WORKFLOW_CATEGORIES.map((cat) => (
                            <button
                                key={cat}
                                type="button"
                                onClick={() => setSelectedWfCategory(cat)}
                                title={`Filter templates by ${cat}`}
                                aria-label={`Filter by ${cat}`}
                                style={{
                                    fontSize: 12,
                                    fontWeight: selectedWfCategory === cat ? 600 : 500,
                                    padding: '4px 10px',
                                    borderRadius: 20,
                                    border: '1px solid',
                                    borderColor: selectedWfCategory === cat ? 'var(--primary-color, #6366f1)' : 'var(--border-color)',
                                    background: selectedWfCategory === cat ? 'rgba(99, 102, 241, 0.15)' : 'transparent',
                                    color: selectedWfCategory === cat ? 'var(--text-primary, #ffffff)' : 'var(--text-secondary)',
                                    cursor: 'pointer',
                                    transition: 'all 0.15s ease'
                                }}
                            >
                                {cat}
                            </button>
                        ))}
                    </div>
                </div>
                <div className="dash-templates-grid">
                    {filteredStarters.map((starter, index) => (
                        <motion.button
                            className="dash-template-card"
                            type="button"
                            key={starter.name}
                            custom={index}
                            variants={fadeIn}
                            initial="hidden"
                            animate="visible"
                            onClick={() => handleUseStarter(starter)}
                            disabled={creatingTemplate !== null}
                            title={`Use template: ${starter.name}`}
                            aria-label={`Use template: ${starter.name}`}
                        >
                            <div className="dash-template-top">
                                <div className="dash-template-icon">{starter.icon}</div>
                                <div className="dash-template-info">
                                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', marginBottom: 2 }}>
                                        <div className="dash-template-name">{starter.name}</div>
                                    </div>
                                    <div className="dash-template-apps">{starter.apps}</div>
                                </div>
                            </div>
                            <div className="dash-template-desc">{starter.desc}</div>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 12, width: '100%' }}>
                                <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-tertiary, #71717a)', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                                    {starter.category}
                                </span>
                                <span className="dash-template-action">{creatingTemplate === starter.name ? 'Adding starter...' : 'Use starter'} <HiArrowRight /></span>
                            </div>
                        </motion.button>
                    ))}
                </div>
            </section>

            <section className="dash-section">
                <div className="dash-section-head">
                    <span className="dash-section-title">Connect your apps</span>
                    <Link
                        to="/dashboard/connections"
                        className="dash-section-link"
                        title="Browse all available app integrations"
                        aria-label="Browse integrations"
                    >
                        Browse integrations <HiArrowRight />
                    </Link>
                </div>
                <div className="dash-apps-row">
                    {featuredApps.map((app) => {
                        const isConnected = connectedAppKeys.has(app.appKey);
                        return (
                            <Link
                                className="dash-app-chip"
                                key={app.appKey}
                                to={`/dashboard/connections?connect=${app.appKey}`}
                                title={`${app.name}: ${isConnected ? 'Connected' : 'Click to connect'}`}
                                aria-label={`${app.name} (${isConnected ? 'Connected' : 'Connect'})`}
                            >
                                <span className="dash-app-chip-icon">{app.icon}</span>
                                <div>
                                    <div className="dash-app-chip-name">{app.name}</div>
                                    <div className={`dash-app-chip-status ${isConnected ? 'connected' : ''}`}>
                                        {isConnected ? 'Connected' : 'Connect'}
                                    </div>
                                </div>
                            </Link>
                        );
                    })}
                    <Link
                        className="dash-app-add"
                        to="/dashboard/connections"
                        title="Browse all 114 integrations"
                        aria-label="Browse all integrations"
                    >
                        <HiPlus />
                    </Link>
                </div>
            </section>

            <section className="dash-section">
                <div className="dash-section-head"><span className="dash-section-title">Learn and explore</span></div>
                <div className="dash-resources-grid">
                    {resources.map((resource, index) => (
                        <motion.div key={resource.title} custom={index + 8} variants={fadeIn} initial="hidden" animate="visible">
                            <Link
                                to={resource.to}
                                className="dash-resource-card"
                                title={`Open ${resource.title}`}
                                aria-label={resource.title}
                            >
                                <div className="dash-resource-icon">{resource.icon}</div>
                                <div className="dash-resource-title">{resource.title}</div>
                                <div className="dash-resource-desc">{resource.desc}</div>
                            </Link>
                        </motion.div>
                    ))}
                </div>
            </section>

            <section className="dash-section">
                <div className="dash-section-head">
                    <span className="dash-section-title">Recent workflows</span>
                    <Link
                        to="/dashboard/workflows"
                        className="dash-section-link"
                        title="View all workflows"
                        aria-label="View all workflows"
                    >
                        View all <HiArrowRight />
                    </Link>
                </div>
                {recent.length === 0 ? (
                    <div className="dash-empty-state"><HiOutlineLightningBolt /><span>No workflows yet. Pick a starter above or <Link to="/dashboard/workflows/new">create one from scratch</Link>.</span></div>
                ) : (
                    <div className="dash-recent-list">
                        {recent.map((workflow, index) => (
                            <motion.div key={workflow.id} custom={index + 11} variants={fadeIn} initial="hidden" animate="visible">
                                <Link
                                    to={`/dashboard/workflows/${workflow.id}`}
                                    className="dash-recent-row"
                                    title={`Open workflow: ${workflow.name}`}
                                    aria-label={`Open workflow: ${workflow.name}`}
                                >
                                    <div className="dash-recent-icon"><HiOutlineLightningBolt /></div>
                                    <div className="dash-recent-info">
                                        <div className="dash-recent-name">{workflow.name}</div>
                                        <div className="dash-recent-meta">{workflow.lastRunAt ? `Last run ${formatRelative(workflow.lastRunAt)}` : `Created ${formatRelative(workflow.createdAt)}`}</div>
                                    </div>
                                    <span className={`dash-recent-status ${workflow.isActive ? 'active' : 'draft'}`}>
                                        {workflow.isActive ? 'Active' : 'Draft'}
                                    </span>
                                </Link>
                            </motion.div>
                        ))}
                    </div>
                )}
            </section>
        </div>
    );
}
