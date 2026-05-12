import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { HiOutlineMail, HiOutlineLockClosed, HiOutlineEye, HiOutlineEyeOff, HiOutlineShieldCheck } from 'react-icons/hi';
import { FcGoogle } from 'react-icons/fc';
import { SiGithub } from 'react-icons/si';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/authStore';
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
    const loginFn = useAuthStore((state) => state.login);
    const verifyMfaFn = useAuthStore((state) => state.verifyMfa);
    
    const [showPw, setShowPw] = useState(false);
    const [isMfaStep, setIsMfaStep] = useState(false);
    const [savedEmail, setSavedEmail] = useState('');
    const [globalError, setGlobalError] = useState('');

    const { register: registerLogin, handleSubmit: handleLoginSubmit, formState: { errors: loginErrors, isSubmitting: isLoggingIn } } = useForm({
        resolver: zodResolver(loginSchema),
    });

    const { register: registerMfa, handleSubmit: handleMfaSubmit, formState: { errors: mfaErrors, isSubmitting: isVerifyingMfa } } = useForm({
        resolver: zodResolver(mfaSchema),
    });

    const onLogin = async (data) => {
        setGlobalError('');
        try {
            const res = await loginFn(data.email, data.password, data.rememberMe);
            if (res.mfaRequired) {
                setSavedEmail(data.email);
                setIsMfaStep(true);
            } else {
                navigate('/dashboard');
            }
        } catch (error) {
            setGlobalError(error.message);
        }
    };

    const onMfa = async (data) => {
        setGlobalError('');
        try {
            await verifyMfaFn(savedEmail, data.code);
            navigate('/dashboard');
        } catch (error) {
            setGlobalError(error.message);
        }
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
                        {...registerLogin('password')}
                        error={loginErrors.password?.message}
                    />

                    <div className="auth-form-row">
                        <label className="auth-remember">
                            <input type="checkbox" {...registerLogin('rememberMe')} />
                            Remember me
                        </label>
                        <Link to="/reset-password" className="auth-forgot">
                            Forgot password?
                        </Link>
                    </div>

                    <button type="submit" className="auth-btn" disabled={isLoggingIn}>
                        {isLoggingIn ? 'Logging in...' : 'Log in'}
                    </button>

                    <div className="auth-divider">
                        <span className="auth-divider-line" />
                        <span className="auth-divider-text">or continue with</span>
                        <span className="auth-divider-line" />
                    </div>

                    <div style={{ display: 'flex', gap: '10px' }}>
                        <button 
                            type="button" 
                            className="auth-oauth-btn" 
                            onClick={() => window.location.href = 'https://api.crescendo.run/oauth2/authorization/google'}
                            style={{ flex: 1 }}
                        >
                            <span className="auth-oauth-icon"><FcGoogle /></span>
                            Google
                        </button>
                        <button 
                            type="button" 
                            className="auth-oauth-btn" 
                            onClick={() => window.location.href = 'https://api.crescendo.run/oauth2/authorization/github'}
                            style={{ flex: 1, color: 'var(--text-primary)' }}
                        >
                            <span className="auth-oauth-icon"><SiGithub /></span>
                            GitHub
                        </button>
                    </div>
                    
                    <div className="auth-footer">
                        Don't have an account? <Link to="/register">Sign up</Link>
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
                    
                    <div className="auth-footer" style={{ marginTop: '20px' }}>
                        <button type="button" onClick={() => setIsMfaStep(false)} style={{ background: 'none', border: 'none', color: 'var(--text-accent)', cursor: 'pointer', fontSize: '0.85rem' }}>
                            Back to login
                        </button>
                    </div>
                </form>
            )}
        </div>
    );
}
