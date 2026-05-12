import { useState } from 'react';
import { HiCheck, HiOutlineTrash, HiOutlineX } from 'react-icons/hi';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/authStore';
import api from '../../api/axios';
import './Settings.css';

export default function ProfileSettings() {
    const { user, checkAuth, deleteAccount } = useAuthStore();
    const navigate = useNavigate();
    const [username, setUsername] = useState(user?.username || '');
    const [statusMsg, setStatusMsg] = useState({ type: '', text: '' });
    const [isSaving, setIsSaving] = useState(false);
    const [showDelete, setShowDelete] = useState(false);
    const [deleteConfirm, setDeleteConfirm] = useState('');
    const [isDeleting, setIsDeleting] = useState(false);

    const handleSave = async (e) => {
        e.preventDefault();
        setStatusMsg({ type: '', text: '' });
        
        if (username === user?.username) {
            return;
        }

        setIsSaving(true);
        try {
            await api.patch('/users/me', { username });
            await checkAuth();
            setStatusMsg({ type: 'success', text: 'Profile updated successfully' });
        } catch (error) {
            setStatusMsg({ type: 'error', text: error.response?.data?.message || 'Failed to update profile' });
        } finally {
            setIsSaving(false);
        }
    };

    const handleDeleteAccount = async () => {
        if (deleteConfirm !== user?.username) return;
        setIsDeleting(true);
        try {
            await deleteAccount();
            navigate('/');
        } catch {
            setStatusMsg({ type: 'error', text: 'Failed to delete account' });
        } finally {
            setIsDeleting(false);
            setShowDelete(false);
        }
    };

    return (
        <>
            <div className="settings-section">
                <h2 className="settings-section-title">Profile</h2>
                <p className="settings-section-desc">
                    Manage your personal information and how others see you on Crescendo.
                </p>

                <form className="settings-form" onSubmit={handleSave}>
                    <Input 
                        label="Username" 
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                    />

                    <Input
                        label="Email"
                        value={user?.email || ''}
                        disabled
                    />
                    <div style={{ marginTop: -10, display: 'flex', alignItems: 'center', gap: '10px' }}>
                        {user?.limits?.tier && user.limits.tier !== 'UNVERIFIED' && user.limits.tier !== 'GUEST' ? (
                            <span className="settings-badge settings-badge-verified"><HiCheck style={{ marginRight: '4px' }}/> Verified</span>
                        ) : (
                            <span className="settings-badge settings-badge-disabled">⚠ Unverified</span>
                        )}
                        {user?.linkedAccounts?.some(a => a.provider === 'GOOGLE') && (
                            <span className="settings-badge settings-badge-verified">
                                Google Linked
                            </span>
                        )}
                        {user?.linkedAccounts?.some(a => a.provider === 'GITHUB') && (
                            <span className="settings-badge settings-badge-verified">
                                GitHub Linked
                            </span>
                        )}
                    </div>

                    <div style={{ marginTop: 4 }}>
                        <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Member Since</label>
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', padding: '10px 14px', background: 'var(--bg-elevated)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-primary)' }}>
                            {user?.createdAt ? new Date(user.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : '—'}
                        </div>
                    </div>

                    {statusMsg.text && (
                        <div style={{ 
                            padding: '10px', 
                            borderRadius: '6px', 
                            fontSize: '0.85rem',
                            color: statusMsg.type === 'error' ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.7)',
                            background: statusMsg.type === 'error' ? 'rgba(255,255,255,0.04)' : 'rgba(255,255,255,0.06)',
                            border: statusMsg.type === 'error' ? '1px solid rgba(255,255,255,0.08)' : '1px solid rgba(255,255,255,0.1)'
                        }}>
                            {statusMsg.text}
                        </div>
                    )}

                    <button type="submit" className="settings-btn settings-btn-primary" disabled={isSaving}>
                        {isSaving ? 'Saving...' : 'Save Changes'}
                    </button>
                </form>
            </div>

            <div className="settings-divider" />

            {/* Danger Zone — Account Deletion */}
            <div className="settings-section">
                <h2 className="settings-section-title" style={{ color: 'rgba(255,255,255,0.5)' }}>Danger Zone</h2>
                <p className="settings-section-desc">
                    Permanently delete your account and all associated data. This action cannot be undone.
                </p>
                <button className="settings-btn settings-btn-danger" onClick={() => setShowDelete(true)}>
                    <HiOutlineTrash style={{ marginRight: 6 }} /> Delete Account
                </button>
            </div>

            {/* Delete Confirmation Modal */}
            <AnimatePresence>
                {showDelete && (
                    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowDelete(false)}>
                        <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={(e) => e.stopPropagation()}>
                            <div className="conn-modal-header">
                                <h2>Delete Account</h2>
                                <button className="conn-modal-close" onClick={() => setShowDelete(false)}><HiOutlineX /></button>
                            </div>
                            <div className="conn-modal-body">
                                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: 16 }}>
                                    This will permanently delete your account, all workflows, connections, and data. This action <strong>cannot be undone</strong>.
                                </p>
                                <label className="conn-form-label">
                                    Type <strong>{user?.username}</strong> to confirm
                                    <input
                                        className="conn-form-input"
                                        value={deleteConfirm}
                                        onChange={(e) => setDeleteConfirm(e.target.value)}
                                        placeholder={user?.username}
                                    />
                                </label>
                            </div>
                            <div className="conn-modal-footer">
                                <button className="conn-btn-secondary" onClick={() => setShowDelete(false)}>Cancel</button>
                                <button
                                    className="conn-btn-danger"
                                    onClick={handleDeleteAccount}
                                    disabled={deleteConfirm !== user?.username || isDeleting}
                                >
                                    {isDeleting ? 'Deleting...' : 'Delete My Account'}
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
}

