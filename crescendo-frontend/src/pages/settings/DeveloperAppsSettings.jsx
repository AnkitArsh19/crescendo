import { useEffect, useState } from 'react';
import {
  HiOutlineClipboardCopy,
  HiOutlineKey,
  HiOutlinePlus,
  HiOutlineRefresh,
  HiOutlineTrash,
  HiOutlineX,
} from 'react-icons/hi';
import { developerAppsApi } from '../../api/developerApi';
import useToastStore from '../../store/toastStore';
import './Settings.css';

const AVAILABLE_SCOPES = [
  ['workflow:read', 'Read workflows'],
  ['workflow:write', 'Manage workflows'],
  ['workflow:trigger', 'Trigger workflows'],
  ['run:read', 'Read workflow runs'],
  ['run:cancel', 'Cancel workflow runs'],
  ['connection:read', 'Read connections'],
  ['connection:write', 'Manage connections'],
  ['email:send', 'Send email'],
  ['app:read', 'Read app catalog'],
  ['ai:build', 'Build AI workflow drafts'],
];

const DEFAULT_SCOPES = ['workflow:read', 'workflow:trigger', 'run:read', 'app:read'];

export default function DeveloperAppsSettings() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [secret, setSecret] = useState(null);
  const addToast = useToastStore((state) => state.addToast);

  const load = async () => {
    setLoading(true);
    try {
      setApplications(await developerAppsApi.list());
    } catch {
      addToast('Failed to load developer applications', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const rotateSecret = async (application) => {
    if (!window.confirm(`Rotate the secret for ${application.name}? Existing tokens will be revoked.`)) return;
    try {
      const result = await developerAppsApi.rotateSecret(application.id);
      setSecret({ label: `${application.name} client secret`, value: result.clientSecret });
    } catch (error) {
      addToast(error.response?.data?.message || 'Failed to rotate secret', 'error');
    }
  };

  const deactivate = async (application) => {
    if (!window.confirm(`Deactivate ${application.name} and revoke its tokens?`)) return;
    await developerAppsApi.deactivate(application.id);
    await load();
  };

  const remove = async (application) => {
    if (!window.confirm(`Permanently delete ${application.name}?`)) return;
    await developerAppsApi.delete(application.id);
    await load();
  };

  return (
    <div>
      <div className="settings-section-header">
        <div>
          <h2 className="settings-section-title">Developer API</h2>
          <p className="settings-section-desc">Applications that access Crescendo on behalf of a user.</p>
        </div>
        <button className="settings-btn-primary" onClick={() => setShowCreate(true)}>
          <HiOutlinePlus /> Register App
        </button>
      </div>

      {secret && <SecretBanner secret={secret} onClose={() => setSecret(null)} />}

      {loading ? (
        <div className="settings-skeleton-list"><div className="settings-skeleton-row" /></div>
      ) : applications.length === 0 ? (
        <div className="settings-empty">
          <HiOutlineKey className="settings-empty-icon" />
          <p>No developer applications registered.</p>
        </div>
      ) : (
        <div className="developer-app-list">
          {applications.map((application) => (
            <article className="developer-app-item" key={application.id}>
              <div className="developer-app-main">
                <div>
                  <h3>{application.name}</h3>
                  <code>{application.clientId}</code>
                </div>
                <span className={`developer-app-status ${application.active ? 'active' : ''}`}>
                  {application.active ? 'Active' : 'Inactive'}
                </span>
              </div>
              <div className="developer-app-meta">
                <span>{application.publicClient ? 'Public PKCE client' : 'Confidential client'}</span>
                <span>{application.redirectUris.length} redirect URI{application.redirectUris.length === 1 ? '' : 's'}</span>
                <span>{application.scopes.length} scope{application.scopes.length === 1 ? '' : 's'}</span>
              </div>
              <div className="developer-app-actions">
                {!application.publicClient && application.active && (
                  <button className="settings-btn-secondary" onClick={() => rotateSecret(application)}>
                    <HiOutlineRefresh /> Rotate Secret
                  </button>
                )}
                {application.active && (
                  <button className="settings-btn-secondary" onClick={() => deactivate(application)}>Deactivate</button>
                )}
                <button className="settings-icon-btn settings-danger-icon" title="Delete" onClick={() => remove(application)}>
                  <HiOutlineTrash />
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {showCreate && (
        <CreateDeveloperApp
          onClose={() => setShowCreate(false)}
          onCreated={(created) => {
            setApplications((current) => [created.application, ...current]);
            if (created.clientSecret) {
              setSecret({ label: `${created.application.name} client secret`, value: created.clientSecret });
            }
            setShowCreate(false);
          }}
        />
      )}
    </div>
  );
}

function SecretBanner({ secret, onClose }) {
  return (
    <div className="apikey-new-banner">
      <HiOutlineKey className="apikey-warn-icon" />
      <div className="apikey-new-content">
        <strong>{secret.label}. It will not be shown again.</strong>
        <code className="apikey-new-value">{secret.value}</code>
      </div>
      <button className="apikey-copy-btn" onClick={() => navigator.clipboard.writeText(secret.value)}>
        <HiOutlineClipboardCopy /> Copy
      </button>
      <button className="settings-icon-btn" onClick={onClose}><HiOutlineX /></button>
    </div>
  );
}

function CreateDeveloperApp({ onClose, onCreated }) {
  const [name, setName] = useState('');
  const [publicClient, setPublicClient] = useState(false);
  const [redirectUris, setRedirectUris] = useState('http://localhost:3000/callback');
  const [scopes, setScopes] = useState(DEFAULT_SCOPES);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const toggleScope = (scope) => {
    setScopes((current) =>
      current.includes(scope) ? current.filter((item) => item !== scope) : [...current, scope]);
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      const result = await developerAppsApi.create({
        name: name.trim(),
        publicClient,
        redirectUris: redirectUris.split('\n').map((uri) => uri.trim()).filter(Boolean),
        scopes,
      });
      onCreated(result);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Failed to register application');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="conn-modal-backdrop" onClick={onClose}>
      <form className="conn-modal developer-app-modal" onSubmit={submit} onClick={(event) => event.stopPropagation()}>
        <div className="conn-modal-header">
          <h2>Register Application</h2>
          <button type="button" className="conn-modal-close" onClick={onClose}><HiOutlineX /></button>
        </div>
        <div className="conn-modal-body">
          {error && <div className="conn-modal-error">{error}</div>}
          <label className="conn-form-label">
            Application Name
            <input className="conn-form-input" value={name} onChange={(event) => setName(event.target.value)} required autoFocus />
          </label>
          <div className="conn-form-label">
            Client Type
            <div className="developer-client-type">
              <button type="button" className={!publicClient ? 'active' : ''} onClick={() => setPublicClient(false)}>Confidential</button>
              <button type="button" className={publicClient ? 'active' : ''} onClick={() => setPublicClient(true)}>Public PKCE</button>
            </div>
          </div>
          <label className="conn-form-label">
            Redirect URIs
            <textarea className="conn-form-input developer-uri-input" value={redirectUris} onChange={(event) => setRedirectUris(event.target.value)} required />
          </label>
          <fieldset className="developer-scope-fieldset">
            <legend>Scopes</legend>
            <div className="developer-scope-grid">
              {AVAILABLE_SCOPES.map(([scope, label]) => (
                <label key={scope}>
                  <input type="checkbox" checked={scopes.includes(scope)} onChange={() => toggleScope(scope)} />
                  <span><strong>{label}</strong><code>{scope}</code></span>
                </label>
              ))}
            </div>
          </fieldset>
        </div>
        <div className="conn-modal-footer">
          <button type="button" className="conn-btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="conn-btn-primary" disabled={saving || scopes.length === 0}>
            {saving ? 'Registering...' : 'Register App'}
          </button>
        </div>
      </form>
    </div>
  );
}
