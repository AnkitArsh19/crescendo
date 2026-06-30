import { useState, useMemo, useEffect, useRef } from 'react';
import { HiSearch, HiX, HiCheck, HiOutlineExternalLink, HiOutlineEye, HiOutlineEyeOff, HiOutlineShieldCheck, HiOutlineArrowLeft, HiOutlineLockClosed, HiOutlineKey } from 'react-icons/hi';
import { HiOutlineBolt } from 'react-icons/hi2';
import { motion } from 'framer-motion';
import ReactMarkdown from 'react-markdown';
import { appCatalogApi } from '../../../api/appCatalogApi';
import useConnectionStore from '../../../store/connectionStore';
import api from '../../../api/axios';
import './AppBrowserModal.css';

const markdownComponents = {
    p: ({...props}) => <p style={{margin: '0 0 10px 0'}} {...props}/>,
    a: ({...props}) => <a style={{color: 'var(--brand-primary)', textDecoration: 'none'}} {...props}/>,
    ul: ({...props}) => <ul style={{margin: '0 0 10px 0', paddingLeft: '20px'}} {...props}/>,
    li: ({...props}) => <li style={{marginBottom: '4px'}} {...props}/>,
    strong: ({...props}) => <strong style={{color: 'var(--text-primary)', fontWeight: 600}} {...props}/>,
};

/**
 * AppBrowserModal — universal app browser and connector.
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
    const [detailApp, setDetailApp] = useState(null); 
    const [activeConnectMode, setActiveConnectMode] = useState(null); // 'OAUTH2', 'CUSTOM_OAUTH2', 'APIKEY'
    const [connectError, setConnectError] = useState(null);
    const [name, setName] = useState('');
    const [credentials, setCredentials] = useState({});
    const [showPasswords, setShowPasswords] = useState({});
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [platformKeyApps, setPlatformKeyApps] = useState(new Set());
    const searchRef = useRef(null);
    const { createConnection } = useConnectionStore();
    // Admin check removed as it is unused, the backend API enforces access anyway

    const categories = useMemo(() => {
        const cats = new Set();
        apps.forEach((a) => {
            if (a.category) cats.add(a.category.toLowerCase());
        });
        return ['all', ...Array.from(cats).sort()];
    }, [apps]);

    const connectedAppKeys = useMemo(() => {
        return new Set(connections.map((c) => c.appKey));
    }, [connections]);

    const filtered = useMemo(() => {
        let result = apps;

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

    useEffect(() => {
        if (!detailApp) searchRef.current?.focus();
    }, [detailApp]);

    useEffect(() => {
        api.get('/admin/platform-keys/available')
            .then(res => {
                const keys = new Set(res.data.map(k => k.appKey));
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

    const handleConnectClick = (e, app) => {
        e.stopPropagation();
        setConnectError(null);

        if (app.authType === 'NONE') {
            if (!connectOnly) onSelect?.(app);
            return;
        }

        setDetailApp(app);
        setActiveConnectMode(null);
        setName(`My ${app.name}`);
        setCredentials({});
        setConnectError(null);
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
                        onConnected?.();
                        if (!connectOnly) onClose?.();
                    }
                };
                window.addEventListener('message', messageHandler);

                const pollTimer = setInterval(() => {
                    if (popup.closed) {
                        clearInterval(pollTimer);
                        window.removeEventListener('message', messageHandler);
                        onConnected?.();
                        if (!connectOnly) onClose?.();
                    }
                }, 1000);
            }
        } catch {
            if (app.altAuthType === 'APIKEY') {
                setActiveConnectMode('APIKEY');
            } else {
                setConnectError(`OAuth not configured for ${app.name}. Contact admin to set up credentials or use your own custom OAuth app.`);
            }
        }
    };

    const handleOAuthConnectFromForm = () => {
        setConnectError(null);
        if (!name.trim()) { setConnectError('Connection name is required'); return; }

        let opts = { connectionId: undefined };

        if (activeConnectMode === 'CUSTOM_OAUTH2') {
            if (!credentials.clientId?.trim()) { setConnectError('Client ID is required'); return; }
            if (!credentials.clientSecret?.trim()) { setConnectError('Client Secret is required'); return; }
            
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
        if (!name.trim()) { setConnectError('Connection name is required'); return; }

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
            await createConnection({ appKey: detailApp.appKey, name: name.trim(), credentials });
            setDetailApp(null);
            setActiveConnectMode(null);
            setConnectError(null);
            onConnected?.();
        } catch (e) {
            setConnectError(e.response?.data?.message || 'Failed to create connection');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (detailApp) {
        const isConnected = connectedAppKeys.has(detailApp.appKey);
        const hasApiKey = detailApp.credentialSchema?.length > 0 || detailApp.authType === 'APIKEY';
        const hasOAuth = detailApp.authType === 'OAUTH2';
        const hasAltApiKey = detailApp.altAuthType === 'APIKEY';
        const needsAuth = detailApp.authType && detailApp.authType !== 'NONE';
        const hasPlatformKey = (detailApp.hasPlatformKey || platformKeyApps.has(detailApp.appKey)) && !connectOnly;

        const schema = getActiveSchema(detailApp);
        const isEffectiveOAuth = (detailApp.authType === 'OAUTH2' && activeConnectMode !== 'APIKEY');
        const handleActionSubmit = isEffectiveOAuth ? handleOAuthConnectFromForm : handleCreateConnection;

        return (
            <div className="abm-overlay" onClick={() => { if (!activeConnectMode) setDetailApp(null); }}>
                <motion.div
                    className="abm-modal detail-mode"
                    initial={{ opacity: 0, scale: 0.95, y: 12 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    transition={{ duration: 0.2 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="abm-header">
                        <div className="abm-header-top">
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <button className="abm-close" onClick={() => {
                                    if (activeConnectMode) setActiveConnectMode(null);
                                    else setDetailApp(null);
                                }}>
                                    <HiOutlineArrowLeft />
                                </button>
                                <span className="abm-title">
                                    {activeConnectMode ? `Connect ${detailApp.name}` : detailApp.name}
                                </span>
                            </div>
                            <button className="abm-close" onClick={onClose}><HiX /></button>
                        </div>
                    </div>

                    <div className="abm-body abm-body-detail">
                        {!activeConnectMode && (
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
                                    {detailApp.category && (
                                        <div className="abm-detail-category-badge">{detailApp.category}</div>
                                    )}
                                </div>
                            </div>
                        )}

                        {!activeConnectMode && detailApp.description && (
                            <div className="abm-detail-description">
                                <ReactMarkdown components={markdownComponents}>
                                    {detailApp.description}
                                </ReactMarkdown>
                            </div>
                        )}

                        {needsAuth ? (
                            <div className="abm-auth-section">
                                {!activeConnectMode ? (
                                    <>
                                        {isConnected && (
                                            <div className="abm-connected-block">
                                                <div className="abm-connected-banner">
                                                    <HiCheck size={18} />
                                                    <span>You already have an active connection to this app.</span>
                                                </div>
                                                {!connectOnly && (
                                                    <button 
                                                        className="abm-btn-primary abm-btn-select-existing"
                                                        onClick={() => onSelect?.(detailApp)}
                                                    >
                                                        Select Existing Connection
                                                    </button>
                                                )}
                                            </div>
                                        )}

                                        <div className="abm-connect-dropdown-wrap" style={{ marginTop: '16px' }}>
                                            {isConnected && <div className="abm-options-divider"><span>Or connect a different account</span></div>}
                                            
                                            <label className="abm-form-label">
                                                Select Connection Method
                                                <select 
                                                    className="abm-form-input"
                                                    value=""
                                                    onChange={(e) => {
                                                        const val = e.target.value;
                                                        if (val === 'ADMIN_KEY') {
                                                            onSelect?.(detailApp, 'ADMIN_KEY');
                                                        } else if (val) {
                                                            setActiveConnectMode(val);
                                                        }
                                                    }}
                                                >
                                                    <option value="" disabled>Choose a connection method...</option>
                                                    {hasPlatformKey && <option value="ADMIN_KEY">Use Crescendo's Key (Recommended)</option>}
                                                    {hasOAuth && <option value="OAUTH2">Connect with OAuth</option>}
                                                    {hasOAuth && <option value="CUSTOM_OAUTH2">Bring Your Own OAuth App</option>}
                                                    {(hasApiKey || hasAltApiKey) && <option value="APIKEY">Add API Key</option>}
                                                </select>
                                            </label>
                                        </div>
                                    </>
                                ) : (
                                    <div className="abm-credential-form">
                                        {connectError && <div className="abm-error-toast">{connectError}</div>}

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

                                        {activeConnectMode === 'CUSTOM_OAUTH2' && (
                                            <div className="abm-custom-oauth-fields">
                                                <div className="abm-info-box">
                                                    Register an OAuth app in the developer portal with redirect URI:<br/>
                                                    <code style={{userSelect: 'all', marginTop: '4px', display: 'block'}}>{window.location.origin}/api/connections/oauth/{detailApp.appKey}/callback</code>
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

                                        {activeConnectMode === 'APIKEY' && (
                                            schema.length > 0 ? (
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
                                            )
                                        )}

                                        {detailApp.helpUrl && (
                                            <a href={detailApp.helpUrl} target="_blank" rel="noopener noreferrer" className="abm-help-link">
                                                <HiOutlineExternalLink /> Read Setup Guide
                                            </a>
                                        )}

                                        <div className="abm-security-note">
                                            <HiOutlineShieldCheck />
                                            <span>Credentials are encrypted with AES-256-GCM before storage.</span>
                                        </div>

                                        <div className="abm-form-footer">
                                            <button className="abm-btn-secondary" onClick={() => setActiveConnectMode(null)}>Back</button>
                                            <button 
                                                className="abm-btn-primary" 
                                                onClick={handleActionSubmit} 
                                                disabled={isSubmitting}
                                            >
                                                {isSubmitting ? 'Connecting...' : (isEffectiveOAuth ? 'Connect via OAuth' : 'Create Connection')}
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="abm-no-auth-state">
                                <p style={{ marginBottom: 16 }}>This app does not require authentication.</p>
                                {!connectOnly && (
                                    <button className="abm-btn-primary" onClick={() => onSelect?.(detailApp)}>
                                        Use App
                                    </button>
                                )}
                            </div>
                        )}
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
                            <div>No apps match "{search}"</div>
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
                                                    if (needsAuth && !isConnected && !connectOnly) {
                                                        handleConnectClick(e, app);
                                                        return;
                                                    }
                                                    if (connectOnly) {
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
                                                    <span
                                                        className="abm-app-connect-btn"
                                                        role="button"
                                                        tabIndex={0}
                                                        onClick={(e) => handleConnectClick(e, app)}
                                                        onKeyDown={(e) => e.key === 'Enter' && handleConnectClick(e, app)}
                                                    >
                                                        {(!needsAuth || isConnected) ? 'Options' : 'Connect'}
                                                    </span>
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
