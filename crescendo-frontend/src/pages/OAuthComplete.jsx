import { useEffect, useState, useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
    HiCheckCircle,
    HiOutlineDesktopComputer,
    HiOutlineClipboardCopy,
    HiSun,
    HiMoon,
    HiOutlineArrowRight
} from 'react-icons/hi';
import { useTheme } from '../components/ThemeContext';
import { BorderBeam } from '../components/ui/BorderBeam';
import DotCanvas from '../components/DotCanvas';
import './auth/Auth.css';

/**
 * OAuthComplete — handles the redirect after a third-party OAuth app authorization.
 * 
 * In standard web popup mode (window.opener exists):
 * - Posts the connection result to window.opener via postMessage and closes itself.
 * 
 * In desktop browser mode (no window.opener or opened from desktop):
 * - Auto-triggers crescendo://connections/callback deep link using hidden iframe.
 * - Displays a sleek, theme-aware handoff card matching OpenApp.jsx.
 * - Provides "Open Crescendo Desktop App" native button, "Copy Link" fallback, and web continue options.
 */
export default function OAuthComplete() {
    const { theme, toggleTheme } = useTheme();
    const [parsedData, setParsedData] = useState(null);
    const [isDesktopFlow, setIsDesktopFlow] = useState(false);
    const [copiedLink, setCopiedLink] = useState(false);
    const redirectedRef = useRef(false);

    const hash = window.location.hash.substring(1); // remove #
    const searchParams = new URLSearchParams(window.location.search);
    const oauthError = searchParams.get('error');
    const errorProvider = searchParams.get('provider');

    const deepLinkUrl = useMemo(() => {
        return `crescendo://connections/callback?data=${encodeURIComponent(hash || '')}`;
    }, [hash]);

    useEffect(() => {
        let data = null;
        try {
            if (hash) {
                const json = atob(hash.replace(/-/g, '+').replace(/_/g, '/'));
                data = JSON.parse(json);
                setParsedData(data);
            }
        } catch (e) {
            console.warn('OAuth complete: failed to parse result', e);
        }

        const fromDesktop = !window.opener || 
                            searchParams.get('from') === 'desktop' || 
                            sessionStorage.getItem('crescendo_from_desktop_oauth') === 'true';

        // Standard web popup flow
        if (window.opener) {
            try {
                window.opener.postMessage(
                    oauthError
                        ? { type: 'oauth-error', appKey: errorProvider, error: 'Crescendo could not finish this connection. Please try again.' }
                        : (data || { type: 'oauth-connected' }),
                    window.location.origin
                );
            } catch (e) {
                console.warn('Failed to postMessage to opener:', e);
            }
            window.close();
            const fallbackTimer = setTimeout(() => {
                window.location.href = '/dashboard/connections';
            }, 1500);
            return () => clearTimeout(fallbackTimer);
        }

        // Desktop browser flow
        if (fromDesktop) {
            setIsDesktopFlow(true);
            sessionStorage.removeItem('crescendo_from_desktop_oauth');

            // Automatically trigger deep-link prompt using Slack/Zoom iframe pattern
            if (!redirectedRef.current) {
                redirectedRef.current = true;
                const timer = setTimeout(() => {
                    try {
                        const iframe = document.createElement('iframe');
                        iframe.style.display = 'none';
                        iframe.src = deepLinkUrl;
                        document.body.appendChild(iframe);
                        setTimeout(() => {
                            try { iframe.remove(); } catch {}
                        }, 4000);
                    } catch (err) {
                        console.warn('Protocol auto-launch:', err);
                    }
                }, 300);
                return () => clearTimeout(timer);
            }
        }
    }, [hash, deepLinkUrl, oauthError, errorProvider]);

    const handleCopyLink = async () => {
        try {
            await navigator.clipboard.writeText(deepLinkUrl);
            setCopiedLink(true);
            setTimeout(() => setCopiedLink(false), 3000);
        } catch (err) {
            console.error('Failed to copy link:', err);
        }
    };

    const appKey = parsedData?.appKey;
    const connectionName = parsedData?.connectionName || (appKey ? `${appKey.charAt(0).toUpperCase() + appKey.slice(1)} Connection` : 'App Connection');
    const appLogo = appKey ? `/icons/${appKey}.svg` : null;

    if (isDesktopFlow) {
        return (
            <div style={{
                position: 'relative',
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '24px',
                backgroundColor: 'var(--bg-primary)',
                color: 'var(--text-primary)',
                overflow: 'hidden'
            }}>
                {/* Background Canvas */}
                <div style={{ position: 'absolute', inset: 0, zIndex: 0 }}>
                    <DotCanvas />
                </div>

                {/* Top Right Theme Toggle */}
                <button
                    className="auth-theme-toggle"
                    onClick={toggleTheme}
                    style={{ position: 'absolute', top: 24, right: 24, zIndex: 10 }}
                    aria-label="Toggle theme"
                >
                    {theme === 'dark' ? <HiSun /> : <HiMoon />}
                </button>

                {/* Centered Handoff Card */}
                <motion.div
                    className="auth-card"
                    initial={{ opacity: 0, y: 16 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
                    style={{
                        maxWidth: 440,
                        textAlign: 'center',
                        padding: '36px 32px',
                        position: 'relative',
                        zIndex: 1
                    }}
                >
                    <BorderBeam duration={8} borderWidth={0.5} />

                    {/* Brand Logo with Funnel Display font */}
                    <Link to="/" className="auth-card-logo">
                        <img
                            src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                            alt="Crescendo"
                        />
                        <span className="auth-logo-text">Crescendo</span>
                    </Link>

                    {/* Connected App Icon Badge */}
                    <div style={{
                        width: 68,
                        height: 68,
                        borderRadius: '50%',
                        background: 'rgba(34, 197, 94, 0.1)',
                        border: '1px solid rgba(34, 197, 94, 0.3)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        margin: '0 auto 20px',
                        position: 'relative'
                    }}>
                        <motion.div
                            animate={{ scale: [1, 1.35, 1], opacity: [0.5, 0, 0.5] }}
                            transition={{ repeat: Infinity, duration: 2.2, ease: 'easeInOut' }}
                            style={{
                                position: 'absolute',
                                inset: 0,
                                borderRadius: '50%',
                                background: 'rgba(34, 197, 94, 0.18)'
                            }}
                        />
                        {appLogo ? (
                            <img
                                src={appLogo}
                                alt={connectionName}
                                style={{ width: 34, height: 34, objectFit: 'contain', zIndex: 1 }}
                                onError={(e) => {
                                    e.target.style.display = 'none';
                                    const fallback = e.target.parentElement.querySelector('.fallback-icon');
                                    if (fallback) fallback.style.display = 'block';
                                }}
                            />
                        ) : null}
                        <HiCheckCircle
                            size={34}
                            className="fallback-icon"
                            style={{
                                color: '#22c55e',
                                zIndex: 1,
                                display: appLogo ? 'none' : 'block'
                            }}
                        />
                    </div>

                    <div className="auth-header">
                        <h1 className="auth-title" style={{ fontSize: '1.45rem', marginBottom: 8 }}>
                            Connected!
                        </h1>
                        <p className="auth-subtitle" style={{ lineHeight: 1.55 }}>
                            <strong>{connectionName}</strong> was authorized successfully. We’ve prompted the <strong>Crescendo Desktop App</strong> to continue.
                        </p>
                    </div>

                    {/* Primary Action: Native Anchor to trigger Chrome Protocol Modal reliably */}
                    <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 12 }}>
                        <a
                            href={deepLinkUrl}
                            className="auth-btn"
                            style={{
                                textDecoration: 'none',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: 10,
                                fontSize: '0.94rem',
                                padding: '12px'
                            }}
                        >
                            <HiOutlineDesktopComputer size={20} />
                            <span>Open Crescendo Desktop</span>
                        </a>

                        {/* Copy App Link fallback */}
                        <button
                            type="button"
                            className="auth-copy-link-btn"
                            onClick={handleCopyLink}
                        >
                            {copiedLink ? (
                                <HiCheckCircle style={{ color: '#22c55e', fontSize: '1.15rem' }} />
                            ) : (
                                <HiOutlineClipboardCopy style={{ fontSize: '1.15rem' }} />
                            )}
                            <span>{copiedLink ? 'App link copied to clipboard!' : 'Copy app link'}</span>
                        </button>
                    </div>

                    {/* Secondary Web Continue Link */}
                    <div style={{ marginTop: 16 }}>
                        <Link
                            to="/dashboard/connections"
                            style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: 6,
                                fontSize: '0.82rem',
                                color: 'var(--text-secondary)',
                                textDecoration: 'none',
                                transition: 'color 0.15s ease'
                            }}
                            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--text-primary)')}
                            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-secondary)')}
                        >
                            <span>Continue in Web Dashboard</span>
                            <HiOutlineArrowRight size={14} />
                        </Link>
                    </div>

                    <div className="auth-footer" style={{ marginTop: 24, fontSize: '0.8rem', color: 'var(--text-tertiary)' }}>
                        You can safely close this browser window after returning to Crescendo.
                    </div>
                </motion.div>
            </div>
        );
    }

    // Normal web popup closing view
    return (
        <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100vh',
            fontFamily: 'Inter, system-ui, sans-serif',
            color: 'var(--text-secondary, #888)',
            background: 'var(--bg-primary, #0a0a0a)',
        }}>
            <div style={{
                width: 32,
                height: 32,
                borderRadius: '50%',
                border: '3px solid #22c55e',
                borderTopColor: 'transparent',
                animation: 'spin 0.8s linear infinite',
                marginBottom: 16,
            }} />
            <p style={{ margin: 0, fontSize: '0.9rem' }}>Connected! Closing window…</p>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
    );
}
