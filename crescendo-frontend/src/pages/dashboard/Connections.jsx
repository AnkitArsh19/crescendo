/* eslint-disable no-unused-vars */
/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlinePlus, HiOutlineTrash, HiOutlinePencil, HiOutlineLink,
  HiOutlineX, HiOutlineRefresh, HiOutlineSearch,
  HiOutlineShieldCheck, HiOutlineExternalLink, HiOutlineKey,
  HiOutlineLockClosed, HiOutlineArrowLeft, HiOutlineEye, HiOutlineEyeOff,
  HiOutlineChevronDown, HiOutlineChevronRight, HiOutlineInformationCircle,
} from 'react-icons/hi';
import useConnectionStore from '../../store/connectionStore';
import { appCatalogApi } from '../../api/appCatalogApi';
import AppBrowserModal from './nodes/AppBrowserModal';
import './Connections.css';

// ─── Constants ──────────────────────────────────────────────────────────────────

const STATUS_META = {
  ACTIVE:  { label: 'Active',  className: 'status-active' },
  ERROR:   { label: 'Error',   className: 'status-error' },
  REAUTH:  { label: 'Re-auth', className: 'status-reauth' },
};

const CATEGORY_LABELS = {
  communication: 'Communication',
  productivity: 'Productivity',
  developer: 'Developer Tools',
  ai: 'AI & Machine Learning',
  payments: 'Payments & Commerce',
  fun: 'Fun & Lifestyle',
};

const CATEGORY_ORDER = ['communication', 'productivity', 'developer', 'ai', 'payments', 'fun'];

// ─── Main Component ─────────────────────────────────────────────────────────────

export default function Connections() {
  const { connections, isLoading, error, fetchConnections, createConnection, deleteConnection } = useConnectionStore();
  const [apps, setApps] = useState([]);
  const [showAddModal, setShowAddModal] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [search, setSearch] = useState('');
  const [preselectedAppKey, setPreselectedAppKey] = useState(null);

  useEffect(() => {
    fetchConnections();
    appCatalogApi.list()
      .then(setApps)
      .catch(() => setApps([]));
  }, [fetchConnections]);

  // Check for OAuth callback success
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('connected')) {
      fetchConnections(); // refresh after OAuth callback
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [fetchConnections]);

  // Handle ?connect=<appKey> from canvas redirect — auto-open add modal
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const connectKey = params.get('connect');
    if (connectKey && apps.length > 0) {
      setPreselectedAppKey(connectKey);
      setShowAddModal(true);
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [apps]);

  const filtered = connections.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase()) ||
    c.appKey.toLowerCase().includes(search.toLowerCase())
  );

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try { await deleteConnection(deleteTarget); } catch { /* store handles */ }
    setDeleteTarget(null);
  };

  return (
    <div className="connections-page">
      {/* Header */}
      <div className="connections-header">
        <div>
          <h1 className="connections-title">Connections</h1>
          <p className="connections-subtitle">
            Securely connect your apps — credentials are encrypted at rest with AES-256
          </p>
        </div>
        <button className="conn-btn-primary" onClick={() => setShowAddModal(true)}>
          <HiOutlinePlus /> Add Connection
        </button>
      </div>

      {/* Search */}
      {connections.length > 0 && (
        <div className="connections-search-bar">
          <HiOutlineSearch className="conn-search-icon" />
          <input
            type="text"
            placeholder="Search connections..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="conn-search-input"
          />
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="conn-error-banner">
          {error}
          <button onClick={fetchConnections} className="conn-retry-btn"><HiOutlineRefresh /> Retry</button>
        </div>
      )}

      {/* Loading */}
      {isLoading && (
        <div className="connections-grid">
          {[1, 2, 3].map((i) => (
            <div key={i} className="conn-card conn-skeleton">
              <div className="skel-line skel-title" />
              <div className="skel-line skel-sub" />
              <div className="skel-line skel-badge" />
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {!isLoading && connections.length === 0 && !error && (
        <motion.div className="conn-empty" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}>
          <div className="conn-empty-icon"><HiOutlineLink /></div>
          <h2>No connections yet</h2>
          <p>Connect your apps to start building powerful automated workflows.</p>
          <button className="conn-btn-primary" onClick={() => setShowAddModal(true)}>
            <HiOutlinePlus /> Add your first connection
          </button>
        </motion.div>
      )}

      {/* Connection Cards */}
      {!isLoading && filtered.length > 0 && (
        <div className="connections-grid">
          <AnimatePresence mode="popLayout">
            {filtered.map((conn, i) => {
              const statusMeta = STATUS_META[conn.status] || STATUS_META.ACTIVE;
              const matchingApp = apps.find(a => a.appKey === conn.appKey);
              return (
                <motion.div key={conn.id} className="conn-card"
                  initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95 }} transition={{ duration: 0.3, delay: i * 0.05 }} layout
                >
                  <div className="conn-card-header">
                    <div className="conn-card-app-icon">
                      {matchingApp ? (
                        <img 
                          src={matchingApp.logoUrl || `/icons/${conn.appKey}.svg`}
                          alt={matchingApp.name || conn.appKey}
                          className="app-logo-img"
                          onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }}
                        />
                      ) : null}
                      <span style={{ display: matchingApp ? 'none' : 'block' }}>
                        {matchingApp?.name?.charAt(0).toUpperCase() || conn.appKey?.charAt(0).toUpperCase()}
                      </span>
                    </div>
                    <div className="conn-card-info">
                      <h3 className="conn-card-name">{conn.name}</h3>
                      <span className="conn-card-app">{matchingApp?.name || conn.appKey}</span>
                    </div>
                    <span className={`conn-status-badge ${statusMeta.className}`}>
                      <span className="conn-status-dot" />
                      {statusMeta.label}
                    </span>
                  </div>
                  <div className="conn-card-meta">
                    <span>Created {new Date(conn.createdAt).toLocaleDateString()}</span>
                    {conn.updatedAt && <span>Updated {new Date(conn.updatedAt).toLocaleDateString()}</span>}
                  </div>
                  <div className="conn-card-actions">
                    <button className="conn-action-btn" title="Edit"><HiOutlinePencil /></button>
                    <button className="conn-action-btn conn-action-danger" title="Delete" onClick={() => setDeleteTarget(conn.id)}>
                      <HiOutlineTrash />
                    </button>
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>
      )}

      {/* No search results */}
      {!isLoading && connections.length > 0 && filtered.length === 0 && (
        <div className="conn-empty-search"><p>No connections match &ldquo;{search}&rdquo;</p></div>
      )}

      {/* Add Connection Modal — uses the same AppBrowserModal as canvas */}
      <AnimatePresence>
        {showAddModal && (
          <AppBrowserModal
            apps={apps}
            connections={connections}
            connectOnly={true}
            title="Add Connection"
            onClose={() => { setShowAddModal(false); setPreselectedAppKey(null); }}
            onConnected={() => { fetchConnections(); }}
          />
        )}
      </AnimatePresence>

      {/* Delete Confirmation */}
      <AnimatePresence>
        {deleteTarget && (
          <ConfirmDeleteModal onCancel={() => setDeleteTarget(null)} onConfirm={handleDelete} />
        )}
      </AnimatePresence>
    </div>
  );
}


// ─── Confirm Delete Modal ───────────────────────────────────────────────────────

function ConfirmDeleteModal({ onCancel, onConfirm }) {
  const [loading, setLoading] = useState(false);
  const handleConfirm = async () => {
    setLoading(true);
    await onConfirm();
    setLoading(false);
  };

  return (
    <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onCancel}>
      <motion.div className="conn-modal conn-modal-sm"
        initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }} transition={{ duration: 0.2 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="conn-modal-header">
          <h2>Delete Connection</h2>
          <button className="conn-modal-close" onClick={onCancel}><HiOutlineX /></button>
        </div>
        <div className="conn-modal-body">
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            This will permanently remove this connection and any workflows using it may stop functioning. This action cannot be undone.
          </p>
        </div>
        <div className="conn-modal-footer">
          <button className="conn-btn-secondary" onClick={onCancel}>Cancel</button>
          <button className="conn-btn-danger" onClick={handleConfirm} disabled={loading}>
            {loading ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </motion.div>
    </motion.div>
  );
}

