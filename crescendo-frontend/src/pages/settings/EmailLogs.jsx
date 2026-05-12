import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlineMail, HiOutlinePlus, HiOutlineX, HiOutlineChevronDown, HiOutlineChevronUp } from 'react-icons/hi';
import { emailsApi, templatesApi } from '../../api/emailServiceApi';
import './Settings.css';

const EMAIL_STATUS = {
  PENDING:   { label: 'Pending',   className: 'es-pending' },
  SENT:      { label: 'Sent',      className: 'es-sent' },
  DELIVERED: { label: 'Delivered', className: 'es-delivered' },
  BOUNCED:   { label: 'Bounced',   className: 'es-bounced' },
  FAILED:    { label: 'Failed',    className: 'es-failed' },
};

export default function EmailLogs() {
  const [emails, setEmails] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);
  const [showSend, setShowSend] = useState(false);

  const fetchEmails = async () => {
    setLoading(true);
    try { setEmails(await emailsApi.list()); } catch { /* */ }
    setLoading(false);
  };

  useEffect(() => { fetchEmails(); }, []);

  return (
    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
      <div className="settings-section-header">
        <div>
          <h2 className="settings-section-title">Email Logs</h2>
          <p className="settings-section-desc">Track sent emails and their delivery status.</p>
        </div>
        <button className="settings-btn-primary" onClick={() => setShowSend(true)}>
          <HiOutlinePlus /> Send Test Email
        </button>
      </div>

      {loading ? (
        <div className="settings-skeleton-list">{[1, 2, 3].map((i) => <div key={i} className="settings-skeleton-row" />)}</div>
      ) : emails.length === 0 ? (
        <div className="settings-empty">
          <HiOutlineMail className="settings-empty-icon" />
          <p>No emails sent yet. Send a test email to get started.</p>
        </div>
      ) : (
        <div className="email-log-list">
          {emails.map((email) => {
            const statusMeta = EMAIL_STATUS[email.status] || EMAIL_STATUS.PENDING;
            const isExpanded = expanded === email.id;
            return (
              <motion.div key={email.id} className="email-log-item" layout>
                <button className="email-log-row" onClick={() => setExpanded(isExpanded ? null : email.id)}>
                  <span className={`email-status-badge ${statusMeta.className}`}>{statusMeta.label}</span>
                  <span className="email-log-to">{email.to}</span>
                  <span className="email-log-subject">{email.subject}</span>
                  <span className="email-log-date">{email.createdAt ? new Date(email.createdAt).toLocaleString() : '—'}</span>
                  {isExpanded ? <HiOutlineChevronUp /> : <HiOutlineChevronDown />}
                </button>
                <AnimatePresence>
                  {isExpanded && (
                    <motion.div className="email-log-detail" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
                      <div className="email-detail-grid">
                        <div><span className="email-detail-label">From</span><span>{email.from}</span></div>
                        <div><span className="email-detail-label">To</span><span>{email.to}</span></div>
                        <div><span className="email-detail-label">Provider</span><span>{email.provider || '—'}</span></div>
                        <div><span className="email-detail-label">Message ID</span><code>{email.providerMessageId || '—'}</code></div>
                        {email.error && <div><span className="email-detail-label">Error</span><span className="email-detail-error">{email.error}</span></div>}
                        {email.sentAt && <div><span className="email-detail-label">Sent At</span><span>{new Date(email.sentAt).toLocaleString()}</span></div>}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            );
          })}
        </div>
      )}

      {/* Send Test Email Modal */}
      <AnimatePresence>
        {showSend && (
          <SendEmailModal
            onClose={() => setShowSend(false)}
            onSent={(email) => { setEmails([email, ...emails]); setShowSend(false); }}
          />
        )}
      </AnimatePresence>
    </motion.div>
  );
}

function SendEmailModal({ onClose, onSent }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [subject, setSubject] = useState('');
  const [htmlBody, setHtmlBody] = useState('');
  const [textBody, setTextBody] = useState('');
  const [templateId, setTemplateId] = useState('');
  const [templates, setTemplates] = useState([]);
  const [useTemplate, setUseTemplate] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState('');

  useEffect(() => {
    templatesApi.list().then(setTemplates).catch(() => setTemplates([]));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!from.trim() || !to.trim() || !subject.trim()) { setErr('From, To, and Subject are required'); return; }
    setSubmitting(true); setErr('');
    try {
      const payload = { from: from.trim(), to: to.trim(), subject: subject.trim() };
      if (useTemplate && templateId) {
        payload.templateId = templateId;
      } else {
        payload.htmlBody = htmlBody;
        payload.textBody = textBody;
      }
      const data = await emailsApi.send(payload);
      onSent(data);
    } catch (error) {
      setErr(error.response?.data?.message || 'Failed to send email');
    } finally { setSubmitting(false); }
  };

  return (
    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.form
        className="conn-modal"
        style={{ maxWidth: 520 }}
        initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
        onClick={(e) => e.stopPropagation()}
        onSubmit={handleSubmit}
      >
        <div className="conn-modal-header">
          <h2>Send Test Email</h2>
          <button type="button" className="conn-modal-close" onClick={onClose}><HiOutlineX /></button>
        </div>
        <div className="conn-modal-body" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
          {err && <div className="conn-modal-error">{err}</div>}

          <label className="conn-form-label">
            From <input className="conn-form-input" value={from} onChange={(e) => setFrom(e.target.value)} placeholder="hello@yourdomain.com" />
          </label>
          <label className="conn-form-label">
            To <input className="conn-form-input" value={to} onChange={(e) => setTo(e.target.value)} placeholder="recipient@example.com" />
          </label>
          <label className="conn-form-label">
            Subject <input className="conn-form-input" value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="Test email subject" />
          </label>

          <div className="email-send-toggle">
            <button type="button" className={`email-toggle-btn ${!useTemplate ? 'active' : ''}`} onClick={() => setUseTemplate(false)}>Raw Body</button>
            <button type="button" className={`email-toggle-btn ${useTemplate ? 'active' : ''}`} onClick={() => setUseTemplate(true)}>Use Template</button>
          </div>

          {useTemplate ? (
            <label className="conn-form-label">
              Template
              <select className="conn-form-input" value={templateId} onChange={(e) => setTemplateId(e.target.value)}>
                <option value="">Select a template...</option>
                {templates.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </label>
          ) : (
            <>
              <label className="conn-form-label">
                HTML Body <textarea className="conn-form-textarea" value={htmlBody} onChange={(e) => setHtmlBody(e.target.value)} placeholder="<h1>Hello</h1>" rows={4} />
              </label>
              <label className="conn-form-label">
                Text Body <textarea className="conn-form-textarea" value={textBody} onChange={(e) => setTextBody(e.target.value)} placeholder="Hello, plain text fallback" rows={2} />
              </label>
            </>
          )}
        </div>
        <div className="conn-modal-footer">
          <button type="button" className="conn-btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="conn-btn-primary" disabled={submitting}>{submitting ? 'Sending...' : 'Send Email'}</button>
        </div>
      </motion.form>
    </motion.div>
  );
}
