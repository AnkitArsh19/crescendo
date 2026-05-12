import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlinePlus, HiOutlineTrash, HiOutlinePencil, HiOutlineX } from 'react-icons/hi';
import useContactStore from '../../../store/contactStore';
import '../../settings/Settings.css';

export default function ContactsPage() {
    const { contacts, loading, fetchContacts, createContact, updateContact, deleteContact } = useContactStore();
    const [showCreate, setShowCreate] = useState(false);
    const [editId, setEditId] = useState(null);
    const [form, setForm] = useState({ email: '', firstName: '', lastName: '' });
    const [saving, setSaving] = useState(false);

    useEffect(() => { fetchContacts(); }, [fetchContacts]);

    const resetForm = () => { setForm({ email: '', firstName: '', lastName: '' }); setEditId(null); setShowCreate(false); };

    const handleSave = async () => {
        if (!form.email) return;
        setSaving(true);
        try {
            if (editId) {
                await updateContact(editId, form);
            } else {
                await createContact(form);
            }
            resetForm();
        } catch { /* toast */ }
        setSaving(false);
    };

    const handleToggleSubscribed = async (c) => {
        await updateContact(c.id, { subscribed: !c.subscribed });
    };

    const openEdit = (c) => {
        setForm({ email: c.email, firstName: c.firstName || '', lastName: c.lastName || '' });
        setEditId(c.id);
        setShowCreate(true);
    };

    if (loading) {
        return (
            <div className="settings-skeleton-list">
                {[...Array(4)].map((_, i) => <div key={i} className="settings-skeleton-row" />)}
            </div>
        );
    }

    return (
        <>
            <div className="settings-section-header">
                <div>
                    <h2 className="settings-section-title">Contacts</h2>
                    <p className="settings-section-desc">Manage your audience for broadcast campaigns.</p>
                </div>
                <button className="settings-btn settings-btn-primary" onClick={() => { resetForm(); setShowCreate(true); }}>
                    <HiOutlinePlus /> Add Contact
                </button>
            </div>

            {contacts.length === 0 ? (
                <div className="settings-empty">
                    <div className="settings-empty-icon">📧</div>
                    <p>No contacts yet. Add your first contact to start building your audience.</p>
                </div>
            ) : (
                <div className="settings-table">
                    <div className="settings-table-head" style={{ gridTemplateColumns: '2fr 1fr 1fr 80px 80px' }}>
                        <span>Email</span><span>First Name</span><span>Last Name</span><span>Subscribed</span><span></span>
                    </div>
                    {contacts.map((c) => (
                        <motion.div
                            key={c.id}
                            className="settings-table-row"
                            style={{ gridTemplateColumns: '2fr 1fr 1fr 80px 80px' }}
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                        >
                            <span className="settings-table-cell-name">{c.email}</span>
                            <span>{c.firstName || '—'}</span>
                            <span>{c.lastName || '—'}</span>
                            <span>
                                <button
                                    className={`contact-toggle ${c.subscribed !== false ? 'on' : ''}`}
                                    onClick={() => handleToggleSubscribed(c)}
                                    title={c.subscribed !== false ? 'Subscribed' : 'Unsubscribed'}
                                />
                            </span>
                            <span style={{ display: 'flex', gap: 4 }}>
                                <button className="settings-icon-btn" onClick={() => openEdit(c)}><HiOutlinePencil /></button>
                                <button className="settings-icon-btn settings-danger-icon" onClick={() => deleteContact(c.id)}><HiOutlineTrash /></button>
                            </span>
                        </motion.div>
                    ))}
                </div>
            )}

            {/* Create / Edit Modal */}
            <AnimatePresence>
                {showCreate && (
                    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={resetForm}>
                        <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={e => e.stopPropagation()}>
                            <div className="conn-modal-header">
                                <h2>{editId ? 'Edit Contact' : 'Add Contact'}</h2>
                                <button className="conn-modal-close" onClick={resetForm}><HiOutlineX /></button>
                            </div>
                            <div className="conn-modal-body">
                                <label className="conn-form-label">
                                    Email *
                                    <input className="conn-form-input" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} placeholder="user@example.com" disabled={!!editId} />
                                </label>
                                <label className="conn-form-label">
                                    First Name
                                    <input className="conn-form-input" value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })} placeholder="Jane" />
                                </label>
                                <label className="conn-form-label">
                                    Last Name
                                    <input className="conn-form-input" value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })} placeholder="Doe" />
                                </label>
                            </div>
                            <div className="conn-modal-footer">
                                <button className="conn-btn-secondary" onClick={resetForm}>Cancel</button>
                                <button className="conn-btn-primary" onClick={handleSave} disabled={saving || !form.email}>
                                    {saving ? 'Saving...' : editId ? 'Update' : 'Add Contact'}
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
}
