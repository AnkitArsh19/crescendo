import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
    HiOutlineUser,
    HiOutlineMail,
    HiOutlineLockClosed,
    HiOutlineEye,
    HiOutlineEyeOff,
    HiOutlineKey,
} from 'react-icons/hi';
import { FcGoogle } from 'react-icons/fc';
import { SiGithub } from 'react-icons/si';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/authStore';
import { browserSupportsWebAuthn, startRegistration } from '@simplewebauthn/browser';
import api from '../../api/axios';
import { getDeviceMetadata } from '../../utils/deviceFingerprint';
import './Auth.css';

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

const registerSchema = z.object({
    username: z.string().min(1, 'Username is required').max(100, 'Username cannot exceed 100 characters'),
    email: z.string().email('Please enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string()
}).refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
});

export default function Register() {
    const { theme } = useTheme();
    const navigate = useNavigate();
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
        try {
            await registerFn(data.email, data.username, data.password);

            // Redirect to dashboard on successful registration
            navigate('/dashboard', { state: { justLoggedIn: true } });
        } catch (error) {
            setGlobalError(error.message);
        }
    };

    const updatePasswordlessData = (field) => (event) => setPasswordlessData((current) => ({ ...current, [field]: event.target.value }));

    const handleOAuthLogin = (provider) => {
        const { deviceId, deviceLabel } = getDeviceMetadata();
        document.cookie = `crescendo_device_id_transfer=${encodeURIComponent(deviceId)}; path=/; max-age=300; SameSite=Lax`;
        document.cookie = `crescendo_device_label_transfer=${encodeURIComponent(deviceLabel)}; path=/; max-age=300; SameSite=Lax`;
        window.location.href = `https://api.crescendo.run/oauth2/authorization/${provider}`;
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
                <Link to="/" className="auth-logo"><img src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'} alt="Crescendo" /><span className="auth-logo-text">Crescendo</span></Link>
                <div className="auth-header"><h1 className="auth-title">Create account with a passkey</h1><p className="auth-subtitle">Verify your email first, then use your device’s secure screen lock instead of a password.</p></div>
                {globalError && <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: 16, textAlign: 'center', background: 'rgba(239, 68, 68, 0.1)', padding: 10, borderRadius: 6 }}>{globalError}</div>}
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
            <Link to="/" className="auth-logo">
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo</span>
            </Link>

            <div className="auth-header">
                <h1 className="auth-title">Create an account</h1>
                <p className="auth-subtitle">Start automating your workflows today</p>
            </div>

            {globalError && (
                <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '16px', textAlign: 'center', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '6px' }}>
                    {globalError}
                </div>
            )}

            <form className="auth-form" onSubmit={handleSubmit(onSubmit)}>
                <Input
                    label="Username"
                    type="text"
                    placeholder="Enter your username"
                    icon={<HiOutlineUser />}
                    {...register("username")}
                    error={errors.username?.message}
                    autoComplete="username"
                />

                <Input
                    label="Email"
                    type="email"
                    placeholder="Enter your email"
                    icon={<HiOutlineMail />}
                    {...register("email")}
                    error={errors.email?.message}
                    autoComplete="email"
                />

                <Input
                    label="Password"
                    type={showPw ? 'text' : 'password'}
                    placeholder="Create a password"
                    icon={<HiOutlineLockClosed />}
                    rightIcon={showPw ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                    onRightIconClick={() => setShowPw(!showPw)}
                    {...register("password")}
                    error={errors.password?.message}
                    autoComplete="new-password"
                />

                <Input
                    label="Confirm password"
                    type="password"
                    placeholder="Re-enter your password"
                    icon={<HiOutlineLockClosed />}
                    {...register("confirmPassword")}
                    error={errors.confirmPassword?.message}
                    autoComplete="new-password"
                />

                {watchPassword && (
                    <div style={{ marginTop: '-4px', marginBottom: errors.password || errors.confirmPassword ? 0 : '8px' }}>
                        <div className="password-strength">
                            {[1, 2, 3, 4].map((i) => (
                                <div
                                    key={i}
                                    className={`password-strength-bar ${i <= strength
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
                Already have an account? <Link to="/login">Log in</Link>
                <div style={{ marginTop: '16px' }}>
                    <button
                        type="button"
                        onClick={() => { useAuthStore.getState().enterGuestMode(); navigate('/dashboard'); }}
                        style={{ color: 'var(--text-tertiary)', fontSize: '0.8rem', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline' }}
                    >
                        Continue as guest
                    </button>
                </div>
            </div>
        </div>
    );
}
