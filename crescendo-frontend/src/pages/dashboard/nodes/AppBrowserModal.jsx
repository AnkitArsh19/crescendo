import { useState, useMemo, useEffect, useRef } from 'react';
import {
    HiSearch,
    HiX,
    HiCheck,
    HiOutlineExternalLink,
    HiOutlineEye,
    HiOutlineEyeOff,
    HiOutlineShieldCheck,
    HiOutlineArrowLeft,
    HiOutlineLockClosed,
    HiOutlineKey,
    HiChevronRight,
    HiOutlineDocumentText,
    HiOutlineLightningBolt,
    HiOutlineRefresh,
} from 'react-icons/hi';
import { HiOutlineBolt } from 'react-icons/hi2';
import { motion } from 'framer-motion';
import ReactMarkdown from 'react-markdown';
import { appCatalogApi } from '../../../api/appCatalogApi';
import useConnectionStore from '../../../store/connectionStore';
import api from '../../../api/axios';
import './AppBrowserModal.css';

const markdownComponents = {
    p: ({ ...props }) => <p style={{ margin: '0 0 12px 0', lineHeight: '1.65', color: 'var(--text-secondary)' }} {...props} />,
    a: ({ ...props }) => <a style={{ color: 'var(--brand-primary, #6366f1)', textDecoration: 'none' }} target="_blank" rel="noopener noreferrer" {...props} />,
    ul: ({ ...props }) => <ul style={{ margin: '0 0 12px 0', paddingLeft: '20px', color: 'var(--text-secondary)' }} {...props} />,
    ol: ({ ...props }) => <ol style={{ margin: '0 0 12px 0', paddingLeft: '20px', color: 'var(--text-secondary)' }} {...props} />,
    li: ({ ...props }) => <li style={{ marginBottom: '6px', lineHeight: '1.55' }} {...props} />,
    strong: ({ ...props }) => <strong style={{ color: 'var(--text-primary)', fontWeight: 600 }} {...props} />,
    code: ({ ...props }) => <code style={{ background: 'rgba(255,255,255,0.08)', padding: '2px 6px', borderRadius: '4px', fontSize: '0.82rem', color: 'var(--text-primary)' }} {...props} />,
    h1: ({ ...props }) => <h1 style={{ color: 'var(--text-accent)', fontSize: '1rem', margin: '14px 0 8px' }} {...props} />,
    h2: ({ ...props }) => <h2 style={{ color: 'var(--text-accent)', fontSize: '0.92rem', margin: '12px 0 6px' }} {...props} />,
    h3: ({ ...props }) => <h3 style={{ color: 'var(--text-accent)', fontSize: '0.86rem', margin: '10px 0 4px' }} {...props} />,
};

const TRIGGER_ONLY_APPS = new Set(['schedule', 'form', 'native-form', 'rss']);
const ACTION_ONLY_APPS = new Set([
    'agent', 'readpdf', 'smtp', 'gemini', 'openai', 'sarvam', 'pomodoro',
    'cat-facts', 'giphy', 'quotes', 'joke-api', 'nasa-apod', 'weather',
    'github-stats', 'leetcode', 'http'
]);

/**
 * AppBrowserModal — universal app browser, connector, and options inspector.
 */
export default function AppBrowserModal({
    apps = [],
    connections = [],
    onSelect,
    onClose,
    title = 'Choose an App',
    connectOnly = false,
    onConnected,
    initialAppKey = null,
    targetType = 'all', // 'trigger' | 'action' | 'all'
}) {
    const [search, setSearch] = useState('');
    const [activeTab, setActiveTab] = useState('all');
    const [detailApp, setDetailApp] = useState(null);
    const [detailSection, setDetailSection] = useState(initialAppKey ? 'connection' : 'overview'); // 'overview' | 'actions' | 'connection'
    const [activeConnectMode, setActiveConnectMode] = useState(null); // 'OAUTH2', 'CUSTOM_OAUTH2', 'APIKEY'
    const [connectError, setConnectError] = useState(null);
    const [name, setName] = useState('');
    const [credentials, setCredentials] = useState({});
    const [showPasswords, setShowPasswords] = useState({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    const visibleApps = useMemo(() => {
        if (!apps || !Array.isArray(apps)) return [];
        return apps.filter((app) => {
            const key = (app.appKey || '').toLowerCase();
            if (targetType === 'trigger') {
                if (app.hasTriggers !== undefined) return app.hasTriggers;
                return !ACTION_ONLY_APPS.has(key);
            }
            if (targetType === 'action') {
                if (app.hasActions !== undefined) return app.hasActions;
                return !TRIGGER_ONLY_APPS.has(key);
            }
            return true;
        });
    }, [apps, targetType]);

    useEffect(() => {
        if (initialAppKey && visibleApps.length > 0) {
            const found = visibleApps.find(a => a.appKey === initialAppKey);
            if (found) {
                setDetailApp(found);
                setDetailSection('connection');
            }
        }
    }, [initialAppKey, visibleApps]);
    const [platformKeyApps, setPlatformKeyApps] = useState(new Set());
    const [actionSearch, setActionSearch] = useState('');
    const searchRef = useRef(null);
    const { createConnection } = useConnectionStore();

    const categories = useMemo(() => {
        const cats = new Set();
        visibleApps.forEach((a) => {
            if (a.category) cats.add(a.category.toLowerCase());
        });
        return ['all', ...Array.from(cats).sort()];
    }, [visibleApps]);

    const connectedAppKeys = useMemo(() => {
        return new Set(connections.map((c) => c.appKey));
    }, [connections]);

    // ── Fetch Full App Metadata When Opening Detail View ───────────────────────
    useEffect(() => {
        if (detailApp?.appKey) {
            appCatalogApi.get(detailApp.appKey).then((fullApp) => {
                if (fullApp) {
                    setDetailApp((prev) => (prev?.appKey === fullApp.appKey ? { ...prev, ...fullApp } : prev));
                }
            }).catch(() => {});
        }
    }, [detailApp?.appKey]);

    // ── Search Scoring & Ranking Algorithm ───────────────────────────────────
    const searchScoreMap = useMemo(() => {
        if (!search.trim()) return new Map();
        const q = search.trim().toLowerCase();
        const map = new Map();

        visibleApps.forEach((app) => {
            const appName = (app.name || '').toLowerCase();
            const appKey = (app.appKey || '').toLowerCase();
            const category = (app.category || '').toLowerCase();
            const desc = (app.description || '').toLowerCase();
            const actions = Array.isArray(app.actions) ? app.actions : [];
            const triggers = Array.isArray(app.triggers) ? app.triggers : [];

            let score = 0;

            // 1. Exact match on name or key
            if (appName === q || appKey === q) {
                score += 10000;
            }
            // 2. Name starts with query
            else if (appName.startsWith(q) || appKey.startsWith(q)) {
                score += 5000;
            }
            // 3. Word in name starts with query
            else if (appName.split(/[\s-_]+/).some((w) => w.startsWith(q))) {
                score += 3500;
            }
            // 4. Name or key contains query as substring
            else if (appName.includes(q) || appKey.includes(q)) {
                score += 2000;
            }
            // 5. Category matches or starts with query
            else if (category === q) {
                score += 1500;
            } else if (category.startsWith(q)) {
                score += 1200;
            }
            // 6. Action/Trigger name exact or startsWith
            else if (
                actions.some((a) => (a.name || '').toLowerCase().startsWith(q)) ||
                triggers.some((t) => (t.name || '').toLowerCase().startsWith(q))
            ) {
                score += 800;
            }
            // 7. Action/Trigger name or key contains query
            else if (
                actions.some(
                    (a) =>
                        (a.name || '').toLowerCase().includes(q) ||
                        (a.actionKey || '').toLowerCase().includes(q)
                ) ||
                triggers.some(
                    (t) =>
                        (t.name || '').toLowerCase().includes(q) ||
                        (t.triggerKey || '').toLowerCase().includes(q)
                )
            ) {
                score += 400;
            }
            // 8. Description contains query
            else if (desc.includes(q)) {
                score += 100;
            }

            if (score > 0) {
                map.set(app.appKey, score);
            }
        });

        return map;
    }, [visibleApps, search]);

    const filtered = useMemo(() => {
        if (!search.trim()) {
            if (activeTab === 'all') return visibleApps;
            return visibleApps.filter((a) => (a.category || '').toLowerCase() === activeTab);
        }

        // Searching across all apps with relevance ranking
        const matches = visibleApps.filter((a) => searchScoreMap.has(a.appKey));
        return matches.sort((a, b) => {
            const scoreA = searchScoreMap.get(a.appKey) || 0;
            const scoreB = searchScoreMap.get(b.appKey) || 0;
            if (scoreB !== scoreA) return scoreB - scoreA;
            return a.name.localeCompare(b.name);
        });
    }, [visibleApps, search, activeTab, searchScoreMap]);

    const grouped = useMemo(() => {
        if (search.trim()) {
            return { 'Search Results': filtered };
        }
        if (activeTab !== 'all') return { [activeTab]: filtered };
        const groups = {};
        filtered.forEach((app) => {
            const cat = app.category || 'other';
            if (!groups[cat]) groups[cat] = [];
            groups[cat].push(app);
        });
        return groups;
    }, [filtered, activeTab, search]);

    useEffect(() => {
        if (!detailApp) searchRef.current?.focus();
    }, [detailApp]);

    useEffect(() => {
        api.get('/admin/platform-keys/available')
            .then((res) => {
                const keys = new Set(res.data.map((k) => k.appKey));
                setPlatformKeyApps(keys);
            })
            .catch(() => {});
    }, []);

    useEffect(() => {
        const handler = (e) => {
            if (e.key === 'Escape') {
                if (detailApp) {
                    if (activeConnectMode) setActiveConnectMode(null);
                    else setDetailApp(null);
                } else {
                    onClose?.();
                }
            }
        };
        document.addEventListener('keydown', handler);
        return () => document.removeEventListener('keydown', handler);
    }, [onClose, detailApp, activeConnectMode]);

    useEffect(() => {
        if (connectError) {
            const timer = setTimeout(() => setConnectError(null), 5000);
            return () => clearTimeout(timer);
        }
    }, [connectError]);

    const getActiveSchema = (app) => {
        if (!app?.credentialSchema?.length) return [];
        return app.credentialSchema;
    };

    // ── Open Options / Description modal for any app ─────────────────────────
    const handleOpenOptions = (e, app, defaultTab = 'overview') => {
        e?.stopPropagation();
        setConnectError(null);
        setDetailApp(app);
        setDetailSection(defaultTab);
        setActiveConnectMode(null);
        setName(`My ${app.name}`);
        setCredentials({});
        setActionSearch('');
    };

    // ── Direct 1-Click Connect (e.g. from card button) ──────────────────────
    const handleDirectConnect = (e, app) => {
        e?.stopPropagation();
        setConnectError(null);

        if (app.authType === 'NONE') {
            if (!connectOnly) onSelect?.(app);
            return;
        }

        if (app.authType === 'OAUTH2') {
            setName(`My ${app.name}`);
            setDetailApp(app);
            startOAuth(app);
            return;
        }

        // For API key or schema-based apps, open the options modal on connection tab
        handleOpenOptions(e, app, 'connection');
        setActiveConnectMode('APIKEY');
    };

    const startOAuth = async (app, opts = {}) => {
        try {
            const providerKey = app.appKey;
            const { authorizationUrl } = await appCatalogApi.getOAuthUrl(providerKey, opts);

            const popup = window.open(authorizationUrl, 'oauth_popup', 'width=600,height=700,scrollbars=yes');
            if (popup) {
                const messageHandler = (event) => {
                    if (event.data?.type === 'oauth-connected') {
                        window.removeEventListener('message', messageHandler);
                        const connData = event.data;
                        onConnected?.(connData);
                        if (!connectOnly) {
                            onSelect?.(app, 'PERSONAL', connData.connectionId, connData.connectionName);
                            onClose?.();
                        }
                        setDetailApp(null);
                    }
                };
                window.addEventListener('message', messageHandler);

                const pollTimer = setInterval(() => {
                    if (popup.closed) {
                        clearInterval(pollTimer);
                        window.removeEventListener('message', messageHandler);
                        onConnected?.();
                        if (!connectOnly) {
                            const matching = connections.find((c) => c.appKey === app.appKey);
                            onSelect?.(app, 'PERSONAL', matching?.id, matching?.name);
                            onClose?.();
                        }
                        setDetailApp(null);
                    }
                }, 1000);
            }
        } catch {
            if (app.altAuthType === 'APIKEY') {
                setActiveConnectMode('APIKEY');
            } else {
                setConnectError(`OAuth authorization could not be started for ${app.name}. You can also use Custom OAuth with your developer credentials.`);
            }
        }
    };

    const handleOAuthConnectFromForm = () => {
        setConnectError(null);
        if (!name.trim()) {
            setConnectError('Connection name is required');
            return;
        }

        let opts = { connectionId: undefined };

        if (activeConnectMode === 'CUSTOM_OAUTH2') {
            if (!credentials.clientId?.trim()) {
                setConnectError('Client ID is required');
                return;
            }
            if (!credentials.clientSecret?.trim()) {
                setConnectError('Client Secret is required');
                return;
            }

            opts.customClientId = credentials.clientId.trim();
            opts.customClientSecret = credentials.clientSecret.trim();
            if (credentials.scopes?.trim()) {
                opts.customScopes = credentials.scopes.trim();
            }
        }

        setIsSubmitting(true);
        startOAuth(detailApp, opts).finally(() => setIsSubmitting(false));
    };

    const handleCreateConnection = async () => {
        setConnectError(null);
        if (!name.trim()) {
            setConnectError('Connection name is required');
            return;
        }

        const schema = getActiveSchema(detailApp);
        if (schema.length === 0 && !credentials.apiKey?.trim()) {
            setConnectError('API Key is required');
            return;
        }

        for (const field of schema) {
            if (field.required && (!credentials[field.key] || !credentials[field.key].toString().trim())) {
                setConnectError(`${field.label} is required`);
                return;
            }
        }

        setIsSubmitting(true);
        try {
            const created = await createConnection({ appKey: detailApp.appKey, name: name.trim(), credentials });
            const newConnId = created?.id || created?.connectionId;
            setDetailApp(null);
            setActiveConnectMode(null);
            setConnectError(null);
            onConnected?.(created);
            if (!connectOnly) {
                onSelect?.(detailApp, 'PERSONAL', newConnId, name.trim());
                onClose?.();
            }
        } catch (e) {
            setConnectError(e.response?.data?.message || 'Failed to create connection');
        } finally {
            setIsSubmitting(false);
        }
    };

    // ── Detail / Options View Rendering ──────────────────────────────────────
    if (detailApp) {
        const isConnected = connectedAppKeys.has(detailApp.appKey);
        const existingConnection = connections.find((c) => c.appKey === detailApp.appKey);
        const hasSchema = detailApp.credentialSchema && detailApp.credentialSchema.length > 0;
        const hasApiKey = detailApp.authType === 'APIKEY' || detailApp.altAuthType === 'APIKEY' || hasSchema;
        const hasOAuth = detailApp.authType === 'OAUTH2';
        const isNoAuth = detailApp.authType === 'NONE';
        const hasPlatformKey = Boolean((detailApp.hasPlatformKey || platformKeyApps.has(detailApp.appKey)) && !connectOnly);

        const schema = getActiveSchema(detailApp);
        const actionsList = Array.isArray(detailApp.actions) ? detailApp.actions : [];
        const triggersList = Array.isArray(detailApp.triggers) ? detailApp.triggers : [];
        const totalOpsCount = actionsList.length + triggersList.length;

        // Build list of valid auth methods
        const methods = [];
        if (hasPlatformKey) {
            methods.push({
                id: 'ADMIN_KEY',
                title: 'Platform Key (Zero Setup)',
                badge: 'Managed',
                badgeClass: 'badge-managed',
                description: "Use Crescendo's pre-configured platform credentials. No API key required.",
                icon: <HiOutlineShieldCheck />,
            });
        }
        if (hasOAuth) {
            methods.push({
                id: 'OAUTH2',
                title: 'OAuth 2.0 (1-Click Login)',
                badge: 'Recommended',
                badgeClass: 'badge-recommended',
                description: `Fast and secure authorization via ${detailApp.name}'s official login popup.`,
                icon: <HiOutlineLockClosed />,
            });
            methods.push({
                id: 'CUSTOM_OAUTH2',
                title: 'Custom OAuth App (BYOK)',
                badge: 'Advanced',
                badgeClass: 'badge-advanced',
                description: `Use your own ${detailApp.name} Developer App Client ID & Secret.`,
                icon: <HiOutlineKey />,
            });
        }
        if (hasApiKey) {
            methods.push({
                id: 'APIKEY',
                title: hasSchema ? 'API Credentials' : 'API Key / Token',
                badge: hasOAuth ? 'Manual' : 'Direct',
                badgeClass: hasOAuth ? 'badge-manual' : 'badge-direct',
                description: `Authenticate using your ${detailApp.name} API key or personal access token.`,
                icon: <HiOutlineKey />,
            });
        }

        // Filter actions & triggers in tab
        const filteredActions = actionsList.filter(
            (a) =>
                !actionSearch.trim() ||
                (a.name || '').toLowerCase().includes(actionSearch.toLowerCase()) ||
                (a.description || '').toLowerCase().includes(actionSearch.toLowerCase()) ||
                (a.actionKey || '').toLowerCase().includes(actionSearch.toLowerCase())
        );

        const filteredTriggers = triggersList.filter(
            (t) =>
                !actionSearch.trim() ||
                (t.name || '').toLowerCase().includes(actionSearch.toLowerCase()) ||
                (t.description || '').toLowerCase().includes(actionSearch.toLowerCase()) ||
                (t.triggerKey || '').toLowerCase().includes(actionSearch.toLowerCase())
        );

        return (
            <div className="abm-overlay" onClick={() => { if (!activeConnectMode) setDetailApp(null); }}>
                <motion.div
                    className="abm-modal detail-mode"
                    initial={{ opacity: 0, scale: 0.65, y: 40 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.75, y: 20 }}
                    transition={{ type: 'spring', stiffness: 380, damping: 18, mass: 0.8 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    <div className="abm-header">
                        <div className="abm-header-top">
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <button
                                    className="abm-close"
                                    onClick={() => {
                                        if (activeConnectMode) setActiveConnectMode(null);
                                        else setDetailApp(null);
                                    }}
                                    title={activeConnectMode ? 'Back to connection methods' : 'Back to apps catalog'}
                                    aria-label="Back"
                                >
                                    <HiOutlineArrowLeft />
                                </button>
                                <span className="abm-title">
                                    {activeConnectMode === 'CUSTOM_OAUTH2'
                                        ? `Custom OAuth · ${detailApp.name}`
                                        : activeConnectMode === 'APIKEY'
                                        ? `API Credentials · ${detailApp.name}`
                                        : `${detailApp.name}`}
                                </span>
                            </div>
                            <button
                                className="abm-close"
                                onClick={onClose}
                                title="Close (Esc)"
                                aria-label="Close modal"
                            >
                                <HiX />
                            </button>
                        </div>
                    </div>

                    <div className="abm-body abm-body-detail">
                        {/* App Hero Banner */}
                        <div className="abm-detail-hero">
                            <div className="abm-detail-icon-large">
                                <img
                                    src={detailApp.logoUrl || `/icons/${detailApp.appKey}.svg`}
                                    alt={detailApp.name}
                                    className="app-logo-img"
                                    onError={(e) => { e.target.style.display = 'none'; }}
                                />
                            </div>
                            <div className="abm-detail-hero-text">
                                <div className="abm-detail-title-large">{detailApp.name}</div>
                                <div className="abm-detail-meta-row">
                                    {detailApp.category && (
                                        <span className="abm-detail-category-badge">{detailApp.category}</span>
                                    )}
                                    {totalOpsCount > 0 && (
                                        <span className="abm-detail-counts">
                                            {actionsList.length > 0 && `${actionsList.length} Action${actionsList.length !== 1 ? 's' : ''}`}
                                            {actionsList.length > 0 && triggersList.length > 0 && ' · '}
                                            {triggersList.length > 0 && `${triggersList.length} Trigger${triggersList.length !== 1 ? 's' : ''}`}
                                        </span>
                                    )}
                                    {detailApp.helpUrl && (
                                        <a
                                            href={detailApp.helpUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="abm-help-link"
                                            title={`Open official ${detailApp.name} setup guide and documentation`}
                                        >
                                            Documentation <HiOutlineExternalLink />
                                        </a>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Error Toast */}
                        {connectError && <div className="abm-error-toast">{connectError}</div>}

                        {/* Detail Navigation Tabs */}
                        {!activeConnectMode && (
                            <nav className="abm-detail-nav" aria-label="App detail sections">
                                <button
                                    type="button"
                                    className={`abm-detail-nav-btn ${detailSection === 'overview' ? 'active' : ''}`}
                                    onClick={() => setDetailSection('overview')}
                                    title="View app description and overview"
                                >
                                    <HiOutlineDocumentText /> Overview
                                </button>
                                {totalOpsCount > 0 && (
                                    <button
                                        type="button"
                                        className={`abm-detail-nav-btn ${detailSection === 'actions' ? 'active' : ''}`}
                                        onClick={() => setDetailSection('actions')}
                                        title="View supported triggers and actions"
                                    >
                                        <HiOutlineLightningBolt /> Operations ({totalOpsCount})
                                    </button>
                                )}
                                {!isNoAuth && (
                                    <button
                                        type="button"
                                        className={`abm-detail-nav-btn ${detailSection === 'connection' ? 'active' : ''}`}
                                        onClick={() => setDetailSection('connection')}
                                        title="Manage connection and credentials"
                                    >
                                        <HiOutlineLockClosed /> Connection
                                        {isConnected && <span className="abm-detail-status-dot" title="Active connection exists" />}
                                    </button>
                                )}
                            </nav>
                        )}

                        {/* ── TAB 1: OVERVIEW ── */}
                        {!activeConnectMode && detailSection === 'overview' && (
                            <div className="abm-overview-container">
                                <div className="abm-overview-scroll">
                                    {detailApp.description ? (
                                        <ReactMarkdown components={markdownComponents}>
                                            {detailApp.description}
                                        </ReactMarkdown>
                                    ) : (
                                        <p style={{ color: 'var(--text-tertiary)', fontStyle: 'italic' }}>
                                            Connect your {detailApp.name} account to run triggers and automate actions in Crescendo workflows.
                                        </p>
                                    )}
                                </div>

                                <div className="abm-overview-footer">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        {isConnected ? (
                                            <span className="abm-app-connected-badge">
                                                <HiCheck style={{ fontSize: '0.65rem' }} /> Connected: {existingConnection?.name || detailApp.name}
                                            </span>
                                        ) : !isNoAuth ? (
                                            <span style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
                                                Requires account authorization
                                            </span>
                                        ) : (
                                            <span style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
                                                No credentials required
                                            </span>
                                        )}
                                    </div>

                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        {isConnected ? (
                                            <>
                                                <button
                                                    type="button"
                                                    className="abm-btn-secondary"
                                                    onClick={() => setDetailSection('connection')}
                                                    title="Manage account settings"
                                                >
                                                    Manage Connection
                                                </button>
                                                {!connectOnly && (
                                                    <button
                                                        type="button"
                                                        className="abm-btn-primary"
                                                        onClick={() => {
                                                            onSelect?.(detailApp, 'PERSONAL', existingConnection?.id, existingConnection?.name);
                                                            onClose?.();
                                                        }}
                                                        title="Select this app with your existing connection"
                                                    >
                                                        Use in Workflow
                                                    </button>
                                                )}
                                            </>
                                        ) : !isNoAuth ? (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                <button
                                                    type="button"
                                                    className="abm-btn-secondary"
                                                    onClick={() => setDetailSection('connection')}
                                                    title="Choose a connection method and connect account"
                                                >
                                                    Connect Account
                                                </button>
                                                {!connectOnly && (
                                                    <button
                                                        type="button"
                                                        className="abm-btn-primary"
                                                        onClick={() => {
                                                            const isUsingPlatform = (detailApp.hasPlatformKey || platformKeyApps.has(detailApp.appKey));
                                                            onSelect?.(detailApp, isUsingPlatform ? 'ADMIN_KEY' : 'PERSONAL', null, '');
                                                            onClose?.();
                                                        }}
                                                        title={`Use ${detailApp.name} in this workflow step`}
                                                    >
                                                        Use in Workflow
                                                    </button>
                                                )}
                                            </div>
                                        ) : (
                                            !connectOnly && (
                                                <button
                                                    type="button"
                                                    className="abm-btn-primary"
                                                    onClick={() => {
                                                        onSelect?.(detailApp);
                                                        onClose?.();
                                                    }}
                                                    title="Use this app in workflow"
                                                >
                                                    Use {detailApp.name} in Workflow
                                                </button>
                                            )
                                        )}
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* ── TAB 2: OPERATIONS (ACTIONS & TRIGGERS) ── */}
                        {!activeConnectMode && detailSection === 'actions' && (
                            <div className="abm-overview-container">
                                <div className="abm-search-box" style={{ height: '36px', marginBottom: '8px' }}>
                                    <div className="abm-search-icon"><HiSearch /></div>
                                    <input
                                        className="abm-search-input"
                                        type="text"
                                        placeholder={`Search ${totalOpsCount} operations in ${detailApp.name}…`}
                                        value={actionSearch}
                                        onChange={(e) => setActionSearch(e.target.value)}
                                    />
                                    {actionSearch && (
                                        <button
                                            type="button"
                                            className="abm-search-clear"
                                            onClick={() => setActionSearch('')}
                                            title="Clear"
                                        >
                                            <HiX />
                                        </button>
                                    )}
                                </div>

                                <div className="abm-actions-list-container">
                                    {filteredTriggers.length > 0 && (
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                            <div className="abm-methods-title">Triggers ({filteredTriggers.length})</div>
                                            {filteredTriggers.map((trg) => (
                                                <div key={trg.triggerKey} className="abm-action-item-card">
                                                    <div className="abm-action-item-header">
                                                        <span className="abm-action-item-title">{trg.name}</span>
                                                        <span className="abm-action-item-type trigger">Trigger</span>
                                                    </div>
                                                    {trg.description && (
                                                        <span className="abm-action-item-desc">{trg.description}</span>
                                                    )}
                                                    <span className="abm-action-item-key">{trg.triggerKey}</span>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {filteredActions.length > 0 && (
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                            <div className="abm-methods-title">Actions ({filteredActions.length})</div>
                                            {filteredActions.map((act) => (
                                                <div key={act.actionKey} className="abm-action-item-card">
                                                    <div className="abm-action-item-header">
                                                        <span className="abm-action-item-title">{act.name}</span>
                                                        <span className="abm-action-item-type action">Action</span>
                                                    </div>
                                                    {act.description && (
                                                        <span className="abm-action-item-desc">{act.description}</span>
                                                    )}
                                                    <span className="abm-action-item-key">{act.actionKey}</span>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {filteredTriggers.length === 0 && filteredActions.length === 0 && (
                                        <p className="adm-empty-hint" style={{ textAlign: 'center', padding: '24px' }}>
                                            No operations match &ldquo;{actionSearch}&rdquo;
                                        </p>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* ── TAB 3: CONNECTION & AUTH ── */}
                        {(activeConnectMode || detailSection === 'connection') && (
                            <>
                                {isConnected && !activeConnectMode && (
                                    <div className="abm-connected-block">
                                        <div className="abm-connected-banner">
                                            <HiCheck size={18} />
                                            <span>
                                                Active Connection: <strong>{existingConnection?.name || detailApp.name}</strong>
                                            </span>
                                        </div>
                                        {!connectOnly && (
                                            <button
                                                type="button"
                                                className="abm-btn-primary abm-btn-select-existing"
                                                onClick={() => {
                                                    onSelect?.(detailApp, 'PERSONAL', existingConnection?.id, existingConnection?.name);
                                                    onClose?.();
                                                }}
                                                title={`Use existing ${existingConnection?.name || detailApp.name} account for this step`}
                                            >
                                                Use This Connection
                                            </button>
                                        )}
                                    </div>
                                )}

                                {!activeConnectMode ? (
                                    /* Connection Methods Selector */
                                    <div className="abm-methods-section">
                                        <div className="abm-methods-title">
                                            {isConnected ? 'Connect Another Account or Select Method' : 'Select Connection Method'}
                                        </div>
                                        {methods.map((m) => (
                                            <button
                                                key={m.id}
                                                type="button"
                                                className="abm-method-card"
                                                onClick={() => {
                                                    if (m.id === 'ADMIN_KEY') {
                                                        onSelect?.(detailApp, 'ADMIN_KEY');
                                                        onClose?.();
                                                    } else {
                                                        setActiveConnectMode(m.id);
                                                    }
                                                }}
                                                title={m.title}
                                            >
                                                <div className="abm-method-card-left">
                                                    <div className="abm-method-icon">{m.icon}</div>
                                                    <div className="abm-method-info">
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                            <span className="abm-method-title">{m.title}</span>
                                                            <span className={`abm-method-badge ${m.badgeClass}`}>{m.badge}</span>
                                                        </div>
                                                        <span className="abm-method-desc">{m.description}</span>
                                                    </div>
                                                </div>
                                                <HiChevronRight style={{ color: 'var(--text-tertiary)', fontSize: '1.2rem' }} />
                                            </button>
                                        ))}
                                    </div>
                                ) : activeConnectMode === 'OAUTH2' ? (
                                    /* 1-Click OAuth Box */
                                    <div className="abm-oauth-direct-box">
                                        <label className="abm-form-label">
                                            Connection Name
                                            <input
                                                type="text"
                                                className="abm-form-input"
                                                value={name}
                                                onChange={(e) => setName(e.target.value)}
                                                placeholder={`e.g. My ${detailApp.name}`}
                                            />
                                        </label>

                                        <button
                                            type="button"
                                            className="abm-oauth-connect-btn"
                                            onClick={handleOAuthConnectFromForm}
                                            disabled={isSubmitting}
                                            title={`Authorize with ${detailApp.name}`}
                                            aria-label={`Connect with ${detailApp.name}`}
                                        >
                                            <HiOutlineLockClosed style={{ fontSize: '1.1rem' }} />
                                            {isSubmitting ? 'Opening authorization…' : `Connect with ${detailApp.name}`}
                                        </button>

                                        <span className="abm-oauth-hint">
                                            A secure authorization popup will open. Authorize Crescendo to connect your account.
                                        </span>

                                        <div className="abm-security-note">
                                            <HiOutlineShieldCheck />
                                            <span>Credentials are encrypted with AES-256-GCM before storage.</span>
                                        </div>

                                        <button
                                            type="button"
                                            className="abm-byok-toggle"
                                            onClick={() => setActiveConnectMode('CUSTOM_OAUTH2')}
                                            title="Use custom Developer Client ID and Secret"
                                        >
                                            Advanced: Bring Your Own OAuth App (Client ID &amp; Secret)
                                        </button>
                                    </div>
                                ) : (
                                    /* Credential Forms (Custom OAuth / API Key) */
                                    <div className="abm-credential-form">
                                        <label className="abm-form-label">
                                            Connection Name
                                            <input
                                                type="text"
                                                className="abm-form-input"
                                                value={name}
                                                onChange={(e) => setName(e.target.value)}
                                                placeholder={`e.g. My ${detailApp.name}`}
                                            />
                                        </label>

                                        {activeConnectMode === 'CUSTOM_OAUTH2' && (
                                            <div className="abm-custom-oauth-fields">
                                                <div className="abm-info-box">
                                                    Register an OAuth app in the {detailApp.name} developer portal with redirect URI:<br />
                                                    <code style={{ userSelect: 'all', marginTop: '4px', display: 'block' }}>
                                                        {window.location.origin}/api/connections/oauth/{detailApp.appKey}/callback
                                                    </code>
                                                </div>
                                                <label className="abm-form-label">
                                                    Client ID <span className="abm-required">*</span>
                                                    <input
                                                        type="text"
                                                        className="abm-form-input"
                                                        value={credentials.clientId || ''}
                                                        onChange={(e) => setCredentials((prev) => ({ ...prev, clientId: e.target.value }))}
                                                        placeholder="Enter Client ID"
                                                    />
                                                </label>
                                                <label className="abm-form-label">
                                                    Client Secret <span className="abm-required">*</span>
                                                    <div className="abm-password-wrap">
                                                        <input
                                                            type={!showPasswords['clientSecret'] ? 'password' : 'text'}
                                                            className="abm-form-input"
                                                            value={credentials.clientSecret || ''}
                                                            onChange={(e) => setCredentials((prev) => ({ ...prev, clientSecret: e.target.value }))}
                                                            placeholder="Enter Client Secret"
                                                        />
                                                        <button
                                                            type="button"
                                                            className="abm-eye-toggle"
                                                            title={showPasswords['clientSecret'] ? 'Hide secret' : 'Show secret'}
                                                            aria-label={showPasswords['clientSecret'] ? 'Hide secret' : 'Show secret'}
                                                            onClick={() => setShowPasswords((prev) => ({ ...prev, clientSecret: !prev.clientSecret }))}
                                                        >
                                                            {showPasswords['clientSecret'] ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                                                        </button>
                                                    </div>
                                                </label>
                                                <label className="abm-form-label">
                                                    Scopes (Optional)
                                                    <input
                                                        type="text"
                                                        className="abm-form-input"
                                                        value={credentials.scopes || ''}
                                                        onChange={(e) => setCredentials((prev) => ({ ...prev, scopes: e.target.value }))}
                                                        placeholder="e.g. read write (space-separated)"
                                                    />
                                                    <span className="abm-field-help">Leave blank to use standard default scopes</span>
                                                </label>
                                            </div>
                                        )}

                                        {activeConnectMode === 'APIKEY' && (
                                            schema.length > 0 ? (
                                                schema.map((field) => (
                                                    <label key={field.key} className="abm-form-label">
                                                        {field.label} {field.required && <span className="abm-required">*</span>}
                                                        <div className="abm-password-wrap">
                                                            <input
                                                                type={field.type === 'password' && !showPasswords[field.key] ? 'password' : 'text'}
                                                                className="abm-form-input"
                                                                value={credentials[field.key] || ''}
                                                                onChange={(e) => setCredentials((prev) => ({ ...prev, [field.key]: e.target.value }))}
                                                                placeholder={field.placeholder || `Enter ${field.label.toLowerCase()}`}
                                                            />
                                                            {field.type === 'password' && (
                                                                <button
                                                                    type="button"
                                                                    className="abm-eye-toggle"
                                                                    title={showPasswords[field.key] ? 'Hide value' : 'Show value'}
                                                                    aria-label={showPasswords[field.key] ? 'Hide value' : 'Show value'}
                                                                    onClick={() => setShowPasswords((prev) => ({ ...prev, [field.key]: !prev[field.key] }))}
                                                                >
                                                                    {showPasswords[field.key] ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                                                                </button>
                                                            )}
                                                        </div>
                                                        {field.helpText && <span className="abm-field-help">{field.helpText}</span>}
                                                    </label>
                                                ))
                                            ) : (
                                                <label className="abm-form-label">
                                                    API Key / Access Token <span className="abm-required">*</span>
                                                    <div className="abm-password-wrap">
                                                        <input
                                                            type={!showPasswords['apiKey'] ? 'password' : 'text'}
                                                            className="abm-form-input"
                                                            value={credentials.apiKey || ''}
                                                            onChange={(e) => setCredentials({ apiKey: e.target.value })}
                                                            placeholder="Paste your API key or personal access token"
                                                        />
                                                        <button
                                                            type="button"
                                                            className="abm-eye-toggle"
                                                            title={showPasswords['apiKey'] ? 'Hide API key' : 'Show API key'}
                                                            aria-label={showPasswords['apiKey'] ? 'Hide API key' : 'Show API key'}
                                                            onClick={() => setShowPasswords((prev) => ({ ...prev, apiKey: !prev.apiKey }))}
                                                        >
                                                            {showPasswords['apiKey'] ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                                                        </button>
                                                    </div>
                                                </label>
                                            )
                                        )}

                                        <div className="abm-security-note">
                                            <HiOutlineShieldCheck />
                                            <span>Credentials are encrypted with AES-256-GCM before storage.</span>
                                        </div>

                                        <div className="abm-form-footer">
                                            <button
                                                type="button"
                                                className="abm-btn-secondary"
                                                onClick={() => setActiveConnectMode(null)}
                                                title="Back to options"
                                            >
                                                Back
                                            </button>
                                            <button
                                                type="button"
                                                className="abm-btn-primary"
                                                onClick={activeConnectMode === 'CUSTOM_OAUTH2' ? handleOAuthConnectFromForm : handleCreateConnection}
                                                disabled={isSubmitting}
                                                title={`Save and connect ${detailApp.name}`}
                                            >
                                                {isSubmitting ? 'Connecting…' : (activeConnectMode === 'CUSTOM_OAUTH2' ? 'Authorize Custom App' : 'Save Connection')}
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </motion.div>
            </div>
        );
    }

    // ── Main App Catalog Grid ───────────────────────────────────────────────
    return (
        <div className="abm-overlay" onClick={onClose}>
            <motion.div
                className="abm-modal"
                initial={{ opacity: 0, scale: 0.65, y: 40 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.75, y: 20 }}
                transition={{ type: 'spring', stiffness: 380, damping: 18, mass: 0.8 }}
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="abm-header">
                    <div className="abm-header-top">
                        <span className="abm-title">{title}</span>
                        <button
                            className="abm-close"
                            onClick={onClose}
                            title="Close (Esc)"
                            aria-label="Close"
                        >
                            <HiX />
                        </button>
                    </div>

                    {/* Centered Search Box */}
                    <div className="abm-search-box">
                        <div className="abm-search-icon">
                            <HiSearch />
                        </div>
                        <input
                            ref={searchRef}
                            className="abm-search-input"
                            type="text"
                            placeholder="Search apps by name, category, or actions…"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                        <div className="abm-search-suffix">
                            {search && (
                                <span className="abm-search-count">
                                    {filtered.length} result{filtered.length !== 1 ? 's' : ''}
                                </span>
                            )}
                            {search && (
                                <button
                                    type="button"
                                    className="abm-search-clear"
                                    onClick={() => setSearch('')}
                                    title="Clear search"
                                    aria-label="Clear search"
                                >
                                    <HiX />
                                </button>
                            )}
                        </div>
                    </div>

                    {/* Error toast */}
                    {connectError && <div className="abm-error-toast">{connectError}</div>}

                    {/* Category tabs */}
                    <div className="abm-tabs" role="tablist">
                        {categories.map((cat) => (
                            <button
                                key={cat}
                                className={`abm-tab ${activeTab === cat ? 'active' : ''}`}
                                onClick={() => {
                                    setActiveTab(cat);
                                    if (search.trim()) setSearch('');
                                }}
                                title={`Filter by ${cat}`}
                                role="tab"
                                aria-selected={activeTab === cat}
                            >
                                {cat}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Body */}
                <div className="abm-body">
                    {Object.keys(grouped).length === 0 ? (
                        <div className="abm-empty">
                            <div className="abm-empty-icon"><HiSearch /></div>
                            <div>No apps match &ldquo;{search}&rdquo;</div>
                            <div className="abm-empty-hint">Try searching for app names, integrations, or keywords</div>
                        </div>
                    ) : (
                        Object.entries(grouped).map(([category, catApps]) => (
                            <div key={category} className="abm-group-container">
                                <div className="abm-category-label">{category}</div>
                                <div className="abm-grid">
                                    {catApps.map((app) => {
                                        const isConnected = connectedAppKeys.has(app.appKey);
                                        const needsAuth = app.authType && app.authType !== 'NONE';
                                        const COMING_SOON_APPS = new Set();
                                        const isComingSoon = COMING_SOON_APPS.has(app.appKey);

                                        return (
                                            <div
                                                key={app.appKey}
                                                className={`abm-app-card ${isConnected ? 'connected' : ''} ${isComingSoon ? 'coming-soon' : ''}`}
                                                title={isComingSoon ? 'Coming soon' : `Select ${app.name}`}
                                                onClick={(e) => {
                                                    if (isComingSoon) return;
                                                    if (connectOnly) {
                                                        handleOpenOptions(e, app, 'connection');
                                                        return;
                                                    }
                                                    const existing = connections.find((c) => c.appKey === app.appKey);
                                                    const isUsingPlatform = (app.hasPlatformKey || platformKeyApps.has(app.appKey)) && !existing;
                                                    onSelect?.(
                                                        app,
                                                        isUsingPlatform ? 'ADMIN_KEY' : 'PERSONAL',
                                                        existing?.id || null,
                                                        existing?.name || existing?.accountIdentifier || ''
                                                    );
                                                    onClose?.();
                                                }}
                                            >
                                                <div className="abm-app-icon">
                                                    <img
                                                        src={app.logoUrl || `/icons/${app.appKey}.svg`}
                                                        alt={app.name}
                                                        className="app-logo-img"
                                                        onError={(e) => {
                                                            e.target.style.display = 'none';
                                                            e.target.nextSibling.style.display = 'block';
                                                        }}
                                                    />
                                                    <HiOutlineBolt className="abm-app-icon-fallback" style={{ display: 'none' }} />
                                                </div>

                                                <div className="abm-app-info">
                                                    <div className="abm-app-name">{app.name}</div>
                                                    {app.description && (
                                                        <div className="abm-app-desc">{app.description}</div>
                                                    )}
                                                </div>

                                                <div className="abm-app-actions" onClick={(e) => e.stopPropagation()}>
                                                    {isComingSoon ? (
                                                        <span className="abm-app-coming-soon-badge">
                                                            Coming Soon
                                                        </span>
                                                    ) : isConnected ? (
                                                        <>
                                                            <span className="abm-app-connected-badge">
                                                                <HiCheck style={{ fontSize: '0.65rem' }} /> Connected
                                                            </span>
                                                            <button
                                                                type="button"
                                                                className="abm-action-btn-secondary"
                                                                title={`View ${app.name} description, operations, and connection options`}
                                                                aria-label={`Options for ${app.name}`}
                                                                onClick={(e) => handleOpenOptions(e, app, 'overview')}
                                                            >
                                                                Options
                                                            </button>
                                                        </>
                                                    ) : needsAuth && app.authType === 'OAUTH2' ? (
                                                        <>
                                                            <button
                                                                type="button"
                                                                className="abm-action-btn-primary"
                                                                title={`Connect ${app.name} account`}
                                                                aria-label={`Connect ${app.name}`}
                                                                onClick={(e) => handleDirectConnect(e, app)}
                                                            >
                                                                Connect
                                                            </button>
                                                            <button
                                                                type="button"
                                                                className="abm-action-btn-secondary"
                                                                title={`View ${app.name} description, operations, and connection options`}
                                                                aria-label={`Options for ${app.name}`}
                                                                onClick={(e) => handleOpenOptions(e, app, 'overview')}
                                                            >
                                                                Options
                                                            </button>
                                                        </>
                                                    ) : (
                                                        <button
                                                            type="button"
                                                            className="abm-action-btn-secondary"
                                                            title={`View ${app.name} description, operations, and connection options`}
                                                            aria-label={`Options for ${app.name}`}
                                                            onClick={(e) => handleOpenOptions(e, app, 'overview')}
                                                        >
                                                            Options
                                                        </button>
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </motion.div>
        </div>
    );
}
