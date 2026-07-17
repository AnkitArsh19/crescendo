import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { startRegistration, browserSupportsWebAuthn } from '@simplewebauthn/browser';
import { HiOutlineKey, HiOutlineMail, HiCheck, HiArrowLeft } from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import api from '../../api/axios';
import './Auth.css';

export default function RecoverPasskey() {
    const { theme } = useTheme();
    const [params] = useSearchParams();
    const token = params.get('token');
    const [email, setEmail] = useState('');
    const [status, setStatus] = useState('');
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);

    const requestLink = async (event) => {
        event.preventDefault();
        setBusy(true); setError('');
        try {
            await api.post('/auth/webauthn/recovery/magic-link', { email });
            setStatus('sent');
        } catch {
            // Preserve the server's non-enumerating behavior.
            setStatus('sent');
        } finally {
            setBusy(false);
        }
    };

    const recover = async () => {
        if (!browserSupportsWebAuthn()) {
            setError('Passkeys are not supported by this browser or device.');
            return;
        }
        setBusy(true); setError('');
        try {
            const { data: options } = await api.post('/auth/webauthn/recovery/register/start', { recoveryToken: token });
            const registration = await startRegistration({ optionsJSON: options });
            await api.post('/auth/webauthn/recovery/register/finish', {
                ...registration,
                transactionId: options.transactionId,
                recoveryToken: token,
                credentialName: 'Recovered passkey',
            });
            setStatus('recovered');
        } catch (err) {
            setError(err?.response?.data?.message || err?.message || 'This recovery link is invalid or has expired.');
        } finally {
            setBusy(false);
        }
    };

    const title = token ? 'Recover your passkey' : 'Lost your passkey?';
    return (
        <div className="auth-card">
            <div className="auth-logo"><img src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'} alt="Crescendo" /><span className="auth-logo-text">Crescendo</span></div>
            <div className="auth-header"><h1 className="auth-title">{title}</h1><p className="auth-subtitle">{token ? 'Add a replacement passkey to regain access.' : 'Enter your email and we’ll send a recovery link if your account is eligible.'}</p></div>
            {error && <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: 16, textAlign: 'center' }}>{error}</div>}
            {!token && status !== 'sent' && <form className="auth-form" onSubmit={requestLink}><Input label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" icon={<HiOutlineMail />} required /><button type="submit" className="auth-btn" disabled={busy}>{busy ? 'Sending…' : 'Send recovery link'}</button></form>}
            {token && status !== 'recovered' && <button type="button" className="auth-btn" disabled={busy} onClick={recover} style={{ width: '100%', display: 'inline-flex', justifyContent: 'center', gap: 8, alignItems: 'center' }}><HiOutlineKey />{busy ? 'Setting up passkey…' : 'Set up replacement passkey'}</button>}
            {status === 'sent' && <div className="auth-success"><div className="auth-success-icon"><HiCheck /></div><h2 className="auth-title">Check your email</h2><p className="auth-subtitle" style={{ marginTop: 8 }}>If this is a passkey-only account, a recovery link is on its way.</p></div>}
            {status === 'recovered' && <div className="auth-success"><div className="auth-success-icon"><HiCheck /></div><h2 className="auth-title">Passkey added</h2><p className="auth-subtitle" style={{ marginTop: 8 }}>You can now sign in with your new passkey.</p></div>}
            <div className="auth-footer" style={{ marginTop: 24 }}><Link to="/login" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}><HiArrowLeft /> Back to login</Link></div>
        </div>
    );
}
