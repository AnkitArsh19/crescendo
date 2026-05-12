import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  HiOutlineUserGroup,
  HiOutlineShieldCheck,
  HiOutlineShieldExclamation,
  HiOutlineExclamationCircle,
  HiOutlineMail,
  HiOutlineKey,
  HiOutlinePlus,
  HiOutlineTrash,
  HiOutlineCheck,
  HiOutlineX,
} from 'react-icons/hi';
import api from '../../api/axios';
import { appCatalogApi } from '../../api/appCatalogApi';
import useAuthStore from '../../store/authStore';
import './AdminPage.css';

export default function AdminPage() {
  const { user } = useAuthStore();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  // Admin emails
  const [adminEmails, setAdminEmails] = useState([]);
  const [newEmail, setNewEmail] = useState('');
  const [emailLoading, setEmailLoading] = useState(false);

  // Platform keys
  const [platformKeys, setPlatformKeys] = useState([]);
  const [availableApps, setAvailableApps] = useState([]);
  const [pkForm, setPkForm] = useState({ appKey: '', appName: '', credentials: {} });
  const [pkNewField, setPkNewField] = useState({ key: '', value: '' });
  const [showPkForm, setShowPkForm] = useState(false);
  const [pkLoading, setPkLoading] = useState(false);

  const isAdmin = user?.role === 'ADMIN' || user?.limits?.tier === 'ADMIN';

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/admin/users');
      setUsers(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const fetchAdminEmails = async () => {
    try {
      const res = await api.get('/admin/emails');
      setAdminEmails(res.data);
    } catch { /* ignore */ }
  };

  const fetchPlatformKeys = async () => {
    try {
      const res = await api.get('/admin/platform-keys');
      setPlatformKeys(res.data);
    } catch { /* ignore */ }
  };

  useEffect(() => {
    if (isAdmin) {
      fetchUsers();
      fetchAdminEmails();
      fetchPlatformKeys();
      appCatalogApi.list().then(setAvailableApps).catch(() => {});
    } else {
      setLoading(false);
    }
  }, [isAdmin]);

  const handleAppSelection = (e) => {
    const selectedKey = e.target.value;
    const selectedApp = availableApps.find(a => a.appKey === selectedKey);
    
    if (selectedApp) {
      // Auto-populate required credential fields with empty strings
      const initialCreds = {};
      if (selectedApp.credentialSchema) {
        selectedApp.credentialSchema.forEach(field => {
          initialCreds[field.key] = '';
        });
      }
      
      setPkForm({
        appKey: selectedApp.appKey,
        appName: selectedApp.name,
        credentials: initialCreds
      });
    } else {
      setPkForm({ appKey: '', appName: '', credentials: {} });
    }
  };

  const handlePromote = async (userId) => {
    setActionLoading(userId);
    try {
      await api.post(`/admin/users/${userId}/promote`);
      await fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to promote user');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDemote = async (userId) => {
    setActionLoading(userId);
    try {
      await api.post(`/admin/users/${userId}/demote`);
      await fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to demote user');
    } finally {
      setActionLoading(null);
    }
  };

  const handleAddEmail = async () => {
    if (!newEmail.trim()) return;
    setEmailLoading(true);
    try {
      await api.post('/admin/emails', { email: newEmail.trim() });
      setNewEmail('');
      await fetchAdminEmails();
      await fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add email');
    } finally {
      setEmailLoading(false);
    }
  };

  const handleRemoveEmail = async (email) => {
    try {
      await api.delete(`/admin/emails/${encodeURIComponent(email)}`);
      await fetchAdminEmails();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to remove email');
    }
  };

  const handleSavePlatformKey = async () => {
    if (!pkForm.appKey.trim()) return;
    setPkLoading(true);
    try {
      await api.post('/admin/platform-keys', {
        appKey: pkForm.appKey.trim(),
        appName: pkForm.appName.trim() || pkForm.appKey.trim(),
        credentials: pkForm.credentials,
      });
      setPkForm({ appKey: '', appName: '', credentials: {} });
      setPkNewField({ key: '', value: '' });
      setShowPkForm(false);
      await fetchPlatformKeys();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save platform key');
    } finally {
      setPkLoading(false);
    }
  };

  const handleDeletePlatformKey = async (appKey) => {
    try {
      await api.delete(`/admin/platform-keys/${appKey}`);
      await fetchPlatformKeys();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete platform key');
    }
  };

  const handleTogglePlatformKey = async (appKey) => {
    try {
      await api.patch(`/admin/platform-keys/${appKey}/toggle`);
      await fetchPlatformKeys();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to toggle platform key');
    }
  };

  const addCredentialField = () => {
    if (!pkNewField.key.trim()) return;
    setPkForm(prev => ({
      ...prev,
      credentials: { ...prev.credentials, [pkNewField.key.trim()]: pkNewField.value },
    }));
    setPkNewField({ key: '', value: '' });
  };

  const removeCredentialField = (key) => {
    setPkForm(prev => {
      const creds = { ...prev.credentials };
      delete creds[key];
      return { ...prev, credentials: creds };
    });
  };

  if (!isAdmin) {
    return (
      <div className="adm-page">
        <div className="adm-forbidden">
          <HiOutlineExclamationCircle />
          <h2>Access Denied</h2>
          <p>Admin privileges required.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="adm-page">
      <div className="adm-header">
        <div>
          <h1 className="adm-title">Admin Panel</h1>
          <p className="adm-subtitle">Manage users, admin access, and platform keys</p>
        </div>
      </div>

      {error && (
        <div className="adm-error">
          <HiOutlineExclamationCircle /> {error}
          <button className="adm-error-dismiss" onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {/* ── Admin Emails Section ────────────────────────────────────── */}
      <div className="adm-section">
        <h2 className="adm-section-title">
          <HiOutlineMail /> Admin Emails
        </h2>
        <p className="adm-section-desc">
          Users registering with these emails are automatically promoted to admin.
        </p>

        <div className="adm-email-add">
          <input
            type="email"
            placeholder="Enter email to whitelist..."
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            className="adm-email-input"
            onKeyDown={(e) => e.key === 'Enter' && handleAddEmail()}
          />
          <button
            className="adm-email-add-btn"
            onClick={handleAddEmail}
            disabled={emailLoading || !newEmail.trim()}
          >
            <HiOutlinePlus /> {emailLoading ? 'Adding...' : 'Add'}
          </button>
        </div>

        <div className="adm-email-list">
          {adminEmails.map((ae) => (
            <div key={ae.id} className="adm-email-row">
              <span className="adm-email-address">{ae.email}</span>
              <span className="adm-email-by">by {ae.addedBy || 'system'}</span>
              <button className="adm-email-remove" onClick={() => handleRemoveEmail(ae.email)}>
                <HiOutlineTrash />
              </button>
            </div>
          ))}
          {adminEmails.length === 0 && (
            <p className="adm-empty-hint">No admin emails configured yet.</p>
          )}
        </div>
      </div>

      {/* ── Platform Keys Section ──────────────────────────────────── */}
      <div className="adm-section">
        <h2 className="adm-section-title">
          <HiOutlineKey /> Platform Keys
        </h2>
        <p className="adm-section-desc">
          Configure platform-wide API keys. Users without their own connection will use these automatically.
        </p>

        <div className="adm-pk-list">
          {platformKeys.map((pk) => (
            <div key={pk.id} className="adm-pk-row">
              <div className="adm-pk-info">
                <span className="adm-pk-name">{pk.appName || pk.appKey}</span>
                <span className="adm-pk-key">{pk.appKey}</span>
              </div>
              <span className="adm-pk-usage">{pk.usageCount} uses</span>
              <button
                className={`adm-pk-toggle ${pk.enabled ? 'enabled' : 'disabled'}`}
                onClick={() => handleTogglePlatformKey(pk.appKey)}
              >
                {pk.enabled ? <><HiOutlineCheck /> On</> : <><HiOutlineX /> Off</>}
              </button>
              <button className="adm-pk-delete" onClick={() => handleDeletePlatformKey(pk.appKey)}>
                <HiOutlineTrash />
              </button>
            </div>
          ))}
          {platformKeys.length === 0 && !showPkForm && (
            <p className="adm-empty-hint">No platform keys configured yet.</p>
          )}
        </div>

        {showPkForm ? (
          <div className="adm-pk-form">
            <div className="adm-pk-form-row">
              <select 
                className="adm-pk-input" 
                value={pkForm.appKey}
                onChange={handleAppSelection}
              >
                <option value="">Select App</option>
                {availableApps
                  .filter(app => app.authType === 'APIKEY' || app.altAuthType === 'APIKEY')
                  .map(app => (
                  <option key={app.appKey} value={app.appKey}>
                    {app.name}
                  </option>
                ))}
              </select>
              <input
                placeholder="Display name (optional)"
                value={pkForm.appName}
                onChange={(e) => setPkForm(prev => ({ ...prev, appName: e.target.value }))}
                className="adm-pk-input"
              />
            </div>

            {pkForm.appKey && (
              <>
                <div className="adm-pk-creds-label">Credentials</div>
                {Object.entries(pkForm.credentials).map(([k, v]) => (
                  <div key={k} className="adm-pk-cred-row">
                    <span className="adm-pk-cred-key">{k}</span>
                    <input
                      type="password"
                      placeholder={`Enter ${k}`}
                      value={v}
                      onChange={(e) => setPkForm(prev => ({
                        ...prev,
                        credentials: { ...prev.credentials, [k]: e.target.value }
                      }))}
                      className="adm-pk-input"
                    />
                    <button className="adm-pk-cred-remove" onClick={() => removeCredentialField(k)}>
                      <HiOutlineX />
                    </button>
                  </div>
                ))}
              </>
            )}
            <div className="adm-pk-form-row">
              <input
                placeholder="Key name (e.g. apiKey)"
                value={pkNewField.key}
                onChange={(e) => setPkNewField(prev => ({ ...prev, key: e.target.value }))}
                className="adm-pk-input adm-pk-input-sm"
              />
              <input
                placeholder="Value"
                type="password"
                value={pkNewField.value}
                onChange={(e) => setPkNewField(prev => ({ ...prev, value: e.target.value }))}
                className="adm-pk-input"
              />
              <button className="adm-pk-add-field" onClick={addCredentialField} disabled={!pkNewField.key.trim()}>
                <HiOutlinePlus />
              </button>
            </div>

            <div className="adm-pk-form-actions">
              <button className="adm-action-btn demote" onClick={() => { setShowPkForm(false); setPkForm({ appKey: '', appName: '', credentials: {} }); }}>
                Cancel
              </button>
              <button
                className="adm-action-btn promote"
                onClick={handleSavePlatformKey}
                disabled={pkLoading || !pkForm.appKey.trim() || Object.keys(pkForm.credentials).length === 0}
              >
                {pkLoading ? 'Saving...' : 'Save Platform Key'}
              </button>
            </div>
          </div>
        ) : (
          <button className="adm-add-pk-btn" onClick={() => setShowPkForm(true)}>
            <HiOutlinePlus /> Add Platform Key
          </button>
        )}
      </div>

      {/* ── Users Section ──────────────────────────────────────────── */}
      <div className="adm-section">
        <h2 className="adm-section-title">
          <HiOutlineUserGroup /> Users ({users.length})
        </h2>

        {loading ? (
          <div className="adm-list">
            {[1, 2, 3].map((i) => (
              <div key={i} className="adm-user-row adm-skeleton" />
            ))}
          </div>
        ) : (
          <div className="adm-list">
            <div className="adm-user-row adm-user-header">
              <span className="adm-col adm-col-email">Email</span>
              <span className="adm-col adm-col-username">Username</span>
              <span className="adm-col adm-col-role">Role</span>
              <span className="adm-col adm-col-actions">Actions</span>
            </div>

            {users.map((u, i) => (
              <motion.div
                key={u.id}
                className="adm-user-row"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.03 }}
              >
                <span className="adm-col adm-col-email">{u.email}</span>
                <span className="adm-col adm-col-username">{u.username || '—'}</span>
                <span className="adm-col adm-col-role">
                  <span className={`adm-role-badge ${u.role === 'ADMIN' ? 'admin' : ''}`}>
                    {u.role}
                  </span>
                </span>
                <span className="adm-col adm-col-actions">
                  {u.id === user?.userId ? (
                    <span className="adm-you-badge">You</span>
                  ) : u.role === 'ADMIN' ? (
                    <button
                      className="adm-action-btn demote"
                      disabled={actionLoading === u.id}
                      onClick={() => handleDemote(u.id)}
                    >
                      <HiOutlineShieldExclamation />
                      {actionLoading === u.id ? 'Demoting…' : 'Demote'}
                    </button>
                  ) : (
                    <button
                      className="adm-action-btn promote"
                      disabled={actionLoading === u.id}
                      onClick={() => handlePromote(u.id)}
                    >
                      <HiOutlineShieldCheck />
                      {actionLoading === u.id ? 'Promoting…' : 'Promote'}
                    </button>
                  )}
                </span>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
