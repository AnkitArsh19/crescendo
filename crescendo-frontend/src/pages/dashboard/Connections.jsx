/* eslint-disable no-unused-vars */
/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlinePlus, HiOutlineTrash, HiOutlinePencil, HiOutlineLink,
  HiOutlineX, HiOutlineRefresh, HiOutlineSearch,
  HiOutlineShieldCheck, HiOutlineExternalLink, HiOutlineKey,
  HiOutlineLockClosed, HiOutlineArrowLeft, HiOutlineEye, HiOutlineEyeOff,
  HiOutlineChevronDown, HiOutlineChevronRight, HiOutlineInformationCircle,
  HiCheck,
} from 'react-icons/hi';
import useConnectionStore from '../../store/connectionStore';
import { appCatalogApi } from '../../api/appCatalogApi';
import { connectionsApi } from '../../api/connectionsApi';
import AppBrowserModal from './nodes/AppBrowserModal';
import './Connections.css';

// ─── Constants ──────────────────────────────────────────────────────────────────

const STATUS_META = {
  ACTIVE:  { label: 'Active',  className: 'status-active' },
  ERROR:   { label: 'Error',   className: 'status-error' },
  REAUTH:  { label: 'Re-auth', className: 'status-reauth' },
};

export default function Connections() {
  const { connections, isLoading, error, fetchConnections, createConnection, deleteConnection } = useConnectionStore();
  const [apps, setApps] = useState([]);
  const [showAddModal, setShowAddModal] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [editTarget, setEditTarget] = useState(null);
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
      fetchConnections();
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [fetchConnections]);

  // Handle ?connect=<appKey> from canvas redirect
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
        <button
          className="conn-btn-primary"
          onClick={() => setShowAddModal(true)}
          title="Connect a new app or service"
          aria-label="Add Connection"
        >
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
          <button
            onClick={fetchConnections}
            className="conn-retry-btn"
            title="Retry loading connections"
            aria-label="Retry loading connections"
          >
            <HiOutlineRefresh /> Retry
          </button>
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
          <button
            className="conn-btn-primary"
            onClick={() => setShowAddModal(true)}
            title="Connect your first app"
            aria-label="Add your first connection"
          >
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
                          className="app-logo-img"
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
                    <button
                      className="conn-action-btn"
                      title={`Edit ${conn.name} settings`}
                      aria-label={`Edit ${conn.name} settings`}
                      onClick={() => setEditTarget(conn)}
                    >
                      <HiOutlinePencil />
                    </button>
                    <button
                      className="conn-action-btn conn-action-danger"
                      title={`Delete ${conn.name}`}
                      aria-label={`Delete ${conn.name}`}
                      onClick={() => setDeleteTarget(conn.id)}
                    >
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

      {/* Add Connection Modal */}
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

      {/* Edit Connection Modal */}
      <AnimatePresence>
        {editTarget && (
          <EditConnectionModal
            connection={editTarget}
            app={apps.find((a) => a.appKey === editTarget.appKey)}
            onCancel={() => setEditTarget(null)}
            onSaved={() => { fetchConnections(); setEditTarget(null); }}
            onReconnected={() => { fetchConnections(); setEditTarget(null); }}
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


// ─── Edit Connection Modal ───────────────────────────────────────────────────────

function EditConnectionModal({ connection, app, onCancel, onSaved, onReconnected }) {
  const [name, setName] = useState(connection.name || '');
  const [credentials, setCredentials] = useState({});
  const [showPasswords, setShowPasswords] = useState({});
  const [isSaving, setIsSaving] = useState(false);
  const [testState, setTestState] = useState(null); // { loading, success, message }
  const [error, setError] = useState(null);

  const isOAuth = app?.authType === 'OAUTH2' || connection.authType === 'OAUTH2';
  const schema = app?.credentialSchema || [];

  const handleTest = async () => {
    setTestState({ loading: true });
    try {
      const res = await connectionsApi.test(connection.id);
      if (res.success) {
        setTestState({ loading: false, success: true, message: res.message || 'Connection verified successfully!' });
      } else {
        setTestState({ loading: false, success: false, message: res.message || 'Connection test failed' });
      }
    } catch (e) {
      setTestState({ loading: false, success: false, message: e.response?.data?.message || e.message || 'Test failed' });
    }
  };

  const handleReconnectOAuth = async () => {
    try {
      const { authorizationUrl } = await appCatalogApi.getOAuthUrl(connection.appKey, { connectionId: connection.id });
      const popup = window.open(authorizationUrl, 'oauth_popup', 'width=600,height=700,scrollbars=yes');
      if (popup) {
        const handler = (ev) => {
          if (ev.data?.type === 'oauth-connected') {
            window.removeEventListener('message', handler);
            onReconnected?.();
          }
        };
        window.addEventListener('message', handler);
      }
    } catch (e) {
      setError(e.response?.data?.message || 'Could not start OAuth reconnect');
    }
  };

  const handleSave = async () => {
    if (!name.trim()) {
      setError('Connection name is required');
      return;
    }
    setIsSaving(true);
    setError(null);
    try {
      const payload = { name: name.trim() };
      if (Object.keys(credentials).length > 0) {
        payload.credentials = credentials;
      }
      await connectionsApi.update(connection.id, payload);
      onSaved?.();
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to update connection');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onCancel}>
      <motion.div
        className="conn-modal conn-modal-md"
        initial={{ opacity: 0, scale: 0.94, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.94, y: 20 }}
        transition={{ duration: 0.22 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="conn-modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div className="conn-card-app-icon" style={{ width: '32px', height: '32px' }}>
              <img
                src={app?.logoUrl || `/icons/${connection.appKey}.svg`}
                alt={app?.name || connection.appKey}
                className="app-logo-img"
                onError={(e) => { e.target.style.display = 'none'; }}
              />
            </div>
            <h2>Edit Connection · {app?.name || connection.appKey}</h2>
          </div>
          <button className="conn-modal-close" onClick={onCancel} title="Close" aria-label="Close"><HiOutlineX /></button>
        </div>

        <div className="conn-modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {error && <div className="abm-error-toast">{error}</div>}

          {/* Connection Name */}
          <label className="abm-form-label">
            Connection Display Name
            <input
              type="text"
              className="abm-form-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. My Production Workspace"
            />
          </label>

          {/* OAuth Provider Box */}
          {isOAuth && (
            <div style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-primary)', borderRadius: 'var(--radius-md)', padding: '14px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontSize: '0.84rem', fontWeight: 600, color: 'var(--text-accent)' }}>OAuth Authorization</div>
                  <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)' }}>Re-authorize with {app?.name || connection.appKey} to refresh token scopes</div>
                </div>
                <button
                  type="button"
                  className="conn-btn-primary"
                  style={{ padding: '6px 14px', fontSize: '0.78rem' }}
                  onClick={handleReconnectOAuth}
                  title={`Reconnect with ${app?.name || connection.appKey}`}
                >
                  <HiOutlineRefresh /> Reconnect
                </button>
              </div>
            </div>
          )}

          {/* API Key / Schema Fields */}
          {!isOAuth && (
            schema.length > 0 ? (
              schema.map((field) => (
                <label key={field.key} className="abm-form-label">
                  {field.label} (Update)
                  <div className="abm-password-wrap">
                    <input
                      type={field.type === 'password' && !showPasswords[field.key] ? 'password' : 'text'}
                      className="abm-form-input"
                      value={credentials[field.key] || ''}
                      onChange={(e) => setCredentials((prev) => ({ ...prev, [field.key]: e.target.value }))}
                      placeholder="Leave blank to keep existing credential"
                    />
                    {field.type === 'password' && (
                      <button
                        type="button"
                        className="abm-eye-toggle"
                        title={showPasswords[field.key] ? 'Hide value' : 'Show value'}
                        onClick={() => setShowPasswords((prev) => ({ ...prev, [field.key]: !prev[field.key] }))}
                      >
                        {showPasswords[field.key] ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                      </button>
                    )}
                  </div>
                </label>
              ))
            ) : (
              <label className="abm-form-label">
                API Key / Token (Update)
                <div className="abm-password-wrap">
                  <input
                    type={!showPasswords['apiKey'] ? 'password' : 'text'}
                    className="abm-form-input"
                    value={credentials.apiKey || ''}
                    onChange={(e) => setCredentials({ apiKey: e.target.value })}
                    placeholder="Leave blank to keep existing API key"
                  />
                  <button
                    type="button"
                    className="abm-eye-toggle"
                    title={showPasswords['apiKey'] ? 'Hide API key' : 'Show API key'}
                    onClick={() => setShowPasswords((prev) => ({ ...prev, apiKey: !prev.apiKey }))}
                  >
                    {showPasswords['apiKey'] ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                  </button>
                </div>
              </label>
            )
          )}

          {/* Test Status Banner */}
          {testState && !testState.loading && (
            <div style={{
              padding: '10px 14px',
              borderRadius: 'var(--radius-md)',
              fontSize: '0.8rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              background: testState.success ? 'rgba(34, 197, 94, 0.1)' : 'rgba(239, 68, 68, 0.1)',
              color: testState.success ? '#22c55e' : '#ef4444',
              border: `1px solid ${testState.success ? 'rgba(34, 197, 94, 0.25)' : 'rgba(239, 68, 68, 0.25)'}`,
            }}>
              {testState.success ? <HiCheck /> : <HiOutlineX />}
              <span>{testState.message}</span>
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '6px' }}>
            <button
              type="button"
              className="conn-btn-secondary"
              style={{ fontSize: '0.78rem', padding: '6px 14px' }}
              onClick={handleTest}
              disabled={testState?.loading}
              title="Test connection responsiveness"
            >
              {testState?.loading ? 'Testing…' : 'Test Connection'}
            </button>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>Encrypted with AES-256</span>
          </div>
        </div>

        <div className="conn-modal-footer">
          <button className="conn-btn-secondary" onClick={onCancel}>Cancel</button>
          <button className="conn-btn-primary" onClick={handleSave} disabled={isSaving}>
            {isSaving ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </motion.div>
    </motion.div>
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
