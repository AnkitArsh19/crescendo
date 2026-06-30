import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlinePlus, HiOutlineTrash, HiOutlineKey, HiOutlineX, HiOutlineClipboardCopy, HiOutlineExclamation, HiOutlineRefresh } from 'react-icons/hi';
import { apiKeysApi } from '../../api/emailServiceApi';
import ConfirmModal from '../../components/ui/ConfirmModal';
import './Settings.css';

export default function ApiKeysSettings() {
  const [keys, setKeys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [revokeTarget, setRevokeTarget] = useState(null);
  const [newKey, setNewKey] = useState(null); // shown once after creation
  const [confirmRotate, setConfirmRotate] = useState(null);

  const fetchKeys = async () => {
    setLoading(true);
    try {
      const data = await apiKeysApi.list();
      setKeys(data);
    } catch { /* silent */ }
    setLoading(false);
  };

  useEffect(() => { fetchKeys(); }, []);

  const handleRevoke = async () => {
    if (!revokeTarget) return;
    try {
      await apiKeysApi.revoke(revokeTarget);
      setKeys(keys.filter((k) => k.id !== revokeTarget));
    } catch { /* silent */ }
    setRevokeTarget(null);
  };

  const handleRotate = async (id) => {
    setConfirmRotate(id);
  };

  const confirmRotateKey = async () => {
    if (!confirmRotate) return;
    try {
      const data = await apiKeysApi.rotate(confirmRotate);
      setNewKey(data.plainKey);
      await fetchKeys();
    } catch { /* silent */ }
    setConfirmRotate(null);
  };

  return (
    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
      <div className="settings-section-header">
        <div>
          <h2 className="settings-section-title">API Keys</h2>
          <p className="settings-section-desc">Manage keys for the email sending API. Keys use the <code>re_</code> prefix.</p>
        </div>
        <button className="settings-btn-primary" onClick={() => setShowCreate(true)}>
          <HiOutlinePlus /> Create Key
        </button>
      </div>

      {/* New Key Banner */}
      <AnimatePresence>
        {newKey && (
          <motion.div
            className="apikey-new-banner"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
          >
            <HiOutlineExclamation className="apikey-warn-icon" />
            <div className="apikey-new-content">
              <strong>Save this key now — it won&apos;t be shown again</strong>
              <code className="apikey-new-value">{newKey}</code>
            </div>
            <button className="apikey-copy-btn" onClick={() => { navigator.clipboard.writeText(newKey); }}>
              <HiOutlineClipboardCopy /> Copy
            </button>
            <button className="settings-icon-btn" onClick={() => setNewKey(null)}>
              <HiOutlineX />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Table */}
      {loading ? (
        <div className="settings-skeleton-list">
          {[1, 2].map((i) => <div key={i} className="settings-skeleton-row" />)}
        </div>
      ) : keys.length === 0 ? (
        <div className="settings-empty">
          <HiOutlineKey className="settings-empty-icon" />
          <p>No API keys yet. Create one to start sending emails programmatically.</p>
        </div>
      ) : (
        <div className="settings-table apikey-table">
          <div className="settings-table-head">
            <span>Name</span>
            <span>Prefix</span>
            <span>Scopes</span>
            <span>Expires</span>
            <span>Status</span>
            <span></span>
          </div>
          {keys.map((k) => (
            <div key={k.id} className="settings-table-row">
              <span className="settings-table-cell-name">{k.name}</span>
              <code className="settings-table-cell-code">{k.prefix}...</code>
              <span className="settings-table-cell-date">{k.scopes?.length || 0}</span>
              <span className="settings-table-cell-date">
                {k.expiresAt ? new Date(k.expiresAt).toLocaleDateString() : 'Never'}
              </span>
              <span className="settings-table-cell-date">{k.status || 'ACTIVE'}</span>
              <div className="apikey-row-actions">
                {k.status === 'ACTIVE' && (
                  <button className="settings-icon-btn" onClick={() => handleRotate(k.id)} title="Rotate">
                    <HiOutlineRefresh />
                  </button>
                )}
                <button className="settings-icon-btn settings-danger-icon" onClick={() => setRevokeTarget(k.id)} title="Revoke">
                  <HiOutlineTrash />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      <AnimatePresence>
        {showCreate && (
          <CreateKeyModal
            onClose={() => setShowCreate(false)}
            onCreated={(plainKey, keyData) => {
              setNewKey(plainKey);
              setKeys([...keys, keyData]);
              setShowCreate(false);
            }}
          />
        )}
      </AnimatePresence>

      {/* Revoke Confirmation */}
      <AnimatePresence>
        {revokeTarget && (
          <InlineConfirmModal
            title="Revoke API Key"
            message="This will immediately invalidate this key. Any applications using it will lose access."
            confirmLabel="Revoke"
            onCancel={() => setRevokeTarget(null)}
            onConfirm={handleRevoke}
          />
        )}
      </AnimatePresence>

      <InlineConfirmModal
        open={!!confirmRotate}
        onClose={() => setConfirmRotate(null)}
        title="Rotate API Key?"
        description="The current key will remain valid for 24 hours. A new key will be generated immediately."
        onConfirm={confirmRotateKey}
        confirmText="Rotate Key"
      />
    </motion.div>
  );
}

function CreateKeyModal({ onClose, onCreated }) {
  const scopeOptions = [
    'workflow:read', 'workflow:write', 'workflow:trigger', 'run:read', 'run:cancel',
    'connection:read', 'connection:write', 'email:send', 'app:read', 'ai:build',
  ];
  const [name, setName] = useState('');
  const [expiresInDays, setExpiresInDays] = useState(90);
  const [rateLimitPerMinute, setRateLimitPerMinute] = useState(100);
  const [scopes, setScopes] = useState(['workflow:read', 'workflow:trigger', 'run:read', 'email:send', 'app:read']);
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) { setErr('Name required'); return; }
    setSubmitting(true);
    setErr('');
    try {
      const data = await apiKeysApi.create({ name: name.trim(), expiresInDays, rateLimitPerMinute, scopes });
      onCreated(data.plainKey, {
        id: data.id,
        name: data.name,
        prefix: data.prefix,
        scopes: data.scopes,
        rateLimitPerMinute: data.rateLimitPerMinute,
        createdAt: new Date().toISOString(),
        lastUsedAt: null,
        expiresAt: data.expiresAt,
        status: 'ACTIVE',
      });
    } catch (error) {
      setErr(error.response?.data?.message || 'Failed to create key');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.form
        className="conn-modal conn-modal-sm"
        initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
        transition={{ duration: 0.2 }}
        onClick={(e) => e.stopPropagation()}
        onSubmit={handleSubmit}
      >
        <div className="conn-modal-header">
          <h2>Create API Key</h2>
          <button type="button" className="conn-modal-close" onClick={onClose}><HiOutlineX /></button>
        </div>
        <div className="conn-modal-body">
          {err && <div className="conn-modal-error">{err}</div>}
          <label className="conn-form-label">
            Key Name
            <input className="conn-form-input" value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g., Production" autoFocus />
          </label>
          <div className="apikey-create-grid">
            <label className="conn-form-label">
              Expires
              <select className="conn-form-input" value={expiresInDays} onChange={(e) => setExpiresInDays(Number(e.target.value))}>
                <option value={30}>30 days</option>
                <option value={90}>90 days</option>
                <option value={180}>180 days</option>
                <option value={365}>1 year</option>
              </select>
            </label>
            <label className="conn-form-label">
              Requests per minute
              <input className="conn-form-input" type="number" min="1" max="10000" value={rateLimitPerMinute} onChange={(e) => setRateLimitPerMinute(Number(e.target.value))} />
            </label>
          </div>
          <fieldset className="apikey-scope-fieldset">
            <legend>Scopes</legend>
            <div className="apikey-scope-grid">
              {scopeOptions.map((scope) => (
                <label key={scope}>
                  <input
                    type="checkbox"
                    checked={scopes.includes(scope)}
                    onChange={() => setScopes((current) => current.includes(scope)
                      ? current.filter((item) => item !== scope)
                      : [...current, scope])}
                  />
                  <code>{scope}</code>
                </label>
              ))}
            </div>
          </fieldset>
        </div>
        <div className="conn-modal-footer">
          <button type="button" className="conn-btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="conn-btn-primary" disabled={submitting || scopes.length === 0}>{submitting ? 'Creating...' : 'Create'}</button>
        </div>
      </motion.form>
    </motion.div>
  );
}

function InlineConfirmModal({ title, message, confirmLabel, onCancel, onConfirm }) {
  const [loading, setLoading] = useState(false);
  const handleConfirm = async () => { setLoading(true); await onConfirm(); setLoading(false); };

  return (
    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onCancel}>
      <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={(e) => e.stopPropagation()}>
        <div className="conn-modal-header"><h2>{title}</h2><button className="conn-modal-close" onClick={onCancel}><HiOutlineX /></button></div>
        <div className="conn-modal-body"><p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>{message}</p></div>
        <div className="conn-modal-footer">
          <button className="conn-btn-secondary" onClick={onCancel}>Cancel</button>
          <button className="conn-btn-danger" onClick={handleConfirm} disabled={loading}>{loading ? 'Working...' : confirmLabel}</button>
        </div>
      </motion.div>
    </motion.div>
  );
}
