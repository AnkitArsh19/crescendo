import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlinePlus, HiOutlineTrash, HiOutlineGlobe, HiOutlineX, HiOutlineClipboardCopy, HiOutlineRefresh, HiOutlineCheck } from 'react-icons/hi';
import { connectionsApi } from '../../api/connectionsApi';
import { domainsApi } from '../../api/emailServiceApi';
import './Settings.css';
import { HiExclamationCircle } from 'react-icons/hi';

const DOMAIN_STATUS = {
  PENDING:  { label: 'Pending',  className: 'ds-pending' },
  VERIFIED: { label: 'Verified', className: 'ds-verified' },
  FAILED:   { label: 'Failed',   className: 'ds-failed' },
};

export default function DomainsSettings() {
  const [domains, setDomains] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [verifying, setVerifying] = useState(null);
  const [connecting, setConnecting] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const fetchDomains = async () => {
    setLoading(true);
    try { setDomains(await domainsApi.list()); } catch { /* */ }
    setLoading(false);
  };

  useEffect(() => { fetchDomains(); }, []);

  const handleVerify = async (id) => {
    setVerifying(id);
    try {
      const updated = await domainsApi.verify(id);
      setDomains(domains.map((d) => d.id === id ? updated : d));
    } catch { /* */ }
    setVerifying(null);
  };

  const handleConnect = async (id) => {
    setConnecting(id);
    try {
      const { url } = await domainsApi.getDomainConnectUrl(id);
      if (url) {
        window.location.href = url;
      }
    } catch (error) {
      alert(error.response?.data?.error || 'Failed to connect automatically.');
    }
    setConnecting(null);
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await domainsApi.delete(deleteTarget);
      setDomains(domains.filter((d) => d.id !== deleteTarget));
    } catch { /* */ }
    setDeleteTarget(null);
  };

  return (
    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
      <div className="settings-section-header">
        <div>
          <h2 className="settings-section-title">Domains</h2>
          <p className="settings-section-desc">Add and verify custom sending domains for email delivery.</p>
        </div>
        <button className="settings-btn-primary" onClick={() => setShowAdd(true)}>
          <HiOutlinePlus /> Add Domain
        </button>
      </div>

      {loading ? (
        <div className="settings-skeleton-list">{[1, 2].map((i) => <div key={i} className="settings-skeleton-row" />)}</div>
      ) : domains.length === 0 ? (
        <div className="settings-empty">
          <HiOutlineGlobe className="settings-empty-icon" />
          <p>No domains configured. Add a custom sending domain to start.</p>
        </div>
      ) : (
        <div className="domain-list">
          {domains.map((d) => {
            const statusMeta = DOMAIN_STATUS[d.status] || DOMAIN_STATUS.PENDING;
            return (
              <motion.div key={d.id} className="domain-card" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
                <div className="domain-card-header">
                  <div className="domain-card-icon"><HiOutlineGlobe /></div>
                  <div className="domain-card-info">
                    <h3>{d.domainName}</h3>
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '4px' }}>
                      <span className={`domain-status ${statusMeta.className}`}>{statusMeta.label}</span>
                      {d.healthStatus && (
                        <span className={`domain-status ds-${d.healthStatus.toLowerCase()}`} style={{ fontWeight: 'bold' }}>
                          Health: {d.healthStatus}
                        </span>
                      )}
                      {d.warnings && d.warnings.length > 0 && (
                        <div className="settings-warning-icon" title={d.warnings.join('\n')}>
                          <HiExclamationCircle style={{ color: 'var(--alert-red)' }} />
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="domain-card-actions">
                    {d.status !== 'VERIFIED' && (
                      <>
                        <button className="settings-btn-primary" onClick={() => handleConnect(d.id)} disabled={connecting === d.id}>
                          {connecting === d.id ? <HiOutlineRefresh className="spin" /> : <HiOutlineCheck />}
                          {connecting === d.id ? 'Connecting...' : 'Connect Automatically'}
                        </button>
                        <button className="settings-btn-secondary" onClick={() => handleVerify(d.id)} disabled={verifying === d.id}>
                          {verifying === d.id ? <HiOutlineRefresh className="spin" /> : <HiOutlineCheck />}
                          {verifying === d.id ? 'Verifying...' : 'Verify'}
                        </button>
                      </>
                    )}
                    <button className="settings-icon-btn settings-danger-icon" onClick={() => setDeleteTarget(d.id)}>
                      <HiOutlineTrash />
                    </button>
                  </div>
                </div>

                {/* DNS Records */}
                {d.requiredDnsRecords && d.requiredDnsRecords.length > 0 && (
                  <div className="domain-dns">
                    <h4>Required DNS Records</h4>
                    {d.requiredDnsRecords.map((rec, i) => (
                      <div key={i} className="dns-record">
                        <div className="dns-record-row">
                          <span className="dns-label">Type</span><code>{rec.type}</code>
                        </div>
                        <div className="dns-record-row">
                          <span className="dns-label">Name</span><code>{rec.name}</code>
                          <button className="dns-copy" onClick={() => navigator.clipboard.writeText(rec.name)}><HiOutlineClipboardCopy /></button>
                        </div>
                        <div className="dns-record-row">
                          <span className="dns-label">Value</span><code>{rec.value}</code>
                          <button className="dns-copy" onClick={() => navigator.clipboard.writeText(rec.value)}><HiOutlineClipboardCopy /></button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </motion.div>
            );
          })}
        </div>
      )}

      {/* Add Domain Modal */}
      <AnimatePresence>
        {showAdd && (
          <AddDomainModal
            onClose={() => setShowAdd(false)}
            onAdded={(d) => { setDomains([...domains, d]); setShowAdd(false); }}
          />
        )}
      </AnimatePresence>

      {/* Delete Confirmation */}
      <AnimatePresence>
        {deleteTarget && (
          <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setDeleteTarget(null)}>
            <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={(e) => e.stopPropagation()}>
              <div className="conn-modal-header"><h2>Delete Domain</h2></div>
              <div className="conn-modal-body"><p style={{ color: 'var(--text-secondary)' }}>Emails from this domain will no longer be deliverable.</p></div>
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

function AddDomainModal({ onClose, onAdded }) {
  const [domainName, setDomainName] = useState('');
  const [allowedEmailType, setAllowedEmailType] = useState('TRANSACTIONAL_ONLY');
  const [emailProviderConnectionId, setEmailProviderConnectionId] = useState('');
  const [connections, setConnections] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState('');

  useEffect(() => {
    connectionsApi.list().then(conns => {
      // Filter for providers that support email sending (e.g. SendGrid, Postmark)
      setConnections(conns.filter(c => c.appKey === 'sendgrid' || c.appKey === 'postmark'));
    }).catch(() => {});
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!domainName.trim()) { setErr('Domain name required'); return; }
    setSubmitting(true); setErr('');
    try {
      const payload = {
        domainName: domainName.trim(),
        allowedEmailType: allowedEmailType,
        emailProviderConnectionId: emailProviderConnectionId || null
      };
      const data = await domainsApi.add(payload);
      onAdded(data);
    } catch (error) {
      setErr(error.response?.data?.message || 'Failed to add domain');
    } finally { setSubmitting(false); }
  };

  return (
    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.form className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <div className="conn-modal-header">
          <h2>Add Domain</h2>
          <button type="button" className="conn-modal-close" onClick={onClose}><HiOutlineX /></button>
        </div>
        <div className="conn-modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {err && <div className="conn-modal-error">{err}</div>}
          <label className="conn-form-label">
            Domain Name
            <input className="conn-form-input" value={domainName} onChange={(e) => setDomainName(e.target.value)} placeholder="mail.example.com" autoFocus />
          </label>
          <label className="conn-form-label">
            Usage Type
            <select className="conn-form-input" value={allowedEmailType} onChange={(e) => setAllowedEmailType(e.target.value)}>
              <option value="TRANSACTIONAL_ONLY">Transactional Only (Recommended)</option>
              <option value="MARKETING_AND_BULK">Marketing and Bulk</option>
              <option value="BOTH">Both</option>
            </select>
          </label>
          <label className="conn-form-label">
            Email Provider (BYOK) - Optional
            <select className="conn-form-input" value={emailProviderConnectionId} onChange={(e) => setEmailProviderConnectionId(e.target.value)}>
              <option value="">Crescendo Shared Infrastructure (Default)</option>
              {connections.map(c => (
                <option key={c.id} value={c.id}>{c.name} ({c.appKey})</option>
              ))}
            </select>
          </label>
        </div>
        <div className="conn-modal-footer">
          <button type="button" className="conn-btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="conn-btn-primary" disabled={submitting}>{submitting ? 'Adding...' : 'Add Domain'}</button>
        </div>
      </motion.form>
    </motion.div>
  );
}
