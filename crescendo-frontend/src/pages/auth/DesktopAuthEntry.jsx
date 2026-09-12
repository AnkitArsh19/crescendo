import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { HiSun, HiMoon, HiOutlineDesktopComputer } from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import { BorderBeam } from '../../components/ui/BorderBeam';
import DotCanvas from '../../components/DotCanvas';
import useAuthStore from '../../store/authStore';
import { redirectToDesktopHandoff } from '../../utils/desktopAuth';
import Login from './Login';
import Register from './Register';
import './Auth.css';

export default function DesktopAuthEntry() {
    const { theme, toggleTheme } = useTheme();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const mode = searchParams.get('mode') || 'login';

    const checkAuth = useAuthStore((state) => state.checkAuth);
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const user = useAuthStore((state) => state.user);
    const accessToken = useAuthStore((state) => state.accessToken);

    const [isChecking, setIsChecking] = useState(true);
    const [isTransferring, setIsTransferring] = useState(false);

    useEffect(() => {
        // Mark session so any subsequent callbacks know we're authenticating for desktop
        sessionStorage.setItem('crescendo_from_desktop', 'true');

        checkAuth()
            .catch(() => {})
            .finally(() => {
                setIsChecking(false);
            });
    }, [checkAuth]);

    const handleContinueWithActiveSession = async () => {
        setIsTransferring(true);
        try {
            await redirectToDesktopHandoff(navigate);
        } finally {
            setIsTransferring(false);
        }
    };

    const handleSwitchAccount = () => {
        useAuthStore.getState().logout();
    };

    if (isChecking) {
        return (
            <div style={{
                position: 'fixed',
                inset: 0,
                backgroundColor: 'var(--bg-primary)',
                color: 'var(--text-secondary)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 16
            }}>
                <div style={{
                    width: 36,
                    height: 36,
                    border: '2px solid var(--border-secondary)',
                    borderTopColor: 'var(--text-accent)',
                    borderRadius: '50%',
                    animation: 'spin 0.8s linear infinite'
                }} />
                <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
                <span style={{ fontSize: '0.92rem', fontFamily: 'var(--font-sans)' }}>
                    Verifying session…
                </span>
            </div>
        );
    }

    // 1. If browser already has a verified active session on this domain:
    // Offer 1-click connect to desktop app
    if (isAuthenticated && (user || accessToken)) {
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

                {/* Theme Toggle */}
                <button
                    className="auth-theme-toggle"
                    onClick={toggleTheme}
                    style={{ position: 'absolute', top: 24, right: 24, zIndex: 10 }}
                    aria-label="Toggle theme"
                >
                    {theme === 'dark' ? <HiSun /> : <HiMoon />}
                </button>

                {/* 1-Click Streamlined Authorization Card */}
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

                    <div className="auth-header" style={{ marginBottom: 20 }}>
                        <h1 className="auth-title">Connect to Desktop</h1>
                        <p className="auth-subtitle" style={{ lineHeight: 1.55 }}>
                            You're signed in on this browser. Continue to connect your Crescendo desktop app.
                        </p>
                    </div>

                    {/* User Profile Card */}
                    <div style={{
                        background: 'var(--bg-elevated)',
                        border: '1px solid var(--border-secondary)',
                        borderRadius: 'var(--radius-md)',
                        padding: '14px 16px',
                        marginBottom: '20px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        textAlign: 'left'
                    }}>
                        <div style={{
                            width: 40,
                            height: 40,
                            borderRadius: '50%',
                            background: 'var(--text-accent)',
                            color: 'var(--bg-primary)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontWeight: 700,
                            fontSize: '1rem',
                            flexShrink: 0
                        }}>
                            {(user?.username || user?.email || 'U')[0].toUpperCase()}
                        </div>
                        <div style={{ overflow: 'hidden' }}>
                            <div style={{ fontSize: '0.92rem', fontWeight: 600, color: 'var(--text-primary)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                                {user?.username || user?.name || 'Crescendo User'}
                            </div>
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                                {user?.email}
                            </div>
                        </div>
                    </div>

                    <button
                        type="button"
                        className="auth-btn"
                        onClick={handleContinueWithActiveSession}
                        disabled={isTransferring}
                        style={{
                            width: '100%',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 10,
                            padding: '12px',
                            fontSize: '0.94rem'
                        }}
                    >
                        <HiOutlineDesktopComputer size={20} />
                        <span>{isTransferring ? 'Connecting…' : `Continue as ${user?.username || user?.name || 'User'}`}</span>
                    </button>

                    <button
                        type="button"
                        onClick={handleSwitchAccount}
                        style={{
                            background: 'none',
                            border: 'none',
                            color: 'var(--text-secondary)',
                            fontSize: '0.82rem',
                            marginTop: '16px',
                            cursor: 'pointer',
                            textDecoration: 'underline'
                        }}
                    >
                        Sign in with a different account
                    </button>
                </motion.div>
            </div>
        );
    }

    // 2. Otherwise, render login or registration inside centered theme container
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
            <div style={{ position: 'absolute', inset: 0, zIndex: 0 }}>
                <DotCanvas />
            </div>

            <button
                className="auth-theme-toggle"
                onClick={toggleTheme}
                style={{ position: 'absolute', top: 24, right: 24, zIndex: 10 }}
                aria-label="Toggle theme"
            >
                {theme === 'dark' ? <HiSun /> : <HiMoon />}
            </button>

            <div style={{ width: '100%', maxWidth: '440px', position: 'relative', zIndex: 1 }}>
                {mode === 'register' ? <Register /> : <Login />}
            </div>
        </div>
    );
}
