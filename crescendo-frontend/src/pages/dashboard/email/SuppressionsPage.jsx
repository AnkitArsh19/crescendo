import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlinePlus, HiOutlineTrash, HiOutlineX, HiOutlineUpload } from 'react-icons/hi';
import { suppressionsApi } from '../../../api/suppressionsApi';
import '../../settings/Settings.css';

const reasonLabels = { BOUNCED: 'Bounced', MANUAL: 'Manual', UNSUBSCRIBED: 'Unsubscribed' };

export default function SuppressionsPage() {
    const [list, setList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showAdd, setShowAdd] = useState(false);
    const [email, setEmail] = useState('');
    const [saving, setSaving] = useState(false);
    const [importing, setImporting] = useState(false);
    const fileInputRef = useRef(null);

    const handleFileUpload = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        
        if (file.size > 10 * 1024 * 1024) {
            alert('File size exceeds the 10MB limit for CSV imports.');
            if (fileInputRef.current) fileInputRef.current.value = '';
            return;
        }

        setImporting(true);
        const formData = new FormData();
        formData.append('file', file);
        try {
            await suppressionsApi.import(formData);
            fetchData(); // Refresh list after import
        } catch { /* */ }
        setImporting(false);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    const fetchData = async () => {
        setLoading(true);
        try {
            const data = await suppressionsApi.list();
            setList(Array.isArray(data) ? data : []);
        } catch { /* */ }
        setLoading(false);
    };

    useEffect(() => { fetchData(); }, []);

    const handleAdd = async () => {
        if (!email) return;
        setSaving(true);
        try {
            const item = await suppressionsApi.add(email);
            setList([item, ...list]);
            setEmail('');
            setShowAdd(false);
        } catch { /* */ }
        setSaving(false);
    };

    const handleRemove = async (id) => {
        try {
            await suppressionsApi.remove(id);
            setList(list.filter(s => s.id !== id));
        } catch { /* */ }
    };

    if (loading) {
        return (
            <div className="settings-skeleton-list">
                {[...Array(3)].map((_, i) => <div key={i} className="settings-skeleton-row" />)}
            </div>
        );
    }

    return (
        <>
            <div className="settings-section-header">
                <div>
                    <h2 className="settings-section-title">Suppressions</h2>
                    <p className="settings-section-desc">Email addresses that will not receive any emails. Bounced addresses are auto-added.</p>
                </div>
                <div style={{ display: 'flex', gap: '12px' }}>
                    <input type="file" accept=".csv,.json" ref={fileInputRef} onChange={handleFileUpload} style={{ display: 'none' }} />
                    <button className="settings-btn settings-btn-secondary" onClick={() => fileInputRef.current?.click()} disabled={importing}>
                        <HiOutlineUpload /> {importing ? 'Importing...' : 'Import CSV'}
                    </button>
                    <button className="settings-btn settings-btn-primary" onClick={() => setShowAdd(true)}>
                        <HiOutlinePlus /> Add Suppression
                    </button>
                </div>
            </div>

            {list.length === 0 ? (
                <div className="settings-empty">
                    <div className="settings-empty-icon"><HiOutlineBan size={32} /></div>
                    <p>No suppression records found. Your list is clean!</p>
                </div>
            ) : (
                <div className="settings-table">
                    <div className="settings-table-head" style={{ gridTemplateColumns: '2fr 1fr 1fr 60px' }}>
                        <span>Email</span><span>Reason</span><span>Added</span><span></span>
                    </div>
                    {list.map((s) => (
                        <motion.div
                            key={s.id}
                            className="settings-table-row"
                            style={{ gridTemplateColumns: '2fr 1fr 1fr 60px' }}
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                        >
                            <span className="settings-table-cell-name">{s.email}</span>
                            <span><span className={`email-status-badge ${s.reason === 'BOUNCED' ? 'es-failed' : 'es-pending'}`}>{reasonLabels[s.reason] || s.reason}</span></span>
                            <span style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)' }}>{s.createdAt ? new Date(s.createdAt).toLocaleDateString() : '—'}</span>
                            <span>
                                <button className="settings-icon-btn settings-danger-icon" onClick={() => handleRemove(s.id)} title="Remove"><HiOutlineTrash /></button>
                            </span>
                        </motion.div>
                    ))}
                </div>
            )}

            {/* Add modal */}
            <AnimatePresence>
                {showAdd && (
                    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowAdd(false)}>
                        <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={e => e.stopPropagation()}>
                            <div className="conn-modal-header">
                                <h2>Add Suppression</h2>
                                <button className="conn-modal-close" onClick={() => setShowAdd(false)}><HiOutlineX /></button>
                            </div>
                            <div className="conn-modal-body">
                                <label className="conn-form-label">
                                    Email Address *
                                    <input className="conn-form-input" value={email} onChange={e => setEmail(e.target.value)} placeholder="user@example.com" />
                                </label>
                            </div>
                            <div className="conn-modal-footer">
                                <button className="conn-btn-secondary" onClick={() => setShowAdd(false)}>Cancel</button>
                                <button className="conn-btn-primary" onClick={handleAdd} disabled={saving || !email}>
                                    {saving ? 'Adding...' : 'Add Suppression'}
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
}
