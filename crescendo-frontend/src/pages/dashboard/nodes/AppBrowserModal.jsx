import { useState, useMemo, useEffect, useRef } from 'react';
import { HiSearch, HiX, HiCheck, HiOutlineExternalLink, HiOutlineEye, HiOutlineEyeOff, HiOutlineShieldCheck, HiOutlineArrowLeft, HiOutlineLockClosed, HiOutlineKey } from 'react-icons/hi';
import { HiOutlineBolt } from 'react-icons/hi2';
import { motion, AnimatePresence } from 'framer-motion';
import ReactMarkdown from 'react-markdown';
import { appCatalogApi } from '../../../api/appCatalogApi';
import useConnectionStore from '../../../store/connectionStore';
import useAuthStore from '../../../store/authStore';
import api from '../../../api/axios';
import AppSetupGuide from '../../../components/AppSetupGuide';
import './AppBrowserModal.css';

/**
 * AppBrowserModal — universal app browser and connector.
 *
 * Used in BOTH the canvas (workflow node picker) AND the connections page.
 * Handles the full connect flow inline:
 *   1. Browse/search apps
 *   2. Show setup guide
 *   3. Credential entry (APIKEY) or OAuth popup (OAUTH2)
 *   4. Create connection
 *
 * Props:
 *   apps         — Array of catalog app objects
 *   connections  — Array of user's existing connections
 *   onSelect     — (app) => void — called when user picks an app for workflow
 *   onClose      — () => void
 *   title        — modal title
 *   connectOnly  — if true, only connect (don't call onSelect). Used on connections page.
 *   onConnected  — () => void — called after a connection is created
 */
export default function AppBrowserModal({
    apps = [],
    connections = [],
    onSelect,
    onClose,
    title = 'Choose an App',
    connectOnly = false,
    onConnected,
}) {
    const [search, setSearch] = useState('');
    const [activeTab, setActiveTab] = useState('all');
    const [guideApp, setGuideApp] = useState(null);
    const [detailApp, setDetailApp] = useState(null); // App detail panel (connections page)
    const [connectApp, setConnectApp] = useState(null); // App for inline credential form
    const [forceAuthMode, setForceAuthMode] = useState(null); // 'OAUTH2', 'CUSTOM_OAUTH2', or 'APIKEY'
    const [platformToggleApp, setPlatformToggleApp] = useState(null); // App for Platform vs Personal toggle
    const [connectError, setConnectError] = useState(null);
    const [name, setName] = useState('');
    const [credentials, setCredentials] = useState({});
    const [showPasswords, setShowPasswords] = useState({});
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [platformKeyApps, setPlatformKeyApps] = useState(new Set());
    const searchRef = useRef(null);
    const { createConnection } = useConnectionStore();
    const { user, isGuest } = useAuthStore();
    const isAdmin = !isGuest && (user?.role === 'ADMIN' || user?.limits?.tier === 'ADMIN');

    // Extract unique categories
    const categories = useMemo(() => {
        const cats = new Set();
        apps.forEach((a) => {
            if (a.category) cats.add(a.category.toLowerCase());
        });
        return ['all', ...Array.from(cats).sort()];
    }, [apps]);

    // Which apps have connections
    const connectedAppKeys = useMemo(() => {
        return new Set(connections.map((c) => c.appKey));
    }, [connections]);

    // Filter apps by search + category (exclude stripe)
    const filtered = useMemo(() => {
        let result = apps.filter(a => a.appKey !== 'stripe');

        if (activeTab !== 'all') {
            result = result.filter((a) => (a.category || '').toLowerCase() === activeTab);
        }

        if (search) {
            const q = search.toLowerCase();
            result = result.filter(
                (a) =>
                    a.name?.toLowerCase().includes(q) ||
                    a.description?.toLowerCase().includes(q) ||
                    a.appKey?.toLowerCase().includes(q)
            );
        }

        return result;
    }, [apps, search, activeTab]);

    // Group by category for display
    const grouped = useMemo(() => {
        if (activeTab !== 'all') return { [activeTab]: filtered };
        const groups = {};
        filtered.forEach((app) => {
            const cat = app.category || 'other';
            if (!groups[cat]) groups[cat] = [];
            groups[cat].push(app);
        });
        return groups;
    }, [filtered, activeTab]);

    // Focus search on mount, load platform keys
    useEffect(() => {
        if (!guideApp && !connectApp && !platformToggleApp && !detailApp) searchRef.current?.focus();
    }, [guideApp, connectApp, platformToggleApp, detailApp]);

    useEffect(() => {
        api.get('/admin/platform-keys/available')
            .then(res => {
                const keys = new Set(res.data.map(k => k.appKey));
                setPlatformKeyApps(keys);
            })
            .catch(() => {});
    }, []);

    // Close on Escape
    useEffect(() => {
        const handler = (e) => {
            if (e.key === 'Escape') {
                if (connectApp) setConnectApp(null);
                else if (platformToggleApp) setPlatformToggleApp(null);
                else if (guideApp) setGuideApp(null);
                else if (detailApp) setDetailApp(null);
                else onClose?.();
            }
        };
        document.addEventListener('keydown', handler);
        return () => document.removeEventListener('keydown', handler);
    }, [onClose, guideApp, connectApp, platformToggleApp, detailApp]);

    // Clear error after 5 seconds
    useEffect(() => {
        if (connectError) {
            const timer = setTimeout(() => setConnectError(null), 5000);
            return () => clearTimeout(timer);
        }
    }, [connectError]);

    // Resolve the right provider key for OAuth calls
    const resolveProviderKey = (appKey) => {
        const map = {
            'gmail': 'gmail',
            'google-sheets': 'google-sheets',
            'google-drive': 'google-drive',
            'google-calendar': 'google-calendar',
            'google-forms': 'google-forms',
            'google-tasks': 'google-tasks',
            'google-slides': 'google-slides',
            'google-docs': 'google-docs',
            'microsoft-excel': 'microsoft-excel',
            'microsoft-outlook': 'microsoft-outlook',
            'microsoft-teams': 'microsoft-teams',
        };
        return map[appKey] || appKey;
    };

    // Get the credential schema for a given app
    const getActiveSchema = (app) => {
        if (!app?.credentialSchema?.length) return [];
        return app.credentialSchema;
    };

    /**
     * Handle the Connect button click — show guide first, then route.
     */
    const handleConnectClick = (e, app) => {
        e.stopPropagation();
        setConnectError(null);

        if (app.authType === 'NONE') {
            if (!connectOnly) onSelect?.(app);
            return;
        }

        // Show setup guide first
        setGuideApp(app);
    };

    /**
     * After guide continues — route based on platform key or auth type.
     */
    const handleGuideContinue = () => {
        const app = guideApp;
        setGuideApp(null);

        if (app.hasPlatformKey) {
            setPlatformToggleApp(app);
        } else {
            showCredentialForm(app);
        }
    };

    /**
     * Start OAuth popup flow.
     */
    const startOAuth = async (app, opts = {}) => {
        try {
            const providerKey = resolveProviderKey(app.appKey);
            const { authorizationUrl } = await appCatalogApi.getOAuthUrl(providerKey, opts);

            const popup = window.open(authorizationUrl, 'oauth_popup', 'width=600,height=700,scrollbars=yes');
            if (popup) {
                // Listen for postMessage from the popup instead of polling
                const messageHandler = (event) => {
                    if (event.data?.type === 'oauth-connected') {
                        window.removeEventListener('message', messageHandler);
                        onConnected?.();
                        if (!connectOnly) onClose?.();
                    }
                };
                window.addEventListener('message', messageHandler);

                // Fallback: also poll in case postMessage fails
                const pollTimer = setInterval(() => {
                    if (popup.closed) {
                        clearInterval(pollTimer);
                        window.removeEventListener('message', messageHandler);
                        onConnected?.();
                        if (!connectOnly) onClose?.();
                    }
                }, 1000);
            }
        } catch (err) {
            // If OAuth fails and app has altAuthType=APIKEY, show credential form
            if (app.altAuthType === 'APIKEY') {
                showCredentialForm(app);
            } else {
                setConnectError(`OAuth not configured for ${app.name}. Contact admin to set up credentials or use your own custom OAuth app.`);
            }
        }
    };

    /**
     * Show inline credential form for APIKEY apps.
     */
    const showCredentialForm = (app, mode = null) => {
        setConnectApp(app);
        setForceAuthMode(mode);
        setName(`My ${app.name}`);
        setCredentials({});
        setUseCustomOAuth(mode === 'CUSTOM_OAUTH2'); // Used for OAuth choices
        setConnectError(null);
    };

    const [useCustomOAuth, setUseCustomOAuth] = useState(false);

    /**
     * Start OAuth from the form with custom credentials if provided
     */
    const handleOAuthConnectFromForm = () => {
        setConnectError(null);
        if (!name.trim()) { setConnectError('Connection name is required'); return; }

        let opts = { connectionId: undefined };

        if (useCustomOAuth) {
            if (!credentials.clientId?.trim()) { setConnectError('Client ID is required'); return; }
            if (!credentials.clientSecret?.trim()) { setConnectError('Client Secret is required'); return; }
            
            opts.customClientId = credentials.clientId.trim();
            opts.customClientSecret = credentials.clientSecret.trim();
            if (credentials.scopes?.trim()) {
                opts.customScopes = credentials.scopes.trim();
            }
        }
        
        setIsSubmitting(true);
        startOAuth(connectApp, opts).finally(() => setIsSubmitting(false));
    };

    /**
     * Submit the credential form — create connection.
     */
    const handleCreateConnection = async () => {
        setConnectError(null);
        if (!name.trim()) { setConnectError('Connection name is required'); return; }

        const schema = getActiveSchema(connectApp);
        // If there's no schema, require at least apiKey
        if (schema.length === 0 && !credentials.apiKey?.trim()) {
            setConnectError('API Key is required');
            return;
        }

        // Validate required fields
        for (const field of schema) {
            if (field.required && (!credentials[field.key] || !credentials[field.key].toString().trim())) {
                setConnectError(`${field.label} is required`);
                return;
            }
        }

        setIsSubmitting(true);
        try {
            await createConnection({ appKey: connectApp.appKey, name: name.trim(), credentials });
            setConnectApp(null);
            setConnectError(null);
            onConnected?.();
            // Don't close modal — user might want to connect another app
        } catch (e) {
            setConnectError(e.response?.data?.message || 'Failed to create connection');
        } finally {
            setIsSubmitting(false);
        }
    };

    // If showing guide, render it
    if (guideApp) {
        return (
            <AppSetupGuide
                app={guideApp}
                onContinue={handleGuideContinue}
                onClose={() => setGuideApp(null)}
            />
        );
    }

    // If showing app detail panel (connectOnly mode: card click)
    if (detailApp) {
        const isConnected = connectedAppKeys.has(detailApp.appKey);
        const hasApiKey = detailApp.credentialSchema?.length > 0 || detailApp.authType === 'APIKEY';
        const hasOAuth = detailApp.authType === 'OAUTH2';
        const hasAltApiKey = detailApp.altAuthType === 'APIKEY';
        const needsAuth = detailApp.authType && detailApp.authType !== 'NONE';

        const startConnect = (e) => {
            setDetailApp(null);
            handleConnectClick(e || { stopPropagation: () => {} }, detailApp);
        };

        return (
            <div className="abm-overlay" onClick={() => setDetailApp(null)}>
                <motion.div
                    className="abm-modal"
                    initial={{ opacity: 0, scale: 0.95, y: 12 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    transition={{ duration: 0.2 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="abm-header">
                        <div className="abm-header-top">
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <button className="abm-close" onClick={() => setDetailApp(null)}>
                                    <HiOutlineArrowLeft />
                                </button>
                                <span className="abm-title">{detailApp.name}</span>
                            </div>
                            <button className="abm-close" onClick={onClose}><HiX /></button>
                        </div>
                    </div>

                    <div className="abm-body" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                        {/* App header */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                            <div className="abm-app-icon" style={{ width: '56px', height: '56px', flexShrink: 0 }}>
                                <img
                                    src={detailApp.logoUrl || `/icons/${detailApp.appKey}.svg`}
                                    alt={detailApp.name}
                                    className="app-logo-img"
                                    style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                                    onError={(e) => { e.target.style.display = 'none'; }}
                                />
                            </div>
                            <div>
                                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-accent)' }}>{detailApp.name}</div>
                                {detailApp.category && (
                                    <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '1px', marginTop: '2px' }}>
                                        {detailApp.category}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Description */}
                        {detailApp.description && (
                            <div style={{
                                fontSize: '0.83rem', color: 'var(--text-secondary)', lineHeight: 1.7,
                                background: 'var(--bg-elevated)', borderRadius: 'var(--radius-md)',
                                padding: '14px 16px', border: '1px solid var(--border-primary)',
                                maxHeight: '200px', overflowY: 'auto'
                            }}>
                                <ReactMarkdown
                                    components={{
                                        p: ({node, ...props}) => <p style={{margin: '0 0 10px 0'}} {...props}/>,
                                        a: ({node, ...props}) => <a style={{color: 'var(--brand-primary)', textDecoration: 'none'}} {...props}/>,
                                        ul: ({node, ...props}) => <ul style={{margin: '0 0 10px 0', paddingLeft: '20px'}} {...props}/>,
                                        li: ({node, ...props}) => <li style={{marginBottom: '4px'}} {...props}/>,
                                        strong: ({node, ...props}) => <strong style={{color: 'var(--text-primary)', fontWeight: 600}} {...props}/>,
                                    }}
                                >
                                    {detailApp.description}
                                </ReactMarkdown>
                            </div>
                        )}

                        {/* Auth type badge */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
                            {hasOAuth && <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><HiOutlineLockClosed /> OAuth 2.0</span>}
                            {hasApiKey && !hasOAuth && <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><HiOutlineKey /> API Key</span>}
                            {hasAltApiKey && <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>· <HiOutlineKey /> or own API key</span>}
                            {!needsAuth && <span>No authentication required</span>}
                        </div>

                        {/* Connect actions */}
                        {needsAuth ? (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                {isConnected && (
                                    <div style={{
                                        display: 'flex', alignItems: 'center', gap: '8px',
                                        padding: '12px 16px', borderRadius: 'var(--radius-md)',
                                        background: 'rgba(34,197,94,0.08)', border: '1px solid rgba(34,197,94,0.2)',
                                        color: '#22c55e', fontSize: '0.83rem', fontWeight: 500
                                    }}>
                                        <HiCheck /> You already have an active connection to this app.
                                    </div>
                                )}
                                {/* OAuth Option */}
                                {hasOAuth && (
                                    <button
                                        className="abm-app-card"
                                        style={{ padding: '14px 16px', justifyContent: 'flex-start', gap: '12px', border: '1px solid var(--border-secondary)' }}
                                        onClick={(e) => {
                                            setDetailApp(null);
                                            handleConnectClick(e || { stopPropagation: () => {} }, detailApp);
                                        }}
                                    >
                                        <HiOutlineLockClosed size={18} />
                                        <div style={{ textAlign: 'left' }}>
                                            <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>
                                                Connect with OAuth
                                            </div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: '2px' }}>
                                                Authorize via your account
                                            </div>
                                        </div>
                                    </button>
                                )}

                                {/* Custom OAuth Option */}
                                {hasOAuth && (
                                    <button
                                        className="abm-app-card"
                                        style={{ padding: '14px 16px', justifyContent: 'flex-start', gap: '12px', border: '1px solid var(--border-secondary)' }}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setDetailApp(null);
                                            showCredentialForm(detailApp, 'CUSTOM_OAUTH2');
                                        }}
                                    >
                                        <HiOutlineLockClosed size={18} />
                                        <div style={{ textAlign: 'left' }}>
                                            <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>
                                                Bring Your Own OAuth App
                                            </div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: '2px' }}>
                                                Provide your own Client ID and Secret
                                            </div>
                                        </div>
                                    </button>
                                )}

                                {/* API Key Option */}
                                {(hasApiKey || hasAltApiKey) && (
                                    <button
                                        className="abm-app-card"
                                        style={{ padding: '14px 16px', justifyContent: 'flex-start', gap: '12px', border: '1px solid var(--border-secondary)' }}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setDetailApp(null);
                                            showCredentialForm(detailApp, 'APIKEY');
                                        }}
                                    >
                                        <HiOutlineKey size={18} />
                                        <div style={{ textAlign: 'left' }}>
                                            <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>
                                                Add API Key
                                            </div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: '2px' }}>
                                                Provide your own API credentials
                                            </div>
                                        </div>
                                    </button>
                                )}
                            </div>
                        ) : (
                            <div style={{ color: 'var(--text-tertiary)', fontSize: '0.83rem' }}>This app does not require authentication.</div>
                        )}
                    </div>
                </motion.div>
            </div>
        );
    }

    // If showing platform vs personal toggle
    if (platformToggleApp) {
        return (
            <div className="abm-overlay" onClick={() => setPlatformToggleApp(null)}>
                <motion.div
                    className="abm-modal"
                    initial={{ opacity: 0, scale: 0.95, y: 12 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    transition={{ duration: 0.2 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="abm-header">
                        <div className="abm-header-top">
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <button className="abm-close" onClick={() => setPlatformToggleApp(null)}>
                                    <HiOutlineArrowLeft />
                                </button>
                                <span className="abm-title">How do you want to connect {platformToggleApp.name}?</span>
                            </div>
                            <button className="abm-close" onClick={onClose}>
                                <HiX />
                            </button>
                        </div>
                    </div>
                    <div className="abm-body">
                        <p style={{ marginBottom: '1.5rem', color: 'var(--text-secondary)' }}>
                            <strong>{platformToggleApp.name}</strong> supports using Crescendo's platform credentials or connecting your own personal account.
                        </p>
                        
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            <button 
                                className="abm-app-card" 
                                style={{ textAlign: 'left', display: 'flex', alignItems: 'center', position: 'relative', border: '1px solid var(--border-secondary)', padding: '16px' }}
                                onClick={() => {
                                    if (connectOnly) {
                                        setPlatformToggleApp(null);
                                        onClose?.();
                                    } else {
                                        onSelect?.(platformToggleApp, 'ADMIN_KEY');
                                    }
                                }}
                            >
                                <div className="abm-app-icon" style={{ background: 'var(--bg-elevated)', border: 'none' }}>
                                    <HiOutlineBolt size={20} />
                                </div>
                                <div className="abm-app-info">
                                    <div className="abm-app-name">Use Crescendo's key</div>
                                    <div className="abm-app-desc" style={{ whiteSpace: 'normal', overflow: 'visible', textOverflow: 'clip' }}>Ready to use immediately. No setup required.</div>
                                </div>
                                <span className="abm-app-connected-badge" style={{ background: 'var(--bg-elevated)', color: 'var(--text-accent)' }}>Recommended</span>
                            </button>

                            <button 
                                className="abm-app-card" 
                                style={{ textAlign: 'left', display: 'flex', alignItems: 'center', padding: '16px' }}
                                onClick={() => {
                                    const app = platformToggleApp;
                                    setPlatformToggleApp(null);
                                    showCredentialForm(app);
                                }}
                            >
                                <div className="abm-app-icon" style={{ background: 'var(--bg-elevated)', border: 'none' }}>
                                    {platformToggleApp.authType === 'OAUTH2' ? <HiOutlineLockClosed size={20} /> : <HiOutlineKey size={20} />}
                                </div>
                                <div className="abm-app-info">
                                    <div className="abm-app-name">Connect your own</div>
                                    <div className="abm-app-desc" style={{ whiteSpace: 'normal', overflow: 'visible', textOverflow: 'clip' }}>
                                        {platformToggleApp.authType === 'OAUTH2'
                                            ? 'Authorize via the provider with your personal account'
                                            : 'Provide your own API key for dedicated usage limits'}
                                    </div>
                                </div>
                            </button>
                        </div>
                    </div>
                </motion.div>
            </div>
        );
    }

    // If showing credential form, render inline
    if (connectApp) {
        const schema = getActiveSchema(connectApp);
        const isEffectiveOAuth = (connectApp.authType === 'OAUTH2' && forceAuthMode !== 'APIKEY');

        return (
            <div className="abm-overlay" onClick={() => setConnectApp(null)}>
                <motion.div
                    className="abm-modal"
                    initial={{ opacity: 0, scale: 0.95, y: 12 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    transition={{ duration: 0.2 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="abm-header">
                        <div className="abm-header-top">
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <button className="abm-close" onClick={() => setConnectApp(null)}>
                                    <HiOutlineArrowLeft />
                                </button>
                                <span className="abm-title">Connect {connectApp.name}</span>
                            </div>
                            <button className="abm-close" onClick={onClose}>
                                <HiX />
                            </button>
                        </div>
                    </div>

                    <div className="abm-body abm-credential-form">
                        {connectError && (
                            <div className="abm-error-toast">{connectError}</div>
                        )}

                        <label className="abm-form-label">
                            Connection Name
                            <input
                                type="text"
                                className="abm-form-input"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Enter a friendly name"
                            />
                        </label>

                        {isEffectiveOAuth ? (
                            <>
                                <div className="abm-oauth-choice">
                                    <label className="abm-radio-label">
                                        <input 
                                            type="radio" 
                                            name="oauthSource" 
                                            checked={!useCustomOAuth} 
                                            onChange={() => setUseCustomOAuth(false)} 
                                        />
                                        Standard Connection (Use Crescendo Managed App)
                                    </label>
                                    <label className="abm-radio-label">
                                        <input 
                                            type="radio" 
                                            name="oauthSource" 
                                            checked={useCustomOAuth} 
                                            onChange={() => setUseCustomOAuth(true)} 
                                        />
                                        Bring Your Own OAuth App (Advanced)
                                    </label>
                                </div>

                                {useCustomOAuth && (
                                    <div className="abm-custom-oauth-fields">
                                        <div className="abm-info-box">
                                            Register an OAuth app in the {connectApp.name} developer portal with redirect URI:<br/>
                                            <code style={{userSelect: 'all', marginTop: '4px', display: 'block'}}>{window.location.origin}/api/connections/oauth/{resolveProviderKey(connectApp.appKey)}/callback</code>
                                        </div>
                                        <label className="abm-form-label">
                                            Client ID <span className="abm-required">*</span>
                                            <input
                                                type="text"
                                                className="abm-form-input"
                                                value={credentials.clientId || ''}
                                                onChange={(e) => setCredentials(prev => ({ ...prev, clientId: e.target.value }))}
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
                                                    onChange={(e) => setCredentials(prev => ({ ...prev, clientSecret: e.target.value }))}
                                                    placeholder="Enter Client Secret"
                                                />
                                                <button
                                                    type="button"
                                                    className="abm-eye-toggle"
                                                    onClick={() => setShowPasswords(prev => ({ ...prev, clientSecret: !prev.clientSecret }))}
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
                                                onChange={(e) => setCredentials(prev => ({ ...prev, scopes: e.target.value }))}
                                                placeholder="e.g. read write (space-separated)"
                                            />
                                            <span className="abm-field-help">Leave blank to use default scopes</span>
                                        </label>
                                    </div>
                                )}
                            </>
                        ) : schema.length > 0 ? (
                            schema.map(field => (
                                <label key={field.key} className="abm-form-label">
                                    {field.label} {field.required && <span className="abm-required">*</span>}
                                    <div className="abm-password-wrap">
                                        <input
                                            type={field.type === 'password' && !showPasswords[field.key] ? 'password' : 'text'}
                                            className="abm-form-input"
                                            value={credentials[field.key] || ''}
                                            onChange={(e) => setCredentials(prev => ({ ...prev, [field.key]: e.target.value }))}
                                            placeholder={field.placeholder || ''}
                                        />
                                        {field.type === 'password' && (
                                            <button
                                                type="button"
                                                className="abm-eye-toggle"
                                                onClick={() => setShowPasswords(prev => ({ ...prev, [field.key]: !prev[field.key] }))}
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
                                API Key / Token <span className="abm-required">*</span>
                                <div className="abm-password-wrap">
                                    <input
                                        type="password"
                                        className="abm-form-input"
                                        value={credentials.apiKey || ''}
                                        onChange={(e) => setCredentials({ apiKey: e.target.value })}
                                        placeholder="Paste your API key or token"
                                    />
                                </div>
                            </label>
                        )}

                        {connectApp.helpUrl && (
                            <a href={connectApp.helpUrl} target="_blank" rel="noopener noreferrer" className="abm-help-link">
                                <HiOutlineExternalLink /> Where do I find my credentials?
                            </a>
                        )}

                        <div className="abm-security-note">
                            <HiOutlineShieldCheck />
                            <span>Credentials are encrypted with AES-256-GCM before storage.</span>
                        </div>

                        <div className="abm-form-footer">
                            <button className="abm-btn-secondary" onClick={() => setConnectApp(null)}>Back</button>
                            <button 
                                className="abm-btn-primary" 
                                onClick={isEffectiveOAuth ? handleOAuthConnectFromForm : handleCreateConnection} 
                                disabled={isSubmitting}
                            >
                                {isSubmitting ? 'Connecting...' : (isEffectiveOAuth ? 'Connect via OAuth' : 'Create Connection')}
                            </button>
                        </div>
                    </div>
                </motion.div>
            </div>
        );
    }

    return (
        <div className="abm-overlay" onClick={onClose}>
            <motion.div
                className="abm-modal"
                initial={{ opacity: 0, scale: 0.95, y: 12 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 12 }}
                transition={{ duration: 0.2, ease: [0.4, 0, 0.2, 1] }}
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="abm-header">
                    <div className="abm-header-top">
                        <span className="abm-title">{title}</span>
                        <button className="abm-close" onClick={onClose}>
                            <HiX />
                        </button>
                    </div>

                    {/* Search */}
                    <div className="abm-search">
                        <HiSearch className="abm-search-icon" />
                        <input
                            ref={searchRef}
                            className="abm-search-input"
                            type="text"
                            placeholder="Search apps…"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                        {search && (
                            <span className="abm-search-count">
                                {filtered.length} result{filtered.length !== 1 ? 's' : ''}
                            </span>
                        )}
                    </div>

                    {/* Error toast */}
                    {connectError && (
                        <div className="abm-error-toast">{connectError}</div>
                    )}

                    {/* Category tabs */}
                    <div className="abm-tabs">
                        {categories.map((cat) => (
                            <button
                                key={cat}
                                className={`abm-tab ${activeTab === cat ? 'active' : ''}`}
                                onClick={() => setActiveTab(cat)}
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
                            <div className="abm-empty-hint">Try a different search term</div>
                        </div>
                    ) : (
                        Object.entries(grouped).map(([category, catApps]) => (
                            <div key={category}>
                                {activeTab === 'all' && (
                                    <div className="abm-category-label">{category}</div>
                                )}
                                <div className="abm-grid">
                                    {catApps.map((app) => {
                                        const isConnected = connectedAppKeys.has(app.appKey);
                                        const needsAuth = app.authType && app.authType !== 'NONE';

                                        return (
                                            <button
                                                key={app.appKey}
                                                className={`abm-app-card ${isConnected ? 'connected' : ''}`}
                                                onClick={(e) => {
                                                    if (connectOnly) {
                                                        // Card click in connectOnly mode → show detail panel
                                                        setDetailApp(app);
                                                        return;
                                                    }
                                                    if (needsAuth) {
                                                        handleConnectClick(e, app);
                                                        return;
                                                    }
                                                    onSelect?.(app);
                                                }}
                                            >
                                                <div className="abm-app-icon">
                                                    <img 
                                                        src={app.logoUrl || `/icons/${app.appKey}.svg`}
                                                        alt={app.name}
                                                        className="app-logo-img"
                                                        onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }}
                                                    />
                                                    <HiOutlineBolt className="abm-app-icon-fallback" style={{ display: 'none' }} />
                                                </div>
                                                <div className="abm-app-info">
                                                    <div className="abm-app-name">{app.name}</div>
                                                    {app.description && (
                                                        <div className="abm-app-desc">{app.description}</div>
                                                    )}
                                                </div>
                                                <div className="abm-app-actions">
                                                    {isConnected ? (
                                                        <span className="abm-app-connected-badge">
                                                            <HiCheck style={{ fontSize: '0.6rem' }} /> Connected
                                                        </span>
                                                    ) : null}
                                                    {needsAuth ? (
                                                        <span
                                                            className="abm-app-connect-btn"
                                                            role="button"
                                                            tabIndex={0}
                                                            onClick={(e) => handleConnectClick(e, app)}
                                                            onKeyDown={(e) => e.key === 'Enter' && handleConnectClick(e, app)}
                                                        >
                                                            Connect
                                                        </span>
                                                    ) : (
                                                        <span className="abm-app-badge">No auth</span>
                                                    )}
                                                </div>
                                            </button>
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
