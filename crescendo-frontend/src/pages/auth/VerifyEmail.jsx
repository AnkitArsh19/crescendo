import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { HiCheckCircle, HiXCircle, HiOutlineMail } from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import api from '../../api/axios';
import './Auth.css';

export default function VerifyEmail() {
    const { theme } = useTheme();
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState('verifying'); // 'verifying' | 'success' | 'error'
    const [errorMsg, setErrorMsg] = useState('');

    useEffect(() => {
        const token = searchParams.get('token');
        if (!token) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setStatus('error');
            setErrorMsg('No verification token found in the link.');
            return;
        }

        api.post(`/auth/verify-email?token=${encodeURIComponent(token)}`)
            .then(() => setStatus('success'))
            .catch((err) => {
                setStatus('error');
                setErrorMsg(
                    err.response?.data?.message ||
                    err.response?.data ||
                    'The verification link is invalid or has expired.'
                );
            });
    }, [searchParams]);

    return (
        <div className="auth-card">
            <div className="auth-logo">
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo</span>
            </div>

            <motion.div
                key={status}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4 }}
                style={{ textAlign: 'center', padding: '8px 0 16px' }}
            >
                {status === 'verifying' && (
                    <>
                        <div className="auth-spinner" style={{ margin: '0 auto 16px' }} />
                        <h1 className="auth-title">Verifying your email…</h1>
                        <p className="auth-subtitle">Just a moment.</p>
                    </>
                )}

                {status === 'success' && (
                    <>
                        <HiCheckCircle style={{ fontSize: '3rem', color: 'var(--color-success, #22c55e)', marginBottom: 12 }} />
                        <h1 className="auth-title">Email verified!</h1>
                        <p className="auth-subtitle">
                            Your account is now fully active. You can sign in and start building.
                        </p>
                        <Link to="/login" className="auth-submit" style={{ display: 'inline-flex', marginTop: 24, textDecoration: 'none', justifyContent: 'center' }}>
                            Go to Login
                        </Link>
                    </>
                )}

                {status === 'error' && (
                    <>
                        <HiXCircle style={{ fontSize: '3rem', color: 'var(--color-error, #ef4444)', marginBottom: 12 }} />
                        <h1 className="auth-title">Verification failed</h1>
                        <p className="auth-subtitle">{errorMsg}</p>
                        <p className="auth-subtitle" style={{ marginTop: 16 }}>
                            <HiOutlineMail style={{ verticalAlign: 'middle', marginRight: 4 }} />
                            Need a new link?{' '}
                            <Link to="/login" style={{ color: 'var(--text-accent)' }}>
                                Sign in
                            </Link>{' '}
                            and request a resend from settings.
                        </p>
                    </>
                )}
            </motion.div>
        </div>
    );
}
