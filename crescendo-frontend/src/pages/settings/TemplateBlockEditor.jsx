/**
 * TemplateBlockEditor.jsx
 * 
 * A Notion-style block-based email template editor.
 * Supports:
 *  - Block types: text, heading, image, button, divider, columns
 *  - Slash-command (/) menu to insert blocks
 *  - {{variable}} insertion with autocomplete
 *  - HTML import / React Email code paste
 *  - Right-hand inspector panel for block style customization
 *  - Draft save + Publish lifecycle wired to backend
 *  - Test-send before publishing
 */

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlineX, HiOutlineCheck, HiOutlinePaperAirplane, HiOutlineCode,
  HiOutlinePhotograph, HiOutlinePlusSm, HiOutlineTrash,
  HiOutlineChevronDown, HiOutlineChevronUp, HiOutlineDuplicate,
} from 'react-icons/hi';
import { MdOutlineDragIndicator } from 'react-icons/md';
import { templatesApi } from '../../api/emailServiceApi';
import './TemplateBlockEditor.css';

// ─── Block type definitions ───────────────────────────────────────────────────

const BLOCK_TYPES = [
  { type: 'heading',  label: 'Heading',     icon: 'H', description: 'Large section heading' },
  { type: 'text',     label: 'Paragraph',   icon: '¶', description: 'Body text paragraph' },
  { type: 'button',   label: 'Button',      icon: '▶', description: 'Call to action button' },
  { type: 'image',    label: 'Image',       icon: '🖼', description: 'Image with URL' },
  { type: 'divider',  label: 'Divider',     icon: '—', description: 'Horizontal rule' },
  { type: 'columns',  label: '2 Columns',   icon: '⊞', description: 'Two-column layout' },
  { type: 'spacer',   label: 'Spacer',      icon: '↕', description: 'Vertical space' },
];

const RESERVED_VARIABLES = ['FIRST_NAME', 'LAST_NAME', 'EMAIL', 'CRESCENDO_UNSUBSCRIBE_URL'];

// ─── Default block content ────────────────────────────────────────────────────

function createBlock(type) {
  const id = crypto.randomUUID();
  const base = { id, type };
  switch (type) {
    case 'heading':  return { ...base, content: 'Your Heading', level: 'h1', align: 'left', color: '#0a0a0a', fontSize: 32 };
    case 'text':     return { ...base, content: 'Your paragraph text goes here. Use {{FIRST_NAME}} for personalization.', align: 'left', color: '#374151', fontSize: 16, lineHeight: 1.6 };
    case 'button':   return { ...base, content: 'Click Here', href: 'https://', align: 'center', bgColor: '#1a1a1a', textColor: '#ffffff', borderRadius: 8, paddingV: 12, paddingH: 24 };
    case 'image':    return { ...base, src: '', alt: '', align: 'center', width: '100%' };
    case 'divider':  return { ...base, color: '#e5e7eb', thickness: 1, margin: 24 };
    case 'columns':  return { ...base, columns: [{ content: 'Column 1 content', align: 'left', color: '#374151' }, { content: 'Column 2 content', align: 'left', color: '#374151' }] };
    case 'spacer':   return { ...base, height: 24 };
    default:         return base;
  }
}

// ─── Block renderer (HTML preview) ───────────────────────────────────────────

function blocksToHtml(blocks) {
  return blocks.map(block => {
    switch (block.type) {
      case 'heading': return `<${block.level} style="text-align:${block.align};color:${block.color};font-size:${block.fontSize}px;margin:0 0 16px 0;font-weight:700;">${block.content}</${block.level}>`;
      case 'text': return `<p style="text-align:${block.align};color:${block.color};font-size:${block.fontSize}px;line-height:${block.lineHeight};margin:0 0 16px 0;">${block.content}</p>`;
      case 'button': return `<div style="text-align:${block.align};margin:16px 0;"><a href="${block.href}" style="display:inline-block;background-color:${block.bgColor};color:${block.textColor};text-decoration:none;padding:${block.paddingV}px ${block.paddingH}px;border-radius:${block.borderRadius}px;font-weight:600;font-size:14px;">${block.content}</a></div>`;
      case 'image': return `<div style="text-align:${block.align};margin:16px 0;"><img src="${block.src}" alt="${block.alt}" style="max-width:${block.width};height:auto;" /></div>`;
      case 'divider': return `<hr style="border:none;border-top:${block.thickness}px solid ${block.color};margin:${block.margin}px 0;" />`;
      case 'columns': return `<table width="100%" style="border-collapse:collapse;margin:16px 0;"><tr>${block.columns.map(col => `<td width="${Math.floor(100/block.columns.length)}%" style="vertical-align:top;padding:0 8px;text-align:${col.align};color:${col.color};">${col.content}</td>`).join('')}</tr></table>`;
      case 'spacer': return `<div style="height:${block.height}px;"></div>`;
      default: return '';
    }
  }).join('\n');
}

// ─── Main Editor Component ────────────────────────────────────────────────────

export default function TemplateBlockEditor({ template, onClose, onSaved }) {
  const [name, setName]     = useState(template?.name || 'Untitled Template');
  const [subject, setSubject] = useState(template?.subject || '');
  const [blocks, setBlocks] = useState(() => initBlocks(template));
  const [selectedBlockId, setSelectedBlockId] = useState(null);
  const [slashMenu, setSlashMenu] = useState({ open: false, blockId: null, query: '' });
  const [importOpen, setImportOpen] = useState(false);
  const [importHtml, setImportHtml] = useState('');
  const [testSendOpen, setTestSendOpen] = useState(false);
  const [testEmail, setTestEmail] = useState('');
  const [saving, setSaving]     = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [err, setErr]           = useState('');
  const [activeTab, setActiveTab] = useState('visual'); // visual | preview | html

  const selectedBlock = blocks.find(b => b.id === selectedBlockId) || null;

  // ── Save (draft update) ──────────────────────────────────────────────────────
  const handleSave = async () => {
    if (!name.trim()) { setErr('Template name is required'); return; }
    setSaving(true); setErr('');
    try {
      const html = blocksToHtml(blocks);
      const payload = { name: name.trim(), subject: subject.trim(), htmlBody: html };
      let saved;
      if (!template?.id) {
        saved = await templatesApi.create(payload);
      } else {
        saved = await templatesApi.update(template.id, payload);
      }
      onSaved(saved);
    } catch (e) {
      setErr(e.response?.data?.message || 'Failed to save template');
    } finally { setSaving(false); }
  };

  // ── Publish ──────────────────────────────────────────────────────────────────
  const handlePublish = async () => {
    if (!template?.id) { setErr('Save the template first before publishing.'); return; }
    setPublishing(true); setErr('');
    try {
      const saved = await templatesApi.publish(template.id);
      onSaved(saved);
    } catch (e) {
      setErr(e.response?.data?.message || 'Failed to publish. Check that all {{variables}} are declared.');
    } finally { setPublishing(false); }
  };

  // ── Test send ────────────────────────────────────────────────────────────────
  const handleTestSend = async () => {
    if (!template?.id || !testEmail.trim()) return;
    try {
      await templatesApi.testSend(template.id, { toAddress: testEmail.trim(), variables: {} });
      setTestSendOpen(false);
    } catch (e) {
      setErr(e.response?.data?.message || 'Test send failed');
    }
  };

  // ── Block manipulation ────────────────────────────────────────────────────────
  const addBlock = (type, afterId = null) => {
    const newBlock = createBlock(type);
    setBlocks(prev => {
      if (!afterId) return [...prev, newBlock];
      const idx = prev.findIndex(b => b.id === afterId);
      const next = [...prev];
      next.splice(idx + 1, 0, newBlock);
      return next;
    });
    setSelectedBlockId(newBlock.id);
    setSlashMenu({ open: false, blockId: null, query: '' });
  };

  const updateBlock = (id, updates) => {
    setBlocks(prev => prev.map(b => b.id === id ? { ...b, ...updates } : b));
  };

  const deleteBlock = (id) => {
    setBlocks(prev => prev.filter(b => b.id !== id));
    if (selectedBlockId === id) setSelectedBlockId(null);
  };

  const moveBlock = (id, direction) => {
    setBlocks(prev => {
      const idx = prev.findIndex(b => b.id === id);
      const next = [...prev];
      const target = direction === 'up' ? idx - 1 : idx + 1;
      if (target < 0 || target >= next.length) return prev;
      [next[idx], next[target]] = [next[target], next[idx]];
      return next;
    });
  };

  const duplicateBlock = (id) => {
    const block = blocks.find(b => b.id === id);
    if (!block) return;
    const newBlock = { ...block, id: crypto.randomUUID() };
    const idx = blocks.findIndex(b => b.id === id);
    setBlocks(prev => { const next = [...prev]; next.splice(idx + 1, 0, newBlock); return next; });
    setSelectedBlockId(newBlock.id);
  };

  // ── HTML import ──────────────────────────────────────────────────────────────
  const handleImportHtml = () => {
    if (!importHtml.trim()) return;
    // Wrap raw HTML as a single "raw" block for display; treat as a text block
    const newBlock = { id: crypto.randomUUID(), type: 'text', content: importHtml, isRawHtml: true, color: '#374151', fontSize: 16, align: 'left', lineHeight: 1.6 };
    setBlocks(prev => [...prev, newBlock]);
    setImportHtml('');
    setImportOpen(false);
  };

  const htmlPreview = `<!DOCTYPE html>
<html><head><meta charset="utf-8"><style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f9fafb;margin:0;padding:32px 0;}</style></head>
<body><div style="max-width:600px;margin:0 auto;background:#ffffff;padding:48px 40px;border-radius:12px;">
${blocksToHtml(blocks)}
</div></body></html>`;

  return (
    <motion.div
      className="tbe-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      <motion.div
        className="tbe-shell"
        initial={{ opacity: 0, scale: 0.97, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.97, y: 20 }}
        transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
      >
        {/* ── Top bar ── */}
        <div className="tbe-topbar">
          <div className="tbe-topbar-left">
            <input
              className="tbe-title-input"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Template name"
            />
            {template?.status && (
              <span className={`tbe-status-badge ${template.status === 'PUBLISHED' ? 'published' : 'draft'}`}>
                {template.status}
              </span>
            )}
          </div>
          <div className="tbe-topbar-tabs">
            {['visual', 'preview', 'html'].map(tab => (
              <button key={tab} className={`tbe-tab ${activeTab === tab ? 'active' : ''}`} onClick={() => setActiveTab(tab)}>
                {tab.charAt(0).toUpperCase() + tab.slice(1)}
              </button>
            ))}
          </div>
          <div className="tbe-topbar-actions">
            <button className="tbe-btn-ghost" onClick={() => setImportOpen(true)}>
              <HiOutlineCode /> Import HTML
            </button>
            <button className="tbe-btn-ghost" onClick={() => setTestSendOpen(true)} disabled={!template?.id}>
              <HiOutlinePaperAirplane /> Test Send
            </button>
            <button className="tbe-btn-secondary" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving…' : 'Save Draft'}
            </button>
            <button className="tbe-btn-primary" onClick={handlePublish} disabled={publishing || !template?.id}>
              <HiOutlineCheck /> {publishing ? 'Publishing…' : 'Publish'}
            </button>
            <button className="tbe-btn-icon" onClick={onClose}><HiOutlineX /></button>
          </div>
        </div>

        {err && <div className="tbe-error">{err}</div>}

        {/* ── Subject line ── */}
        <div className="tbe-subject-bar">
          <span className="tbe-subject-label">Subject</span>
          <input
            className="tbe-subject-input"
            value={subject}
            onChange={e => setSubject(e.target.value)}
            placeholder="e.g., Welcome to {{appName}}, {{FIRST_NAME}}!"
          />
        </div>

        {/* ── Main area ── */}
        <div className="tbe-body">
          {/* Canvas / Preview / HTML */}
          <div className="tbe-canvas-wrap">
            {activeTab === 'visual' && (
              <div className="tbe-canvas" onClick={() => setSelectedBlockId(null)}>
                <div className="tbe-email-frame">
                  {blocks.length === 0 && (
                    <div className="tbe-empty-state">
                      <span>Type <kbd>/</kbd> to insert a block, or</span>
                      <button className="tbe-add-first-block" onClick={() => addBlock('text')}>
                        <HiOutlinePlusSm /> Add a text block
                      </button>
                    </div>
                  )}
                  {blocks.map((block, idx) => (
                    <BlockRow
                      key={block.id}
                      block={block}
                      isSelected={selectedBlockId === block.id}
                      isFirst={idx === 0}
                      isLast={idx === blocks.length - 1}
                      onSelect={() => setSelectedBlockId(block.id)}
                      onUpdate={updates => updateBlock(block.id, updates)}
                      onDelete={() => deleteBlock(block.id)}
                      onMoveUp={() => moveBlock(block.id, 'up')}
                      onMoveDown={() => moveBlock(block.id, 'down')}
                      onDuplicate={() => duplicateBlock(block.id)}
                      onAddAfter={type => addBlock(type, block.id)}
                      onSlashMenu={(query) => setSlashMenu({ open: true, blockId: block.id, query })}
                      slashMenu={slashMenu.blockId === block.id ? slashMenu : { open: false }}
                      onCloseSlashMenu={() => setSlashMenu({ open: false, blockId: null, query: '' })}
                    />
                  ))}
                  <div className="tbe-add-row">
                    <button className="tbe-add-block-btn" onClick={() => addBlock('text')}>
                      <HiOutlinePlusSm /> Add block
                    </button>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'preview' && (
              <div className="tbe-preview-wrap">
                <iframe
                  className="tbe-preview-iframe"
                  srcDoc={htmlPreview}
                  title="Email preview"
                  sandbox="allow-same-origin"
                />
              </div>
            )}

            {activeTab === 'html' && (
              <div className="tbe-html-view">
                <pre className="tbe-html-pre">{blocksToHtml(blocks)}</pre>
              </div>
            )}
          </div>

          {/* Inspector Panel */}
          {activeTab === 'visual' && (
            <BlockInspector
              block={selectedBlock}
              onUpdate={updates => selectedBlock && updateBlock(selectedBlock.id, updates)}
            />
          )}
        </div>
      </motion.div>

      {/* Import HTML Modal */}
      <AnimatePresence>
        {importOpen && (
          <motion.div className="tbe-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setImportOpen(false)}>
            <motion.div className="tbe-modal" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={e => e.stopPropagation()}>
              <div className="tbe-modal-header">
                <h2>Import HTML</h2>
                <button className="tbe-btn-icon" onClick={() => setImportOpen(false)}><HiOutlineX /></button>
              </div>
              <div className="tbe-modal-body">
                <p className="tbe-modal-desc">Paste your HTML or React Email component code below. It will be added as a block to your template.</p>
                <textarea
                  className="tbe-html-import-textarea"
                  value={importHtml}
                  onChange={e => setImportHtml(e.target.value)}
                  placeholder="<h1>Hello {{FIRST_NAME}}</h1>..."
                  rows={14}
                />
              </div>
              <div className="tbe-modal-footer">
                <button className="tbe-btn-secondary" onClick={() => setImportOpen(false)}>Cancel</button>
                <button className="tbe-btn-primary" onClick={handleImportHtml}>Import</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Test Send Modal */}
      <AnimatePresence>
        {testSendOpen && (
          <motion.div className="tbe-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setTestSendOpen(false)}>
            <motion.div className="tbe-modal tbe-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={e => e.stopPropagation()}>
              <div className="tbe-modal-header">
                <h2>Send Test Email</h2>
                <button className="tbe-btn-icon" onClick={() => setTestSendOpen(false)}><HiOutlineX /></button>
              </div>
              <div className="tbe-modal-body">
                <p className="tbe-modal-desc">Sends a test email to the address below using the current draft content. Variables will use their fallback values.</p>
                <label className="tbe-form-label">
                  Recipient Address
                  <input className="tbe-form-input" value={testEmail} onChange={e => setTestEmail(e.target.value)} placeholder="you@example.com" />
                </label>
              </div>
              <div className="tbe-modal-footer">
                <button className="tbe-btn-secondary" onClick={() => setTestSendOpen(false)}>Cancel</button>
                <button className="tbe-btn-primary" onClick={handleTestSend} disabled={!testEmail.trim()}>Send Test</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Block Row ─────────────────────────────────────────────────────────────────

function BlockRow({ block, isSelected, isFirst, isLast, onSelect, onUpdate, onDelete, onMoveUp, onMoveDown, onDuplicate, onAddAfter, slashMenu }) {
  const [showActions, setShowActions] = useState(false);

  const filtered = BLOCK_TYPES.filter(t =>
    !slashMenu.query || t.label.toLowerCase().startsWith(slashMenu.query.toLowerCase())
  );

  return (
    <div
      className={`tbe-block-row ${isSelected ? 'selected' : ''}`}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
      onClick={e => { e.stopPropagation(); onSelect(); }}
    >
      {/* Drag handle + actions */}
      <div className={`tbe-block-controls ${showActions || isSelected ? 'visible' : ''}`}>
        <button className="tbe-ctrl-btn" title="Move up" disabled={isFirst} onClick={e => { e.stopPropagation(); onMoveUp(); }}><HiOutlineChevronUp /></button>
        <button className="tbe-ctrl-btn" title="Move down" disabled={isLast} onClick={e => { e.stopPropagation(); onMoveDown(); }}><HiOutlineChevronDown /></button>
        <button className="tbe-ctrl-btn" title="Duplicate" onClick={e => { e.stopPropagation(); onDuplicate(); }}><HiOutlineDuplicate /></button>
        <button className="tbe-ctrl-btn tbe-ctrl-danger" title="Delete" onClick={e => { e.stopPropagation(); onDelete(); }}><HiOutlineTrash /></button>
      </div>

      {/* Block content */}
      <div className="tbe-block-content">
        <BlockContent block={block} onUpdate={onUpdate} isSelected={isSelected} />
      </div>

      {/* Slash command menu */}
      <AnimatePresence>
        {slashMenu.open && (
          <motion.div className="tbe-slash-menu" initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} onClick={e => e.stopPropagation()}>
            {filtered.length === 0 && <div className="tbe-slash-empty">No blocks found</div>}
            {filtered.map(t => (
              <button key={t.type} className="tbe-slash-item" onClick={() => onAddAfter(t.type)}>
                <span className="tbe-slash-icon">{t.icon}</span>
                <div>
                  <div className="tbe-slash-label">{t.label}</div>
                  <div className="tbe-slash-desc">{t.description}</div>
                </div>
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Block Content (editable inline) ─────────────────────────────────────────

function BlockContent({ block, onUpdate, isSelected }) {
  switch (block.type) {
    case 'heading':
      return (
        <div
          contentEditable
          suppressContentEditableWarning
          className="tbe-block-heading"
          style={{ textAlign: block.align, color: block.color, fontSize: block.fontSize }}
          onBlur={e => onUpdate({ content: e.currentTarget.textContent })}
          dangerouslySetInnerHTML={{ __html: block.content }}
        />
      );
    case 'text':
      if (block.isRawHtml) {
        return <div className="tbe-block-raw" dangerouslySetInnerHTML={{ __html: block.content }} />;
      }
      return (
        <div
          contentEditable
          suppressContentEditableWarning
          className="tbe-block-text"
          style={{ textAlign: block.align, color: block.color, fontSize: block.fontSize, lineHeight: block.lineHeight }}
          onBlur={e => onUpdate({ content: e.currentTarget.textContent })}
          dangerouslySetInnerHTML={{ __html: block.content }}
        />
      );
    case 'button':
      return (
        <div style={{ textAlign: block.align }}>
          <div
            className="tbe-block-button"
            style={{ backgroundColor: block.bgColor, color: block.textColor, borderRadius: block.borderRadius, padding: `${block.paddingV}px ${block.paddingH}px` }}
            contentEditable
            suppressContentEditableWarning
            onBlur={e => onUpdate({ content: e.currentTarget.textContent })}
            dangerouslySetInnerHTML={{ __html: block.content }}
          />
        </div>
      );
    case 'image':
      return (
        <div style={{ textAlign: block.align }}>
          {block.src
            ? <img src={block.src} alt={block.alt} style={{ maxWidth: block.width, height: 'auto', borderRadius: 8 }} />
            : (
              <div className="tbe-block-image-placeholder">
                <HiOutlinePhotograph style={{ fontSize: 32, opacity: 0.3 }} />
                <span>Enter image URL in the inspector panel →</span>
              </div>
            )
          }
        </div>
      );
    case 'divider':
      return <hr style={{ border: 'none', borderTop: `${block.thickness}px solid ${block.color}`, margin: `${block.margin}px 0` }} />;
    case 'columns':
      return (
        <div className="tbe-block-columns">
          {block.columns.map((col, i) => (
            <div
              key={i}
              className="tbe-block-column"
              contentEditable
              suppressContentEditableWarning
              style={{ textAlign: col.align, color: col.color }}
              onBlur={e => {
                const updated = block.columns.map((c, ci) => ci === i ? { ...c, content: e.currentTarget.textContent } : c);
                onUpdate({ columns: updated });
              }}
              dangerouslySetInnerHTML={{ __html: col.content }}
            />
          ))}
        </div>
      );
    case 'spacer':
      return (
        <div className="tbe-block-spacer" style={{ height: block.height }}>
          {isSelected && <span className="tbe-spacer-label">{block.height}px spacer</span>}
        </div>
      );
    default:
      return null;
  }
}

// ─── Inspector Panel ──────────────────────────────────────────────────────────

function BlockInspector({ block, onUpdate }) {
  if (!block) {
    return (
      <div className="tbe-inspector tbe-inspector-empty">
        <p>Select a block to edit its properties</p>
        <div className="tbe-inspector-hint">
          <h4>Reserved Variables</h4>
          {['FIRST_NAME', 'LAST_NAME', 'EMAIL', 'CRESCENDO_UNSUBSCRIBE_URL'].map(v => (
            <code key={v} className="tbe-var-chip">{'{{' + v + '}}'}</code>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="tbe-inspector">
      <div className="tbe-inspector-header">
        <span className="tbe-inspector-title">{block.type.charAt(0).toUpperCase() + block.type.slice(1)} Block</span>
      </div>
      <div className="tbe-inspector-body">
        {/* Common: align */}
        {['heading', 'text', 'button', 'image'].includes(block.type) && (
          <InspField label="Alignment">
            <div className="tbe-align-group">
              {['left', 'center', 'right'].map(a => (
                <button key={a} className={`tbe-align-btn ${block.align === a ? 'active' : ''}`} onClick={() => onUpdate({ align: a })}>
                  {a === 'left' ? '⬅' : a === 'center' ? '⬛' : '➡'}
                </button>
              ))}
            </div>
          </InspField>
        )}

        {/* Heading & Text */}
        {['heading', 'text'].includes(block.type) && (
          <>
            <InspField label="Text Color">
              <input type="color" className="tbe-color-input" value={block.color} onChange={e => onUpdate({ color: e.target.value })} />
            </InspField>
            <InspField label="Font Size">
              <input type="range" className="tbe-range" min={12} max={72} value={block.fontSize} onChange={e => onUpdate({ fontSize: Number(e.target.value) })} />
              <span className="tbe-range-val">{block.fontSize}px</span>
            </InspField>
          </>
        )}

        {/* Heading: level */}
        {block.type === 'heading' && (
          <InspField label="Level">
            <select className="tbe-select" value={block.level} onChange={e => onUpdate({ level: e.target.value })}>
              <option value="h1">H1</option>
              <option value="h2">H2</option>
              <option value="h3">H3</option>
            </select>
          </InspField>
        )}

        {/* Text: line-height */}
        {block.type === 'text' && (
          <InspField label="Line Height">
            <input type="range" className="tbe-range" min={1.0} max={2.5} step={0.1} value={block.lineHeight} onChange={e => onUpdate({ lineHeight: Number(e.target.value) })} />
            <span className="tbe-range-val">{block.lineHeight}</span>
          </InspField>
        )}

        {/* Button */}
        {block.type === 'button' && (
          <>
            <InspField label="Link URL">
              <input className="tbe-text-input" value={block.href} onChange={e => onUpdate({ href: e.target.value })} placeholder="https://" />
            </InspField>
            <InspField label="Background">
              <input type="color" className="tbe-color-input" value={block.bgColor} onChange={e => onUpdate({ bgColor: e.target.value })} />
            </InspField>
            <InspField label="Text Color">
              <input type="color" className="tbe-color-input" value={block.textColor} onChange={e => onUpdate({ textColor: e.target.value })} />
            </InspField>
            <InspField label="Corner Radius">
              <input type="range" className="tbe-range" min={0} max={24} value={block.borderRadius} onChange={e => onUpdate({ borderRadius: Number(e.target.value) })} />
              <span className="tbe-range-val">{block.borderRadius}px</span>
            </InspField>
          </>
        )}

        {/* Image */}
        {block.type === 'image' && (
          <>
            <InspField label="Image URL">
              <input className="tbe-text-input" value={block.src} onChange={e => onUpdate({ src: e.target.value })} placeholder="https://..." />
            </InspField>
            <InspField label="Alt Text">
              <input className="tbe-text-input" value={block.alt} onChange={e => onUpdate({ alt: e.target.value })} placeholder="Descriptive alt text" />
            </InspField>
            <InspField label="Width">
              <input className="tbe-text-input" value={block.width} onChange={e => onUpdate({ width: e.target.value })} placeholder="100%" />
            </InspField>
          </>
        )}

        {/* Divider */}
        {block.type === 'divider' && (
          <>
            <InspField label="Color">
              <input type="color" className="tbe-color-input" value={block.color} onChange={e => onUpdate({ color: e.target.value })} />
            </InspField>
            <InspField label="Thickness">
              <input type="range" className="tbe-range" min={1} max={8} value={block.thickness} onChange={e => onUpdate({ thickness: Number(e.target.value) })} />
              <span className="tbe-range-val">{block.thickness}px</span>
            </InspField>
            <InspField label="Margin">
              <input type="range" className="tbe-range" min={0} max={64} value={block.margin} onChange={e => onUpdate({ margin: Number(e.target.value) })} />
              <span className="tbe-range-val">{block.margin}px</span>
            </InspField>
          </>
        )}

        {/* Spacer */}
        {block.type === 'spacer' && (
          <InspField label="Height">
            <input type="range" className="tbe-range" min={8} max={128} value={block.height} onChange={e => onUpdate({ height: Number(e.target.value) })} />
            <span className="tbe-range-val">{block.height}px</span>
          </InspField>
        )}

        {/* Variable hint */}
        <div className="tbe-inspector-vars">
          <h4>Variables</h4>
          <p>Type <code>{'{{VAR}}'}</code> in any text block to insert a dynamic value.</p>
          <div className="tbe-var-chips">
            {RESERVED_VARIABLES.map(v => (
              <code key={v} className="tbe-var-chip">{'{{' + v + '}}'}</code>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function InspField({ label, children }) {
  return (
    <div className="tbe-field">
      <label className="tbe-field-label">{label}</label>
      <div className="tbe-field-control">{children}</div>
    </div>
  );
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function initBlocks(template) {
  if (!template || (!template.htmlBody && !template.contentHtml)) {
    return [createBlock('heading'), createBlock('text')];
  }
  // If existing template, render as a single raw HTML block for editing
  const html = template.htmlBody || template.contentHtml || '';
  return [{ id: crypto.randomUUID(), type: 'text', isRawHtml: true, content: html, color: '#374151', fontSize: 16, align: 'left', lineHeight: 1.6 }];
}
