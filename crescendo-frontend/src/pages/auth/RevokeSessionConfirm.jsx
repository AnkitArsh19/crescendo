import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { sessionsApi } from '../../api/sessionsApi';
import { HiOutlineShieldExclamation, HiCheckCircle } from 'react-icons/hi';
import './Auth.css'; // Reuse existing auth page styles

export default function RevokeSessionConfirm() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();

    const [status, setStatus] = useState('loading'); // 'loading', 'confirm', 'success', 'error', 'conflict'
    const [targetInfo, setTargetInfo] = useState(null);
    const [errorMessage, setErrorMessage] = useState('');
    const [isRevoking, setIsRevoking] = useState(false);

    useEffect(() => {
        if (!token) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setStatus('error');
            setErrorMessage('Invalid link. No token provided.');
            return;
        }

        const fetchTarget = async () => {
            try {
                const data = await sessionsApi.getRevokeConfirmTarget(token);
                setTargetInfo(data);
                setStatus('confirm');
            } catch (error) {
                if (error.response?.status === 409) {
                    setStatus('conflict');
                } else {
                    setStatus('error');
                    setErrorMessage(error.response?.data || 'Invalid or expired link.');
                }
            }
        };

        fetchTarget();
    }, [token]);

    const handleRevoke = async () => {
        setIsRevoking(true);
        try {
            await sessionsApi.revokeByToken(token);
            setStatus('success');
        } catch (error) {
            setStatus('error');
            setErrorMessage(error.response?.data || 'Failed to revoke session. The link may have expired.');
        } finally {
            setIsRevoking(false);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-header">
                    <h2>Session Revocation</h2>
                </div>

                {status === 'loading' && (
                    <div className="auth-form">
                        <p style={{ textAlign: 'center', color: '#6b7280' }}>Verifying link...</p>
                    </div>
                )}

                {status === 'confirm' && (
                    <div className="auth-form" style={{ textAlign: 'center' }}>
                        <HiOutlineShieldExclamation size={48} color="#dc2626" style={{ margin: '0 auto 16px' }} />
                        <h3 style={{ marginTop: 0, color: '#111827' }}>Revoke Access</h3>
                        <p style={{ color: '#4b5563', marginBottom: '24px' }}>
                            You are about to revoke access for: <br/>
                            <strong style={{ color: '#111827' }}>{targetInfo?.deviceLabel}</strong>
                        </p>
                        <p style={{ fontSize: '13px', color: '#6b7280', marginBottom: '24px' }}>
                            This device will be signed out immediately.
                        </p>
                        
                        <button 
                            className="auth-button" 
                            style={{ backgroundColor: '#dc2626', marginBottom: '12px' }}
                            onClick={handleRevoke}
                            disabled={isRevoking}
                        >
                            {isRevoking ? 'Revoking...' : 'Yes, Revoke Access'}
                        </button>
                        
                        <button 
                            className="auth-button" 
                            style={{ backgroundColor: '#f3f4f6', color: '#374151' }}
                            onClick={() => navigate('/login')}
                            disabled={isRevoking}
                        >
                            Cancel — this was me
                        </button>
                    </div>
                )}

                {status === 'success' && (
                    <div className="auth-form" style={{ textAlign: 'center' }}>
                        <HiCheckCircle size={48} color="#059669" style={{ margin: '0 auto 16px' }} />
                        <h3 style={{ marginTop: 0, color: '#059669' }}>Access Revoked</h3>
                        <p style={{ color: '#4b5563', marginBottom: '24px' }}>
                            The session has been terminated. You can now close this window or return to the login page.
                        </p>
                        <button className="auth-button" onClick={() => navigate('/login')}>
                            Go to Login
                        </button>
                    </div>
                )}

                {status === 'conflict' && (
                    <div className="auth-form" style={{ textAlign: 'center' }}>
                        <h3 style={{ marginTop: 0, color: '#111827' }}>Already Revoked</h3>
                        <p style={{ color: '#4b5563', marginBottom: '24px' }}>
                            This session was already revoked. No further action is needed.
                        </p>
                        <button className="auth-button" onClick={() => navigate('/login')}>
                            Go to Login
                        </button>
                    </div>
                )}

                {status === 'error' && (
                    <div className="auth-form" style={{ textAlign: 'center' }}>
                        <h3 style={{ marginTop: 0, color: '#dc2626' }}>Error</h3>
                        <p style={{ color: '#4b5563', marginBottom: '24px' }}>
                            {errorMessage}
                        </p>
                        <button className="auth-button" onClick={() => navigate('/login')}>
                            Go to Login
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
