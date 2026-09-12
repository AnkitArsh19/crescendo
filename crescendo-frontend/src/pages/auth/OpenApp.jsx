import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
    HiOutlineDesktopComputer,
    HiOutlineClipboardCopy,
    HiCheckCircle,
    HiSun,
    HiMoon,
    HiOutlineShieldCheck
} from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import { BorderBeam } from '../../components/ui/BorderBeam';
import DotCanvas from '../../components/DotCanvas';
import './Auth.css';

export default function OpenApp() {
    const { theme, toggleTheme } = useTheme();
    const [searchParams] = useSearchParams();
    const redirectedRef = useRef(false);

    const [copiedLink, setCopiedLink] = useState(false);
    const [copiedCode, setCopiedCode] = useState(false);

    const code = searchParams.get('code') || '';
    const accessToken = searchParams.get('access_token') || searchParams.get('token') || '';
    const refreshToken = searchParams.get('refresh_token') || '';
    const expiresAt = searchParams.get('expires_at') || '';

    // Construct deep-link URI targeting Crescendo desktop app
    const deepLinkUrl = useMemo(() => {
        const params = new URLSearchParams();
        if (code) {
            params.set('code', code);
        } else {
            if (accessToken) params.set('access_token', accessToken);
            if (refreshToken) params.set('refresh_token', refreshToken);
            if (expiresAt) params.set('expires_at', expiresAt);
        }
        return `crescendo://auth/callback?${params.toString()}`;
    }, [code, accessToken, refreshToken, expiresAt]);

    // Automatically trigger deep-link prompt using standard Slack/Zoom iframe pattern
    useEffect(() => {
        if ((code || accessToken) && !redirectedRef.current) {
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
    }, [deepLinkUrl, code, accessToken]);

    const handleCopyLink = async () => {
        try {
            await navigator.clipboard.writeText(deepLinkUrl);
            setCopiedLink(true);
            setTimeout(() => setCopiedLink(false), 3000);
        } catch (err) {
            console.error('Failed to copy link:', err);
        }
    };

    const handleCopyCode = async () => {
        try {
            await navigator.clipboard.writeText(code);
            setCopiedCode(true);
            setTimeout(() => setCopiedCode(false), 3000);
        } catch (err) {
            console.error('Failed to copy code:', err);
        }
    };

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

                {/* Brand Logo with exact Funnel Display font */}
                <Link to="/" className="auth-card-logo">
                    <img
                        src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                        alt="Crescendo"
                    />
                    <span className="auth-logo-text">Crescendo</span>
                </Link>

                {/* Animated Handoff Icon */}
                <div style={{
                    width: 64,
                    height: 64,
                    borderRadius: '50%',
                    background: 'rgba(59, 130, 246, 0.1)',
                    border: '1px solid rgba(59, 130, 246, 0.3)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 20px',
                    position: 'relative'
                }}>
                    <motion.div
                        animate={{ scale: [1, 1.4, 1], opacity: [0.5, 0, 0.5] }}
                        transition={{ repeat: Infinity, duration: 2.2, ease: 'easeInOut' }}
                        style={{
                            position: 'absolute',
                            inset: 0,
                            borderRadius: '50%',
                            background: 'rgba(59, 130, 246, 0.2)'
                        }}
                    />
                    <HiOutlineDesktopComputer size={28} style={{ color: 'var(--text-accent)', zIndex: 1 }} />
                </div>

                <div className="auth-header">
                    <h1 className="auth-title">Open Crescendo Desktop</h1>
                    <p className="auth-subtitle" style={{ lineHeight: 1.55 }}>
                        You're signed in! A browser prompt should appear to open Crescendo. If it didn't, click the button below.
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
                        <span>Open Crescendo</span>
                    </a>

                    {/* Secondary Safe Copy Options */}
                    <button
                        type="button"
                        className="auth-copy-link-btn"
                        onClick={handleCopyLink}
                    >
                        {copiedLink ? <HiCheckCircle style={{ color: '#22c55e', fontSize: '1.15rem' }} /> : <HiOutlineClipboardCopy style={{ fontSize: '1.15rem' }} />}
                        <span>{copiedLink ? 'App link copied to clipboard!' : 'Copy app link'}</span>
                    </button>
                </div>

                {/* Single-Use 60s Code Display (Safe manual fallback) */}
                {code && (
                    <div style={{
                        marginTop: 20,
                        padding: '14px',
                        background: 'var(--bg-elevated)',
                        border: '1px solid var(--border-secondary)',
                        borderRadius: 'var(--radius-md)',
                        textAlign: 'left'
                    }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                            <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                                60s Sign-in Code
                            </span>
                            <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', display: 'flex', alignItems: 'center', gap: 4 }}>
                                <HiOutlineShieldCheck /> Single-use
                            </span>
                        </div>

                        <div className="auth-code-box">
                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '240px' }}>
                                {code}
                            </span>
                            <button
                                type="button"
                                onClick={handleCopyCode}
                                style={{
                                    background: 'transparent',
                                    border: 'none',
                                    color: copiedCode ? '#22c55e' : 'var(--text-accent)',
                                    cursor: 'pointer',
                                    fontSize: '0.8rem',
                                    fontWeight: 600,
                                    padding: '4px 8px'
                                }}
                            >
                                {copiedCode ? 'Copied!' : 'Copy Code'}
                            </button>
                        </div>
                        <p style={{ fontSize: '0.73rem', color: 'var(--text-tertiary)', margin: '8px 0 0', lineHeight: 1.4 }}>
                            Paste this code into the desktop app if your browser blocks protocol popups.
                        </p>
                    </div>
                )}

                <div className="auth-footer" style={{ marginTop: 20, fontSize: '0.8rem', color: 'var(--text-tertiary)' }}>
                    You can safely close this browser tab after connecting.
                </div>
            </motion.div>
        </div>
    );
}
