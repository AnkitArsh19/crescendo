import { useEffect, useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlinePlus, HiOutlineTrash, HiOutlinePencil, HiOutlineLink,
  HiOutlineX, HiOutlineRefresh, HiOutlineSearch,
  HiOutlineShieldCheck, HiOutlineExternalLink, HiOutlineKey,
  HiOutlineLockClosed, HiOutlineArrowLeft, HiOutlineEye, HiOutlineEyeOff,
} from 'react-icons/hi';
import useConnectionStore from '../../store/connectionStore';
import { appCatalogApi } from '../../api/appCatalogApi';
import AppBrowserModal from './nodes/AppBrowserModal';
import './Connections.css';

// ─── Constants ──────────────────────────────────────────────────────────────────

const STATUS_META = {
  ACTIVE:  { label: 'Active',  className: 'status-active' },
  ERROR:   { label: 'Error',   className: 'status-error' },
  REAUTH:  { label: 'Re-auth', className: 'status-reauth' },
};

const CATEGORY_LABELS = {
  communication: 'Communication',
  productivity: 'Productivity',
  developer: 'Developer Tools',
  ai: 'AI & Machine Learning',
  payments: 'Payments & Commerce',
  fun: 'Fun & Lifestyle',
};

const CATEGORY_ORDER = ['communication', 'productivity', 'developer', 'ai', 'payments', 'fun'];

// ─── Main Component ─────────────────────────────────────────────────────────────

export default function Connections() {
  const { connections, isLoading, error, fetchConnections, createConnection, deleteConnection } = useConnectionStore();
  const [apps, setApps] = useState([]);
  const [showAddModal, setShowAddModal] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [search, setSearch] = useState('');
  const [preselectedAppKey, setPreselectedAppKey] = useState(null);

  useEffect(() => {
    fetchConnections();
    appCatalogApi.list()
      .then(setApps)
      .catch(() => setApps([]));
  }, [fetchConnections]);

  // Check for OAuth callback success
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('connected')) {
      fetchConnections(); // refresh after OAuth callback
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [fetchConnections]);

  // Handle ?connect=<appKey> from canvas redirect — auto-open add modal
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const connectKey = params.get('connect');
    if (connectKey && apps.length > 0) {
      setPreselectedAppKey(connectKey);
      setShowAddModal(true);
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [apps]);

  const filtered = connections.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase()) ||
    c.appKey.toLowerCase().includes(search.toLowerCase())
  );

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try { await deleteConnection(deleteTarget); } catch { /* store handles */ }
    setDeleteTarget(null);
  };

  return (
    <div className="connections-page">
      {/* Header */}
      <div className="connections-header">
        <div>
          <h1 className="connections-title">Connections</h1>
          <p className="connections-subtitle">
            Securely connect your apps — credentials are encrypted at rest with AES-256
          </p>
        </div>
        <button className="conn-btn-primary" onClick={() => setShowAddModal(true)}>
          <HiOutlinePlus /> Add Connection
        </button>
      </div>

      {/* Search */}
      {connections.length > 0 && (
        <div className="connections-search-bar">
          <HiOutlineSearch className="conn-search-icon" />
          <input
            type="text"
            placeholder="Search connections..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="conn-search-input"
          />
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="conn-error-banner">
          {error}
          <button onClick={fetchConnections} className="conn-retry-btn"><HiOutlineRefresh /> Retry</button>
        </div>
      )}

      {/* Loading */}
      {isLoading && (
        <div className="connections-grid">
          {[1, 2, 3].map((i) => (
            <div key={i} className="conn-card conn-skeleton">
              <div className="skel-line skel-title" />
              <div className="skel-line skel-sub" />
              <div className="skel-line skel-badge" />
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {!isLoading && connections.length === 0 && !error && (
        <motion.div className="conn-empty" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}>
          <div className="conn-empty-icon"><HiOutlineLink /></div>
          <h2>No connections yet</h2>
          <p>Connect your apps to start building powerful automated workflows.</p>
          <button className="conn-btn-primary" onClick={() => setShowAddModal(true)}>
            <HiOutlinePlus /> Add your first connection
          </button>
        </motion.div>
      )}

      {/* Connection Cards */}
      {!isLoading && filtered.length > 0 && (
        <div className="connections-grid">
          <AnimatePresence mode="popLayout">
            {filtered.map((conn, i) => {
              const statusMeta = STATUS_META[conn.status] || STATUS_META.ACTIVE;
              const matchingApp = apps.find(a => a.appKey === conn.appKey);
              return (
                <motion.div key={conn.id} className="conn-card"
                  initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95 }} transition={{ duration: 0.3, delay: i * 0.05 }} layout
                >
                  <div className="conn-card-header">
                    <div className="conn-card-app-icon">
                      {matchingApp ? (
                        <img 
                          src={matchingApp.logoUrl || `/icons/${conn.appKey}.svg`}
                          alt={matchingApp.name || conn.appKey}
                          className={matchingApp.logoUrl ? "app-logo-clearbit-dark" : "app-logo-white-svg"}
                          onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }}
                        />
                      ) : null}
                      <span style={{ display: matchingApp ? 'none' : 'block' }}>
                        {matchingApp?.name?.charAt(0).toUpperCase() || conn.appKey?.charAt(0).toUpperCase()}
                      </span>
                    </div>
                    <div className="conn-card-info">
                      <h3 className="conn-card-name">{conn.name}</h3>
                      <span className="conn-card-app">{matchingApp?.name || conn.appKey}</span>
                    </div>
                    <span className={`conn-status-badge ${statusMeta.className}`}>
                      <span className="conn-status-dot" />
                      {statusMeta.label}
                    </span>
                  </div>
                  <div className="conn-card-meta">
                    <span>Created {new Date(conn.createdAt).toLocaleDateString()}</span>
                    {conn.updatedAt && <span>Updated {new Date(conn.updatedAt).toLocaleDateString()}</span>}
                  </div>
                  <div className="conn-card-actions">
                    <button className="conn-action-btn" title="Edit"><HiOutlinePencil /></button>
                    <button className="conn-action-btn conn-action-danger" title="Delete" onClick={() => setDeleteTarget(conn.id)}>
                      <HiOutlineTrash />
                    </button>
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>
      )}

      {/* No search results */}
      {!isLoading && connections.length > 0 && filtered.length === 0 && (
        <div className="conn-empty-search"><p>No connections match &ldquo;{search}&rdquo;</p></div>
      )}

      {/* Add Connection Modal — uses the same AppBrowserModal as canvas */}
      <AnimatePresence>
        {showAddModal && (
          <AppBrowserModal
            apps={apps}
            connections={connections}
            connectOnly={true}
            title="Add Connection"
            onClose={() => { setShowAddModal(false); setPreselectedAppKey(null); }}
            onConnected={() => { fetchConnections(); }}
          />
        )}
      </AnimatePresence>

      {/* Delete Confirmation */}
      <AnimatePresence>
        {deleteTarget && (
          <ConfirmDeleteModal onCancel={() => setDeleteTarget(null)} onConfirm={handleDelete} />
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Add Connection Modal ─────────────────────────────────────────────────────

function AddConnectionModal({ apps, preselectedAppKey, onClose, onCreate }) {
  // Step 1 = pick app, Step 1.5 = setup guide, Step 2 = choose auth method (dual-auth only), Step 3 = enter credentials / OAuth
  const [step, setStep] = useState(1);
  const [selectedApp, setSelectedApp] = useState(null);
  const [showGuide, setShowGuide] = useState(false);
  const [chosenAuth, setChosenAuth] = useState(null); // 'primary' or 'alt'
  const [name, setName] = useState('');
  const [credentials, setCredentials] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [err, setErr] = useState('');
  const [appSearch, setAppSearch] = useState('');
  const [activeCategory, setActiveCategory] = useState(null);
  const [showPasswords, setShowPasswords] = useState({});

  // Auto-select app if preselectedAppKey is provided (from canvas ?connect= redirect)
  useEffect(() => {
    if (preselectedAppKey && apps.length > 0 && !selectedApp) {
      const match = apps.find(a => a.appKey === preselectedAppKey);
      if (match) {
        handleSelectApp(match);
      }
    }
  }, [preselectedAppKey, apps]); // eslint-disable-line react-hooks/exhaustive-deps

  // Group apps by category
  const categorizedApps = useMemo(() => {
    const groups = {};
    apps.forEach(app => {
      const cat = app.category || 'other';
      if (!groups[cat]) groups[cat] = [];
      groups[cat].push(app);
    });
    return groups;
  }, [apps]);

  const filteredApps = useMemo(() => {
    if (!appSearch.trim()) return null; // null = show categories
    return apps.filter(a =>
      a.name.toLowerCase().includes(appSearch.toLowerCase()) ||
      a.appKey.toLowerCase().includes(appSearch.toLowerCase())
    );
  }, [apps, appSearch]);

  const hasDualAuth = selectedApp?.altAuthType != null;

  const activeAuthType = chosenAuth === 'alt' ? selectedApp?.altAuthType : selectedApp?.authType;

  // For dual-auth apps, filter credentialSchema by authOption
  const activeSchema = useMemo(() => {
    if (!selectedApp?.credentialSchema?.length) return [];
    if (!hasDualAuth) return selectedApp.credentialSchema;
    return selectedApp.credentialSchema.filter(field =>
      field.authOption === (chosenAuth === 'alt' ? selectedApp.altAuthType : selectedApp.authType) || !field.authOption
    );
  }, [selectedApp, chosenAuth, hasDualAuth]);

  const handleSelectApp = (app) => {
    setSelectedApp(app);
    setName(`My ${app.name}`);
    setCredentials({});
    setErr('');
    // Show setup guide first
    setShowGuide(true);
  };

  const handleGuideContinue = () => {
    setShowGuide(false);
    if (selectedApp.altAuthType) {
      setStep(2); // go to auth choice
    } else {
      setChosenAuth('primary');
      setStep(3);
    }
  };

  const handleAuthChoice = (choice) => {
    setChosenAuth(choice);
    setCredentials({});
    setStep(3);
  };

  const handleOAuthConnect = async () => {
    setErr('');
    try {
      // Map appKey to provider key
      const providerKey = resolveProviderKey(selectedApp.appKey);
      const { authorizationUrl } = await appCatalogApi.getOAuthUrl(providerKey);

      // Open OAuth in popup window to preserve parent session
      const popup = window.open(authorizationUrl, 'oauth_popup', 'width=600,height=700,scrollbars=yes');

      // Poll for popup close → refresh connections
      if (popup) {
        const pollTimer = setInterval(() => {
          if (popup.closed) {
            clearInterval(pollTimer);
            // Popup closed — OAuth may have completed, refresh and close modal
            onClose();
            window.location.reload();
          }
        }, 500);
      }
    } catch (e) {
      setErr(e.response?.data?.message || 'OAuth is not yet configured for this app. Please add credentials manually or contact support.');
    }
  };

  const handleCreate = async () => {
    setErr('');
    if (!name.trim()) { setErr('Name is required'); return; }

    // Validate required fields from schema
    for (const field of activeSchema) {
      if (field.required && (!credentials[field.key] || !credentials[field.key].toString().trim())) {
        setErr(`${field.label} is required`);
        return;
      }
    }

    setIsSubmitting(true);
    try {
      await onCreate({ appKey: selectedApp.appKey, name: name.trim(), credentials });
      onClose();
    } catch (e) {
      setErr(e.response?.data?.message || 'Failed to create connection');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBack = () => {
    if (step === 3 && hasDualAuth) setStep(2);
    else if (step === 3 || step === 2) { setStep(1); setSelectedApp(null); setChosenAuth(null); }
  };

  const modalTitle = step === 1
    ? 'Select an App'
    : step === 2
    ? `How do you want to connect ${selectedApp?.name}?`
    : `Connect ${selectedApp?.name}`;

  return (
    <>
    {/* App Setup Guide overlay */}
    <AnimatePresence>
      {showGuide && selectedApp && (
        <AppSetupGuide
          app={selectedApp}
          onContinue={handleGuideContinue}
          onClose={() => { setShowGuide(false); setSelectedApp(null); setStep(1); }}
        />
      )}
    </AnimatePresence>

    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div className="conn-modal conn-modal-lg"
        initial={{ opacity: 0, scale: 0.95, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }} transition={{ duration: 0.25 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="conn-modal-header">
          <div className="conn-modal-header-left">
            {step > 1 && (
              <button className="conn-modal-back" onClick={handleBack}><HiOutlineArrowLeft /></button>
            )}
            <h2>{modalTitle}</h2>
          </div>
          <button className="conn-modal-close" onClick={onClose}><HiOutlineX /></button>
        </div>

        {/* ── STEP 1: App Picker ────────────────────────────────────── */}
        {step === 1 && (
          <div className="conn-modal-body">
            <div className="conn-modal-search">
              <HiOutlineSearch />
              <input type="text" placeholder="Search apps..." value={appSearch}
                onChange={(e) => setAppSearch(e.target.value)} autoFocus />
            </div>

            {/* Category Chips */}
            {!filteredApps && (
              <div className="conn-category-chips">
                <button className={`conn-chip ${activeCategory === null ? 'active' : ''}`}
                  onClick={() => setActiveCategory(null)}>All</button>
                {CATEGORY_ORDER.filter(c => categorizedApps[c]?.length).map(cat => (
                  <button key={cat} className={`conn-chip ${activeCategory === cat ? 'active' : ''}`}
                    onClick={() => setActiveCategory(cat)}>
                    {CATEGORY_LABELS[cat] || cat}
                  </button>
                ))}
              </div>
            )}

            {/* Search Results */}
            {filteredApps && (
              <div className="conn-app-grid">
                {filteredApps.length === 0 && <div className="conn-modal-empty"><p>No apps match your search.</p></div>}
                {filteredApps.map(app => <AppPickerItem key={app.appKey} app={app} onSelect={handleSelectApp} />)}
              </div>
            )}

            {/* Category Groups */}
            {!filteredApps && (
              <div className="conn-app-categories">
                {(activeCategory ? [activeCategory] : CATEGORY_ORDER).filter(c => categorizedApps[c]?.length).map(cat => (
                  <div key={cat} className="conn-category-group">
                    <h3 className="conn-category-label">{CATEGORY_LABELS[cat] || cat}</h3>
                    <div className="conn-app-grid">
                      {categorizedApps[cat].map(app => <AppPickerItem key={app.appKey} app={app} onSelect={handleSelectApp} />)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* ── STEP 2: Auth Method Choice (dual-auth apps only) ───── */}
        {step === 2 && selectedApp && (
          <div className="conn-modal-body">
            <p className="conn-auth-intro">
              <strong>{selectedApp.name}</strong> supports two authentication methods. Choose the one that fits your use case:
            </p>

            <div className="conn-auth-options">
              {/* Primary Auth */}
              <button className="conn-auth-option" onClick={() => handleAuthChoice('primary')}>
                <div className="conn-auth-option-icon">
                  {selectedApp.authType === 'OAUTH2' ? <HiOutlineLockClosed /> : <HiOutlineKey />}
                </div>
                <div className="conn-auth-option-info">
                  <span className="conn-auth-option-title">
                    {selectedApp.authType === 'OAUTH2' ? 'Sign in with OAuth' : 'API Key / Bot Token'}
                  </span>
                  <span className="conn-auth-option-desc">
                    {selectedApp.authType === 'OAUTH2'
                      ? 'Authorize via the provider — no manual key entry needed'
                      : 'Paste your bot token or API key for server-side automation (bots, webhooks)'}
                  </span>
                </div>
                <span className="conn-auth-badge">{selectedApp.authType}</span>
              </button>

              {/* Alt Auth */}
              <button className="conn-auth-option" onClick={() => handleAuthChoice('alt')}>
                <div className="conn-auth-option-icon">
                  {selectedApp.altAuthType === 'OAUTH2' ? <HiOutlineLockClosed /> : <HiOutlineKey />}
                </div>
                <div className="conn-auth-option-info">
                  <span className="conn-auth-option-title">
                    {selectedApp.altAuthType === 'OAUTH2' ? 'Sign in with OAuth' : 'API Key / Bot Token'}
                  </span>
                  <span className="conn-auth-option-desc">
                    {selectedApp.altAuthType === 'OAUTH2'
                      ? 'Authorize via the provider — use personal account actions'
                      : 'Paste your bot token or API key for server-side automation (bots, webhooks)'}
                  </span>
                </div>
                <span className="conn-auth-badge">{selectedApp.altAuthType}</span>
              </button>
            </div>
          </div>
        )}

        {/* ── STEP 3: Credentials / OAuth ───────────────────────── */}
        {step === 3 && selectedApp && (
          <div className="conn-modal-body">
            {err && <div className="conn-modal-error">{err}</div>}

            {/* OAuth Flow */}
            {activeAuthType === 'OAUTH2' && (
              <div className="conn-oauth-section">
                <div className="conn-oauth-card">
                  <HiOutlineLockClosed className="conn-oauth-icon" />
                  <h3>Sign in with {selectedApp.name}</h3>
                  <p>You'll be redirected to {selectedApp.name} to authorize access. No credentials are entered here — everything is handled securely via OAuth.</p>
                  <button className="conn-btn-oauth" onClick={handleOAuthConnect}>
                    <HiOutlineExternalLink /> Connect with {selectedApp.name}
                  </button>
                </div>
                <div className="conn-security-note">
                  <HiOutlineShieldCheck />
                  <span>We never see your password. Tokens are encrypted with AES-256-GCM and stored securely.</span>
                </div>
              </div>
            )}

            {/* API Key / Credential Form */}
            {activeAuthType !== 'OAUTH2' && activeAuthType !== 'NONE' && (
              <>
                <label className="conn-form-label">
                  Connection Name
                  <input type="text" className="conn-form-input" value={name}
                    onChange={(e) => setName(e.target.value)} placeholder="Enter a friendly name" />
                </label>

                {activeSchema.length > 0 ? (
                  activeSchema.map(field => (
                    <label key={field.key} className="conn-form-label">
                      {field.label} {field.required && <span className="conn-required">*</span>}
                      <div className="conn-password-wrap">
                        <input
                          type={field.type === 'password' && !showPasswords[field.key] ? 'password' : 'text'}
                          className="conn-form-input"
                          value={credentials[field.key] || ''}
                          onChange={(e) => setCredentials(prev => ({ ...prev, [field.key]: e.target.value }))}
                          placeholder={field.placeholder || ''}
                        />
                        {field.type === 'password' && (
                          <button type="button" className="conn-eye-toggle"
                            onClick={() => setShowPasswords(prev => ({ ...prev, [field.key]: !prev[field.key] }))}>
                            {showPasswords[field.key] ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                          </button>
                        )}
                      </div>
                      {field.helpText && <span className="conn-field-help">{field.helpText}</span>}
                    </label>
                  ))
                ) : (
                  <label className="conn-form-label">
                    API Key / Token <span className="conn-required">*</span>
                    <div className="conn-password-wrap">
                      <input type="password" className="conn-form-input"
                        value={credentials.apiKey || ''}
                        onChange={(e) => setCredentials({ apiKey: e.target.value })}
                        placeholder="Paste your API key" />
                    </div>
                  </label>
                )}

                {selectedApp.helpUrl && (
                  <a href={selectedApp.helpUrl} target="_blank" rel="noopener noreferrer" className="conn-help-link">
                    <HiOutlineExternalLink /> Where do I find my credentials?
                  </a>
                )}

                <div className="conn-security-note">
                  <HiOutlineShieldCheck />
                  <span>Credentials are encrypted with AES-256-GCM before storage. We never log or expose raw secrets.</span>
                </div>

                <div className="conn-modal-footer">
                  <button className="conn-btn-secondary" onClick={handleBack}>Back</button>
                  <button className="conn-btn-primary" onClick={handleCreate} disabled={isSubmitting}>
                    {isSubmitting ? 'Creating...' : 'Create Connection'}
                  </button>
                </div>
              </>
            )}

            {/* No Auth */}
            {activeAuthType === 'NONE' && (
              <>
                <label className="conn-form-label">
                  Connection Name
                  <input type="text" className="conn-form-input" value={name}
                    onChange={(e) => setName(e.target.value)} placeholder="Enter a friendly name" />
                </label>
                <p className="conn-no-auth-note">This app doesn't require any credentials. Just name your connection and you're ready to go.</p>
                <div className="conn-modal-footer">
                  <button className="conn-btn-secondary" onClick={handleBack}>Back</button>
                  <button className="conn-btn-primary" onClick={handleCreate} disabled={isSubmitting}>
                    {isSubmitting ? 'Creating...' : 'Create Connection'}
                  </button>
                </div>
              </>
            )}
          </div>
        )}
      </motion.div>
    </motion.div>
    </>
  );
}

// ─── App Picker Item ────────────────────────────────────────────────────────────

function AppPickerItem({ app, onSelect }) {
  return (
    <button className="conn-app-item" onClick={() => onSelect(app)}>
      <div className="conn-app-icon">
        <img 
          src={app.logoUrl || `/icons/${app.appKey}.svg`}
          alt={app.name}
          className={app.logoUrl ? "app-logo-clearbit-dark" : "app-logo-white-svg"}
          onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }}
        />
        <span style={{ display: 'none' }}>{app.name.charAt(0)}</span>
      </div>
      <div className="conn-app-info">
        <span className="conn-app-name">{app.name}</span>
        <span className="conn-app-desc">{app.description || app.appKey}</span>
      </div>
      <span className="conn-app-auth">{app.authType || 'NONE'}</span>
    </button>
  );
}

// ─── Confirm Delete Modal ───────────────────────────────────────────────────────

function ConfirmDeleteModal({ onCancel, onConfirm }) {
  const [loading, setLoading] = useState(false);
  const handleConfirm = async () => {
    setLoading(true);
    await onConfirm();
    setLoading(false);
  };

  return (
    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onCancel}>
      <motion.div className="conn-modal conn-modal-sm"
        initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }} transition={{ duration: 0.2 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="conn-modal-header">
          <h2>Delete Connection</h2>
          <button className="conn-modal-close" onClick={onCancel}><HiOutlineX /></button>
        </div>
        <div className="conn-modal-body">
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            This will permanently remove this connection and any workflows using it may stop functioning. This action cannot be undone.
          </p>
        </div>
        <div className="conn-modal-footer">
          <button className="conn-btn-secondary" onClick={onCancel}>Cancel</button>
          <button className="conn-btn-danger" onClick={handleConfirm} disabled={loading}>
            {loading ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </motion.div>
    </motion.div>
  );
}

// ─── Utility ────────────────────────────────────────────────────────────────────

function resolveProviderKey(appKey) {
  // Most keys map 1:1. Only list overrides here.
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
}
