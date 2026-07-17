import { useState } from 'react';
import { HiOutlineLockClosed, HiX } from 'react-icons/hi';
import api from '../api/axios';
import Input from './ui/Input';
import './ReAuthModal.css';

export default function ReAuthModal({ isOpen, onClose, onSuccess }) {
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsSubmitting(true);
        try {
            const { data } = await api.post('/auth/webauthn/elevate/passkey-mgmt', { password });
            setPassword('');
            onSuccess(data.elevatedToken);
        } catch (err) {
            setError(err.response?.data?.message || 'Incorrect password');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="reauth-modal-overlay">
            <div className="reauth-modal-card">
                <button className="reauth-modal-close" onClick={onClose} title="Cancel">
                    <HiX />
                </button>
                <div className="reauth-modal-icon">
                    <HiOutlineLockClosed />
                </div>
                <h3>Verify it's you</h3>
                <p>Please enter your password to continue.</p>
                
                {error && <div className="reauth-modal-error">{error}</div>}
                
                <form onSubmit={handleSubmit}>
                    <Input
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        autoFocus
                        required
                    />
                    <div className="reauth-modal-actions">
                        <button type="button" className="reauth-modal-btn-cancel" onClick={onClose}>
                            Cancel
                        </button>
                        <button type="submit" className="reauth-modal-btn-submit" disabled={isSubmitting || !password}>
                            {isSubmitting ? 'Verifying...' : 'Continue'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
