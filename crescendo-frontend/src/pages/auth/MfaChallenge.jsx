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

const backupSchema = z.object({
    backupCode: z.string().min(8, 'Backup code must be at least 8 characters'),
});

/**
 * Standalone MFA challenge page for OAuth login flows.
 *
 * When a user with MFA enabled logs in via Google/GitHub, the backend can't show
 * an MFA prompt mid-redirect — so it redirects here with ?email=...&oauth=true.
 * The user enters their TOTP code (or a backup code), we call the appropriate
 * endpoint, and on success store the tokens and navigate to the dashboard.
 */
export default function MfaChallenge() {
    const { theme } = useTheme();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const verifyMfaFn = useAuthStore((s) => s.verifyMfa);
    const useBackupCodeFn = useAuthStore((s) => s.useBackupCode);

    const email = searchParams.get('email') || '';
    const [globalError, setGlobalError] = useState('');
    const [isBackupCodeMode, setIsBackupCodeMode] = useState(false);
    const [backupRemaining, setBackupRemaining] = useState(null);

    const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
        resolver: zodResolver(mfaSchema),
    });

    const { register: registerBackup, handleSubmit: handleBackupSubmit, formState: { errors: backupErrors, isSubmitting: isUsingBackup } } = useForm({
        resolver: zodResolver(backupSchema),
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

    const onBackupCode = async (data) => {
        setGlobalError('');
        try {
            const result = await useBackupCodeFn(email, data.backupCode);
            if (result.remaining !== undefined && result.remaining <= 2) {
                setBackupRemaining(result.remaining);
            }
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
                    {isBackupCodeMode
                        ? 'Enter one of your backup codes to sign in.'
                        : 'Enter the 6-digit code from your authenticator app to complete sign-in.'}
                </p>
            </div>

            {globalError && (
                <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '16px', textAlign: 'center', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '6px' }}>
                    {globalError}
                </div>
            )}

            {isBackupCodeMode ? (
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
                        <Link to="/login" style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                            Back to login
                        </Link>
                    </div>
                </form>
            ) : (
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

                    <div className="auth-footer" style={{ marginTop: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <button type="button" onClick={() => { setIsBackupCodeMode(true); setGlobalError(''); }} style={{ background: 'none', border: 'none', color: 'var(--text-accent)', cursor: 'pointer', fontSize: '0.85rem' }}>
                            Lost access? Use backup code
                        </button>
                        <Link to="/login" style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                            Back to login
                        </Link>
                    </div>
                </form>
            )}
        </div>
    );
}
