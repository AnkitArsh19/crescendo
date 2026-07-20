import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlinePlus, HiOutlineTrash, HiOutlinePencil,
  HiOutlineTemplate, HiOutlineX, HiOutlineUpload,
  HiOutlineBadgeCheck, HiOutlineDocumentText,
} from 'react-icons/hi';
import { templatesApi } from '../../api/emailServiceApi';
import TemplateBlockEditor from './TemplateBlockEditor';
import './Settings.css';

export default function TemplatesSettings() {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null); // null | 'new' | template object
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [cloneModal, setCloneModal] = useState(false);
  const [broadcastId, setBroadcastId] = useState('');

  const fetchTemplates = async () => {
    setLoading(true);
    try { setTemplates(await templatesApi.list()); } catch { /* */ }
    setLoading(false);
  };

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { fetchTemplates(); }, []);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await templatesApi.delete(deleteTarget);
      setTemplates(templates.filter((t) => t.id !== deleteTarget));
    } catch { /* */ }
    setDeleteTarget(null);
  };

  const handleCloneFromBroadcast = async () => {
    if (!broadcastId.trim()) return;
    try {
      const saved = await templatesApi.cloneFromBroadcast(broadcastId.trim());
      setTemplates(prev => [saved, ...prev]);
      setCloneModal(false);
      setBroadcastId('');
      setEditing(saved);
    } catch { /* */ }
  };

  return (
    <motion.div className="email-templates-page" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
      <div className="settings-section-header">
        <div>
          <h2 className="settings-section-title">Templates</h2>
          <p className="settings-section-desc">
            Build reusable emails with personal details such as <code>{'{{FIRST_NAME}}'}</code>, then publish when they are ready to send.
          </p>
        </div>
        <div className="email-template-actions">
          <button className="settings-btn-secondary" onClick={() => setCloneModal(true)}>
            <HiOutlineUpload /> Clone from Broadcast
          </button>
          <button className="settings-btn-primary" onClick={() => setEditing('new')}>
            <HiOutlinePlus /> New Template
          </button>
        </div>
      </div>

      {loading ? (
        <div className="settings-skeleton-list">{[1, 2].map((i) => <div key={i} className="settings-skeleton-row" />)}</div>
      ) : templates.length === 0 ? (
        <div className="settings-empty">
          <HiOutlineTemplate className="settings-empty-icon" />
          <h3>Create your first template</h3>
          <p>Start from a blank, responsive email and reuse it whenever you need it.</p>
          <button className="settings-btn-primary settings-empty-action" onClick={() => setEditing('new')}>
            <HiOutlinePlus /> Create template
          </button>
        </div>
      ) : (
        <div className="template-grid">
          {templates.map((t) => (
            <motion.div key={t.id} className="template-card" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
              <div className="template-card-header">
                <h3>{t.name}</h3>
                <div className="template-card-actions">
                  <button className="settings-icon-btn" onClick={() => setEditing(t)} title="Edit"><HiOutlinePencil /></button>
                  <button className="settings-icon-btn settings-danger-icon" onClick={() => setDeleteTarget(t.id)} title="Delete"><HiOutlineTrash /></button>
                </div>
              </div>
              <p className="template-subject">{t.subject}</p>
              <div className="template-card-footer">
                <span className={`template-status-badge ${t.status === 'PUBLISHED' ? 'published' : 'draft'}`}>
                  {t.status === 'PUBLISHED' ? <HiOutlineBadgeCheck /> : <HiOutlineDocumentText />}
                  {t.status}
                </span>
                <span className="template-date">
                  {t.updatedAt ? new Date(t.updatedAt).toLocaleDateString() : new Date(t.createdAt).toLocaleDateString()}
                </span>
              </div>
            </motion.div>
          ))}
        </div>
      )}

      {/* Full-screen Template Block Editor */}
      <AnimatePresence>
        {editing && (
          <TemplateBlockEditor
            template={editing === 'new' ? null : editing}
            onClose={() => setEditing(null)}
            onSaved={(saved) => {
              if (editing === 'new') {
                setTemplates(prev => [saved, ...prev]);
              } else {
                setTemplates(prev => prev.map((t) => t.id === saved.id ? saved : t));
              }
              // Keep the editor open with the updated saved template (for publish flow)
              setEditing(saved);
            }}
          />
        )}
      </AnimatePresence>

      {/* Delete Confirmation */}
      <AnimatePresence>
        {deleteTarget && (
          <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setDeleteTarget(null)}>
            <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={(e) => e.stopPropagation()}>
              <div className="conn-modal-header"><h2>Delete Template</h2></div>
              <div className="conn-modal-body"><p style={{ color: 'var(--text-secondary)' }}>This template will be permanently removed. Emails already sent are unaffected.</p></div>
              <div className="conn-modal-footer">
                <button className="conn-btn-secondary" onClick={() => setDeleteTarget(null)}>Cancel</button>
                <button className="conn-btn-danger" onClick={handleDelete}>Delete</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Clone from Broadcast modal */}
      <AnimatePresence>
        {cloneModal && (
          <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setCloneModal(false)}>
            <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={(e) => e.stopPropagation()}>
              <div className="conn-modal-header">
                <h2>Clone from Broadcast</h2>
                <button className="conn-modal-close" onClick={() => setCloneModal(false)}><HiOutlineX /></button>
              </div>
              <div className="conn-modal-body">
                <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>Paste the ID of an existing broadcast to copy its HTML content into a new draft template.</p>
                <label className="conn-form-label">
                  Broadcast ID
                  <input className="conn-form-input" value={broadcastId} onChange={e => setBroadcastId(e.target.value)} placeholder="e.g., 3fa85f64-5717-4562-b3fc-2c963f66afa6" />
                </label>
              </div>
              <div className="conn-modal-footer">
                <button className="conn-btn-secondary" onClick={() => setCloneModal(false)}>Cancel</button>
                <button className="conn-btn-primary" onClick={handleCloneFromBroadcast} disabled={!broadcastId.trim()}>Clone</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
