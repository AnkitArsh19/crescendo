import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { HiOutlineMail, HiOutlineLockClosed, HiOutlineEye, HiOutlineEyeOff, HiOutlineShieldCheck, HiOutlineKey, HiOutlineDesktopComputer } from 'react-icons/hi';
import { FcGoogle } from 'react-icons/fc';
import { SiGithub } from 'react-icons/si';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/authStore';
import api from '../../api/axios';
import { beginPasskeyAutofill, cancelPasskeyRequest, loginWithPasskey, passkeysSupported } from '../../api/passkeys';
import { getDeviceMetadata } from '../../utils/deviceFingerprint';
import { BorderBeam } from '../../components/ui/BorderBeam';
import { isTauri } from '../../utils/platform';
import { redirectToDesktopHandoff } from '../../utils/desktopAuth';
import DesktopAuthPrompt from './DesktopAuthPrompt';
import './Auth.css';

const loginSchema = z.object({
    email: z.string().email('Please enter a valid email address'),
    password: z.string().min(1, 'Password is required'),
    rememberMe: z.boolean().default(false),
});

const mfaSchema = z.object({
    code: z.string().length(6, 'MFA code must be exactly 6 digits').regex(/^\d+$/, 'MFA code must contain only numbers'),
});

export default function Login() {
    if (isTauri()) {
        return <DesktopAuthPrompt mode="login" />;
    }

    const { theme } = useTheme();
    const navigate = useNavigate();
    const location = useLocation();
    const searchParams = new URLSearchParams(location.search);

    const isFromDesktop = searchParams.get('from') === 'desktop';
    if (isFromDesktop && typeof sessionStorage !== 'undefined') {
        sessionStorage.setItem('crescendo_from_desktop', 'true');
    }
    const fromDesktop = isFromDesktop || (typeof sessionStorage !== 'undefined' && sessionStorage.getItem('crescendo_from_desktop') === 'true');

    const destination = fromDesktop
        ? '/auth/desktop-success'
        : (location.state?.from
            ? `${location.state.from.pathname || location.state.from}${location.state.from.search || ''}`
            : '/dashboard');

    const redirectedPath = location.state?.from?.pathname || (typeof location.state?.from === 'string' ? location.state.from : '');
    const isStudioRedirect = redirectedPath.startsWith('/dashboard');

    const loginFn = useAuthStore((state) => state.login);
    const verifyMfaFn = useAuthStore((state) => state.verifyMfa);
    const verifyBackupCode = useAuthStore((state) => state.useBackupCode);
    const currentUser = useAuthStore((state) => state.user);
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const isLoading = useAuthStore((state) => state.isLoading);
    const currentAccessToken = useAuthStore((state) => state.accessToken);
    const currentRefreshToken = useAuthStore((state) => state.refreshToken);
    const currentExpiresAt = useAuthStore((state) => state.accessExpiresAt);

    const [showPw, setShowPw] = useState(false);
    const [isMfaStep, setIsMfaStep] = useState(false);
    const [isBackupCodeMode, setIsBackupCodeMode] = useState(false);
    const [backupRemaining, setBackupRemaining] = useState(null);
    const [savedEmail, setSavedEmail] = useState('');
    const [globalError, setGlobalError] = useState('');
    const [isPasskeyLogin, setIsPasskeyLogin] = useState(false);

    const defaultEmail = typeof localStorage !== 'undefined' ? localStorage.getItem('crescendo_remembered_email') || '' : '';
    const defaultRemember = typeof localStorage !== 'undefined' ? Boolean(localStorage.getItem('crescendo_remembered_email')) : false;

    const { register: registerLogin, handleSubmit: handleLoginSubmit, watch, formState: { errors: loginErrors, isSubmitting: isLoggingIn } } = useForm({
        resolver: zodResolver(loginSchema),
        defaultValues: {
            email: defaultEmail,
            password: '',
            rememberMe: defaultRemember,
        },
    });

    const { register: registerMfa, handleSubmit: handleMfaSubmit, formState: { errors: mfaErrors, isSubmitting: isVerifyingMfa } } = useForm({
        resolver: zodResolver(mfaSchema),
    });

    const backupSchema = z.object({ backupCode: z.string().min(8, 'Backup code must be at least 8 characters') });
    const { register: registerBackup, handleSubmit: handleBackupSubmit, formState: { errors: backupErrors, isSubmitting: isUsingBackup } } = useForm({
        resolver: zodResolver(backupSchema),
    });

    useEffect(() => {
        if (!isLoading && isAuthenticated && !fromDesktop) {
            navigate(destination, { replace: true });
        }
    }, [isAuthenticated, isLoading, fromDesktop, destination, navigate]);

    useEffect(() => {
        let active = true;

        beginPasskeyAutofill()
            .then((signedIn) => {
                if (active && signedIn) navigate(destination, { replace: true });
            })
            .catch(() => { });

        return () => {
            active = false;
            cancelPasskeyRequest();
        };
    }, [destination, navigate, location.state]);

    const onLogin = async (data) => {
        setGlobalError('');
        if (data.rememberMe) {
            localStorage.setItem('crescendo_remembered_email', data.email);
        } else {
            localStorage.removeItem('crescendo_remembered_email');
        }

        // When signing in on behalf of the desktop app, do NOT establish a web session in the browser.
        if (fromDesktop) {
            try {
                const { deviceId, deviceLabel } = getDeviceMetadata();
                const response = await api.post('/auth/login', {
                    email: data.email,
                    password: data.password,
                    rememberMe: false,
                    deviceId,
                    deviceLabel
                });

                if (response.status === 202) {
                    setSavedEmail(data.email);
                    setIsMfaStep(true);
                    return;
                }

                const { accessToken, refreshToken, accessExpiresAt } = response.data;
                useAuthStore.getState().setTokens(accessToken, accessExpiresAt, refreshToken);
                useAuthStore.getState().checkAuth().catch(() => {});
                await redirectToDesktopHandoff(navigate, accessToken);
                return;
            } catch (error) {
                if (error.response?.status === 401) {
                    setGlobalError('Invalid email or password');
                } else {
                    setGlobalError(error.response?.data?.message || 'Failed to log in');
                }
                return;
            }
        }

        // Regular browser login flow
        try {
            const res = await loginFn(data.email, data.password, data.rememberMe);
            if (res.mfaRequired) {
                setSavedEmail(data.email);
                setIsMfaStep(true);
            } else {
                navigate(destination, { replace: true, state: { ...location.state, justLoggedIn: true } });
            }
        } catch (error) {
            setGlobalError(error.message);
        }
    };

    const onMfa = async (data) => {
        setGlobalError('');
        if (fromDesktop) {
            try {
                const { deviceId, deviceLabel } = getDeviceMetadata();
                const response = await api.post('/mfa/challenge', {
                    email: savedEmail,
                    code: data.code,
                    deviceId,
                    deviceLabel
                });
                if (response.data.success) {
                    const { accessToken, refreshToken, accessExpiresAt } = response.data;
                    useAuthStore.getState().setTokens(accessToken, accessExpiresAt, refreshToken);
                    useAuthStore.getState().checkAuth().catch(() => {});
                    await redirectToDesktopHandoff(navigate, accessToken);
                    return;
                } else {
                    setGlobalError('Invalid 2FA code');
                }
            } catch (error) {
                setGlobalError(error.response?.data?.message || error.message || 'Failed to verify 2FA');
            }
            return;
        }

        try {
            await verifyMfaFn(savedEmail, data.code);
            navigate(destination, { replace: true, state: { ...location.state, justLoggedIn: true } });
        } catch (error) {
            setGlobalError(error.message);
        }
    };

    const onBackupCode = async (data) => {
        setGlobalError('');
        if (fromDesktop) {
            try {
                const { deviceId, deviceLabel } = getDeviceMetadata();
                const response = await api.post('/mfa/backup/verify', {
                    email: savedEmail,
                    backupCode: data.backupCode,
                    deviceId,
                    deviceLabel
                });
                if (response.data.success) {
                    const { accessToken, refreshToken, accessExpiresAt } = response.data;
                    useAuthStore.getState().setTokens(accessToken, accessExpiresAt, refreshToken);
                    useAuthStore.getState().checkAuth().catch(() => {});
                    await redirectToDesktopHandoff(navigate, accessToken);
                    return;
                }
            } catch (error) {
                setGlobalError(error.response?.data?.message || 'Invalid backup code');
            }
            return;
        }

        try {
            const result = await verifyBackupCode(savedEmail, data.backupCode);
            if (result.remaining !== undefined && result.remaining <= 2) {
                setBackupRemaining(result.remaining);
            }
            navigate(destination, { replace: true, state: { ...location.state, justLoggedIn: true } });
        } catch (error) {
            setGlobalError(error.message);
        }
    };

    const onPasskeyLogin = async () => {
        setGlobalError('');
        setIsPasskeyLogin(true);
        try {
            const tokens = await loginWithPasskey(watch('email') || '');
            if (fromDesktop && tokens?.accessToken) {
                useAuthStore.getState().setTokens(tokens.accessToken, tokens.accessExpiresAt, tokens.refreshToken);
                useAuthStore.getState().checkAuth().catch(() => {});
                await redirectToDesktopHandoff(navigate, tokens.accessToken);
                return;
            }
            navigate(destination, { replace: true, state: { ...location.state, justLoggedIn: true } });
        } catch (error) {
            setGlobalError(error.message);
        } finally {
            setIsPasskeyLogin(false);
        }
    };

    const handleOAuthLogin = (provider) => {
        if (fromDesktop) {
            sessionStorage.setItem('crescendo_from_desktop', 'true');
            document.cookie = 'crescendo_from_desktop=true; path=/; max-age=600; SameSite=Lax';
        }
        const { deviceId, deviceLabel } = getDeviceMetadata();
        document.cookie = `crescendo_device_id_transfer=${encodeURIComponent(deviceId)}; path=/; max-age=300; SameSite=Lax`;
        document.cookie = `crescendo_device_label_transfer=${encodeURIComponent(deviceLabel)}; path=/; max-age=300; SameSite=Lax`;
        // Append ?from=desktop so the backend OAuth success handler can detect desktop origin
        // even if the SameSite=Lax cookie doesn't arrive (belt-and-suspenders).
        const desktopParam = fromDesktop ? '?from=desktop' : '';
        window.location.href = `https://api.crescendo.run/oauth2/authorization/${provider}${desktopParam}`;
    };

    return (
        <div className="auth-card">
            <BorderBeam duration={8} borderWidth={0.5} />
            <Link to="/" className="auth-logo">
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo</span>
            </Link>

            {/* If from desktop and already logged in, offer 1-click connect */}
            {fromDesktop && isAuthenticated && currentUser && currentAccessToken ? (
                <div style={{ textAlign: 'center', padding: '8px 0 16px' }}>
                    <div className="auth-header" style={{ marginBottom: 20 }}>
                        <h1 className="auth-title">Connect Crescendo Desktop</h1>
                        <p className="auth-subtitle">
                            You're currently signed in to Crescendo in this browser.
                        </p>
                    </div>

                    <div style={{
                        background: 'var(--bg-elevated)',
                        border: '1px solid var(--border-secondary)',
                        borderRadius: 'var(--radius-md)',
                        padding: '16px',
                        marginBottom: '20px',
                        textAlign: 'left'
                    }}>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', marginBottom: 4 }}>
                            Signed in account:
                        </div>
                        <div style={{ fontWeight: 600, fontSize: '0.96rem', color: 'var(--text-primary)' }}>
                            {currentUser.email || currentUser.username}
                        </div>
                    </div>

                    <button
                        type="button"
                        className="auth-btn"
                        style={{
                            width: '100%',
                            padding: '12px',
                            fontSize: '0.95rem',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 10
                        }}
                        onClick={async () => {
                            await redirectToDesktopHandoff(navigate, currentAccessToken);
                        }}
                    >
                        <HiOutlineDesktopComputer size={20} />
                        Connect to Desktop App
                    </button>

                    <div style={{ marginTop: 18 }}>
                        <button
                            type="button"
                            onClick={() => useAuthStore.getState().logout()}
                            style={{
                                background: 'none',
                                border: 'none',
                                color: 'var(--text-accent)',
                                fontSize: '0.82rem',
                                cursor: 'pointer',
                                textDecoration: 'underline'
                            }}
                        >
                            Sign in with a different account
                        </button>
                    </div>
                </div>
            ) : (
                <>
                    <div className="auth-header">
                        <h1 className="auth-title">{isMfaStep ? 'Two-Factor Authentication' : 'Welcome back'}</h1>
                        <p className="auth-subtitle">
                            {isMfaStep
                                ? 'Enter the 6-digit code from your authenticator app'
                                : fromDesktop
                                    ? 'Sign in to connect your Crescendo desktop app'
                                    : isStudioRedirect
                                        ? 'Sign in to open your Workflow Studio'
                                        : 'Sign in to your account to continue'}
                        </p>
                    </div>

                    {fromDesktop && !isMfaStep && (
                        <div style={{
                            background: 'rgba(59, 130, 246, 0.08)',
                            border: '1px solid rgba(59, 130, 246, 0.25)',
                            borderRadius: '8px',
                            padding: '10px 14px',
                            marginBottom: '16px',
                            fontSize: '0.8rem',
                            color: 'var(--text-secondary)',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 8
                        }}>
                            <HiOutlineDesktopComputer style={{ color: 'var(--text-accent)', flexShrink: 0 }} size={18} />
                            <span>Authenticating for Crescendo Desktop. Once signed in, you will be redirected back to the desktop application.</span>
                        </div>
                    )}

                    {globalError && (
                        <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '16px', textAlign: 'center', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '6px' }}>
                            {globalError}
                        </div>
                    )}

                    {!isMfaStep ? (
                        <form className="auth-form" onSubmit={handleLoginSubmit(onLogin)}>
                            <Input
                                label="Email"
                                type="email"
                                placeholder="Enter your email"
                                icon={<HiOutlineMail />}
                                autoComplete="username webauthn"
                                {...registerLogin('email')}
                                error={loginErrors.email?.message}
                            />

                            <Input
                                label="Password"
                                type={showPw ? 'text' : 'password'}
                                placeholder="Enter your password"
                                icon={<HiOutlineLockClosed />}
                                rightIcon={showPw ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                                onRightIconClick={() => setShowPw(!showPw)}
                                autoComplete="current-password"
                                {...registerLogin('password')}
                                error={loginErrors.password?.message}
                            />

                            <div className="auth-form-row">
                                <label className="auth-remember">
                                    <input type="checkbox" {...registerLogin('rememberMe')} />
                                    Remember me
                                </label>
                                <Link to="/reset-password" className="auth-forgot">Forgot password?</Link>
                            </div>

                            <button type="submit" className="auth-btn" disabled={isLoggingIn}>
                                {isLoggingIn ? 'Logging in...' : 'Log in'}
                            </button>

                            <button
                                type="button"
                                className="auth-passkey-btn"
                                onClick={onPasskeyLogin}
                                disabled={!passkeysSupported() || isPasskeyLogin}
                                title={passkeysSupported() ? 'Use a passkey, security key, or another device' : 'Passkeys are not supported by this browser'}
                            >
                                <HiOutlineKey />
                                <span>{isPasskeyLogin ? 'Checking for passkeys…' : 'Continue with a passkey'}</span>
                            </button>
                            <p className="auth-passkey-hint">Use your screen lock, security key, or a nearby phone.</p>

                            <div className="auth-divider">
                                <span className="auth-divider-line" />
                                <span className="auth-divider-text">or continue with</span>
                                <span className="auth-divider-line" />
                            </div>

                            <div style={{ display: 'flex', gap: '10px' }}>
                                <button
                                    type="button"
                                    className="auth-oauth-btn"
                                    onClick={() => handleOAuthLogin('google')}
                                    style={{ flex: 1 }}
                                >
                                    <span className="auth-oauth-icon"><FcGoogle /></span>
                                    Google
                                </button>
                                <button
                                    type="button"
                                    className="auth-oauth-btn"
                                    onClick={() => handleOAuthLogin('github')}
                                    style={{ flex: 1, color: 'var(--text-primary)' }}
                                >
                                    <span className="auth-oauth-icon"><SiGithub /></span>
                                    GitHub
                                </button>
                            </div>

                            <div className="auth-footer">
                                <div>Don't have an account? <Link to={fromDesktop ? "/register?from=desktop" : "/register"}>Sign up</Link> <span className="auth-footer-separator">•</span> <Link to="/auth/recover-passkey">Recover passkey</Link></div>
                                {!fromDesktop && (
                                    <button
                                        type="button"
                                        className="auth-guest-link"
                                        onClick={() => { useAuthStore.getState().enterGuestMode(); navigate('/dashboard'); }}
                                    >
                                        Continue as guest
                                    </button>
                                )}
                            </div>
                        </form>
                    ) : isBackupCodeMode ? (
                        <form className="auth-form" onSubmit={handleBackupSubmit(onBackupCode)}>
                            <div style={{ background: 'rgba(234,179,8,0.1)', border: '1px solid rgba(234,179,8,0.4)', borderRadius: '8px', padding: '12px 14px', marginBottom: '16px', fontSize: '0.83rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                                &#9888;&#65039; Backup codes are <strong>single-use</strong>. Each code can only be used once.
                                {backupRemaining !== null && ` You have ${backupRemaining} backup code${backupRemaining !== 1 ? 's' : ''} remaining.`}
                            </div>
                            <Input
                                label="Backup Code"
                                type="text"
                                placeholder="e.g. ABCD-1234-EFGH"
                                icon={<HiOutlineShieldCheck />}
                                {...registerBackup('backupCode')}
                                error={backupErrors.backupCode?.message}
                                autoComplete="off"
                            />
                            <button type="submit" className="auth-btn" disabled={isUsingBackup} style={{ marginTop: '10px' }}>
                                {isUsingBackup ? 'Verifying...' : 'Use Backup Code'}
                            </button>
                            <div className="auth-footer" style={{ marginTop: '20px', display: 'flex', justifyContent: 'space-between' }}>
                                <button type="button" onClick={() => { setIsBackupCodeMode(false); setGlobalError(''); }} style={{ background: 'none', border: 'none', color: 'var(--text-accent)', cursor: 'pointer', fontSize: '0.85rem' }}>
                                    Use authenticator app instead
                                </button>
                                <button type="button" onClick={() => { setIsMfaStep(false); setIsBackupCodeMode(false); setGlobalError(''); }} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '0.85rem' }}>
                                    Back to login
                                </button>
                            </div>
                        </form>
                    ) : (
                        <form className="auth-form" onSubmit={handleMfaSubmit(onMfa)}>
                            <Input
                                label="Verification Code"
                                type="text"
                                placeholder="123456"
                                icon={<HiOutlineShieldCheck />}
                                {...registerMfa('code')}
                                error={mfaErrors.code?.message}
                                autoComplete="off"
                                maxLength="6"
                            />

                            <button type="submit" className="auth-btn" disabled={isVerifyingMfa} style={{ marginTop: '10px' }}>
                                {isVerifyingMfa ? 'Verifying...' : 'Verify & Connect Desktop'}
                            </button>

                            <div className="auth-footer" style={{ marginTop: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <button type="button" onClick={() => { setIsBackupCodeMode(true); setGlobalError(''); }} style={{ background: 'none', border: 'none', color: 'var(--text-accent)', cursor: 'pointer', fontSize: '0.85rem' }}>
                                    Lost access? Use backup code
                                </button>
                                <button type="button" onClick={() => { setIsMfaStep(false); setGlobalError(''); }} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '0.85rem' }}>
                                    Back to login
                                </button>
                            </div>
                        </form>
                    )}
                </>
            )}
        </div>
    );
}
