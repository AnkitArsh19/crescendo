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
} from 'react-icons/hi';
import { FcGoogle } from 'react-icons/fc';
import { SiGithub } from 'react-icons/si';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/authStore';
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
                />

                <Input
                    label="Email"
                    type="email"
                    placeholder="Enter your email"
                    icon={<HiOutlineMail />}
                    {...register("email")}
                    error={errors.email?.message}
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
                />

                <Input
                    label="Confirm password"
                    type="password"
                    placeholder="Re-enter your password"
                    icon={<HiOutlineLockClosed />}
                    {...register("confirmPassword")}
                    error={errors.confirmPassword?.message}
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
