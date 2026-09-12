import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    HiOutlineExternalLink,
    HiOutlineRefresh,
    HiOutlineArrowLeft,
    HiOutlineClipboardCopy,
    HiCheckCircle
} from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import { BorderBeam } from '../../components/ui/BorderBeam';
import { startBrowserLogin, getBrowserLoginUrl, exchangeDesktopHandoffCode } from '../../utils/desktopAuth';
import { useNavigate } from 'react-router-dom';
import './Auth.css';

export default function DesktopAuthPrompt({ mode = 'login' }) {
    const { theme } = useTheme();
    const navigate = useNavigate();
    const [status, setStatus] = useState('idle'); // 'idle' | 'waiting' | 'success'
    const [currentMode, setCurrentMode] = useState(mode);
    const [copied, setCopied] = useState(false);
    const [manualCode, setManualCode] = useState('');
    const [manualError, setManualError] = useState('');
    const [isExchanging, setIsExchanging] = useState(false);

    // Listen for custom desktop auth complete event dispatched by useDesktopAuth deep-link handler
    useEffect(() => {
        const handleAuthComplete = () => {
            setStatus('success');
            setTimeout(() => {
                navigate('/dashboard', { replace: true, state: { justLoggedIn: true } });
            }, 600);
        };

        window.addEventListener('crescendo-desktop-auth-complete', handleAuthComplete);
        return () => window.removeEventListener('crescendo-desktop-auth-complete', handleAuthComplete);
    }, [navigate]);

    const handleLaunchBrowser = async (targetMode = currentMode) => {
        setStatus('waiting');
        try {
            await startBrowserLogin(targetMode);
        } catch (err) {
            console.error('Failed to open browser:', err);
        }
    };

    const handleCopyLink = async () => {
        const url = getBrowserLoginUrl(currentMode);
        try {
            await navigator.clipboard.writeText(url);
            setCopied(true);
            setTimeout(() => setCopied(false), 3000);
        } catch (err) {
            console.error('Failed to copy link:', err);
        }
    };

    const handleManualExchange = async (e) => {
        e.preventDefault();
        if (!manualCode.trim()) return;
        setIsExchanging(true);
        setManualError('');
        try {
            await exchangeDesktopHandoffCode(manualCode);
            setStatus('success');
            setTimeout(() => {
                navigate('/dashboard', { replace: true, state: { justLoggedIn: true } });
            }, 600);
        } catch (err) {
            setManualError(err.message || 'Invalid or expired sign-in code');
        } finally {
            setIsExchanging(false);
        }
    };

    return (
        <div className="auth-card" style={{ maxWidth: 440, padding: '36px 32px' }}>
            <BorderBeam duration={8} borderWidth={0.5} />
            <div className="auth-card-logo" style={{ marginBottom: 20 }}>
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo Desktop</span>
            </div>

            <AnimatePresence mode="wait">
                {status === 'success' ? (
                    <motion.div
                        key="success"
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ duration: 0.3 }}
                        style={{ textAlign: 'center', padding: '16px 0' }}
                    >
                        <div className="auth-success-icon" style={{ color: '#22c55e', margin: '0 auto 16px', display: 'flex', justifyContent: 'center' }}>
                            <HiCheckCircle size={48} />
                        </div>
                        <h1 className="auth-title">Authenticated!</h1>
                        <p className="auth-subtitle">
                            Launching your desktop dashboard…
                        </p>
                    </motion.div>
                ) : status === 'waiting' ? (
                    <motion.div
                        key="waiting"
                        initial={{ opacity: 0, y: 8 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -8 }}
                        transition={{ duration: 0.3 }}
                    >
                        <div className="auth-header" style={{ textAlign: 'center' }}>
                            {/* Animated Pulse Radar */}
                            <div style={{
                                width: 68,
                                height: 68,
                                borderRadius: '50%',
                                background: 'rgba(59, 130, 246, 0.1)',
                                border: '1px solid rgba(59, 130, 246, 0.3)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                margin: '0 auto 18px',
                                position: 'relative'
                            }}>
                                <motion.div
                                    animate={{ scale: [1, 1.45, 1], opacity: [0.6, 0, 0.6] }}
                                    transition={{ repeat: Infinity, duration: 2.2, ease: 'easeInOut' }}
                                    style={{
                                        position: 'absolute',
                                        inset: 0,
                                        borderRadius: '50%',
                                        background: 'rgba(59, 130, 246, 0.25)'
                                    }}
                                />
                                <HiOutlineExternalLink style={{ fontSize: '1.8rem', color: 'var(--text-accent)' }} />
                            </div>

                            <h1 className="auth-title">Complete sign-in in browser</h1>
                            <p className="auth-subtitle" style={{ marginTop: 8, lineHeight: 1.55 }}>
                                We opened Crescendo in your default browser. Once you finish signing in, this desktop app will automatically connect.
                            </p>
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 24 }}>
                            <button
                                type="button"
                                className="auth-btn"
                                onClick={() => handleLaunchBrowser()}
                                style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
                            >
                                <HiOutlineRefresh /> Reopen Browser
                            </button>

                            <button
                                type="button"
                                className="auth-copy-link-btn"
                                onClick={handleCopyLink}
                            >
                                {copied ? <HiCheckCircle style={{ color: '#22c55e', fontSize: '1.1rem' }} /> : <HiOutlineClipboardCopy style={{ fontSize: '1.1rem' }} />}
                                <span>{copied ? 'Sign-in link copied!' : 'Copy sign-in link'}</span>
                            </button>

                            {/* Manual code fallback */}
                            <div style={{
                                marginTop: 12,
                                padding: '14px',
                                background: 'var(--bg-elevated)',
                                border: '1px solid var(--border-secondary)',
                                borderRadius: 'var(--radius-md)',
                                textAlign: 'left'
                            }}>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: 8, fontWeight: 500 }}>
                                    Browser didn't redirect back?
                                </div>
                                <form onSubmit={handleManualExchange} style={{ display: 'flex', gap: 8 }}>
                                    <input
                                        type="text"
                                        value={manualCode}
                                        onChange={(e) => setManualCode(e.target.value)}
                                        placeholder="Paste 60s code or link"
                                        style={{
                                            flex: 1,
                                            padding: '8px 10px',
                                            fontSize: '0.82rem',
                                            background: 'var(--bg-primary)',
                                            border: '1px solid var(--border-secondary)',
                                            borderRadius: 'var(--radius-sm)',
                                            color: 'var(--text-primary)',
                                            outline: 'none'
                                        }}
                                    />
                                    <button
                                        type="submit"
                                        disabled={isExchanging || !manualCode.trim()}
                                        style={{
                                            padding: '8px 14px',
                                            fontSize: '0.82rem',
                                            fontWeight: 600,
                                            background: 'var(--text-accent)',
                                            color: 'var(--bg-primary)',
                                            border: 'none',
                                            borderRadius: 'var(--radius-sm)',
                                            cursor: isExchanging || !manualCode.trim() ? 'not-allowed' : 'pointer',
                                            opacity: isExchanging || !manualCode.trim() ? 0.6 : 1
                                        }}
                                    >
                                        {isExchanging ? '...' : 'Connect'}
                                    </button>
                                </form>
                                {manualError && (
                                    <div style={{ fontSize: '0.78rem', color: '#ef4444', marginTop: 6 }}>
                                        {manualError}
                                    </div>
                                )}
                            </div>

                            <button
                                type="button"
                                className="auth-passkey-btn"
                                onClick={() => setStatus('idle')}
                                style={{ justifyContent: 'center', marginTop: 4 }}
                            >
                                <HiOutlineArrowLeft /> Cancel
                            </button>
                        </div>
                    </motion.div>
                ) : (
                    /* ─────────────────────────────────────────────────────────────
                       IDLE STATE: Clean launcher button with Safe Copy Link
                       ───────────────────────────────────────────────────────────── */
                    <motion.div
                        key="idle"
                        initial={{ opacity: 0, y: 8 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -8 }}
                        transition={{ duration: 0.3 }}
                    >
                        <div className="auth-header">
                            <h1 className="auth-title">
                                {currentMode === 'register' ? 'Create your account' : 'Sign in to Crescendo'}
                            </h1>
                            <p className="auth-subtitle" style={{ lineHeight: 1.5 }}>
                                {currentMode === 'register'
                                    ? 'Set up your account in your browser to start building automated workflows on desktop.'
                                    : 'Log in securely with your default browser to access your workspaces, connections, and workflows.'}
                            </p>
                        </div>

                        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 10 }}>
                            <button
                                type="button"
                                className="auth-btn"
                                onClick={() => handleLaunchBrowser(currentMode)}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    gap: 10,
                                    fontSize: '0.94rem',
                                    padding: '12px'
                                }}
                            >
                                <HiOutlineExternalLink style={{ fontSize: '1.2rem' }} />
                                {currentMode === 'register' ? 'Register with Browser' : 'Sign In with Browser'}
                            </button>

                            <button
                                type="button"
                                className="auth-copy-link-btn"
                                onClick={handleCopyLink}
                            >
                                {copied ? <HiCheckCircle style={{ color: '#22c55e', fontSize: '1.15rem' }} /> : <HiOutlineClipboardCopy style={{ fontSize: '1.15rem' }} />}
                                <span>{copied ? 'Sign-in link copied to clipboard!' : 'Copy sign-in link'}</span>
                            </button>

                            <div style={{ textAlign: 'center', fontSize: '0.74rem', color: 'var(--text-tertiary)', lineHeight: 1.4 }}>
                                Safe: contains no tokens or passwords. Paste into any browser if it doesn't open.
                            </div>
                        </div>

                        <div className="auth-footer" style={{ marginTop: 24 }}>
                            {currentMode === 'register' ? (
                                <span>
                                    Already have an account?{' '}
                                    <button
                                        type="button"
                                        onClick={() => setCurrentMode('login')}
                                        style={{ background: 'none', border: 'none', color: 'var(--text-accent)', fontWeight: 600, cursor: 'pointer' }}
                                    >
                                        Sign in
                                    </button>
                                </span>
                            ) : (
                                <span>
                                    Don't have an account?{' '}
                                    <button
                                        type="button"
                                        onClick={() => setCurrentMode('register')}
                                        style={{ background: 'none', border: 'none', color: 'var(--text-accent)', fontWeight: 600, cursor: 'pointer' }}
                                    >
                                        Create account
                                    </button>
                                </span>
                            )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

