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

const STARTER_TEMPLATES = [
  {
    name: 'Welcome & Onboarding',
    subject: 'Welcome to Crescendo, {{FIRST_NAME}}',
    contentHtml: '<h1 style="color:#0f172a;">Welcome aboard!</h1><p>Hi {{FIRST_NAME}},</p><p>We are excited to have you join us. Whether you are automating workflows or orchestrating APIs, we are here to help you succeed.</p><p><a href="https://app.crescendo.run" style="background:#6366f1;color:#ffffff;padding:12px 24px;text-decoration:none;border-radius:8px;display:inline-block;font-weight:bold;">Go to Dashboard</a></p><p>Best regards,<br/>The Team</p>'
  },
  {
    name: 'Workflow Failure Alert',
    subject: 'Workflow failed: {{WORKFLOW_NAME}}',
    contentHtml: '<h2 style="color:#ef4444;">Workflow Execution Notice</h2><p>Hi {{FIRST_NAME}},</p><p>Your workflow <strong>{{WORKFLOW_NAME}}</strong> encountered an error during its latest execution on {{DATE}}.</p><p style="background:#fef2f2;border-left:4px solid #ef4444;padding:12px;color:#991b1b;"><strong>Error Details:</strong> {{ERROR_MESSAGE}}</p><p>Please inspect your execution history to resolve the issue.</p><p><a href="https://app.crescendo.run/runs" style="background:#ef4444;color:#ffffff;padding:10px 20px;text-decoration:none;border-radius:6px;display:inline-block;">View Execution Log</a></p>'
  },
  {
    name: 'Monthly Billing & Invoice',
    subject: 'Receipt for order #{{ORDER_ID}}',
    contentHtml: '<h2 style="color:#1e293b;">Thank you for your purchase!</h2><p>Hi {{FIRST_NAME}},</p><p>Here is your summary for invoice <strong>#{{ORDER_ID}}</strong> billed on {{BILLING_DATE}}.</p><table style="width:100%;border-collapse:collapse;margin:20px 0;"><tr style="border-bottom:1px solid #e2e8f0;"><td style="padding:8px 0;"><strong>Plan / Item</strong></td><td style="text-align:right;padding:8px 0;"><strong>Amount</strong></td></tr><tr><td style="padding:12px 0;">{{PLAN_NAME}}</td><td style="text-align:right;padding:12px 0;">${{AMOUNT}}</td></tr></table><p>You can view or download your invoice in your billing account settings.</p>'
  },
  {
    name: 'New Security Login Alert',
    subject: 'Security notice: New login from {{DEVICE_NAME}}',
    contentHtml: '<h2 style="color:#f59e0b;">New Login Detected</h2><p>Hi {{FIRST_NAME}},</p><p>We noticed a sign-in to your account from a new device or location:</p><ul><li><strong>Device:</strong> {{DEVICE_NAME}}</li><li><strong>Location:</strong> {{LOCATION_CITY}}, {{LOCATION_COUNTRY}}</li><li><strong>Time:</strong> {{TIME_TIMESTAMP}}</li></ul><p>If this was you, you can safely ignore this notice. If you did not perform this login, please change your password immediately.</p><p><a href="https://app.crescendo.run/settings/security" style="color:#ef4444;font-weight:bold;">Review security settings</a></p>'
  }
];

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

      <div style={{ margin: '24px 0' }}>
        <h3 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 12, textTransform: 'uppercase', letterSpacing: 0.5 }}>
          Quick Start from Pre-made Transactional Templates
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
          {STARTER_TEMPLATES.map((starter, i) => (
            <div key={i} style={{ background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', borderRadius: 10, padding: 14, cursor: 'pointer', transition: 'border-color 0.2s', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }} onClick={() => setEditing({ ...starter })}>
              <div>
                <h4 style={{ margin: '0 0 6px 0', fontSize: 14, color: 'var(--text-primary)' }}>{starter.name}</h4>
                <p style={{ margin: 0, fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.4 }}>{starter.subject}</p>
              </div>
              <span style={{ marginTop: 12, fontSize: 12, color: 'var(--primary-color)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                Use template
              </span>
            </div>
          ))}
        </div>
      </div>

      <h3 style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', margin: '28px 0 16px 0' }}>Your Custom Templates</h3>

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
