import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { HiOutlineUser, HiOutlineMail, HiOutlineLockClosed, HiOutlineEye, HiOutlineEyeOff, HiOutlineKey, HiOutlineDesktopComputer } from 'react-icons/hi';
import { FcGoogle } from 'react-icons/fc';
import { SiGithub } from 'react-icons/si';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/authStore';
import api from '../../api/axios';
import { browserSupportsWebAuthn, startRegistration } from '@simplewebauthn/browser';
import { getDeviceMetadata } from '../../utils/deviceFingerprint';
import { BorderBeam } from '../../components/ui/BorderBeam';
import { isTauri } from '../../utils/platform';
import { redirectToDesktopHandoff } from '../../utils/desktopAuth';
import DesktopAuthPrompt from './DesktopAuthPrompt';
import './Auth.css';

const registerSchema = z.object({
    username: z.string().min(3, 'Username must be at least 3 characters'),
    email: z.string().email('Please enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters')
        .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
        .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
        .regex(/[0-9]/, 'Password must contain at least one number'),
});

const getStrength = (pass) => {
    let score = 0;
    if (!pass) return 0;
    if (pass.length >= 8) score += 1;
    if (/[A-Z]/.test(pass) && /[a-z]/.test(pass)) score += 1;
    if (/[0-9]/.test(pass)) score += 1;
    if (/[^A-Za-z0-9]/.test(pass)) score += 1;
    return score;
};

const strengthLabels = ['Weak', 'Fair', 'Good', 'Strong'];

export default function Register() {
    if (isTauri()) {
        return <DesktopAuthPrompt mode="register" />;
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

    const registerFn = useAuthStore((state) => state.register);

    const [showPw, setShowPw] = useState(false);
    const [globalError, setGlobalError] = useState('');
    const [passwordless, setPasswordless] = useState(false);
    const [passwordlessStep, setPasswordlessStep] = useState('details');
    const [passwordlessData, setPasswordlessData] = useState({ username: '', email: '', otp: '' });
    const [isPasswordlessBusy, setIsPasswordlessBusy] = useState(false);

    const {
        register,
        handleSubmit,
        watch,
        formState: { errors, isSubmitting }
    } = useForm({
        resolver: zodResolver(registerSchema),
    });

    const watchPassword = watch("password", "");
    const strength = getStrength(watchPassword);

    const onSubmit = async (data) => {
        setGlobalError('');

        // When registering on behalf of desktop app, do NOT establish a web session in the browser.
        if (fromDesktop) {
            try {
                const { deviceId, deviceLabel } = getDeviceMetadata();
                const response = await api.post('/auth/register', {
                    email: data.email,
                    username: data.username,
                    password: data.password,
                    deviceId,
                    deviceLabel
                });

                const { accessToken, refreshToken, accessExpiresAt } = response.data;
                useAuthStore.getState().setTokens(accessToken, accessExpiresAt, refreshToken);
                useAuthStore.getState().checkAuth().catch(() => {});
                await redirectToDesktopHandoff(navigate, accessToken);
                return;
            } catch (error) {
                if (error.response?.status === 409) {
                    setGlobalError('Email or username already taken');
                } else {
                    setGlobalError(error.response?.data?.message || 'Failed to create account');
                }
                return;
            }
        }

        // Regular browser register flow
        try {
            await registerFn(data.email, data.username, data.password);
            navigate('/dashboard', { state: { justLoggedIn: true } });
        } catch (error) {
            setGlobalError(error.message);
        }
    };

    const updatePasswordlessData = (field) => (event) => setPasswordlessData((current) => ({ ...current, [field]: event.target.value }));

    const handleOAuthLogin = (provider) => {
        if (fromDesktop) {
            sessionStorage.setItem('crescendo_from_desktop', 'true');
            document.cookie = 'crescendo_from_desktop=true; path=/; max-age=600; SameSite=Lax';
        }
        const { deviceId, deviceLabel } = getDeviceMetadata();
        document.cookie = `crescendo_device_id_transfer=${encodeURIComponent(deviceId)}; path=/; max-age=300; SameSite=Lax`;
        document.cookie = `crescendo_device_label_transfer=${encodeURIComponent(deviceLabel)}; path=/; max-age=300; SameSite=Lax`;
        const desktopParam = fromDesktop ? '?from=desktop' : '';
        window.location.href = `https://api.crescendo.run/oauth2/authorization/${provider}${desktopParam}`;
    };

    const startPasswordless = async (event) => {
        event.preventDefault();
        setGlobalError('');
        setIsPasswordlessBusy(true);
        try {
            await api.post('/auth/webauthn/passwordless/start', { username: passwordlessData.username, email: passwordlessData.email });
            setPasswordlessStep('otp');
        } catch (error) {
            setGlobalError(error.response?.data?.message || 'Could not send a verification code.');
        } finally {
            setIsPasswordlessBusy(false);
        }
    };

    const verifyOtpAndCreatePasskey = async (event) => {
        event.preventDefault();
        if (!browserSupportsWebAuthn()) {
            setGlobalError('Passkeys are not supported by this browser or device.');
            return;
        }
        setGlobalError('');
        setIsPasswordlessBusy(true);
        try {
            const { data: options } = await api.post('/auth/webauthn/passwordless/verify', { email: passwordlessData.email, otp: passwordlessData.otp });
            const registration = await startRegistration({ optionsJSON: options });
            const { data: tokens } = await api.post('/auth/webauthn/passwordless/finish', {
                ...registration,
                transactionId: options.transactionId,
                verificationSessionId: options.verificationSessionId,
                credentialName: 'Passkey',
            });

            if (fromDesktop) {
                useAuthStore.getState().setTokens(tokens.accessToken, tokens.accessExpiresAt, tokens.refreshToken);
                useAuthStore.getState().checkAuth().catch(() => {});
                await redirectToDesktopHandoff(navigate, tokens.accessToken);
                return;
            }

            useAuthStore.getState().setTokens(tokens.accessToken, tokens.accessExpiresAt, tokens.refreshToken, tokens.refreshExpiresAt);
            await useAuthStore.getState().checkAuth();
            navigate('/dashboard', { state: { justLoggedIn: true } });
        } catch (error) {
            setGlobalError(error.response?.data?.message || error.message || 'Could not create your passkey account.');
        } finally {
            setIsPasswordlessBusy(false);
        }
    };

    if (passwordless) {
        return (
            <div className="auth-card">
                <BorderBeam duration={8} borderWidth={2.5} />
                <Link to="/" className="auth-logo">
                    <img src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'} alt="Crescendo" />
                    <span className="auth-logo-text">Crescendo</span>
                </Link>

                <div className="auth-header">
                    <h1 className="auth-title">Create a passkey</h1>
                    <p className="auth-subtitle">Use your device fingerprint, face, or security key instead of a password.</p>
                </div>

                {globalError && (
                    <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '16px', textAlign: 'center', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '6px' }}>
                        {globalError}
                    </div>
                )}

                {passwordlessStep === 'details' ? (
                    <form className="auth-form" onSubmit={startPasswordless}>
                        <Input label="Username" value={passwordlessData.username} onChange={updatePasswordlessData('username')} placeholder="Choose a username" icon={<HiOutlineUser />} required />
                        <Input label="Email" type="email" value={passwordlessData.email} onChange={updatePasswordlessData('email')} placeholder="you@example.com" icon={<HiOutlineMail />} required />
                        <button type="submit" className="auth-btn" disabled={isPasswordlessBusy}>{isPasswordlessBusy ? 'Sending code…' : 'Email me a code'}</button>
                    </form>
                ) : (
                    <form className="auth-form" onSubmit={verifyOtpAndCreatePasskey}>
                        <Input label="6-digit email code" inputMode="numeric" autoComplete="one-time-code" value={passwordlessData.otp} onChange={updatePasswordlessData('otp')} placeholder="123456" icon={<HiOutlineKey />} maxLength="6" required />
                        <button type="submit" className="auth-btn" disabled={isPasswordlessBusy}>{isPasswordlessBusy ? 'Creating passkey…' : 'Verify & create passkey'}</button>
                        <button type="button" className="auth-oauth-btn" onClick={() => setPasswordlessStep('details')}>Use a different email</button>
                    </form>
                )}
                <div className="auth-footer">Prefer a password? <button type="button" onClick={() => setPasswordless(false)} style={{ background: 'none', border: 0, color: 'var(--text-accent)', cursor: 'pointer', font: 'inherit', padding: 0 }}>Sign up with password</button></div>
            </div>
        );
    }

    return (
        <div className="auth-card">
            <BorderBeam duration={8} borderWidth={2.5} />
            <Link to="/" className="auth-logo">
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo</span>
            </Link>

            <div className="auth-header">
                <h1 className="auth-title">Create an account</h1>
                <p className="auth-subtitle">
                    {fromDesktop
                        ? 'Set up your account to start automating on desktop'
                        : 'Start automating your workflows today'}
                </p>
            </div>

            {fromDesktop && (
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
                    <span>Creating account for Crescendo Desktop. Once registered, your session will connect in the desktop app.</span>
                </div>
            )}

            {globalError && (
                <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '16px', textAlign: 'center', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '6px' }}>
                    {globalError}
                </div>
            )}

            <form className="auth-form" onSubmit={handleSubmit(onSubmit)}>
                <Input
                    label="Username"
                    type="text"
                    placeholder="Choose a username"
                    icon={<HiOutlineUser />}
                    autoComplete="username"
                    {...register('username')}
                    error={errors.username?.message}
                />

                <Input
                    label="Email"
                    type="email"
                    placeholder="Enter your email"
                    icon={<HiOutlineMail />}
                    autoComplete="email"
                    {...register('email')}
                    error={errors.email?.message}
                />

                <Input
                    label="Password"
                    type={showPw ? 'text' : 'password'}
                    placeholder="Create a strong password"
                    icon={<HiOutlineLockClosed />}
                    rightIcon={showPw ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                    onRightIconClick={() => setShowPw(!showPw)}
                    autoComplete="new-password"
                    {...register('password')}
                    error={errors.password?.message}
                />

                {watchPassword && (
                    <div className="password-strength">
                        <div className="password-strength-bars">
                            {[0, 1, 2, 3].map((level) => (
                                <div
                                    key={level}
                                    className={`strength-bar ${level < strength
                                        ? strength <= 1
                                            ? 'weak'
                                            : strength <= 2
                                                ? 'medium'
                                                : 'strong'
                                        : ''
                                        }`}
                                />
                            ))}
                        </div>
                        <span className="password-strength-text" style={{ marginTop: '8px', display: 'block' }}>
                            {strengthLabels[strength]}
                        </span>
                    </div>
                )}

                <label className="auth-terms">
                    <input type="checkbox" required />
                    <span>
                        I agree to the <a href="#">Terms of Service</a> and{' '}
                        <a href="#">Privacy Policy</a>
                    </span>
                </label>

                <button type="submit" className="auth-btn" disabled={isSubmitting}>
                    {isSubmitting ? 'Creating Account...' : 'Create Account'}
                </button>

                <button type="button" className="auth-passkey-btn" onClick={() => setPasswordless(true)}>
                    <HiOutlineKey />
                    <span>Continue with a passkey</span>
                </button>
                <p className="auth-passkey-hint">Verify your email, then use your device instead of a password.</p>

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
            </form>

            <div className="auth-footer">
                Already have an account? <Link to={fromDesktop ? "/login?from=desktop" : "/login"}>Log in</Link>
                {!fromDesktop && (
                    <div style={{ marginTop: '16px' }}>
                        <button
                            type="button"
                            onClick={() => { useAuthStore.getState().enterGuestMode(); navigate('/dashboard'); }}
                            style={{ color: 'var(--text-tertiary)', fontSize: '0.8rem', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline' }}
                        >
                            Continue as guest
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
