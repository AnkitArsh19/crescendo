import { useEffect, useMemo, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
    HiCheckCircle,
    HiOutlineDesktopComputer,
    HiOutlineRefresh
} from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import { BorderBeam } from '../../components/ui/BorderBeam';
import useAuthStore from '../../store/authStore';
import './Auth.css';

export default function DesktopAuthSuccess() {
    const { theme } = useTheme();
    const [searchParams] = useSearchParams();
    const redirectedRef = useRef(false);

    const accessToken = searchParams.get('access_token') || '';
    const refreshToken = searchParams.get('refresh_token') || '';
    const expiresAt = searchParams.get('expires_at') || '';

    // Construct deep-link URI targeting Crescendo desktop app
    const deepLinkUrl = useMemo(() => {
        const params = new URLSearchParams();
        if (accessToken) params.set('access_token', accessToken);
        if (refreshToken) params.set('refresh_token', refreshToken);
        if (expiresAt) params.set('expires_at', expiresAt);
        return `crescendo://auth/callback?${params.toString()}`;
    }, [accessToken, refreshToken, expiresAt]);

    const triggerDesktopLaunch = (url) => {
        if (!url) return;
        try {
            // Method 1: direct assignment (triggers browser prompt)
            window.location.href = url;
        } catch {
            // fallback
        }

        // Method 2: hidden iframe for browsers with strict top-frame navigation guards
        try {
            const iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = url;
            document.body.appendChild(iframe);
            setTimeout(() => {
                try { iframe.remove(); } catch {}
            }, 2000);
        } catch {}
    };

    // Automatically trigger deep-link redirect on mount & ensure browser is not kept logged in
    useEffect(() => {
        // Ensure browser web store does not hold active web session
        useAuthStore.getState().logout();

        if (accessToken && !redirectedRef.current) {
            redirectedRef.current = true;
            // Short delay to allow component to render before system prompt opens
            const timer = setTimeout(() => {
                triggerDesktopLaunch(deepLinkUrl);
            }, 250);
            return () => clearTimeout(timer);
        }
    }, [deepLinkUrl, accessToken]);

    const handleManualOpen = (e) => {
        e.preventDefault();
        triggerDesktopLaunch(deepLinkUrl);
    };

    return (
        <div className="auth-card" style={{ maxWidth: 460, textAlign: 'center', padding: '40px 32px' }}>
            <BorderBeam duration={8} borderWidth={0.5} />
            <div className="auth-logo" style={{ marginBottom: 24 }}>
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo</span>
            </div>

            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.35 }}
            >
                {/* Glowing Success Badge */}
                <div style={{
                    width: 72,
                    height: 72,
                    borderRadius: '50%',
                    background: 'rgba(34, 197, 94, 0.12)',
                    border: '1px solid rgba(34, 197, 94, 0.3)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 20px',
                    color: '#22c55e'
                }}>
                    <HiCheckCircle size={44} />
                </div>

                <h1 className="auth-title" style={{ fontSize: '1.65rem', marginBottom: 8 }}>
                    Login Successful!
                </h1>

                <p className="auth-subtitle" style={{ lineHeight: 1.55, marginBottom: 28 }}>
                    You’ve successfully authenticated. We’ve prompted the <strong>Crescendo Desktop App</strong> to open.
                </p>

                {/* Primary Action Button (Direct User Click always triggers OS prompt) */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    <a
                        href={deepLinkUrl}
                        onClick={handleManualOpen}
                        className="auth-btn"
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 10,
                            textDecoration: 'none',
                            fontSize: '0.98rem',
                            padding: '13px',
                            fontWeight: 600
                        }}
                    >
                        <HiOutlineDesktopComputer size={22} />
                        Open Crescendo Desktop App
                    </a>

                    <button
                        type="button"
                        onClick={handleManualOpen}
                        className="auth-passkey-btn"
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 8,
                            fontSize: '0.86rem',
                            padding: '10px'
                        }}
                    >
                        <HiOutlineRefresh />
                        Try Opening Again
                    </button>
                </div>

                {/* Clear Instruction Banner */}
                <div style={{
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border-secondary)',
                    borderRadius: 'var(--radius-md)',
                    padding: '14px 16px',
                    margin: '24px 0 0',
                    fontSize: '0.82rem',
                    color: 'var(--text-secondary)',
                    lineHeight: 1.5,
                    textAlign: 'left'
                }}>
                    <strong>Tip:</strong> If your browser displays a prompt asking <em>"Open Crescendo?"</em>, click <strong>Open</strong> or <strong>Allow</strong>. Once the desktop app opens, you can safely close this browser window.
                </div>
            </motion.div>
        </div>
    );
}
