import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { HiOutlineShieldCheck } from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/authStore';
import './Auth.css';

const mfaSchema = z.object({
    code: z.string().length(6, 'Code must be exactly 6 digits').regex(/^\d+$/, 'Code must contain only numbers'),
});

/**
 * Standalone MFA challenge page for OAuth login flows.
 *
 * When a user with MFA enabled logs in via Google/GitHub, the backend can't show
 * an MFA prompt mid-redirect — so it redirects here with ?email=...&oauth=true.
 * The user enters their TOTP code, we call POST /mfa/challenge, and on success
 * store the tokens and navigate to the dashboard.
 */
export default function MfaChallenge() {
    const { theme } = useTheme();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const verifyMfaFn = useAuthStore((s) => s.verifyMfa);

    const email = searchParams.get('email') || '';
    const [globalError, setGlobalError] = useState('');

    const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
        resolver: zodResolver(mfaSchema),
    });

    useEffect(() => {
        if (!email) {
            navigate('/login', { replace: true });
        }
    }, [email, navigate]);

    const onSubmit = async (data) => {
        setGlobalError('');
        try {
            await verifyMfaFn(email, data.code);
            navigate('/dashboard', { replace: true });
        } catch (error) {
            setGlobalError(error.message);
        }
    };

    if (!email) return null;

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
                <h1 className="auth-title">Two-Factor Authentication</h1>
                <p className="auth-subtitle">
                    Enter the 6-digit code from your authenticator app to complete sign-in.
                </p>
            </div>

            {globalError && (
                <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '16px', textAlign: 'center', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '6px' }}>
                    {globalError}
                </div>
            )}

            <form className="auth-form" onSubmit={handleSubmit(onSubmit)}>
                <Input
                    label="Verification Code"
                    type="text"
                    placeholder="123456"
                    icon={<HiOutlineShieldCheck />}
                    {...register('code')}
                    error={errors.code?.message}
                    autoComplete="off"
                    maxLength="6"
                />

                <button type="submit" className="auth-btn" disabled={isSubmitting} style={{ marginTop: '10px' }}>
                    {isSubmitting ? 'Verifying...' : 'Verify & Log in'}
                </button>

                <div className="auth-footer" style={{ marginTop: '20px' }}>
                    <Link to="/login" style={{ color: 'var(--text-accent)', fontSize: '0.85rem' }}>
                        Back to login
                    </Link>
                </div>
            </form>
        </div>
    );
}
