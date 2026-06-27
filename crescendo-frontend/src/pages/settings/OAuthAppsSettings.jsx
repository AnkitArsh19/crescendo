import { useState, useEffect } from 'react';
import { oauthAppsApi, appCatalogApi } from '../../api/appCatalogApi';
import useToastStore from '../../store/toastStore';
import { HiOutlineKey, HiOutlineTrash, HiOutlinePlus } from 'react-icons/hi';
import './Settings.css'; // Inheriting shared setting styles

export default function OAuthAppsSettings() {
    const [oauthApps, setOauthApps] = useState([]);
    const [catalogApps, setCatalogApps] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [editingApp, setEditingApp] = useState(null); // { providerKey, clientId, clientSecret, scopes }
    const addToast = useToastStore(state => state.addToast);

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [myApps, allApps] = await Promise.all([
                oauthAppsApi.list(),
                appCatalogApi.list()
            ]);
            setOauthApps(myApps);
            setCatalogApps(allApps.filter(app => app.authType === 'OAUTH2'));
        } catch (error) {
            addToast('Failed to load OAuth configurations', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (providerKey) => {
        if (!window.confirm('Are you sure you want to delete this OAuth app configuration? Current connections might stop working.')) return;
        try {
            await oauthAppsApi.delete(providerKey);
            addToast('Configuration deleted', 'success');
            fetchData();
        } catch (error) {
            addToast('Failed to delete configuration', 'error');
        }
    };

    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            await oauthAppsApi.save(editingApp);
            addToast('Configuration saved successfully', 'success');
            setEditingApp(null);
            fetchData();
        } catch (error) {
            addToast(error?.response?.data?.message || 'Failed to save configuration', 'error');
        } finally {
            setSaving(false);
        }
    };

    const startAdd = () => {
        setEditingApp({ providerKey: '', clientId: '', clientSecret: '', scopes: '' });
    };

    if (loading) {
        return <div className="settings-section">Loading...</div>;
    }

    return (
        <div className="settings-section">
            <div className="settings-header">
                <div>
                    <h2>Custom OAuth Apps</h2>
                    <p>Provide your own OAuth client credentials to bypass the platform limits.</p>
                </div>
                {!editingApp && (
                    <button className="btn-primary" onClick={startAdd}>
                        <HiOutlinePlus /> Add App
                    </button>
                )}
            </div>

            {editingApp ? (
                <div className="settings-card" style={{ padding: '24px' }}>
                    <h3 style={{ marginBottom: '16px' }}>{editingApp.id ? 'Edit' : 'Add'} OAuth App</h3>
                    <form onSubmit={handleSave}>
                        <div className="form-group" style={{ marginBottom: '16px' }}>
                            <label>Provider</label>
                            <select 
                                value={editingApp.providerKey} 
                                onChange={e => setEditingApp({...editingApp, providerKey: e.target.value})}
                                required
                                disabled={!!editingApp.id}
                                style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--border-secondary)', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
                            >
                                <option value="">Select an OAuth App...</option>
                                {catalogApps.map(app => (
                                    <option key={app.appKey} value={app.appKey}>{app.name}</option>
                                ))}
                            </select>
                        </div>
                        <div className="form-group" style={{ marginBottom: '16px' }}>
                            <label>Client ID</label>
                            <input 
                                type="text" 
                                value={editingApp.clientId} 
                                onChange={e => setEditingApp({...editingApp, clientId: e.target.value})}
                                required
                                placeholder="e.g. 123456789.apps.googleusercontent.com"
                                style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--border-secondary)', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
                            />
                        </div>
                        <div className="form-group" style={{ marginBottom: '16px' }}>
                            <label>Client Secret</label>
                            <input 
                                type="password" 
                                value={editingApp.clientSecret} 
                                onChange={e => setEditingApp({...editingApp, clientSecret: e.target.value})}
                                required={!editingApp.id}
                                placeholder={editingApp.id ? 'Leave blank to keep existing' : 'e.g. GOCSPX-...'}
                                style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--border-secondary)', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
                            />
                        </div>
                        <div className="form-group" style={{ marginBottom: '24px' }}>
                            <label>Requested Scopes (Optional)</label>
                            <input 
                                type="text" 
                                value={editingApp.scopes || ''} 
                                onChange={e => setEditingApp({...editingApp, scopes: e.target.value})}
                                placeholder="e.g. email profile https://www.googleapis.com/auth/gmail.readonly"
                                style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--border-secondary)', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
                            />
                            <small style={{ color: 'var(--text-secondary)', marginTop: '4px', display: 'block' }}>Space-separated. If left empty, default scopes will be requested.</small>
                        </div>
                        <div style={{ display: 'flex', gap: '12px' }}>
                            <button type="submit" className="btn-primary" disabled={saving}>
                                {saving ? 'Saving...' : 'Save App'}
                            </button>
                            <button type="button" className="btn-secondary" onClick={() => setEditingApp(null)}>
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            ) : (
                <div className="settings-list">
                    {oauthApps.length === 0 ? (
                        <div className="empty-state" style={{ padding: '40px', textAlign: 'center', background: 'var(--bg-secondary)', borderRadius: '8px', border: '1px solid var(--border-secondary)' }}>
                            <HiOutlineKey style={{ fontSize: '32px', color: 'var(--text-secondary)', marginBottom: '16px' }} />
                            <h3>No custom apps configured</h3>
                            <p style={{ color: 'var(--text-secondary)' }}>You are using Crescendo's shared credentials for all integrations.</p>
                        </div>
                    ) : (
                        oauthApps.map(app => {
                            const catalogApp = catalogApps.find(a => a.appKey === app.providerKey);
                            return (
                                <div key={app.id} className="settings-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px', marginBottom: '12px' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                        {catalogApp?.logoUrl && <img src={catalogApp.logoUrl} alt={catalogApp.name} style={{ width: '32px', height: '32px', objectFit: 'contain' }} />}
                                        <div>
                                            <h4 style={{ margin: '0 0 4px 0', fontSize: '1rem', color: 'var(--text-primary)' }}>{catalogApp ? catalogApp.name : app.providerKey}</h4>
                                            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Client ID: {app.clientId}</div>
                                            {app.scopes && <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Scopes: {app.scopes}</div>}
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', gap: '12px' }}>
                                        <button className="btn-icon" onClick={() => setEditingApp({...app, clientSecret: ''})} title="Edit App">
                                            Edit
                                        </button>
                                        <button className="btn-icon danger" onClick={() => handleDelete(app.providerKey)} title="Delete App">
                                            <HiOutlineTrash />
                                        </button>
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            )}
        </div>
    );
}
