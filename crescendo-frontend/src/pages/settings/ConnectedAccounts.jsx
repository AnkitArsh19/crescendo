import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FcGoogle } from 'react-icons/fc';
import { SiGithub } from 'react-icons/si';
import Modal from '../../components/ui/Modal';
import useAuthStore from '../../store/authStore';
import api from '../../api/axios';
import './Settings.css';

const providerMeta = {
    GOOGLE: { name: 'Google', icon: <FcGoogle /> },
    GITHUB: { name: 'GitHub', icon: <SiGithub /> },
};

const allProviders = ['GOOGLE', 'GITHUB'];

export default function ConnectedAccounts() {
    const { user, checkAuth } = useAuthStore();
    const linked = user?.linkedAccounts || [];
    const [revokeModal, setRevokeModal] = useState(null);
    const [statusMsg, setStatusMsg] = useState('');
    const [successMsg, setSuccessMsg] = useState('');
    const [searchParams, setSearchParams] = useSearchParams();

    // Handle query params from the OAuth link redirect
    useEffect(() => {
        const linked = searchParams.get('linked');
        const error = searchParams.get('error');

        if (linked) {
            setSuccessMsg(`${linked.charAt(0).toUpperCase() + linked.slice(1)} connected successfully!`);
            checkAuth(); // refresh user data to show new linked account
            setSearchParams({}, { replace: true }); // clean URL
        } else if (error) {
            const messages = {
                provider_linked_to_other_account: 'This account is already linked to a different user.',
                user_not_found: 'Session expired. Please log in again.',
            };
            setStatusMsg(messages[error] || 'Failed to link provider.');
            setSearchParams({}, { replace: true });
        }
    }, [searchParams, setSearchParams, checkAuth]);

    const findLinked = (provider) => linked.find(a => a.provider === provider);

    const handleRevoke = async (provider) => {
        setStatusMsg('');
        setSuccessMsg('');
        try {
            await api.post('/users/me/unlink-provider', { provider });
            await checkAuth();
            setRevokeModal(null);
        } catch (error) {
            setStatusMsg(error.response?.data?.message || 'Failed to unlink provider');
            setRevokeModal(null);
        }
    };

    const handleConnect = async (provider) => {
        setStatusMsg('');
        setSuccessMsg('');
        try {
            // Step 1: Tell the backend to set the link cookie
            await api.post('/users/me/link-provider/init', { provider });
            // Step 2: Navigate to the OAuth authorization URL
            window.location.href = `https://api.crescendo.run/oauth2/authorization/${provider.toLowerCase()}`;
        } catch (error) {
            setStatusMsg(error.response?.data?.message || 'Failed to start provider linking');
        }
    };

    return (
        <>
            <div className="settings-section">
                <h2 className="settings-section-title">Connected Accounts</h2>
                <p className="settings-section-desc">
                    Manage third-party accounts linked to your Crescendo profile for single sign-on and integrations.
                </p>

                {successMsg && (
                    <div style={{ padding: '10px', borderRadius: '6px', fontSize: '0.85rem', color: '#10b981', background: 'rgba(16, 185, 129, 0.1)', marginBottom: 16 }}>
                        {successMsg}
                    </div>
                )}

                {statusMsg && (
                    <div style={{ padding: '10px', borderRadius: '6px', fontSize: '0.85rem', color: '#ef4444', background: 'rgba(239, 68, 68, 0.1)', marginBottom: 16 }}>
                        {statusMsg}
                    </div>
                )}

                {allProviders.map((provider) => {
                    const meta = providerMeta[provider];
                    const account = findLinked(provider);
                    const connected = !!account;

                    return (
                        <div className="connected-account-card" key={provider}>
                            <div className="connected-account-icon">{meta.icon}</div>
                            <div className="connected-account-info">
                                <div className="connected-account-name">{meta.name}</div>
                                <div className="connected-account-email">
                                    {connected ? account.providerEmail : 'Not connected'}
                                </div>
                                {connected && account.linkedAt && (
                                    <div className="connected-account-email" style={{ marginTop: 2, fontSize: '0.7rem' }}>
                                        Connected {new Date(account.linkedAt).toLocaleDateString()}
                                    </div>
                                )}
                            </div>
                            <div className="connected-account-actions">
                                {connected ? (
                                    <>
                                        <span className="settings-badge settings-badge-enabled">Connected</span>
                                        <button
                                            className="settings-btn settings-btn-danger"
                                            style={{ fontSize: '0.78rem', padding: '6px 14px' }}
                                            onClick={() => setRevokeModal({ provider, name: meta.name })}
                                        >
                                            Revoke
                                        </button>
                                    </>
                                ) : (
                                    <button
                                        className="settings-btn settings-btn-primary"
                                        style={{ fontSize: '0.78rem', padding: '6px 14px' }}
                                        onClick={() => handleConnect(provider)}
                                    >
                                        Connect
                                    </button>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Revoke confirmation modal */}
            <Modal
                open={!!revokeModal}
                onClose={() => setRevokeModal(null)}
                title="Revoke Access"
                description={
                    revokeModal
                        ? `Are you sure you want to disconnect ${revokeModal.name}? You'll no longer be able to sign in with this account.`
                        : ''
                }
            >
                <div className="modal-actions">
                    <button
                        className="modal-btn modal-btn-secondary"
                        onClick={() => setRevokeModal(null)}
                    >
                        Cancel
                    </button>
                    <button
                        className="modal-btn modal-btn-danger"
                        onClick={() => handleRevoke(revokeModal.provider)}
                    >
                        Revoke Access
                    </button>
                </div>
            </Modal>
        </>
    );
}
