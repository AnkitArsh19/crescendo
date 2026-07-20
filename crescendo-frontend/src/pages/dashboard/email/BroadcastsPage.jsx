/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlinePlus, HiOutlineTrash, HiOutlineX, HiOutlinePaperAirplane } from 'react-icons/hi';
import { broadcastsApi } from '../../../api/broadcastsApi';
import { templatesApi } from '../../../api/emailServiceApi';
import '../../settings/Settings.css';

const statusClasses = { DRAFT: 'es-pending', SENDING: 'es-sent', COMPLETED: 'es-delivered', FAILED: 'es-failed' };

export default function BroadcastsPage() {
    const [broadcasts, setBroadcasts] = useState([]);
    const [templates, setTemplates] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showCreate, setShowCreate] = useState(false);
    const [form, setForm] = useState({ templateId: '', fromAddress: '' });
    const [saving, setSaving] = useState(false);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [bc, tpl] = await Promise.all([broadcastsApi.list(), templatesApi.list()]);
            setBroadcasts(Array.isArray(bc) ? bc : []);
            setTemplates(Array.isArray(tpl) ? tpl : []);
        } catch { /* */ }
        setLoading(false);
    };

    // eslint-disable-next-line react-hooks/exhaustive-deps
    useEffect(() => { fetchData(); }, []);

    const handleCreate = async () => {
        if (!form.templateId || !form.fromAddress) return;
        setSaving(true);
        try {
            const bc = await broadcastsApi.create(form);
            setBroadcasts([bc, ...broadcasts]);
            setShowCreate(false);
            setForm({ templateId: '', fromAddress: '' });
        } catch { /* */ }
        setSaving(false);
    };

    const handleSend = async (id) => {
        try {
            await broadcastsApi.send(id);
            fetchData();
        } catch { /* */ }
    };

    const handleDelete = async (id) => {
        try {
            await broadcastsApi.delete(id);
            setBroadcasts(broadcasts.filter(b => b.id !== id));
        } catch { /* */ }
    };

    if (loading) {
        return (
            <div className="settings-skeleton-list">
                {[...Array(3)].map((_, i) => <div key={i} className="settings-skeleton-row" style={{ height: 100 }} />)}
            </div>
        );
    }

    return (
        <>
            <div className="settings-section-header">
                <div>
                    <h2 className="settings-section-title">Broadcasts</h2>
                    <p className="settings-section-desc">Send emails to all your contacts using a template.</p>
                </div>
                <button className="settings-btn settings-btn-primary" onClick={() => setShowCreate(true)}>
                    <HiOutlinePlus /> New Broadcast
                </button>
            </div>

            {broadcasts.length === 0 ? (
                <div className="settings-empty">
                    <div className="settings-empty-icon"><HiOutlineSpeakerphone size={32} /></div>
                    <p>No broadcasts yet. Create your first broadcast campaign to reach your audience.</p>
                </div>
            ) : (
                <div className="domain-list">
                    {broadcasts.map((bc) => (
                        <motion.div key={bc.id} className="domain-card" initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }}>
                            <div className="domain-card-header">
                                <div className="domain-card-info">
                                    <h3>{bc.name || templates.find(t => t.id === bc.templateId)?.name || 'Broadcast'}</h3>
                                    <div style={{ display: 'flex', gap: 12, marginTop: 6, fontSize: '0.8rem', color: 'var(--text-tertiary)' }}>
                                        {bc.fromAddress && <span>From: {bc.fromAddress}</span>}
                                        <span>{new Date(bc.createdAt).toLocaleDateString()}</span>
                                    </div>
                                </div>
                                <span className={`email-status-badge ${statusClasses[bc.status] || 'es-pending'}`}>{bc.status}</span>
                            </div>

                            {/* Progress for non-draft */}
                            {bc.status !== 'DRAFT' && bc.totalCount > 0 && (
                                <div style={{ marginTop: 12 }}>
                                    <div style={{ height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.06)', overflow: 'hidden' }}>
                                        <div style={{ height: '100%', width: `${Math.round((bc.sentCount / bc.totalCount) * 100)}%`, background: 'rgba(255,255,255,0.3)', borderRadius: 2, transition: 'width 0.3s ease' }} />
                                    </div>
                                    <div style={{ display: 'flex', gap: 16, marginTop: 6, fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
                                        <span>Sent: {bc.sentCount || 0}/{bc.totalCount}</span>
                                        {bc.failedCount > 0 && <span>Failed: {bc.failedCount}</span>}
                                    </div>
                                </div>
                            )}

                            {/* Actions for DRAFT */}
                            {bc.status === 'DRAFT' && (
                                <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
                                    <button className="settings-btn settings-btn-primary" style={{ padding: '7px 14px', fontSize: '0.78rem' }} onClick={() => handleSend(bc.id)}>
                                        <HiOutlinePaperAirplane style={{ marginRight: 4 }} /> Send Now
                                    </button>
                                    <button className="settings-icon-btn settings-danger-icon" onClick={() => handleDelete(bc.id)}><HiOutlineTrash /></button>
                                </div>
                            )}
                        </motion.div>
                    ))}
                </div>
            )}

            {/* Create Modal */}
            <AnimatePresence>
                {showCreate && (
                    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowCreate(false)}>
                        <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={e => e.stopPropagation()}>
                            <div className="conn-modal-header">
                                <h2>New Broadcast</h2>
                                <button className="conn-modal-close" onClick={() => setShowCreate(false)}><HiOutlineX /></button>
                            </div>
                            <div className="conn-modal-body">
                                <label className="conn-form-label">
                                    Template *
                                    <select className="conn-form-input" value={form.templateId} onChange={e => setForm({ ...form, templateId: e.target.value })}>
                                        <option value="">Select a template...</option>
                                        {templates.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
                                    </select>
                                </label>
                                <label className="conn-form-label">
                                    From Address *
                                    <input className="conn-form-input" value={form.fromAddress} onChange={e => setForm({ ...form, fromAddress: e.target.value })} placeholder="hello@yourdomain.com" />
                                </label>
                            </div>
                            <div className="conn-modal-footer">
                                <button className="conn-btn-secondary" onClick={() => setShowCreate(false)}>Cancel</button>
                                <button className="conn-btn-primary" onClick={handleCreate} disabled={saving || !form.templateId || !form.fromAddress}>
                                    {saving ? 'Creating...' : 'Create Draft'}
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
}
