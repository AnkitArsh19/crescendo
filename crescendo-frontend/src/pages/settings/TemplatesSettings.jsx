import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlinePlus, HiOutlineTrash, HiOutlinePencil, HiOutlineTemplate, HiOutlineX } from 'react-icons/hi';
import { templatesApi } from '../../api/emailServiceApi';
import './Settings.css';

export default function TemplatesSettings() {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null); // null | 'new' | template object
  const [deleteTarget, setDeleteTarget] = useState(null);

  const fetchTemplates = async () => {
    setLoading(true);
    try { setTemplates(await templatesApi.list()); } catch { /* */ }
    setLoading(false);
  };

  useEffect(() => { fetchTemplates(); }, []);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await templatesApi.delete(deleteTarget);
      setTemplates(templates.filter((t) => t.id !== deleteTarget));
    } catch { /* */ }
    setDeleteTarget(null);
  };

  return (
    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
      <div className="settings-section-header">
        <div>
          <h2 className="settings-section-title">Email Templates</h2>
          <p className="settings-section-desc">Create reusable templates with <code>{'{{variable}}'}</code> interpolation.</p>
        </div>
        <button className="settings-btn-primary" onClick={() => setEditing('new')}>
          <HiOutlinePlus /> Create Template
        </button>
      </div>

      {loading ? (
        <div className="settings-skeleton-list">{[1, 2].map((i) => <div key={i} className="settings-skeleton-row" />)}</div>
      ) : templates.length === 0 ? (
        <div className="settings-empty">
          <HiOutlineTemplate className="settings-empty-icon" />
          <p>No templates yet. Create one to streamline your emails.</p>
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
              <span className="template-date">
                Updated {t.updatedAt ? new Date(t.updatedAt).toLocaleDateString() : new Date(t.createdAt).toLocaleDateString()}
              </span>
            </motion.div>
          ))}
        </div>
      )}

      {/* Template Editor Modal */}
      <AnimatePresence>
        {editing && (
          <TemplateEditorModal
            template={editing === 'new' ? null : editing}
            onClose={() => setEditing(null)}
            onSaved={(saved) => {
              if (editing === 'new') {
                setTemplates([...templates, saved]);
              } else {
                setTemplates(templates.map((t) => t.id === saved.id ? saved : t));
              }
              setEditing(null);
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
              <div className="conn-modal-body"><p style={{ color: 'var(--text-secondary)' }}>This template will be permanently removed.</p></div>
              <div className="conn-modal-footer">
                <button className="conn-btn-secondary" onClick={() => setDeleteTarget(null)}>Cancel</button>
                <button className="conn-btn-danger" onClick={handleDelete}>Delete</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

function TemplateEditorModal({ template, onClose, onSaved }) {
  const [name, setName] = useState(template?.name || '');
  const [subject, setSubject] = useState(template?.subject || '');
  const [htmlBody, setHtmlBody] = useState(template?.htmlBody || '');
  const [textBody, setTextBody] = useState(template?.textBody || '');
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState('');

  const isNew = !template;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim() || !subject.trim()) { setErr('Name and subject are required'); return; }
    setSubmitting(true); setErr('');
    try {
      const payload = { name: name.trim(), subject: subject.trim(), htmlBody, textBody };
      const saved = isNew
        ? await templatesApi.create(payload)
        : await templatesApi.update(template.id, payload);
      onSaved(saved);
    } catch (error) {
      setErr(error.response?.data?.message || 'Failed to save template');
    } finally { setSubmitting(false); }
  };

  return (
    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.form
        className="conn-modal"
        style={{ maxWidth: 600 }}
        initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
        onClick={(e) => e.stopPropagation()}
        onSubmit={handleSubmit}
      >
        <div className="conn-modal-header">
          <h2>{isNew ? 'Create Template' : 'Edit Template'}</h2>
          <button type="button" className="conn-modal-close" onClick={onClose}><HiOutlineX /></button>
        </div>
        <div className="conn-modal-body" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
          {err && <div className="conn-modal-error">{err}</div>}

          <label className="conn-form-label">
            Template Name
            <input className="conn-form-input" value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g., Welcome Email" />
          </label>

          <label className="conn-form-label">
            Subject
            <input className="conn-form-input" value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="Welcome to {{appName}}" />
          </label>

          <label className="conn-form-label">
            HTML Body
            <textarea className="conn-form-textarea" value={htmlBody} onChange={(e) => setHtmlBody(e.target.value)} placeholder="<h1>Hello {{name}}</h1>" rows={6} />
          </label>

          <label className="conn-form-label">
            Text Body
            <textarea className="conn-form-textarea" value={textBody} onChange={(e) => setTextBody(e.target.value)} placeholder="Hello {{name}}, welcome aboard!" rows={3} />
          </label>
        </div>
        <div className="conn-modal-footer">
          <button type="button" className="conn-btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="conn-btn-primary" disabled={submitting}>
            {submitting ? 'Saving...' : (isNew ? 'Create' : 'Save Changes')}
          </button>
        </div>
      </motion.form>
    </motion.div>
  );
}
