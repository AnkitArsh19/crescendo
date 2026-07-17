import { useState, useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
    HiOutlineLockClosed,
    HiOutlineEye,
    HiOutlineEyeOff,
    HiOutlineShieldCheck,
    HiOutlineClipboardCopy,
    HiOutlineDownload,
    HiOutlineKey,
    HiOutlineTrash,
    HiCheck,
    HiOutlineDesktopComputer,
    HiOutlineDeviceMobile,
    HiOutlineLocationMarker,
    HiOutlineClock,
} from 'react-icons/hi';
import Input from '../../components/ui/Input';
import Toggle from '../../components/ui/Toggle';
import Stepper from '../../components/ui/Stepper';
import useAuthStore from '../../store/authStore';
import api from '../../api/axios';
import { sessionsApi } from '../../api/sessionsApi';
import { passkeysSupported, registerPasskey } from '../../api/passkeys';
import ReAuthModal from '../../components/ReAuthModal';
import './Settings.css';

const mfaSteps = ['Scan QR', 'Verify Code', 'Backup Codes'];

function getStrength(pw) {
    if (!pw) return 0;
    let s = 0;
    if (pw.length >= 6) s++;
    if (pw.length >= 10) s++;
    if (/[A-Z]/.test(pw) && /[0-9]/.test(pw)) s++;
    if (/[^A-Za-z0-9]/.test(pw)) s++;
    return Math.min(s, 4);
}

const strengthLabels = ['', 'Weak', 'Fair', 'Good', 'Strong'];

export default function SecuritySettings() {
    const { user, checkAuth } = useAuthStore();

    // -- Change Password State (for users who already have a password) --
    const [showOldPw, setShowOldPw] = useState(false);
    const [showNewPw, setShowNewPw] = useState(false);
    const [oldPw, setOldPw] = useState('');
    const [newPw, setNewPw] = useState('');
    const [confirmPw, setConfirmPw] = useState('');
    const strength = getStrength(newPw);
    const [pwMsg, setPwMsg] = useState({ type: '', text: '' });
    const [isChangingPw, setIsChangingPw] = useState(false);

    // -- Set Password State (OAuth-only users with no password yet) --
    const [showSetNewPw, setShowSetNewPw] = useState(false);
    const [setNewPw1, setSetNewPw1] = useState('');
    const [setNewPw2, setSetNewPw2] = useState('');
    const setPasswordStrength = getStrength(setNewPw1);
    const [setPasswordMsg, setSetPasswordMsg] = useState({ type: '', text: '' });
    const [isSettingPw, setIsSettingPw] = useState(false);

    // -- MFA State --
    const [mfaEnabled, setMfaEnabled] = useState(false);
    const [mfaSetupOpen, setMfaSetupOpen] = useState(false);
    const [mfaStep, setMfaStep] = useState(0);
    const [verifyCode, setVerifyCode] = useState(['', '', '', '', '', '']);
    const [copied, setCopied] = useState(false);

    // MFA API data
    const [qrData, setQrData] = useState(null);
    const [backupCodes, setBackupCodes] = useState([]);
    const [mfaError, setMfaError] = useState('');
    const [isSendingMfa, setIsSendingMfa] = useState(false);

    // -- Passkey State --
    const [passkeys, setPasskeys] = useState([]);
    const [passkeyStatus, setPasskeyStatus] = useState({ type: '', text: '' });
    const [isLoadingPasskeys, setIsLoadingPasskeys] = useState(true);
    const [isAddingPasskey, setIsAddingPasskey] = useState(false);

    // -- Step-up Auth State --
    const [reAuthAction, setReAuthAction] = useState(null); // 'add' | { type: 'remove', passkey } | { type: 'revokeSession', session }

    // -- Sessions State --
    const [sessions, setSessions] = useState([]);
    const [isLoadingSessions, setIsLoadingSessions] = useState(true);
    const [sessionStatus, setSessionStatus] = useState({ type: '', text: '' });

    // Sync local toggle state with actual user object on mount
    useEffect(() => {
        if (user?.mfa?.enabled) {
            setMfaEnabled(true);
        }
    }, [user]);

    const loadPasskeys = async () => {
        try {
            const { data } = await api.get('/auth/webauthn/credentials');
            setPasskeys(data);
        } catch {
            setPasskeyStatus({ type: 'error', text: 'Could not load your passkeys.' });
        } finally {
            setIsLoadingPasskeys(false);
        }
    };

    const loadSessions = async () => {
        try {
            const data = await sessionsApi.getSessions();
            setSessions(data);
        } catch {
            setSessionStatus({ type: 'error', text: 'Could not load your active sessions.' });
        } finally {
            setIsLoadingSessions(false);
        }
    };

    useEffect(() => {
        loadPasskeys();
        loadSessions();
    }, []);

    const addPasskey = async (elevatedToken) => {
        const credentialName = window.prompt('Name this passkey (for example, "Work MacBook")', 'Passkey');
        if (credentialName === null) return;
        setPasskeyStatus({ type: '', text: '' });
        setIsAddingPasskey(true);
        try {
            await registerPasskey(credentialName.trim() || 'Passkey', elevatedToken);
            setPasskeyStatus({ type: 'success', text: 'Passkey added successfully.' });
            await loadPasskeys();
        } catch (error) {
            setPasskeyStatus({ type: 'error', text: error.message });
        } finally {
            setIsAddingPasskey(false);
        }
    };

    const removePasskey = async (passkey, elevatedToken) => {
        setPasskeyStatus({ type: '', text: '' });
        try {
            await api.delete(`/auth/webauthn/credentials/${passkey.id}`, {
                headers: { 'X-Elevated-Token': elevatedToken }
            });
            setPasskeys((current) => current.filter((item) => item.id !== passkey.id));
            setPasskeyStatus({ type: 'success', text: 'Passkey removed.' });
        } catch (error) {
            setPasskeyStatus({ type: 'error', text: error.response?.data?.message || 'Could not remove this passkey.' });
        }
    };

    const handleReAuthSuccess = (elevatedToken) => {
        const action = reAuthAction;
        setReAuthAction(null);
        if (action === 'add') {
            addPasskey(elevatedToken);
        } else if (action?.type === 'remove') {
            removePasskey(action.passkey, elevatedToken);
        } else if (action?.type === 'revokeSession') {
            revokeSession(action.session, elevatedToken);
        }
    };

    const revokeSession = async (session, elevatedToken) => {
        setSessionStatus({ type: '', text: '' });
        try {
            await sessionsApi.revokeSession(session.sessionId);
            setSessions(sessions.filter(s => s.sessionId !== session.sessionId));
            setSessionStatus({ type: 'success', text: 'Session revoked successfully.' });
        } catch (error) {
            setSessionStatus({ type: 'error', text: 'Could not revoke session.' });
        }
    };

    const revokeAllSessions = async () => {
        if (!window.confirm('Sign out everywhere? You will be signed out of all other devices.')) return;
        setSessionStatus({ type: '', text: '' });
        try {
            await sessionsApi.revokeAllSessions();
            setSessions(sessions.filter(s => s.isCurrent));
            setSessionStatus({ type: 'success', text: 'All other sessions revoked.' });
        } catch (error) {
            setSessionStatus({ type: 'error', text: 'Could not revoke all sessions.' });
        }
    };

    // Handler: change existing password
    const handlePwChange = async (e) => {
        e.preventDefault();
        setPwMsg({ type: '', text: '' });

        if (newPw !== confirmPw) {
            return setPwMsg({ type: 'error', text: 'Passwords do not match' });
        }
        if (newPw.length < 8) {
            return setPwMsg({ type: 'error', text: 'Password must be at least 8 characters' });
        }

        setIsChangingPw(true);
        try {
            await api.patch('/auth/change-password', { oldPassword: oldPw, newPassword: newPw });
            setPwMsg({ type: 'success', text: 'Password updated successfully' });
            setOldPw('');
            setNewPw('');
            setConfirmPw('');
        } catch (error) {
            setPwMsg({ type: 'error', text: error.response?.data?.message || 'Failed to change password' });
        } finally {
            setIsChangingPw(false);
        }
    };

    // Handler: set a first password (OAuth-only accounts)
    const handleSetPassword = async (e) => {
        e.preventDefault();
        setSetPasswordMsg({ type: '', text: '' });

        if (setNewPw1.length < 8) {
            return setSetPasswordMsg({ type: 'error', text: 'Password must be at least 8 characters' });
        }
        if (setNewPw1 !== setNewPw2) {
            return setSetPasswordMsg({ type: 'error', text: 'Passwords do not match' });
        }

        setIsSettingPw(true);
        try {
            await api.post('/users/me/password', { password: setNewPw1 });
            setSetPasswordMsg({ type: 'success', text: 'Password set! You can now use it to log in or add a passkey.' });
            setSetNewPw1('');
            setSetNewPw2('');
            // Re-sync user state so hasLocalCredential flips to true
            await checkAuth();
        } catch (error) {
            setSetPasswordMsg({ type: 'error', text: error.response?.data?.message || 'Failed to set password' });
        } finally {
            setIsSettingPw(false);
        }
    };

    const handleMfaToggle = async (val) => {
        setMfaError('');
        if (val && !mfaEnabled) {
            // Start MFA enrollment
            try {
                const res = await api.post('/mfa/enroll/start');
                setQrData(res.data);
                setMfaSetupOpen(true);
                setMfaStep(0);
            } catch {
                setMfaError('Failed to start MFA setup');
            }
        } else if (!val && mfaEnabled) {
            // Disable MFA
            try {
                await api.patch('/mfa/toggle', { enabled: false });
                setMfaEnabled(false);
                setMfaSetupOpen(false);
                await checkAuth(); // Resync user object
            } catch {
                setMfaError('Failed to disable MFA');
            }
        }
    };

    const handleVerifyDigit = (index, value) => {
        if (value.length > 1) return;
        const next = [...verifyCode];
        next[index] = value;
        setVerifyCode(next);
        if (value && index < 5) {
            const el = document.getElementById(`mfa-digit-${index + 1}`);
            el?.focus();
        }
    };

    const completeMfa = async () => {
        const code = verifyCode.join('');
        if (code.length !== 6) return;

        setMfaError('');
        setIsSendingMfa(true);
        try {
            const res = await api.post('/mfa/enroll/confirm', { code: parseInt(code, 10) });
            setBackupCodes(res.data.backupCodesPlain);
            setMfaEnabled(true);
            setMfaStep(2);
            await checkAuth(); // Resync user object
        } catch (error) {
            setMfaError(error.response?.data?.message || 'Invalid code. Please try again.');
        } finally {
            setIsSendingMfa(false);
        }
    };

    const regenerateBackupCodes = async () => {
        try {
            const res = await api.post('/mfa/backup-codes/regenerate');
            setBackupCodes(res.data.backupCodesPlain);
            setMfaSetupOpen(true);
            setMfaStep(2);
        } catch {
            setMfaError('Failed to regenerate backup codes');
        }
    };

    const finishSetup = () => {
        setMfaSetupOpen(false);
        setMfaStep(0);
        setVerifyCode(['', '', '', '', '', '']);
        setBackupCodes([]);
        setQrData(null);
    };

    const handleCopy = () => {
        navigator.clipboard?.writeText(backupCodes.join('\n'));
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleDownload = () => {
        const text = backupCodes.join('\n');
        const blob = new Blob([text], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'crescendo-backup-codes.txt';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    return (
        <>
            <ReAuthModal
                isOpen={!!reAuthAction}
                onClose={() => setReAuthAction(null)}
                onSuccess={handleReAuthSuccess}
            />

            {/* ── Password Section ── */}
            {user?.hasLocalCredential === false ? (
                // OAuth-only user: no password yet — show "Set Password" form
                <div className="settings-section">
                    <h2 className="settings-section-title">Set a Password</h2>
                    <p className="settings-section-desc">
                        You signed up with a social account and don&apos;t have a password yet.
                        Setting one lets you log in with email + password and manage passkeys.
                    </p>

                    <form className="settings-form" onSubmit={handleSetPassword}>
                        <Input
                            label="New password"
                            type={showSetNewPw ? 'text' : 'password'}
                            placeholder="Create a password"
                            icon={<HiOutlineLockClosed />}
                            rightIcon={showSetNewPw ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                            onRightIconClick={() => setShowSetNewPw(!showSetNewPw)}
                            value={setNewPw1}
                            onChange={(e) => setSetNewPw1(e.target.value)}
                            autoComplete="new-password"
                            required
                        />

                        {setNewPw1 && (
                            <>
                                <div className="password-strength">
                                    {[1, 2, 3, 4].map((i) => (
                                        <div
                                            key={i}
                                            className={`password-strength-bar ${i <= setPasswordStrength
                                                ? setPasswordStrength <= 1 ? 'weak' : setPasswordStrength <= 2 ? 'medium' : 'strong'
                                                : ''
                                            }`}
                                        />
                                    ))}
                                </div>
                                <span className="password-strength-text" style={{ marginBottom: 16, display: 'block' }}>
                                    {strengthLabels[setPasswordStrength]}
                                </span>
                            </>
                        )}

                        <Input
                            label="Confirm new password"
                            type="password"
                            placeholder="Re-enter your password"
                            icon={<HiOutlineLockClosed />}
                            value={setNewPw2}
                            onChange={(e) => setSetNewPw2(e.target.value)}
                            autoComplete="new-password"
                            required
                        />

                        {setPasswordMsg.text && (
                            <div style={{
                                padding: '10px',
                                borderRadius: '6px',
                                fontSize: '0.85rem',
                                color: setPasswordMsg.type === 'error' ? '#ef4444' : '#10b981',
                                background: setPasswordMsg.type === 'error' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
                                marginBottom: '16px'
                            }}>
                                {setPasswordMsg.text}
                            </div>
                        )}

                        <button type="submit" className="settings-btn settings-btn-primary" disabled={isSettingPw}>
                            {isSettingPw ? 'Setting password...' : 'Set Password'}
                        </button>
                    </form>
                </div>
            ) : (
                // User already has a password — show "Change Password" form
                <div className="settings-section">
                    <h2 className="settings-section-title">Change Password</h2>
                    <p className="settings-section-desc">
                        Update your password to keep your account secure.
                    </p>

                    <form className="settings-form" onSubmit={handlePwChange}>
                        <Input
                            label="Current password"
                            type={showOldPw ? 'text' : 'password'}
                            placeholder="Enter current password"
                            icon={<HiOutlineLockClosed />}
                            rightIcon={showOldPw ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                            onRightIconClick={() => setShowOldPw(!showOldPw)}
                            value={oldPw}
                            onChange={(e) => setOldPw(e.target.value)}
                            autoComplete="current-password"
                            required
                        />

                        <Input
                            label="New password"
                            type={showNewPw ? 'text' : 'password'}
                            placeholder="Enter new password"
                            icon={<HiOutlineLockClosed />}
                            rightIcon={showNewPw ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                            onRightIconClick={() => setShowNewPw(!showNewPw)}
                            value={newPw}
                            onChange={(e) => setNewPw(e.target.value)}
                            autoComplete="new-password"
                            required
                        />

                        {newPw && (
                            <>
                                <div className="password-strength">
                                    {[1, 2, 3, 4].map((i) => (
                                        <div
                                            key={i}
                                            className={`password-strength-bar ${i <= strength
                                                ? strength <= 1 ? 'weak' : strength <= 2 ? 'medium' : 'strong'
                                                : ''
                                            }`}
                                        />
                                    ))}
                                </div>
                                <span className="password-strength-text" style={{ marginBottom: 16, display: 'block' }}>
                                    {strengthLabels[strength]}
                                </span>
                            </>
                        )}

                        <Input
                            label="Confirm new password"
                            type="password"
                            placeholder="Re-enter new password"
                            icon={<HiOutlineLockClosed />}
                            value={confirmPw}
                            onChange={(e) => setConfirmPw(e.target.value)}
                            autoComplete="new-password"
                            required
                        />

                        {pwMsg.text && (
                            <div style={{
                                padding: '10px',
                                borderRadius: '6px',
                                fontSize: '0.85rem',
                                color: pwMsg.type === 'error' ? '#ef4444' : '#10b981',
                                background: pwMsg.type === 'error' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
                                marginBottom: '16px'
                            }}>
                                {pwMsg.text}
                            </div>
                        )}

                        <button type="submit" className="settings-btn settings-btn-primary" disabled={isChangingPw}>
                            {isChangingPw ? 'Updating...' : 'Update Password'}
                        </button>
                    </form>
                </div>
            )}

            <div className="settings-divider" />

            {/* ── Passkeys ── */}
            <div className="settings-section">
                <div className="settings-section-header">
                    <div>
                        <h2 className="settings-section-title">Passkeys</h2>
                        <p className="settings-section-desc" style={{ marginBottom: 0 }}>
                            Sign in securely with your device&apos;s screen lock, fingerprint, or security key. A passkey satisfies sign-in verification, so no separate TOTP code is required.
                        </p>
                    </div>
                    <button
                        type="button"
                        className="settings-btn settings-btn-primary"
                        disabled={!passkeysSupported() || isAddingPasskey}
                        onClick={() => setReAuthAction('add')}
                        title={passkeysSupported() ? undefined : 'Passkeys are not supported by this browser'}
                    >
                        <HiOutlineKey /> {isAddingPasskey ? 'Adding…' : 'Add passkey'}
                    </button>
                </div>

                {!passkeysSupported() && (
                    <p className="mfa-desc" style={{ marginBottom: 14 }}>
                        Passkeys require a supported browser on a secure (HTTPS) connection.
                    </p>
                )}
                {passkeyStatus.text && (
                    <div style={{ color: passkeyStatus.type === 'error' ? '#ef4444' : '#10b981', fontSize: '0.85rem', marginBottom: 14, padding: 10, borderRadius: 6, background: passkeyStatus.type === 'error' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)' }}>
                        {passkeyStatus.text}
                    </div>
                )}
                <div className="settings-table passkey-table">
                    <div className="settings-table-head">
                        <span>Passkey</span><span>Type</span><span>Last used</span><span>Added</span><span aria-label="Actions" />
                    </div>
                    {isLoadingPasskeys ? (
                        <div className="settings-skeleton-row" />
                    ) : passkeys.length === 0 ? (
                        <div className="settings-empty" style={{ padding: '28px 20px' }}>
                            <HiOutlineKey className="settings-empty-icon" />
                            <p>No passkeys yet. Add one to sign in without a password.</p>
                        </div>
                    ) : passkeys.map((passkey) => (
                        <div className="settings-table-row" key={passkey.id}>
                            <span className="settings-table-cell-name">{passkey.name || 'Passkey'}</span>
                            <span className="settings-table-cell-code">{passkey.backedUp ? 'Synced' : 'Device-bound'}</span>
                            <span className="settings-table-cell-date">{passkey.lastUsedAt ? new Date(passkey.lastUsedAt).toLocaleDateString() : 'Never'}</span>
                            <span className="settings-table-cell-date">{new Date(passkey.createdAt).toLocaleDateString()}</span>
                            <button type="button" className="settings-icon-btn settings-danger-icon" onClick={() => {
                                if (window.confirm(`Remove "${passkey.name || 'this passkey'}"? You will no longer be able to sign in with it.`)) {
                                    setReAuthAction({ type: 'remove', passkey });
                                }
                            }} aria-label={`Remove ${passkey.name || 'passkey'}`}><HiOutlineTrash /></button>
                        </div>
                    ))}
                </div>
            </div>

            <div className="settings-divider" />

            {/* ── Active Sessions ── */}
            <div className="settings-section">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                        <h2 className="settings-section-title">Active Sessions</h2>
                        <p className="settings-section-desc">
                            These are the devices that are currently logged into your account.
                        </p>
                    </div>
                    {sessions.length > 1 && (
                        <button className="settings-btn settings-btn-danger" onClick={revokeAllSessions}>
                            Sign out everywhere
                        </button>
                    )}
                </div>

                {sessionStatus.text && (
                    <div style={{
                        marginTop: '16px', padding: '10px', borderRadius: '6px', fontSize: '0.85rem',
                        color: sessionStatus.type === 'error' ? '#ef4444' : '#10b981',
                        background: sessionStatus.type === 'error' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)'
                    }}>
                        {sessionStatus.text}
                    </div>
                )}

                <div className="settings-table-list" style={{ marginTop: '20px' }}>
                    {isLoadingSessions ? (
                        <div style={{ padding: '20px', textAlign: 'center', color: '#6b7280' }}>Loading sessions...</div>
                    ) : sessions.length === 0 ? (
                        <div style={{ padding: '20px', textAlign: 'center', color: '#6b7280' }}>No active sessions found.</div>
                    ) : (
                        sessions.map((session) => (
                            <div className="settings-table-row" key={session.sessionId}>
                                <div className="settings-table-cell-icon" style={{ color: '#4b5563' }}>
                                    {session.deviceLabel?.toLowerCase().includes('mobile') || session.deviceLabel?.toLowerCase().includes('iphone') || session.deviceLabel?.toLowerCase().includes('android') ? <HiOutlineDeviceMobile size={22} /> : <HiOutlineDesktopComputer size={22} />}
                                </div>
                                <div className="settings-table-cell-main">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <span className="settings-table-cell-title">{session.deviceLabel || 'Unknown Device'}</span>
                                        {session.isCurrent && (
                                            <span style={{ fontSize: '0.7rem', padding: '2px 6px', background: '#e0e7ff', color: '#4338ca', borderRadius: '4px', fontWeight: 500 }}>
                                                This device
                                            </span>
                                        )}
                                    </div>
                                    <div style={{ display: 'flex', gap: '16px', marginTop: '4px', color: '#6b7280', fontSize: '0.8rem' }}>
                                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                            <HiOutlineLocationMarker /> {session.approximateLocation || 'Unknown Location'}
                                        </span>
                                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                            <HiOutlineClock /> Active {new Date(session.lastUsedAt).toLocaleDateString()}
                                        </span>
                                    </div>
                                </div>
                                {!session.isCurrent && (
                                    <button 
                                        type="button" 
                                        className="settings-icon-btn settings-danger-icon" 
                                        onClick={() => setReAuthAction({ type: 'revokeSession', session })}
                                        aria-label="Revoke Session"
                                    >
                                        <HiOutlineTrash />
                                    </button>
                                )}
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* ── Multi-Factor Authentication ── */}
            <div className="settings-section">
                <h2 className="settings-section-title">Multi-Factor Authentication</h2>
                <p className="settings-section-desc">
                    Add an extra layer of security to your account by requiring a verification code in addition to your password.
                </p>

                {mfaError && (
                    <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '16px', padding: '10px', borderRadius: '6px', background: 'rgba(239, 68, 68, 0.1)' }}>
                        {mfaError}
                    </div>
                )}

                <div className="mfa-section">
                    <div className="mfa-header">
                        <div>
                            <span className="mfa-title">
                                <HiOutlineShieldCheck style={{ marginRight: 6, verticalAlign: 'middle' }} />
                                Two-Factor Authentication
                            </span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                            <span className={`settings-badge ${mfaEnabled ? 'settings-badge-enabled' : 'settings-badge-disabled'}`}>
                                {mfaEnabled ? 'Enabled' : 'Disabled'}
                            </span>
                            <Toggle checked={mfaEnabled || mfaSetupOpen} onChange={handleMfaToggle} />
                        </div>
                    </div>
                    <p className="mfa-desc">
                        Use an authenticator app like Google Authenticator or Authy to generate verification codes.
                    </p>

                    <AnimatePresence mode="wait">
                        {mfaSetupOpen && (
                            <motion.div
                                initial={{ opacity: 0, height: 0 }}
                                animate={{ opacity: 1, height: 'auto' }}
                                exit={{ opacity: 0, height: 0 }}
                                transition={{ duration: 0.3 }}
                                style={{ overflow: 'hidden' }}
                            >
                                <Stepper steps={mfaSteps} currentStep={mfaStep} />

                                <AnimatePresence mode="wait">
                                    {/* Step 0: Scan QR */}
                                    {mfaStep === 0 && qrData && (
                                        <motion.div
                                            key="step-0"
                                            initial={{ opacity: 0, x: -10 }}
                                            animate={{ opacity: 1, x: 0 }}
                                            exit={{ opacity: 0, x: 10 }}
                                            transition={{ duration: 0.25 }}
                                        >
                                            <div className="mfa-qr-container">
                                                <div className="mfa-qr-placeholder" style={{ border: 'none', background: 'white', padding: 10 }}>
                                                    <img src={qrData.qrImageDataUri} alt="MFA QR Code" width="160" height="160" style={{ display: 'block' }} />
                                                </div>
                                                <span className="mfa-qr-label">
                                                    Scan this QR code with your authenticator app
                                                </span>
                                            </div>

                                            <div style={{ textAlign: 'center', marginBottom: 16 }}>
                                                <span className="mfa-qr-label">Or enter this secret key manually:</span>
                                            </div>
                                            <div className="mfa-secret">
                                                <code style={{ letterSpacing: '2px', fontSize: '1rem' }}>{qrData.secret}</code>
                                                <button
                                                    className="settings-btn settings-btn-secondary"
                                                    style={{ padding: '4px 10px', fontSize: '0.72rem' }}
                                                    onClick={() => {
                                                        navigator.clipboard?.writeText(qrData.secret);
                                                        setCopied(true);
                                                        setTimeout(() => setCopied(false), 2000);
                                                    }}
                                                >
                                                    {copied ? <HiCheck /> : <HiOutlineClipboardCopy />}
                                                </button>
                                            </div>

                                            <div style={{ marginTop: 20, display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
                                                <button
                                                    className="settings-btn settings-btn-secondary"
                                                    onClick={() => setMfaSetupOpen(false)}
                                                >
                                                    Cancel
                                                </button>
                                                <button
                                                    className="settings-btn settings-btn-primary"
                                                    onClick={() => setMfaStep(1)}
                                                >
                                                    Continue
                                                </button>
                                            </div>
                                        </motion.div>
                                    )}

                                    {/* Step 1: Verify */}
                                    {mfaStep === 1 && (
                                        <motion.div
                                            key="step-1"
                                            initial={{ opacity: 0, x: -10 }}
                                            animate={{ opacity: 1, x: 0 }}
                                            exit={{ opacity: 0, x: 10 }}
                                            transition={{ duration: 0.25 }}
                                        >
                                            <p className="mfa-desc" style={{ textAlign: 'center' }}>
                                                Enter the 6-digit verification code from your authenticator app
                                            </p>

                                            <div className="mfa-verify-input">
                                                {verifyCode.map((d, i) => (
                                                    <input
                                                        key={i}
                                                        id={`mfa-digit-${i}`}
                                                        className="mfa-verify-digit"
                                                        type="text"
                                                        inputMode="numeric"
                                                        maxLength={1}
                                                        value={d}
                                                        onChange={(e) => handleVerifyDigit(i, e.target.value)}
                                                        autoComplete="off"
                                                    />
                                                ))}
                                            </div>

                                            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 20 }}>
                                                <button
                                                    className="settings-btn settings-btn-secondary"
                                                    onClick={() => setMfaStep(0)}
                                                    disabled={isSendingMfa}
                                                >
                                                    Back
                                                </button>
                                                <button
                                                    className="settings-btn settings-btn-primary"
                                                    onClick={completeMfa}
                                                    disabled={isSendingMfa || verifyCode.join('').length !== 6}
                                                >
                                                    {isSendingMfa ? 'Verifying...' : 'Verify & Enable'}
                                                </button>
                                            </div>
                                        </motion.div>
                                    )}

                                    {/* Step 2: Backup Codes */}
                                    {mfaStep === 2 && backupCodes.length > 0 && (
                                        <motion.div
                                            key="step-2"
                                            initial={{ opacity: 0, x: -10 }}
                                            animate={{ opacity: 1, x: 0 }}
                                            exit={{ opacity: 0, x: 10 }}
                                            transition={{ duration: 0.25 }}
                                        >
                                            <div style={{ padding: '16px', background: 'rgba(16, 185, 129, 0.1)', color: '#10b981', borderRadius: '8px', marginBottom: '20px', textAlign: 'center', fontSize: '0.9rem' }}>
                                                <HiCheck style={{ verticalAlign: 'middle', marginRight: '6px', fontSize: '1.2rem' }} />
                                                Two-Factor Authentication is now enabled!
                                            </div>
                                            <p className="mfa-desc" style={{ textAlign: 'center' }}>
                                                Save these backup codes in a safe place. You can use each code once if you lose access to your authenticator app.
                                            </p>

                                            <div className="backup-codes-grid">
                                                {backupCodes.map((code) => (
                                                    <div className="backup-code" key={code}>{code}</div>
                                                ))}
                                            </div>

                                            <div className="backup-codes-actions">
                                                <button className="settings-btn settings-btn-secondary" onClick={handleCopy}>
                                                    <HiOutlineClipboardCopy style={{ marginRight: 4 }} />
                                                    {copied ? 'Copied!' : 'Copy All'}
                                                </button>
                                                <button className="settings-btn settings-btn-secondary" onClick={handleDownload}>
                                                    <HiOutlineDownload style={{ marginRight: 4 }} />
                                                    Download
                                                </button>
                                            </div>

                                            <div style={{ marginTop: 20, display: 'flex', justifyContent: 'flex-end' }}>
                                                <button
                                                    className="settings-btn settings-btn-primary"
                                                    onClick={finishSetup}
                                                >
                                                    Done
                                                </button>
                                            </div>
                                        </motion.div>
                                    )}
                                </AnimatePresence>
                            </motion.div>
                        )}
                    </AnimatePresence>

                    {/* Show regenerate / disable when MFA enabled and setup closed */}
                    {mfaEnabled && !mfaSetupOpen && (
                        <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
                            <button
                                className="settings-btn settings-btn-secondary"
                                onClick={regenerateBackupCodes}
                            >
                                Regenerate Backup Codes
                            </button>
                            <button
                                className="settings-btn settings-btn-danger"
                                onClick={() => handleMfaToggle(false)}
                            >
                                Disable MFA
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}
