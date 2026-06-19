import { useState, useMemo, useEffect, useRef } from 'react';
import { HiSearch, HiX, HiCheck, HiOutlineExternalLink, HiOutlineEye, HiOutlineEyeOff, HiOutlineShieldCheck, HiOutlineArrowLeft, HiOutlineLockClosed } from 'react-icons/hi';
import { HiOutlineBolt } from 'react-icons/hi2';
import { motion, AnimatePresence } from 'framer-motion';
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
    const [connectApp, setConnectApp] = useState(null); // App for inline credential form
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
        if (!guideApp && !connectApp) searchRef.current?.focus();
    }, [guideApp, connectApp]);

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
                else if (guideApp) setGuideApp(null);
                else onClose?.();
            }
        };
        document.addEventListener('keydown', handler);
        return () => document.removeEventListener('keydown', handler);
    }, [onClose, guideApp, connectApp]);

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
     * After guide continues — route based on auth type.
     */
    const handleGuideContinue = () => {
        const app = guideApp;
        setGuideApp(null);

        if (app.authType === 'OAUTH2') {
            startOAuth(app);
        } else if (app.authType === 'APIKEY') {
            showCredentialForm(app);
        } else if (app.altAuthType === 'APIKEY') {
            // Dual auth: default to OAuth, fallback to key
            startOAuth(app);
        }
    };

    /**
     * Start OAuth popup flow.
     */
    const startOAuth = async (app) => {
        try {
            const providerKey = resolveProviderKey(app.appKey);
            const { authorizationUrl } = await appCatalogApi.getOAuthUrl(providerKey);

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
                setConnectError(`OAuth not configured for ${app.name}. Contact admin to set up credentials.`);
            }
        }
    };

    /**
     * Show inline credential form for APIKEY apps.
     */
    const showCredentialForm = (app) => {
        setConnectApp(app);
        setName(`My ${app.name}`);
        setCredentials({});
        setConnectError(null);
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

    // If showing credential form, render inline
    if (connectApp) {
        const schema = getActiveSchema(connectApp);

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

                        {schema.length > 0 ? (
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
                            <button className="abm-btn-primary" onClick={handleCreateConnection} disabled={isSubmitting}>
                                {isSubmitting ? 'Creating...' : 'Create Connection'}
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
                                                    if (connectOnly) return;
                                                    // If app has platform key and user is not admin, allow direct selection
                                                    const hasPlatformKey = platformKeyApps.has(app.appKey);
                                                    if (needsAuth && !isConnected && !hasPlatformKey) {
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
                                                        className={app.logoUrl ? "app-logo-clearbit-dark" : "app-logo-white-svg"}
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
                                                    ) : platformKeyApps.has(app.appKey) && !isAdmin ? (
                                                        <span className="abm-app-connected-badge">
                                                            <HiOutlineLockClosed style={{ fontSize: '0.6rem' }} /> Platform key
                                                        </span>
                                                    ) : needsAuth ? (
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
