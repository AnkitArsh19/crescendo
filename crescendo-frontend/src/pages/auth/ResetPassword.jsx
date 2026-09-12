import { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    HiOutlineMail,
    HiOutlineLockClosed,
    HiOutlineEye,
    HiOutlineEyeOff,
    HiCheck,
    HiCheckCircle,
    HiArrowLeft,
    HiOutlineExclamationCircle
} from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import { BorderBeam } from '../../components/ui/BorderBeam';
import Input from '../../components/ui/Input';
import api from '../../api/axios';
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

export default function ResetPassword() {
    const { theme } = useTheme();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const token = searchParams.get('token');

    // State for Request Reset Link phase
    const [email, setEmail] = useState('');
    const [sent, setSent] = useState(false);
    const [isRequesting, setIsRequesting] = useState(false);
    const [requestError, setRequestError] = useState('');

    // State for Set New Password phase
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showNewPw, setShowNewPw] = useState(false);
    const [showConfirmPw, setShowConfirmPw] = useState(false);
    const [isResetting, setIsResetting] = useState(false);
    const [resetError, setResetError] = useState('');
    const [resetSuccess, setResetSuccess] = useState(false);

    const strength = getStrength(newPassword);

    const handleForgotPasswordSubmit = async (e) => {
        e.preventDefault();
        setRequestError('');

        const trimmedEmail = email.trim();
        if (!trimmedEmail) {
            setRequestError('Please enter your email address.');
            return;
        }

        setIsRequesting(true);
        try {
            await api.post('/auth/forgot-password', { email: trimmedEmail });
            setSent(true);
        } catch (err) {
            const msg =
                err.response?.data?.message ||
                (typeof err.response?.data === 'string' ? err.response?.data : null) ||
                'Unable to send reset email. Please try again.';
            setRequestError(msg);
        } finally {
            setIsRequesting(false);
        }
    };

    const handleResetPasswordSubmit = async (e) => {
        e.preventDefault();
        setResetError('');

        if (!newPassword || newPassword.length < 6) {
            setResetError('Password must be at least 6 characters long.');
            return;
        }

        if (newPassword !== confirmPassword) {
            setResetError('Passwords do not match.');
            return;
        }

        setIsResetting(true);
        try {
            await api.post('/auth/reset-password', {
                resetToken: token,
                newPassword: newPassword
            });
            setResetSuccess(true);
        } catch (err) {
            const msg =
                err.response?.data?.message ||
                (typeof err.response?.data === 'string' ? err.response?.data : null) ||
                'Invalid or expired reset token. Please request a new link.';
            setResetError(msg);
        } finally {
            setIsResetting(false);
        }
    };

    return (
        <div className="auth-card">
            <BorderBeam duration={8} borderWidth={0.5} />
            <div className="auth-logo">
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo</span>
            </div>

            <AnimatePresence mode="wait">
                {/* ─────────────────────────────────────────────────────────────
                    PHASE 2: Resetting password with token in query param
                   ───────────────────────────────────────────────────────────── */}
                {token ? (
                    resetSuccess ? (
                        <motion.div
                            key="reset-success"
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0 }}
                            transition={{ duration: 0.3 }}
                        >
                            <div className="auth-success">
                                <div className="auth-success-icon" style={{ color: '#22c55e' }}>
                                    <HiCheckCircle size={32} />
                                </div>
                                <h2 className="auth-title" style={{ marginTop: 12 }}>Password reset complete!</h2>
                                <p className="auth-subtitle" style={{ marginTop: 8 }}>
                                    Your password has been successfully updated. You can now sign in with your new password.
                                </p>
                            </div>

                            <button
                                type="button"
                                className="auth-btn"
                                onClick={() => navigate('/login')}
                                style={{ marginTop: 20 }}
                            >
                                Continue to Login
                            </button>
                        </motion.div>
                    ) : (
                        <motion.div
                            key="reset-form"
                            initial={{ opacity: 0, x: -10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: 10 }}
                            transition={{ duration: 0.3 }}
                        >
                            <div className="auth-header">
                                <h1 className="auth-title">Set new password</h1>
                                <p className="auth-subtitle">
                                    Choose a secure password for your Crescendo account
                                </p>
                            </div>

                            {resetError && (
                                <div style={{
                                    color: '#ef4444',
                                    fontSize: '0.85rem',
                                    marginBottom: '16px',
                                    textAlign: 'center',
                                    background: 'rgba(239, 68, 68, 0.1)',
                                    padding: '10px 14px',
                                    borderRadius: '6px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '8px'
                                }}>
                                    <HiOutlineExclamationCircle style={{ fontSize: '1.2rem', flexShrink: 0 }} />
                                    <div style={{ flex: 1, textAlign: 'left' }}>
                                        {resetError}
                                    </div>
                                </div>
                            )}

                            <form className="auth-form" onSubmit={handleResetPasswordSubmit}>
                                <Input
                                    label="New Password"
                                    type={showNewPw ? 'text' : 'password'}
                                    placeholder="At least 6 characters"
                                    icon={<HiOutlineLockClosed />}
                                    rightIcon={showNewPw ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                                    onRightIconClick={() => setShowNewPw(!showNewPw)}
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    autoComplete="new-password"
                                    required
                                />

                                {newPassword && (
                                    <>
                                        <div className="password-strength">
                                            {[1, 2, 3, 4].map((level) => (
                                                <div
                                                    key={level}
                                                    className={`password-strength-bar ${
                                                        strength >= level
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
                                        <div className="password-strength-text">
                                            Strength: {strengthLabels[strength]}
                                        </div>
                                    </>
                                )}

                                <Input
                                    label="Confirm New Password"
                                    type={showConfirmPw ? 'text' : 'password'}
                                    placeholder="Re-enter your new password"
                                    icon={<HiOutlineLockClosed />}
                                    rightIcon={showConfirmPw ? <HiOutlineEyeOff /> : <HiOutlineEye />}
                                    onRightIconClick={() => setShowConfirmPw(!showConfirmPw)}
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    autoComplete="new-password"
                                    required
                                />

                                <button
                                    type="submit"
                                    className="auth-btn"
                                    disabled={isResetting}
                                >
                                    {isResetting ? 'Updating password…' : 'Reset Password'}
                                </button>
                            </form>

                            <div className="auth-footer" style={{ marginTop: 20 }}>
                                <button
                                    type="button"
                                    onClick={() => setSearchParams({})}
                                    style={{
                                        background: 'none',
                                        border: 'none',
                                        color: 'var(--text-secondary)',
                                        fontSize: '0.84rem',
                                        cursor: 'pointer',
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: 4
                                    }}
                                >
                                    <HiArrowLeft /> Request a new reset link
                                </button>
                            </div>
                        </motion.div>
                    )
                ) : (
                    /* ─────────────────────────────────────────────────────────────
                        PHASE 1: Request Reset Link (No token in query param)
                       ───────────────────────────────────────────────────────────── */
                    !sent ? (
                        <motion.div
                            key="request-form"
                            initial={{ opacity: 0, x: -10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: 10 }}
                            transition={{ duration: 0.3 }}
                        >
                            <div className="auth-header">
                                <h1 className="auth-title">Reset password</h1>
                                <p className="auth-subtitle">
                                    Enter your account email and we'll send you a link to reset your password
                                </p>
                            </div>

                            {requestError && (
                                <div style={{
                                    color: '#ef4444',
                                    fontSize: '0.85rem',
                                    marginBottom: '16px',
                                    textAlign: 'center',
                                    background: 'rgba(239, 68, 68, 0.1)',
                                    padding: '10px 14px',
                                    borderRadius: '6px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '8px'
                                }}>
                                    <HiOutlineExclamationCircle style={{ fontSize: '1.2rem', flexShrink: 0 }} />
                                    <div style={{ flex: 1, textAlign: 'left' }}>
                                        {requestError}
                                    </div>
                                </div>
                            )}

                            <form className="auth-form" onSubmit={handleForgotPasswordSubmit}>
                                <Input
                                    label="Email"
                                    type="email"
                                    placeholder="you@example.com"
                                    icon={<HiOutlineMail />}
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    autoComplete="email"
                                    required
                                />

                                <button
                                    type="submit"
                                    className="auth-btn"
                                    disabled={isRequesting}
                                >
                                    {isRequesting ? 'Sending link…' : 'Send Reset Link'}
                                </button>
                            </form>

                            <div className="auth-footer">
                                <Link to="/login" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                                    <HiArrowLeft /> Back to login
                                </Link>
                            </div>
                        </motion.div>
                    ) : (
                        <motion.div
                            key="request-sent"
                            initial={{ opacity: 0, x: 10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -10 }}
                            transition={{ duration: 0.3 }}
                        >
                            <div className="auth-success">
                                <div className="auth-success-icon">
                                    <HiCheck />
                                </div>
                                <h2 className="auth-title">Check your email</h2>
                                <p className="auth-subtitle" style={{ marginTop: 8 }}>
                                    If an account exists for <strong style={{ color: 'var(--text-primary)' }}>{email}</strong>,
                                    we've sent a password reset link. Please check your inbox and spam folder.
                                </p>
                            </div>

                            <div style={{ marginTop: 16, textAlign: 'center' }}>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setSent(false);
                                        setRequestError('');
                                    }}
                                    style={{
                                        background: 'none',
                                        border: 'none',
                                        color: 'var(--text-accent)',
                                        fontSize: '0.85rem',
                                        cursor: 'pointer',
                                        textDecoration: 'underline'
                                    }}
                                >
                                    Try another email address
                                </button>
                            </div>

                            <div className="auth-footer" style={{ marginTop: 24 }}>
                                <Link to="/login" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                                    <HiArrowLeft /> Back to login
                                </Link>
                            </div>
                        </motion.div>
                    )
                )}
            </AnimatePresence>
        </div>
    );
}
