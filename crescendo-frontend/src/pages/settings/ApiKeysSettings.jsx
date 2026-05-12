import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlinePlus, HiOutlineTrash, HiOutlineKey, HiOutlineX, HiOutlineClipboardCopy, HiOutlineExclamation } from 'react-icons/hi';
import { apiKeysApi } from '../../api/emailServiceApi';
import './Settings.css';

export default function ApiKeysSettings() {
  const [keys, setKeys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [revokeTarget, setRevokeTarget] = useState(null);
  const [newKey, setNewKey] = useState(null); // shown once after creation

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
        <div className="settings-table">
          <div className="settings-table-head">
            <span>Name</span>
            <span>Prefix</span>
            <span>Created</span>
            <span>Last Used</span>
            <span></span>
          </div>
          {keys.map((k) => (
            <div key={k.id} className="settings-table-row">
              <span className="settings-table-cell-name">{k.name}</span>
              <code className="settings-table-cell-code">{k.prefix}...</code>
              <span className="settings-table-cell-date">
                {k.createdAt ? new Date(k.createdAt).toLocaleDateString() : '—'}
              </span>
              <span className="settings-table-cell-date">
                {k.lastUsedAt ? new Date(k.lastUsedAt).toLocaleDateString() : 'Never'}
              </span>
              <button className="settings-icon-btn settings-danger-icon" onClick={() => setRevokeTarget(k.id)} title="Revoke">
                <HiOutlineTrash />
              </button>
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
          <ConfirmModal
            title="Revoke API Key"
            message="This will immediately invalidate this key. Any applications using it will lose access."
            confirmLabel="Revoke"
            onCancel={() => setRevokeTarget(null)}
            onConfirm={handleRevoke}
          />
        )}
      </AnimatePresence>
    </motion.div>
  );
}

function CreateKeyModal({ onClose, onCreated }) {
  const [name, setName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) { setErr('Name required'); return; }
    setSubmitting(true);
    setErr('');
    try {
      const data = await apiKeysApi.create({ name: name.trim() });
      onCreated(data.plainKey, { id: data.id, name: data.name, prefix: data.prefix, createdAt: new Date().toISOString(), lastUsedAt: null });
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
        </div>
        <div className="conn-modal-footer">
          <button type="button" className="conn-btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="conn-btn-primary" disabled={submitting}>{submitting ? 'Creating...' : 'Create'}</button>
        </div>
      </motion.form>
    </motion.div>
  );
}

function ConfirmModal({ title, message, confirmLabel, onCancel, onConfirm }) {
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
