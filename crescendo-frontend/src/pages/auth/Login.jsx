import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { HiOutlineMail, HiOutlineLockClosed, HiOutlineEye, HiOutlineEyeOff, HiOutlineShieldCheck, HiOutlineKey } from 'react-icons/hi';
import { FcGoogle } from 'react-icons/fc';
import { SiGithub } from 'react-icons/si';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/authStore';
import { beginPasskeyAutofill, cancelPasskeyRequest, loginWithPasskey, passkeysSupported } from '../../api/passkeys';
import { getDeviceMetadata } from '../../utils/deviceFingerprint';
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
    const { theme } = useTheme();
    const navigate = useNavigate();
    const location = useLocation();
    const destination = location.state?.from
        ? `${location.state.from.pathname}${location.state.from.search || ''}`
        : '/dashboard';
    const loginFn = useAuthStore((state) => state.login);
    const verifyMfaFn = useAuthStore((state) => state.verifyMfa);
    const verifyBackupCode = useAuthStore((state) => state.useBackupCode);
    
    const [showPw, setShowPw] = useState(false);
    const [isMfaStep, setIsMfaStep] = useState(false);
    const [isBackupCodeMode, setIsBackupCodeMode] = useState(false);
    const [backupRemaining, setBackupRemaining] = useState(null);
    const [savedEmail, setSavedEmail] = useState('');
    const [globalError, setGlobalError] = useState('');
    const [isPasskeyLogin, setIsPasskeyLogin] = useState(false);

    const { register: registerLogin, handleSubmit: handleLoginSubmit, watch, formState: { errors: loginErrors, isSubmitting: isLoggingIn } } = useForm({
        resolver: zodResolver(loginSchema),
    });

    const { register: registerMfa, handleSubmit: handleMfaSubmit, formState: { errors: mfaErrors, isSubmitting: isVerifyingMfa } } = useForm({
        resolver: zodResolver(mfaSchema),
    });

    const backupSchema = z.object({ backupCode: z.string().min(8, 'Backup code must be at least 8 characters') });
    const { register: registerBackup, handleSubmit: handleBackupSubmit, formState: { errors: backupErrors, isSubmitting: isUsingBackup } } = useForm({
        resolver: zodResolver(backupSchema),
    });

    useEffect(() => {
        let active = true;

        beginPasskeyAutofill()
            .then((signedIn) => {
                if (active && signedIn) navigate(destination, { replace: true });
            })
            // Conditional UI is a progressive enhancement. A failed background
            // request must never prevent the ordinary password sign-in flow.
            .catch(() => {});

        return () => {
            active = false;
            cancelPasskeyRequest();
        };
    }, [destination, navigate, location.state]);

    const onLogin = async (data) => {
        setGlobalError('');
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
        try {
            await verifyMfaFn(savedEmail, data.code);
            navigate(destination, { replace: true, state: { ...location.state, justLoggedIn: true } });
        } catch (error) {
            setGlobalError(error.message);
        }
    };

    const onBackupCode = async (data) => {
        setGlobalError('');
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
            // When email is blank, a discoverable/resident passkey lets the device
            // select the account. With an email entered, only that account's keys
            // are offered.
            await loginWithPasskey(watch('email') || '');
            navigate(destination, { replace: true, state: { ...location.state, justLoggedIn: true } });
        } catch (error) {
            setGlobalError(error.message);
        } finally {
            setIsPasskeyLogin(false);
        }
    };

    const handleOAuthLogin = (provider) => {
        const { deviceId, deviceLabel } = getDeviceMetadata();
        document.cookie = `crescendo_device_id_transfer=${encodeURIComponent(deviceId)}; path=/; max-age=300; SameSite=Lax`;
        document.cookie = `crescendo_device_label_transfer=${encodeURIComponent(deviceLabel)}; path=/; max-age=300; SameSite=Lax`;
        window.location.href = `https://api.crescendo.run/oauth2/authorization/${provider}`;
    };

    return (
        <div className="auth-card">
            <Link to="/" className="auth-logo">
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo</span>
            </Link>

            <div className="auth-header">
                <h1 className="auth-title">{isMfaStep ? 'Two-Factor Authentication' : 'Welcome back'}</h1>
                <p className="auth-subtitle">
                    {isMfaStep ? 'Enter the 6-digit code from your authenticator app' : 'Sign in to your account to continue'}
                </p>
            </div>

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
                        <div>Don't have an account? <Link to="/register">Sign up</Link> <span className="auth-footer-separator">•</span> <Link to="/auth/recover-passkey">Recover passkey</Link></div>
                        <button
                            type="button"
                            className="auth-guest-link"
                            onClick={() => { useAuthStore.getState().enterGuestMode(); navigate('/dashboard'); }}
                        >
                            Continue as guest
                        </button>
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
                        {isVerifyingMfa ? 'Verifying...' : 'Verify & Log in'}
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
        </div>
    );
}
