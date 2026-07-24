/**
 * TemplateBlockEditor.jsx
 *
 * Crescendo Email Template Editor — Resend-inspired
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlineX, HiOutlineCheck, HiOutlinePaperAirplane, HiOutlineCode,
  HiOutlinePhotograph, HiOutlinePlusSm, HiOutlineTrash,
  HiOutlineChevronDown, HiOutlineChevronUp, HiOutlineDuplicate,
  HiOutlinePencil, HiOutlineDocumentText, HiOutlineViewGrid,
  HiOutlineVariable, HiOutlineSparkles, HiOutlineClipboard,
  HiOutlineColorSwatch, HiOutlineAdjustments, HiOutlineTemplate,
  HiOutlineSwitchHorizontal, HiOutlineRefresh, HiOutlineLink,
  HiOutlineUpload, HiOutlineEye, HiOutlineReply, HiArrowLeft,
  HiArrowRight, HiOutlineExternalLink,
} from 'react-icons/hi';
import { MdFormatBold, MdFormatItalic, MdFormatUnderlined, MdLink, MdLinkOff } from 'react-icons/md';
import { templatesApi } from '../../api/emailServiceApi';
import './TemplateBlockEditor.css';


// ─── Constants ────────────────────────────────────────────────────────────────

const GOOGLE_FONTS = [
  { label: 'System Sans-Serif', value: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif' },
  { label: 'Inter', value: '"Inter", sans-serif' },
  { label: 'Roboto', value: '"Roboto", sans-serif' },
  { label: 'Open Sans', value: '"Open Sans", sans-serif' },
  { label: 'Lato', value: '"Lato", sans-serif' },
  { label: 'Poppins', value: '"Poppins", sans-serif' },
  { label: 'Nunito', value: '"Nunito", sans-serif' },
  { label: 'Montserrat', value: '"Montserrat", sans-serif' },
  { label: 'Source Sans 3', value: '"Source Sans 3", sans-serif' },
  { label: 'Georgia (Serif)', value: 'Georgia, serif' },
  { label: 'Playfair Display', value: '"Playfair Display", serif' },
  { label: 'Merriweather', value: '"Merriweather", serif' },
];

const FONT_WEIGHT_OPTIONS = [
  { label: 'Light (300)', value: '300' },
  { label: 'Normal (400)', value: '400' },
  { label: 'Medium (500)', value: '500' },
  { label: 'Semibold (600)', value: '600' },
  { label: 'Bold (700)', value: '700' },
];

const THEME_PRESETS = {
  minimal: {
    textFont: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    titleFont: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    textSize: 15,
    textWeight: '400',
    lineHeight: 1.6,
    letterSpacing: 0,
    textColor: '#374151',
    titleColor: '#111827',
    linkColor: '#6366f1',
    headingSize: 28,
    headingWeight: '700',
  },
  modern: {
    textFont: '"Inter", sans-serif',
    titleFont: '"Inter", sans-serif',
    textSize: 15,
    textWeight: '400',
    lineHeight: 1.65,
    letterSpacing: 0,
    textColor: '#374151',
    titleColor: '#0f172a',
    linkColor: '#3b82f6',
    headingSize: 30,
    headingWeight: '700',
  },
  elegant: {
    textFont: 'Georgia, serif',
    titleFont: '"Playfair Display", serif',
    textSize: 16,
    textWeight: '400',
    lineHeight: 1.75,
    letterSpacing: 0.01,
    textColor: '#1e293b',
    titleColor: '#0f172a',
    linkColor: '#7c3aed',
    headingSize: 32,
    headingWeight: '700',
  },
};

const DEFAULT_GLOBAL_CSS = `/* Custom CSS for your email */

/* Mobile Responsive Styles */
@media (max-width: 600px) {
  .email-body {
    width: 100% !important;
    padding: 16px !important;
  }
  .email-columns {
    display: block !important;
    width: 100% !important;
  }
  .email-column {
    width: 100% !important;
    display: block !important;
    padding: 8px 0 !important;
  }
  img {
    width: 100% !important;
    height: auto !important;
  }
  h1, h2, h3 {
    font-size: 22px !important;
  }
}

/* Dark Mode Support */
@media (prefers-color-scheme: dark) {
  /* Uncomment to enable dark mode overrides */
  /* body { background-color: #1a1a1a !important; } */
}`;

const BLOCK_TYPES = [
  { type: 'heading',     label: 'Heading',         icon: 'H',  description: 'Large section title' },
  { type: 'text',        label: 'Paragraph',       icon: '¶',  description: 'Body text paragraph' },
  { type: 'button',      label: 'Button',          icon: '▶',  description: 'Call to action button' },
  { type: 'image',       label: 'Image',           icon: '🖼', description: 'Image banner or logo' },
  { type: 'media',       label: 'Media / File',    icon: '📎', description: 'Attach a file or media link' },
  { type: 'divider',     label: 'Divider',         icon: '—',  description: 'Horizontal separator' },
  { type: 'columns',     label: '2 Columns',       icon: '⊞',  description: 'Two-column layout' },
  { type: '3columns',    label: '3 Columns',       icon: '☰',  description: 'Three-column layout' },
  { type: 'section',     label: 'Section',         icon: '▢',  description: 'Highlighted section container' },
  { type: 'spacer',      label: 'Spacer',          icon: '↕',  description: 'Vertical spacing' },
  { type: 'unsubscribe', label: 'Unsubscribe',     icon: '⚓',  description: 'Footer unsubscribe link' },
];

const RESERVED_VARIABLES = [
  { key: 'FIRST_NAME', desc: "Recipient's first name" },
  { key: 'LAST_NAME', desc: "Recipient's last name" },
  { key: 'EMAIL', desc: "Recipient's email address" },
  { key: 'COMPANY_NAME', desc: 'Your company name' },
  { key: 'CRESCENDO_UNSUBSCRIBE_URL', desc: 'Unsubscribe link URL' },
];

// ─── Default Block Creator ────────────────────────────────────────────────────

function createBlock(type) {
  const id = crypto.randomUUID();
  const base = { id, type };
  switch (type) {
    case 'heading':    return { ...base, content: 'Your Heading', level: 'h1', align: 'left', color: '#111827', fontSize: 28 };
    case 'text':       return { ...base, content: 'Your paragraph text goes here. Use {{FIRST_NAME}} for dynamic content.', align: 'left', color: '#374151', fontSize: 15, lineHeight: 1.55 };
    case 'button':     return { ...base, content: 'Click Here', href: 'https://', align: 'center', bgColor: '#0f172a', textColor: '#ffffff', borderRadius: 8, paddingV: 12, paddingH: 24 };
    case 'image':      return { ...base, src: '', alt: '', align: 'center', width: '100%' };
    case 'media':      return { ...base, src: '', filename: 'file.pdf', align: 'center' };
    case 'divider':    return { ...base, color: '#e2e8f0', thickness: 1, margin: 24 };
    case 'columns':    return { ...base, columns: [{ content: 'Column 1 content', align: 'left', color: '#374151' }, { content: 'Column 2 content', align: 'left', color: '#374151' }] };
    case '3columns':   return { ...base, columns: [{ content: 'Col 1', align: 'left', color: '#374151' }, { content: 'Col 2', align: 'left', color: '#374151' }, { content: 'Col 3', align: 'left', color: '#374151' }] };
    case 'section':    return { ...base, content: 'Section Container Content', bgColor: '#f8fafc', borderColor: '#e2e8f0', padding: 20, borderRadius: 8 };
    case 'spacer':     return { ...base, height: 24 };
    case 'unsubscribe':return { ...base, text: 'Unsubscribe from our emails', align: 'center', color: '#94a3b8', fontSize: 12 };
    default:           return base;
  }
}

// ─── Blocks → HTML ────────────────────────────────────────────────────────────

// Indent helper for proper email HTML formatting
function indent(str, spaces) {
  const pad = ' '.repeat(spaces);
  return str.split('\n').map(line => pad + line).join('\n');
}

function blockToHtmlStr(block, globalTheme) {
  switch (block.type) {
    case 'heading':
      return [
        `<${block.level}`,
        `  style="`,
        `    text-align:${block.align};`,
        `    color:${block.color};`,
        `    font-size:${block.fontSize}px;`,
        `    font-weight:700;`,
        `    font-family:${globalTheme.titleFont || globalTheme.textFont || 'sans-serif'};`,
        `    margin:0 0 16px 0;`,
        `  "`,
        `>`,
        `  ${block.content}`,
        `</${block.level}>`,
      ].join('\n');
    case 'text':
      return [
        `<p`,
        `  style="`,
        `    text-align:${block.align};`,
        `    color:${block.color};`,
        `    font-size:${block.fontSize}px;`,
        `    line-height:${block.lineHeight || 1.6};`,
        `    margin:0 0 16px 0;`,
        `  "`,
        `>`,
        `  ${block.content}`,
        `</p>`,
      ].join('\n');
    case 'button':
      return [
        `<table`,
        `  align="${block.align}"`,
        `  border="0"`,
        `  cellpadding="0"`,
        `  cellspacing="0"`,
        `  role="presentation"`,
        `  style="margin:16px 0;"`,
        `>`,
        `  <tbody>`,
        `    <tr>`,
        `      <td>`,
        `        <a`,
        `          href="${block.href}"`,
        `          style="`,
        `            display:inline-block;`,
        `            background-color:${block.bgColor};`,
        `            color:${block.textColor};`,
        `            text-decoration:none;`,
        `            padding:${block.paddingV}px ${block.paddingH}px;`,
        `            border-radius:${block.borderRadius}px;`,
        `            font-weight:600;`,
        `            font-size:14px;`,
        `          "`,
        `        >${block.content}</a>`,
        `      </td>`,
        `    </tr>`,
        `  </tbody>`,
        `</table>`,
      ].join('\n');
    case 'image':
      return [
        `<table`,
        `  align="${block.align}"`,
        `  border="0"`,
        `  cellpadding="0"`,
        `  cellspacing="0"`,
        `  role="presentation"`,
        `  style="margin:16px 0;"`,
        `>`,
        `  <tbody>`,
        `    <tr>`,
        `      <td>`,
        `        <img`,
        `          src="${block.src}"`,
        `          alt="${block.alt}"`,
        `          style="max-width:${block.width};height:auto;border-radius:8px;display:block;"`,
        `        />`,
        `      </td>`,
        `    </tr>`,
        `  </tbody>`,
        `</table>`,
      ].join('\n');
    case 'media':
      return [
        `<div style="text-align:${block.align};margin:16px 0;padding:12px;border:1px dashed #cbd5e1;border-radius:6px;">`,
        `  <a href="${block.src}" style="color:${globalTheme.linkColor};">${block.filename}</a>`,
        `</div>`
      ].join('\n');
    case 'divider':
      return `<hr style="border:none;border-top:${block.thickness}px solid ${block.color};margin:${block.margin}px 0;" />`;
    case 'columns':
    case '3columns': {
      const colWidth = block.type === '3columns' ? '33%' : `${Math.floor(100/block.columns.length)}%`;
      const tds = block.columns.map(col => [
        `        <td`,
        `          width="${colWidth}"`,
        `          style="`,
        `            vertical-align:top;`,
        `            padding:0 8px;`,
        `            text-align:${col.align};`,
        `            color:${col.color};`,
        `          "`,
        `        >${col.content}</td>`,
      ].join('\n')).join('\n');
      return [
        `<table`,
        `  align="left"`,
        `  width="100%"`,
        `  border="0"`,
        `  cellpadding="0"`,
        `  cellspacing="0"`,
        `  role="presentation"`,
        `  style="border-collapse:collapse;margin:16px 0;"`,
        `>`,
        `  <tbody>`,
        `    <tr style="width:100%">`,
        tds,
        `    </tr>`,
        `  </tbody>`,
        `</table>`,
      ].join('\n');
    }
    case 'section':
      return [
        `<div`,
        `  style="`,
        `    background-color:${block.bgColor};`,
        `    border:1px solid ${block.borderColor};`,
        `    padding:${block.padding}px;`,
        `    border-radius:${block.borderRadius}px;`,
        `    margin:16px 0;`,
        `  "`,
        `>`,
        `  ${block.content}`,
        `</div>`,
      ].join('\n');
    case 'spacer':
      return `<div style="height:${block.height}px;" aria-hidden="true"></div>`;
    case 'unsubscribe':
      return [
        `<p`,
        `  style="`,
        `    text-align:${block.align};`,
        `    font-size:${block.fontSize}px;`,
        `    color:${block.color};`,
        `    margin:24px 0 8px;`,
        `  "`,
        `>`,
        `  <a`,
        `    href="{{CRESCENDO_UNSUBSCRIBE_URL}}"`,
        `    style="color:${block.color};text-decoration:underline;"`,
        `  >${block.text}</a>`,
        `</p>`,
      ].join('\n');
    default:
      return '';
  }
}

function blocksToHtml(blocks, pageStyle, globalTheme, globalCss) {
  const fontName = (() => {
    if (globalTheme.textFont.includes('Inter')) return 'Inter';
    if (globalTheme.textFont.includes('Roboto')) return 'Roboto';
    if (globalTheme.textFont.includes('Poppins')) return 'Poppins';
    if (globalTheme.textFont.includes('Open Sans')) return 'Open+Sans';
    if (globalTheme.textFont.includes('Lato')) return 'Lato';
    if (globalTheme.textFont.includes('Montserrat')) return 'Montserrat';
    if (globalTheme.textFont.includes('Nunito')) return 'Nunito';
    if (globalTheme.textFont.includes('Playfair')) return 'Playfair+Display';
    if (globalTheme.textFont.includes('Merriweather')) return 'Merriweather';
    return null;
  })();
  const fontImport = fontName
    ? `@import url('https://fonts.googleapis.com/css2?family=${fontName}:wght@300;400;500;600;700&display=swap');`
    : '';

  const bodyContent = blocks
    .map(b => indent(blockToHtmlStr(b, globalTheme), 8))
    .join('\n\n');

  const pt = pageStyle.paddingTop ?? 32;
  const pr = pageStyle.paddingRight ?? 32;
  const pb = pageStyle.paddingBottom ?? 32;
  const pl = pageStyle.paddingLeft ?? 32;

  return `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Email</title>
    <style>
      ${fontImport}
      body {
        margin: 0;
        padding: 0;
        background-color: ${pageStyle.background || '#f4f4f5'};
        font-family: ${globalTheme.textFont || 'sans-serif'};
        -webkit-font-smoothing: antialiased;
      }
      a { color: ${globalTheme.linkColor || '#374151'}; }
      img { border: 0; display: block; max-width: 100%; }
      ${globalCss || ''}
    </style>
  </head>
  <body style="background-color:${pageStyle.background || '#f4f4f5'};padding:32px 0;">
    <table
      align="center"
      width="100%"
      border="0"
      cellpadding="0"
      cellspacing="0"
      role="presentation"
      style="max-width:${pageStyle.bodyWidth || 600}px"
    >
      <tbody>
        <tr style="width:100%">
          <td
            style="
              background-color:${pageStyle.bodyBackground || '#ffffff'};
              padding-top:${pt}px;
              padding-right:${pr}px;
              padding-bottom:${pb}px;
              padding-left:${pl}px;
              border-radius:${pageStyle.borderRadius || 0}px;
              ${pageStyle.borderWidth > 0 ? `border:${pageStyle.borderWidth}px ${pageStyle.borderStyle || 'solid'} ${pageStyle.borderColor || '#e2e8f0'};` : ''}
              font-family:${globalTheme.textFont || 'sans-serif'};
              color:${globalTheme.textColor || '#374151'};
              font-size:${globalTheme.textSize || 15}px;
              line-height:${globalTheme.lineHeight || 1.6};
            "
          >
${bodyContent}
          </td>
        </tr>
      </tbody>
    </table>
  </body>
</html>`;
}

// ─── Main Editor Component ─────────────────────────────────────────────────────

export default function TemplateBlockEditor({ template, onClose, onSaved }) {
  const [name, setName]               = useState(template?.name || 'Untitled Template');
  const [subject, setSubject]         = useState(template?.subject || '');
  const [fromAddress, setFromAddress] = useState('Crescendo <hello@crescendo.run>');
  const [replyTo, setReplyTo]         = useState('');
  const [previewText, setPreviewText] = useState('');
  const [editorMode, setEditorMode]   = useState('visual'); // 'visual' | 'code'

  const [blocks, setBlocks]           = useState(() => initBlocks(template));
  const [history, setHistory]         = useState([]);
  const [future, setFuture]           = useState([]);
  const [selectedBlockId, setSelectedBlockId] = useState(null);
  const [activeDockTool, setActiveDockTool]   = useState(null);
  const [activeInspectorTab, setActiveInspectorTab] = useState('style');
  const [imageUploadOpen, setImageUploadOpen] = useState(false);
  const [imageUploadBlock, setImageUploadBlock] = useState(null);

  // Page & Body style
  const [pageStyle, setPageStyle] = useState({
    background: '#f4f4f5',
    bodyBackground: '#ffffff',
    bodyWidth: 600,
    bodyAlign: 'center',
    paddingTop: 32, paddingRight: 32, paddingBottom: 32, paddingLeft: 32,
    paddingLinked: true,
    bodyMarginTop: 0, bodyMarginBottom: 0,
    borderRadius: 16,
    borderWidth: 0,
    borderColor: '#e2e8f0',
    borderStyle: 'solid',
  });

  // Global Theme
  const [globalTheme, setGlobalTheme] = useState({
    textFont: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    titleFont: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    textSize: 15,
    textWeight: '400',
    lineHeight: 1.6,
    letterSpacing: 0,
    textColor: '#374151',
    titleColor: '#111827',
    linkColor: '#6366f1',
    headingSize: 28,
    headingWeight: '700',
  });

  const [globalCss, setGlobalCss] = useState(DEFAULT_GLOBAL_CSS);
  const [rawHtmlCode, setRawHtmlCode] = useState(() => blocksToHtml([], pageStyle, globalTheme, globalCss));

  const [testSendOpen, setTestSendOpen] = useState(false);
  const [testEmail, setTestEmail]     = useState('');
  const [saving, setSaving]           = useState(false);
  const [publishing, setPublishing]   = useState(false);
  const [err, setErr]                 = useState('');
  const [copiedCode, setCopiedCode]   = useState(false);

  const selectedBlock = blocks.find(b => b.id === selectedBlockId) || null;

  // Sync rawHtmlCode when blocks or style change in visual mode
  useEffect(() => {
    if (editorMode === 'visual') {
      setRawHtmlCode(blocksToHtml(blocks, pageStyle, globalTheme, globalCss));
    }
  }, [blocks, pageStyle, globalTheme, globalCss, editorMode]);

  // Helper to update a single padding side or all
  const updatePadding = useCallback((side, val) => {
    if (pageStyle.paddingLinked) {
      setPageStyle(ps => ({ ...ps, paddingTop: val, paddingRight: val, paddingBottom: val, paddingLeft: val }));
    } else {
      setPageStyle(ps => ({ ...ps, [side]: val }));
    }
  }, [pageStyle.paddingLinked]);

  // ── Saves ────────────────────────────────────────────────────────────────────
  const handleSave = async () => {
    if (!name.trim()) { setErr('Template name is required'); return; }
    setSaving(true); setErr('');
    try {
      const html = editorMode === 'code' ? rawHtmlCode : blocksToHtml(blocks, pageStyle, globalTheme, globalCss);
      const payload = { name: name.trim(), subject: subject.trim(), htmlBody: html };
      const saved = !template?.id ? await templatesApi.create(payload) : await templatesApi.update(template.id, payload);
      onSaved(saved);
    } catch (e) {
      setErr(e.response?.data?.message || 'Failed to save template');
    } finally { setSaving(false); }
  };

  const handlePublish = async () => {
    if (!template?.id) { setErr('Save the template first before publishing.'); return; }
    setPublishing(true); setErr('');
    try {
      const saved = await templatesApi.publish(template.id);
      onSaved(saved);
    } catch (e) {
      setErr(e.response?.data?.message || 'Failed to publish.');
    } finally { setPublishing(false); }
  };

  const handleTestSend = async () => {
    if (!template?.id || !testEmail.trim()) return;
    try {
      await templatesApi.testSend(template.id, { toAddress: testEmail.trim(), variables: {} });
      setTestSendOpen(false);
    } catch (e) {
      setErr(e.response?.data?.message || 'Test send failed');
    }
  };

  // ── Block ops ────────────────────────────────────────────────────────────────
  const pushHistory = useCallback((oldBlocks) => {
    setHistory(h => [...h.slice(-30), oldBlocks]);
    setFuture([]);
  }, []);

  const undo = useCallback(() => {
    if (history.length === 0) return;
    const prev = history[history.length - 1];
    setFuture(f => [blocks, ...f]);
    setBlocks(prev);
    setHistory(h => h.slice(0, -1));
  }, [history, blocks]);

  const redo = useCallback(() => {
    if (future.length === 0) return;
    const next = future[0];
    setHistory(h => [...h, blocks]);
    setBlocks(next);
    setFuture(f => f.slice(1));
  }, [future, blocks]);

  // Keyboard shortcuts
  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && !e.shiftKey && e.key === 'z') { e.preventDefault(); undo(); }
      if ((e.ctrlKey || e.metaKey) && (e.shiftKey && e.key === 'z' || e.key === 'y')) { e.preventDefault(); redo(); }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [undo, redo]);

  const addBlock = (type, afterId = null) => {
    const nb = createBlock(type);
    setBlocks(prev => {
      pushHistory(prev);
      if (!afterId) return [...prev, nb];
      const idx = prev.findIndex(b => b.id === afterId);
      const next = [...prev];
      next.splice(idx + 1, 0, nb);
      return next;
    });
    setSelectedBlockId(nb.id);
    setActiveDockTool(null);
  };

  const updateBlock = (id, updates) => setBlocks(prev => prev.map(b => b.id === id ? { ...b, ...updates } : b));
  const deleteBlock = (id) => {
    setBlocks(prev => { pushHistory(prev); return prev.filter(b => b.id !== id); });
    if (selectedBlockId === id) setSelectedBlockId(null);
  };

  const moveBlock = (id, dir) => {
    setBlocks(prev => {
      const idx = prev.findIndex(b => b.id === id);
      const next = [...prev];
      const target = dir === 'up' ? idx - 1 : idx + 1;
      if (target < 0 || target >= next.length) return prev;
      pushHistory(prev);
      [next[idx], next[target]] = [next[target], next[idx]];
      return next;
    });
  };

  const duplicateBlock = (id) => {
    const block = blocks.find(b => b.id === id);
    if (!block) return;
    const nb = { ...block, id: crypto.randomUUID() };
    const idx = blocks.findIndex(b => b.id === id);
    setBlocks(prev => { pushHistory(prev); const next = [...prev]; next.splice(idx + 1, 0, nb); return next; });
    setSelectedBlockId(nb.id);
  };

  const handleFormatHtml = () => {
    // Idempotent formatter: always re-format from current value without accumulating newlines
    const raw = rawHtmlCode
      // normalize all whitespace sequences to single space
      .replace(/\s+/g, ' ')
      // add newline after every closing > before opening <
      .replace(/> </g, '>\n<')
      .trim();

    // Indent based on tag depth
    let depth = 0;
    const lines = raw.split('\n').map(line => {
      const trimmed = line.trim();
      if (!trimmed) return '';
      const isClosing = /^<\//.test(trimmed);
      const isSelfClosing = /\/>$/.test(trimmed) || /^<(br|hr|img|input|meta|link)[ >]/i.test(trimmed);
      const isOpening = /^<[^/!]/.test(trimmed) && !isSelfClosing;
      if (isClosing && depth > 0) depth--;
      const indented = '  '.repeat(depth) + trimmed;
      if (isOpening && !isSelfClosing) depth++;
      return indented;
    }).filter(l => l !== '');

    setRawHtmlCode(lines.join('\n'));
  };

  const handleCopyCode = () => {
    navigator.clipboard.writeText(rawHtmlCode);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const currentPreviewHtml = editorMode === 'code' ? rawHtmlCode : blocksToHtml(blocks, pageStyle, globalTheme, globalCss);

  const applyThemePreset = (presetKey) => {
    setGlobalTheme(prev => ({ ...prev, ...THEME_PRESETS[presetKey] }));
  };

  const openImageUpload = (blockId) => {
    setImageUploadBlock(blockId);
    setImageUploadOpen(true);
  };

  return (
    <motion.div className="tbe-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
      <motion.div className="tbe-shell" initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.25 }}>

        {/* ── Top Bar ── */}
        <div className="tbe-topbar">
          <div className="tbe-topbar-left">
            <img src="/logo-white.svg" alt="Crescendo" className="tbe-brand-logo" />
            <span className="tbe-breadcrumb-sep">/</span>
            <span className="tbe-breadcrumb-section">Templates</span>
            <span className="tbe-breadcrumb-sep">/</span>
            <input className="tbe-title-input" value={name} onChange={e => setName(e.target.value)} placeholder="Template name" />
            {template?.status && (
              <span className={`tbe-status-badge ${template.status === 'PUBLISHED' ? 'published' : 'draft'}`}>
                {template.status}
              </span>
            )}
          </div>
        <div className="tbe-topbar-actions">
            <button className="tbe-btn-ghost" title="Undo (Ctrl+Z)" onClick={undo} disabled={history.length === 0}>
              <HiArrowLeft />
            </button>
            <button className="tbe-btn-ghost" title="Redo (Ctrl+Y)" onClick={redo} disabled={future.length === 0}>
              <HiArrowRight />
            </button>
            <div className="tbe-topbar-divider" />
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

        {/* ── Main Editor Body ── */}
        <div className="tbe-body">

          {/* ── Far-Left Mode Dock ── */}
          <div className="tbe-mode-dock">
            <button
              className={`tbe-dock-btn ${editorMode === 'visual' ? 'active' : ''}`}
              onClick={() => { setEditorMode('visual'); setSelectedBlockId(null); }}
              title="Visual Editor"
            >
              <HiOutlinePencil />
            </button>
            <button
              className={`tbe-dock-btn ${editorMode === 'code' ? 'active' : ''}`}
              onClick={() => setEditorMode('code')}
              title="Split HTML Code Editor"
            >
              <HiOutlineCode />
            </button>

            {editorMode === 'visual' && (
              <>
                <div className="tbe-dock-divider" />
                <button
                  className={`tbe-dock-btn ${activeDockTool === 'blocks' ? 'active' : ''}`}
                  onClick={() => setActiveDockTool(activeDockTool === 'blocks' ? null : 'blocks')}
                  title="Add Blocks"
                >
                  <HiOutlineDocumentText />
                </button>
                <button
                  className={`tbe-dock-btn ${activeDockTool === 'layouts' ? 'active' : ''}`}
                  onClick={() => setActiveDockTool(activeDockTool === 'layouts' ? null : 'layouts')}
                  title="Add Layouts"
                >
                  <HiOutlineViewGrid />
                </button>
                <button
                  className={`tbe-dock-btn ${activeDockTool === 'vars' ? 'active' : ''}`}
                  onClick={() => setActiveDockTool(activeDockTool === 'vars' ? null : 'vars')}
                  title="Insert Variables"
                >
                  <HiOutlineVariable />
                </button>
              </>
            )}
          </div>

          {/* ── Dock Tool Popover ── */}
          <AnimatePresence>
            {editorMode === 'visual' && activeDockTool && (
              <motion.div className="tbe-dock-popover" initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -10 }}>
                <div className="tbe-popover-header">
                  <span>{activeDockTool === 'blocks' ? 'Add Blocks' : activeDockTool === 'layouts' ? 'Layout Components' : 'Variables'}</span>
                  <button className="tbe-btn-icon" onClick={() => setActiveDockTool(null)}><HiOutlineX /></button>
                </div>
                <div className="tbe-popover-body">
                  {activeDockTool === 'blocks' && (
                    <div className="tbe-popover-grid">
                      {BLOCK_TYPES.filter(b => ['heading', 'text', 'button', 'image', 'media', 'divider', 'spacer', 'unsubscribe'].includes(b.type)).map(b => (
                        <button key={b.type} className="tbe-popover-item" onClick={() => addBlock(b.type)}>
                          <span className="tbe-popover-icon">{b.icon}</span>
                          <div>
                            <div className="tbe-popover-label">{b.label}</div>
                            <div className="tbe-popover-desc">{b.description}</div>
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                  {activeDockTool === 'layouts' && (
                    <div className="tbe-popover-grid">
                      {BLOCK_TYPES.filter(b => ['columns', '3columns', 'section'].includes(b.type)).map(b => (
                        <button key={b.type} className="tbe-popover-item" onClick={() => addBlock(b.type)}>
                          <span className="tbe-popover-icon">{b.icon}</span>
                          <div>
                            <div className="tbe-popover-label">{b.label}</div>
                            <div className="tbe-popover-desc">{b.description}</div>
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                  {activeDockTool === 'vars' && (
                    <div className="tbe-vars-list">
                      <p className="tbe-vars-hint">Click a variable to copy. Use <code>{'{{VAR}}'}</code> inside text blocks.</p>
                      {RESERVED_VARIABLES.map(v => (
                        <div key={v.key} className="tbe-var-row" onClick={() => navigator.clipboard.writeText(`{{${v.key}}}`)}>
                          <div>
                            <code className="tbe-var-code">{'{{' + v.key + '}}'}</code>
                            <div className="tbe-var-desc">{v.desc}</div>
                          </div>
                          <span className="tbe-var-copy">Copy</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* ── Central View ── */}
          {editorMode === 'visual' ? (
            <div className="tbe-canvas-wrap" onClick={() => setSelectedBlockId(null)}>
              <div className="tbe-canvas">
                <div
                  className="tbe-email-frame"
                  style={{
                    width: pageStyle.bodyWidth,
                    backgroundColor: pageStyle.bodyBackground,
                    padding: `${pageStyle.paddingTop}px ${pageStyle.paddingRight}px ${pageStyle.paddingBottom}px ${pageStyle.paddingLeft}px`,
                    borderRadius: pageStyle.borderRadius,
                    border: pageStyle.borderWidth > 0 ? `${pageStyle.borderWidth}px ${pageStyle.borderStyle} ${pageStyle.borderColor}` : 'none',
                  }}
                >
                  {/* Canvas Email Header - Each field on its own row */}
                  <div className="tbe-canvas-header" onClick={e => e.stopPropagation()}>
                    <div className="tbe-header-row">
                      <span className="tbe-header-label">FROM</span>
                      <input className="tbe-header-input" value={fromAddress} onChange={e => setFromAddress(e.target.value)} placeholder="Crescendo <hello@crescendo.run>" />
                    </div>
                    <div className="tbe-header-row">
                      <span className="tbe-header-label">REPLY-TO</span>
                      <input className="tbe-header-input" value={replyTo} onChange={e => setReplyTo(e.target.value)} placeholder="reply@acme.com" />
                    </div>
                    <div className="tbe-header-row">
                      <span className="tbe-header-label">SUBJECT</span>
                      <input className="tbe-header-input tbe-subject-field" value={subject} onChange={e => setSubject(e.target.value)} placeholder="e.g. Welcome {{FIRST_NAME}}!" />
                    </div>
                    <div className="tbe-header-row">
                      <span className="tbe-header-label">PREVIEW</span>
                      <input className="tbe-header-input tbe-preview-field" value={previewText} onChange={e => setPreviewText(e.target.value)} placeholder="Preview text shown in inbox..." />
                    </div>
                  </div>

                  {/* Blocks */}
                  {blocks.length === 0 && (
                    <div className="tbe-empty-state">
                      <HiOutlineTemplate size={32} />
                      <p>Your email canvas is empty</p>
                      <button className="tbe-btn-primary" onClick={() => addBlock('text')}><HiOutlinePlusSm /> Add text block</button>
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
                      onOpenImageUpload={() => openImageUpload(block.id)}
                    />
                  ))}
                </div>
              </div>
            </div>
          ) : (
            /* ── Split HTML Code View ── */
            <div className="tbe-split-code-wrap">
              <div className="tbe-code-editor-pane">
                <div className="tbe-code-toolbar">
                  <span className="tbe-code-title"><HiOutlineCode /> HTML Code Editor</span>
                  <div className="tbe-code-actions">
                    <button className="tbe-code-btn" onClick={handleFormatHtml}><HiOutlineSparkles /> Format</button>
                    <button className="tbe-code-btn" onClick={handleCopyCode}>
                      {copiedCode ? <HiOutlineCheck /> : <HiOutlineClipboard />} {copiedCode ? 'Copied!' : 'Copy HTML'}
                    </button>
                    <button className="tbe-code-btn" onClick={() => setRawHtmlCode(blocksToHtml(blocks, pageStyle, globalTheme, globalCss))}>
                      <HiOutlineRefresh /> Sync from Visual
                    </button>
                  </div>
                </div>
                <textarea
                  className="tbe-code-textarea"
                  value={rawHtmlCode}
                  onChange={e => setRawHtmlCode(e.target.value)}
                  placeholder="<!DOCTYPE html>..."
                  spellCheck={false}
                />
              </div>
              <div className="tbe-code-preview-pane">
                <div className="tbe-code-toolbar">
                  <span className="tbe-code-title">Live Preview</span>
                  <span className="tbe-code-badge">Real-time</span>
                </div>
                <iframe
                  className="tbe-preview-iframe"
                  srcDoc={currentPreviewHtml}
                  title="Live Email Preview"
                  sandbox="allow-same-origin"
                />
              </div>
            </div>
          )}

          {/* ── Right Inspector Panel — always visible ── */}
          <div className="tbe-inspector-panel">
            {selectedBlock && editorMode === 'visual' ? (
              <BlockInspector
                block={selectedBlock}
                onUpdate={updates => updateBlock(selectedBlock.id, updates)}
                onDeselect={() => setSelectedBlockId(null)}
              />
            ) : (
              <div className="tbe-global-inspector">
                <div className="tbe-inspector-tabs">
                  <button className={`tbe-insp-tab ${activeInspectorTab === 'style' ? 'active' : ''}`} onClick={() => setActiveInspectorTab('style')}>
                    <HiOutlineAdjustments /> Page Style
                  </button>
                  <button className={`tbe-insp-tab ${activeInspectorTab === 'theme' ? 'active' : ''}`} onClick={() => setActiveInspectorTab('theme')}>
                    <HiOutlineColorSwatch /> Theme
                  </button>
                  <button className={`tbe-insp-tab ${activeInspectorTab === 'css' ? 'active' : ''}`} onClick={() => setActiveInspectorTab('css')}>
                    <HiOutlineCode /> CSS
                  </button>
                </div>

                <div className="tbe-inspector-body">
                  {/* ── Tab 1: Page Style ── */}
                  {activeInspectorTab === 'style' && (
                    <div className="tbe-insp-section">
                      <div className="tbe-section-title">Page & Canvas</div>

                      <InspField label="Canvas Background">
                        <div className="tbe-color-row">
                          <input type="color" className="tbe-color-swatch" value={pageStyle.background} onChange={e => setPageStyle({ ...pageStyle, background: e.target.value })} />
                          <input className="tbe-color-hex" value={pageStyle.background} onChange={e => setPageStyle({ ...pageStyle, background: e.target.value })} maxLength={7} />
                        </div>
                      </InspField>

                      <InspField label="Body Background">
                        <div className="tbe-color-row">
                          <input type="color" className="tbe-color-swatch" value={pageStyle.bodyBackground} onChange={e => setPageStyle({ ...pageStyle, bodyBackground: e.target.value })} />
                          <input className="tbe-color-hex" value={pageStyle.bodyBackground} onChange={e => setPageStyle({ ...pageStyle, bodyBackground: e.target.value })} maxLength={7} />
                        </div>
                      </InspField>

                      <InspField label="Body Width">
                        <div className="tbe-unit-row">
                          <input type="number" className="tbe-num-input" value={pageStyle.bodyWidth} min={300} max={900} onChange={e => setPageStyle({ ...pageStyle, bodyWidth: Number(e.target.value) })} />
                          <span className="tbe-unit">px</span>
                        </div>
                      </InspField>

                      <InspField label="Body Alignment">
                        <div className="tbe-align-group">
                          {[{ val: 'left', label: 'Left' }, { val: 'center', label: 'Center' }, { val: 'right', label: 'Right' }].map(({ val, label }) => (
                            <button key={val} className={`tbe-align-btn ${pageStyle.bodyAlign === val ? 'active' : ''}`} onClick={() => setPageStyle({ ...pageStyle, bodyAlign: val })}>
                              {label}
                            </button>
                          ))}
                        </div>
                      </InspField>

                      <div className="tbe-section-title tbe-section-title-sm">Padding</div>
                      <div className="tbe-padding-row">
                        <div className="tbe-padding-linked">
                          <button
                            className={`tbe-link-btn ${pageStyle.paddingLinked ? 'active' : ''}`}
                            onClick={() => setPageStyle(ps => ({ ...ps, paddingLinked: !ps.paddingLinked }))}
                            title={pageStyle.paddingLinked ? 'Unlink sides' : 'Link all sides'}
                          >
                            <HiOutlineSwitchHorizontal />
                          </button>
                        </div>
                        <div className="tbe-box-grid">
                          <div className="tbe-box-row">
                            <span />
                            <div className="tbe-box-cell">
                              <label>Top</label>
                              <input type="number" value={pageStyle.paddingTop} min={0} max={120} onChange={e => updatePadding('paddingTop', Number(e.target.value))} />
                            </div>
                            <span />
                          </div>
                          <div className="tbe-box-row">
                            <div className="tbe-box-cell">
                              <label>Left</label>
                              <input type="number" value={pageStyle.paddingLeft} min={0} max={120} onChange={e => updatePadding('paddingLeft', Number(e.target.value))} />
                            </div>
                            <div className="tbe-box-center">Body</div>
                            <div className="tbe-box-cell">
                              <label>Right</label>
                              <input type="number" value={pageStyle.paddingRight} min={0} max={120} onChange={e => updatePadding('paddingRight', Number(e.target.value))} />
                            </div>
                          </div>
                          <div className="tbe-box-row">
                            <span />
                            <div className="tbe-box-cell">
                              <label>Bottom</label>
                              <input type="number" value={pageStyle.paddingBottom} min={0} max={120} onChange={e => updatePadding('paddingBottom', Number(e.target.value))} />
                            </div>
                            <span />
                          </div>
                        </div>
                      </div>

                      <div className="tbe-section-title tbe-section-title-sm">Corner Radius</div>
                      <div className="tbe-range-row">
                        <input type="range" className="tbe-range" min={0} max={32} value={pageStyle.borderRadius} onChange={e => setPageStyle({ ...pageStyle, borderRadius: Number(e.target.value) })} />
                        <span className="tbe-range-val">{pageStyle.borderRadius}px</span>
                      </div>

                      <div className="tbe-section-title tbe-section-title-sm">Border</div>
                      <div className="tbe-border-row">
                        <div className="tbe-unit-row" style={{ flex: 1 }}>
                          <input type="number" className="tbe-num-input" value={pageStyle.borderWidth} min={0} max={8} onChange={e => setPageStyle({ ...pageStyle, borderWidth: Number(e.target.value) })} />
                          <span className="tbe-unit">px</span>
                        </div>
                        <select className="tbe-select-sm" value={pageStyle.borderStyle} onChange={e => setPageStyle({ ...pageStyle, borderStyle: e.target.value })}>
                          <option value="solid">Solid</option>
                          <option value="dashed">Dashed</option>
                          <option value="dotted">Dotted</option>
                        </select>
                        <input type="color" className="tbe-color-swatch-sm" value={pageStyle.borderColor} onChange={e => setPageStyle({ ...pageStyle, borderColor: e.target.value })} />
                      </div>
                    </div>
                  )}

                  {/* ── Tab 2: Theme ── */}
                  {activeInspectorTab === 'theme' && (
                    <div className="tbe-insp-section">
                      <div className="tbe-section-title">Presets</div>
                      <div className="tbe-preset-row">
                        {Object.keys(THEME_PRESETS).map(pk => (
                          <button key={pk} className="tbe-preset-btn" onClick={() => applyThemePreset(pk)}>
                            {pk.charAt(0).toUpperCase() + pk.slice(1)}
                          </button>
                        ))}
                      </div>

                      <div className="tbe-section-title tbe-section-title-sm">Body Font</div>
                      <select className="tbe-select" value={globalTheme.textFont} onChange={e => setGlobalTheme({ ...globalTheme, textFont: e.target.value })}>
                        {GOOGLE_FONTS.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
                      </select>

                      <div className="tbe-section-title tbe-section-title-sm">Title / Heading Font</div>
                      <select className="tbe-select" value={globalTheme.titleFont} onChange={e => setGlobalTheme({ ...globalTheme, titleFont: e.target.value })}>
                        {GOOGLE_FONTS.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
                      </select>

                      <div className="tbe-section-title tbe-section-title-sm">Typography</div>

                      <InspField label="Body Font Size">
                        <div className="tbe-range-row">
                          <input type="range" className="tbe-range" min={12} max={22} value={globalTheme.textSize} onChange={e => setGlobalTheme({ ...globalTheme, textSize: Number(e.target.value) })} />
                          <span className="tbe-range-val">{globalTheme.textSize}px</span>
                        </div>
                      </InspField>

                      <InspField label="Font Weight">
                        <select className="tbe-select" value={globalTheme.textWeight} onChange={e => setGlobalTheme({ ...globalTheme, textWeight: e.target.value })}>
                          {FONT_WEIGHT_OPTIONS.map(w => <option key={w.value} value={w.value}>{w.label}</option>)}
                        </select>
                      </InspField>

                      <InspField label="Line Height">
                        <div className="tbe-range-row">
                          <input type="range" className="tbe-range" min={1.0} max={2.5} step={0.05} value={globalTheme.lineHeight} onChange={e => setGlobalTheme({ ...globalTheme, lineHeight: parseFloat(e.target.value) })} />
                          <span className="tbe-range-val">{globalTheme.lineHeight.toFixed(2)}</span>
                        </div>
                      </InspField>

                      <InspField label="Letter Spacing">
                        <div className="tbe-range-row">
                          <input type="range" className="tbe-range" min={-0.05} max={0.2} step={0.01} value={globalTheme.letterSpacing} onChange={e => setGlobalTheme({ ...globalTheme, letterSpacing: parseFloat(e.target.value) })} />
                          <span className="tbe-range-val">{globalTheme.letterSpacing.toFixed(2)}em</span>
                        </div>
                      </InspField>

                      <InspField label="Heading Size">
                        <div className="tbe-range-row">
                          <input type="range" className="tbe-range" min={18} max={52} value={globalTheme.headingSize} onChange={e => setGlobalTheme({ ...globalTheme, headingSize: Number(e.target.value) })} />
                          <span className="tbe-range-val">{globalTheme.headingSize}px</span>
                        </div>
                      </InspField>

                      <InspField label="Heading Weight">
                        <select className="tbe-select" value={globalTheme.headingWeight} onChange={e => setGlobalTheme({ ...globalTheme, headingWeight: e.target.value })}>
                          {FONT_WEIGHT_OPTIONS.map(w => <option key={w.value} value={w.value}>{w.label}</option>)}
                        </select>
                      </InspField>

                      <div className="tbe-section-title tbe-section-title-sm">Colors</div>

                      <InspField label="Body Text Color">
                        <div className="tbe-color-row">
                          <input type="color" className="tbe-color-swatch" value={globalTheme.textColor} onChange={e => setGlobalTheme({ ...globalTheme, textColor: e.target.value })} />
                          <input className="tbe-color-hex" value={globalTheme.textColor} onChange={e => setGlobalTheme({ ...globalTheme, textColor: e.target.value })} maxLength={7} />
                        </div>
                      </InspField>

                      <InspField label="Heading Color">
                        <div className="tbe-color-row">
                          <input type="color" className="tbe-color-swatch" value={globalTheme.titleColor} onChange={e => setGlobalTheme({ ...globalTheme, titleColor: e.target.value })} />
                          <input className="tbe-color-hex" value={globalTheme.titleColor} onChange={e => setGlobalTheme({ ...globalTheme, titleColor: e.target.value })} maxLength={7} />
                        </div>
                      </InspField>

                      <InspField label="Link Color">
                        <div className="tbe-color-row">
                          <input type="color" className="tbe-color-swatch" value={globalTheme.linkColor} onChange={e => setGlobalTheme({ ...globalTheme, linkColor: e.target.value })} />
                          <input className="tbe-color-hex" value={globalTheme.linkColor} onChange={e => setGlobalTheme({ ...globalTheme, linkColor: e.target.value })} maxLength={7} />
                        </div>
                      </InspField>

                      <div className="tbe-section-title tbe-section-title-sm">Text Decoration</div>
                      <div className="tbe-deco-row">
                        <button title="Bold" className={`tbe-deco-btn ${globalTheme.textWeight === '700' ? 'active' : ''}`} onClick={() => setGlobalTheme(t => ({ ...t, textWeight: t.textWeight === '700' ? '400' : '700' }))}>
                          <strong style={{ fontWeight: 700 }}>B</strong>
                        </button>
                        <button title="Italic (in CSS)" className="tbe-deco-btn" style={{ fontStyle: 'italic' }}>
                          <em>I</em>
                        </button>
                        <button title="Underline links" className="tbe-deco-btn" style={{ textDecoration: 'underline' }}>
                          U
                        </button>
                      </div>

                      <div className="tbe-theme-preview">
                        <p style={{ fontFamily: globalTheme.textFont, fontSize: globalTheme.textSize, color: globalTheme.textColor, lineHeight: globalTheme.lineHeight, letterSpacing: `${globalTheme.letterSpacing}em`, margin: 0 }}>
                          Body text preview. <a href="#" style={{ color: globalTheme.linkColor }}>Link example</a>
                        </p>
                        <p style={{ fontFamily: globalTheme.titleFont, fontSize: globalTheme.headingSize, color: globalTheme.titleColor, fontWeight: globalTheme.headingWeight, margin: '8px 0 0' }}>
                          Heading Preview
                        </p>
                      </div>
                    </div>
                  )}

                  {/* ── Tab 3: Global CSS ── */}
                  {activeInspectorTab === 'css' && (
                    <div className="tbe-insp-section">
                      <div className="tbe-section-title">Global CSS Rules</div>
                      <p className="tbe-css-desc">
                        Write raw CSS selectors. Applied inside the <code>&lt;style&gt;</code> tag.
                        Supports <code>@media</code> breakpoints.
                      </p>
                      <div className="tbe-css-toolbar">
                        <button className="tbe-code-btn" onClick={() => setGlobalCss(DEFAULT_GLOBAL_CSS)}>
                          <HiOutlineRefresh /> Reset to Default
                        </button>
                      </div>
                      <textarea
                        className="tbe-css-textarea"
                        value={globalCss}
                        onChange={e => setGlobalCss(e.target.value)}
                        rows={18}
                        spellCheck={false}
                        placeholder="/* Your custom CSS */"
                      />
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

        </div>
      </motion.div>

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
                <p className="tbe-modal-desc">Sends a test email using current draft content.</p>
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
        {imageUploadOpen && (
          <ImageUploadModal
            onClose={() => { setImageUploadOpen(false); setImageUploadBlock(null); }}
            onConfirm={(url) => {
              if (imageUploadBlock) updateBlock(imageUploadBlock, { src: url });
              setImageUploadOpen(false);
              setImageUploadBlock(null);
            }}
          />
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Block Row — inline toolbar ───────────────────────────────────────────────

function BlockRow({ block, isSelected, isFirst, isLast, onSelect, onUpdate, onDelete, onMoveUp, onMoveDown, onDuplicate, onOpenImageUpload }) {
  return (
    <div
      className={`tbe-block-row ${isSelected ? 'selected' : ''}`}
      onClick={e => { e.stopPropagation(); onSelect(); }}
    >
      {/* Inline control toolbar — shown when selected, inside the block */}
      {isSelected && (
        <div className="tbe-block-toolbar" onClick={e => e.stopPropagation()}>
          <div className="tbe-block-toolbar-left">
            <span className="tbe-block-type-label">{block.type.toUpperCase()}</span>
          </div>
          <div className="tbe-block-toolbar-right">
            <button
              className="tbe-ctrl-btn"
              title="Move up"
              disabled={isFirst}
              onClick={e => { e.stopPropagation(); onMoveUp(); }}
            >
              <HiOutlineChevronUp />
            </button>
            <button
              className="tbe-ctrl-btn"
              title="Move down"
              disabled={isLast}
              onClick={e => { e.stopPropagation(); onMoveDown(); }}
            >
              <HiOutlineChevronDown />
            </button>
            <button
              className="tbe-ctrl-btn"
              title="Duplicate block"
              onClick={e => { e.stopPropagation(); onDuplicate(); }}
            >
              <HiOutlineDuplicate />
            </button>
            {(block.type === 'image' || block.type === 'media') && (
              <button
                className="tbe-ctrl-btn"
                title="Upload / set media"
                onClick={e => { e.stopPropagation(); onOpenImageUpload(); }}
              >
                <HiOutlineUpload />
              </button>
            )}
            <button
              className="tbe-ctrl-btn tbe-ctrl-danger"
              title="Delete block"
              onClick={e => { e.stopPropagation(); onDelete(); }}
            >
              <HiOutlineTrash />
            </button>
          </div>
        </div>
      )}
      <div className="tbe-block-content">
        <BlockContent block={block} onUpdate={onUpdate} isSelected={isSelected} />
      </div>
    </div>
  );
}

// ─── Rich Text Block ── uses innerHTML + execCommand for word-level editing ────

function RichTextBlock({ className, style, html, onCommit, placeholder }) {
  const ref = useRef(null);
  const [showToolbar, setShowToolbar] = useState(false);
  const [linkMode, setLinkMode] = useState(false);
  const [linkUrl, setLinkUrl] = useState('https://');
  const [showVarMenu, setShowVarMenu] = useState(false);
  const savedRange = useRef(null);

  // Only set innerHTML on mount or when html prop changes externally
  useEffect(() => {
    if (ref.current && ref.current.innerHTML !== html) {
      ref.current.innerHTML = html || '';
    }
  }, [html]);

  const saveSelection = () => {
    const sel = window.getSelection();
    if (sel && sel.rangeCount > 0) savedRange.current = sel.getRangeAt(0).cloneRange();
  };

  const restoreSelection = () => {
    const sel = window.getSelection();
    if (savedRange.current) { sel.removeAllRanges(); sel.addRange(savedRange.current); }
  };

  const execCmd = (cmd, value = null) => {
    ref.current.focus();
    restoreSelection();
    document.execCommand(cmd, false, value);
    onCommit(ref.current.innerHTML);
  };

  const handleSelection = () => {
    const sel = window.getSelection();
    if (sel && sel.toString().length > 0) {
      saveSelection();
      setShowToolbar(true);
      setLinkMode(false);
      setShowVarMenu(false);
    } else {
      setShowToolbar(false);
    }
  };

  const handleInsertLink = () => {
    restoreSelection();
    execCmd('createLink', linkUrl);
    // Make it open in a new tab in the output
    const sel = window.getSelection();
    if (sel && sel.rangeCount > 0) {
      const a = sel.getRangeAt(0).commonAncestorContainer.parentElement;
      if (a && a.tagName === 'A') { a.target = '_blank'; a.rel = 'noopener'; }
    }
    onCommit(ref.current.innerHTML);
    setLinkMode(false);
    setShowToolbar(false);
  };

  const handleInsertVar = (varKey) => {
    ref.current.focus();
    restoreSelection();
    document.execCommand('insertHTML', false, `<code style="background:#f1f5f9;color:#475569;padding:1px 5px;border-radius:3px;font-size:0.9em;">{{${varKey}}}</code>`);
    onCommit(ref.current.innerHTML);
    setShowVarMenu(false);
    setShowToolbar(false);
  };

  return (
    <div className="tbe-richtext-wrap" style={{ position: 'relative' }}>
      {showToolbar && (
        <div className="tbe-richtext-toolbar" onMouseDown={e => e.preventDefault()}>
          {!linkMode ? (
            <>
              <button className="tbe-rt-btn" title="Bold" onMouseDown={e => { e.preventDefault(); execCmd('bold'); }}>B</button>
              <button className="tbe-rt-btn tbe-rt-italic" title="Italic" onMouseDown={e => { e.preventDefault(); execCmd('italic'); }}>I</button>
              <button className="tbe-rt-btn tbe-rt-under" title="Underline" onMouseDown={e => { e.preventDefault(); execCmd('underline'); }}>U</button>
              <div className="tbe-rt-sep" />
              <button className="tbe-rt-btn" title="Add link" onMouseDown={e => { e.preventDefault(); saveSelection(); setLinkMode(true); }}><HiOutlineLink /></button>
              <button className="tbe-rt-btn" title="Remove link" onMouseDown={e => { e.preventDefault(); execCmd('unlink'); setShowToolbar(false); }}><span style={{fontSize:11}}>No link</span></button>
              <div className="tbe-rt-sep" />
              <button className="tbe-rt-btn" title="Insert variable" onMouseDown={e => { e.preventDefault(); saveSelection(); setShowVarMenu(v => !v); }}>{'{}'}</button>
            </>
          ) : (
            <div className="tbe-rt-link-row">
              <input
                className="tbe-rt-link-input"
                value={linkUrl}
                onChange={e => setLinkUrl(e.target.value)}
                placeholder="https://... or {{VAR}}"
                onKeyDown={e => e.key === 'Enter' && handleInsertLink()}
                autoFocus
              />
              <button className="tbe-rt-btn" onMouseDown={e => { e.preventDefault(); handleInsertLink(); }}>Apply</button>
              <button className="tbe-rt-btn" onMouseDown={e => { e.preventDefault(); setLinkMode(false); }}>✕</button>
            </div>
          )}
          {showVarMenu && (
            <div className="tbe-rt-var-menu">
              {RESERVED_VARIABLES.map(v => (
                <button key={v.key} className="tbe-rt-var-item" onMouseDown={e => { e.preventDefault(); handleInsertVar(v.key); }}>
                  {'{{' + v.key + '}}'}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
      <div
        ref={ref}
        contentEditable
        suppressContentEditableWarning
        className={className}
        style={style}
        onMouseUp={handleSelection}
        onKeyUp={handleSelection}
        onBlur={() => { onCommit(ref.current.innerHTML); setShowToolbar(false); setLinkMode(false); }}
        data-placeholder={placeholder}
      />
    </div>
  );
}

// ─── Block Content ─────────────────────────────────────────────────────────────

function BlockContent({ block, onUpdate, isSelected }) {
  switch (block.type) {
    case 'heading':
      return (
        <RichTextBlock
          className="tbe-block-heading"
          style={{ textAlign: block.align, color: block.color, fontSize: block.fontSize }}
          html={block.content}
          onCommit={html => onUpdate({ content: html })}
          placeholder="Type your heading..."
        />
      );
    case 'text':
      return (
        <RichTextBlock
          className="tbe-block-text"
          style={{ textAlign: block.align, color: block.color, fontSize: block.fontSize, lineHeight: block.lineHeight }}
          html={block.content}
          onCommit={html => onUpdate({ content: html })}
          placeholder="Type your paragraph text..."
        />
      );
    case 'button':
      return (
        <div style={{ textAlign: block.align }}>
          <div
            className="tbe-block-button"
            style={{ backgroundColor: block.bgColor, color: block.textColor, borderRadius: block.borderRadius, padding: `${block.paddingV}px ${block.paddingH}px` }}
            contentEditable suppressContentEditableWarning
            onBlur={e => onUpdate({ content: e.currentTarget.textContent })}
            dangerouslySetInnerHTML={{ __html: block.content }}
          />
        </div>
      );
    case 'image':
      return (
        <div style={{ textAlign: block.align }}>
          {block.src ? (
            <>
              <img src={block.src} alt={block.alt} style={{ maxWidth: block.width, height: 'auto', borderRadius: 8, display: 'inline-block' }} />
              {block.caption && (
                <p style={{ fontSize: 12, color: '#6b7280', marginTop: 6, textAlign: block.align }}>{block.caption}</p>
              )}
            </>
          ) : (
            <div className="tbe-block-image-placeholder">
              <HiOutlinePhotograph style={{ fontSize: 28, opacity: 0.4 }} />
              <span>No image set. Use the</span>
              <span style={{ background: 'rgba(0,0,0,0.06)', padding: '1px 8px', borderRadius: 4, fontWeight: 600 }}>Upload</span>
              <span>button above.</span>
            </div>
          )}
        </div>
      );
    case 'divider':
      return <hr style={{ border: 'none', borderTop: `${block.thickness}px solid ${block.color}`, margin: `${block.margin}px 0` }} />;
    case 'columns':
      return (
        <div className="tbe-block-columns">
          {block.columns.map((col, i) => (
            <div key={i} className="tbe-block-column" contentEditable suppressContentEditableWarning
              style={{ textAlign: col.align, color: col.color }}
              onBlur={e => { const updated = block.columns.map((c, ci) => ci === i ? { ...c, content: e.currentTarget.textContent } : c); onUpdate({ columns: updated }); }}
              dangerouslySetInnerHTML={{ __html: col.content }}
            />
          ))}
        </div>
      );
    case '3columns':
      return (
        <div className="tbe-block-3columns">
          {block.columns.map((col, i) => (
            <div key={i} className="tbe-block-column" contentEditable suppressContentEditableWarning
              style={{ textAlign: col.align, color: col.color }}
              onBlur={e => { const updated = block.columns.map((c, ci) => ci === i ? { ...c, content: e.currentTarget.textContent } : c); onUpdate({ columns: updated }); }}
              dangerouslySetInnerHTML={{ __html: col.content }}
            />
          ))}
        </div>
      );
    case 'section':
      return (
        <div style={{ backgroundColor: block.bgColor, border: `1px solid ${block.borderColor}`, padding: `${block.padding}px`, borderRadius: `${block.borderRadius}px` }}>
          <div contentEditable suppressContentEditableWarning
            onBlur={e => onUpdate({ content: e.currentTarget.textContent })}
            dangerouslySetInnerHTML={{ __html: block.content }}
          />
        </div>
      );
    case 'spacer':
      return <div className="tbe-block-spacer" style={{ height: block.height }} />;
    case 'media':
      return (
        <div style={{ textAlign: block.align }}>
          <div className="tbe-block-media-placeholder">
            <span style={{ fontSize: 22 }}>📎</span>
            <div className="tbe-block-media-info">
              {block.src ? (
                <a href={block.src} style={{ color: '#374151', textDecoration: 'underline', fontFamily: 'inherit' }}>{block.filename || 'Attached file'}</a>
              ) : (
                <span style={{ color: '#9ca3af', fontFamily: 'inherit' }}>Add file URL and filename in Inspector →</span>
              )}
            </div>
          </div>
        </div>
      );
    case 'unsubscribe':
      return (
        <div style={{ textAlign: block.align, padding: '10px 0' }}>
          <a
            href="#preview-unsubscribe"
            onClick={e => e.preventDefault()}
            style={{ color: block.color, textDecoration: 'underline', fontSize: block.fontSize, fontFamily: 'inherit' }}
          >
            {block.text}
          </a>
        </div>
      );
    default:
      return null;
  }
}

// ─── Block Inspector ───────────────────────────────────────────────────────────

function BlockInspector({ block, onUpdate, onDeselect }) {
  return (
    <div className="tbe-block-inspector">
      <div className="tbe-inspector-header">
        <span className="tbe-inspector-title">{block.type.toUpperCase()} Properties</span>
        <button className="tbe-btn-icon" onClick={onDeselect}><HiOutlineX /></button>
      </div>
      <div className="tbe-inspector-body">
        {['heading', 'text', 'button', 'image', 'unsubscribe'].includes(block.type) && (
          <InspField label="Alignment">
            <div className="tbe-align-group">
              {[{val:'left',label:'Left'},{val:'center',label:'Center'},{val:'right',label:'Right'}].map(({val,label}) => (
                <button key={val} className={`tbe-align-btn ${block.align === val ? 'active' : ''}`} onClick={() => onUpdate({ align: val })}>
                  {label}
                </button>
              ))}
            </div>
          </InspField>
        )}

        {['heading', 'text'].includes(block.type) && (
          <>
            <InspField label="Color">
              <div className="tbe-color-row">
                <input type="color" className="tbe-color-swatch" value={block.color} onChange={e => onUpdate({ color: e.target.value })} />
                <input className="tbe-color-hex" value={block.color} onChange={e => onUpdate({ color: e.target.value })} maxLength={7} />
              </div>
            </InspField>
            <InspField label="Font Size">
              <div className="tbe-range-row">
                <input type="range" className="tbe-range" min={12} max={64} value={block.fontSize} onChange={e => onUpdate({ fontSize: Number(e.target.value) })} />
                <span className="tbe-range-val">{block.fontSize}px</span>
              </div>
            </InspField>
          </>
        )}

        {block.type === 'text' && (
          <InspField label="Line Height">
            <div className="tbe-range-row">
              <input type="range" className="tbe-range" min={1.0} max={2.5} step={0.05} value={block.lineHeight || 1.6} onChange={e => onUpdate({ lineHeight: parseFloat(e.target.value) })} />
              <span className="tbe-range-val">{(block.lineHeight || 1.6).toFixed(2)}</span>
            </div>
          </InspField>
        )}

        {block.type === 'button' && (
          <>
            <InspField label="URL Link">
              <input className="tbe-text-input" value={block.href} onChange={e => onUpdate({ href: e.target.value })} placeholder="https://..." />
            </InspField>
            <InspField label="Background Color">
              <div className="tbe-color-row">
                <input type="color" className="tbe-color-swatch" value={block.bgColor} onChange={e => onUpdate({ bgColor: e.target.value })} />
                <input className="tbe-color-hex" value={block.bgColor} onChange={e => onUpdate({ bgColor: e.target.value })} maxLength={7} />
              </div>
            </InspField>
            <InspField label="Text Color">
              <div className="tbe-color-row">
                <input type="color" className="tbe-color-swatch" value={block.textColor} onChange={e => onUpdate({ textColor: e.target.value })} />
                <input className="tbe-color-hex" value={block.textColor} onChange={e => onUpdate({ textColor: e.target.value })} maxLength={7} />
              </div>
            </InspField>
            <InspField label="Border Radius">
              <div className="tbe-range-row">
                <input type="range" className="tbe-range" min={0} max={40} value={block.borderRadius} onChange={e => onUpdate({ borderRadius: Number(e.target.value) })} />
                <span className="tbe-range-val">{block.borderRadius}px</span>
              </div>
            </InspField>
            <InspField label="Padding (V / H)">
              <div className="tbe-two-col">
                <input type="number" className="tbe-num-input" value={block.paddingV} min={0} max={40} onChange={e => onUpdate({ paddingV: Number(e.target.value) })} />
                <input type="number" className="tbe-num-input" value={block.paddingH} min={0} max={80} onChange={e => onUpdate({ paddingH: Number(e.target.value) })} />
              </div>
            </InspField>
          </>
        )}

        {block.type === 'image' && (
          <>
            <InspField label="Image Source URL">
              <input className="tbe-text-input" value={block.src} onChange={e => onUpdate({ src: e.target.value })} placeholder="https://..." />
            </InspField>
            <InspField label="Alt Text">
              <input className="tbe-text-input" value={block.alt} onChange={e => onUpdate({ alt: e.target.value })} />
            </InspField>
            <InspField label="Width">
              <input className="tbe-text-input" value={block.width} onChange={e => onUpdate({ width: e.target.value })} placeholder="100%" />
            </InspField>
          </>
        )}

        {block.type === 'divider' && (
          <>
            <InspField label="Color">
              <div className="tbe-color-row">
                <input type="color" className="tbe-color-swatch" value={block.color} onChange={e => onUpdate({ color: e.target.value })} />
                <input className="tbe-color-hex" value={block.color} onChange={e => onUpdate({ color: e.target.value })} maxLength={7} />
              </div>
            </InspField>
            <InspField label="Thickness">
              <div className="tbe-range-row">
                <input type="range" className="tbe-range" min={1} max={8} value={block.thickness} onChange={e => onUpdate({ thickness: Number(e.target.value) })} />
                <span className="tbe-range-val">{block.thickness}px</span>
              </div>
            </InspField>
            <InspField label="Margin">
              <div className="tbe-range-row">
                <input type="range" className="tbe-range" min={0} max={64} value={block.margin} onChange={e => onUpdate({ margin: Number(e.target.value) })} />
                <span className="tbe-range-val">{block.margin}px</span>
              </div>
            </InspField>
          </>
        )}

        {block.type === 'spacer' && (
          <InspField label="Height">
            <div className="tbe-range-row">
              <input type="range" className="tbe-range" min={8} max={120} value={block.height} onChange={e => onUpdate({ height: Number(e.target.value) })} />
              <span className="tbe-range-val">{block.height}px</span>
            </div>
          </InspField>
        )}

        {block.type === 'section' && (
          <>
            <InspField label="Background Color">
              <div className="tbe-color-row">
                <input type="color" className="tbe-color-swatch" value={block.bgColor} onChange={e => onUpdate({ bgColor: e.target.value })} />
                <input className="tbe-color-hex" value={block.bgColor} onChange={e => onUpdate({ bgColor: e.target.value })} maxLength={7} />
              </div>
            </InspField>
            <InspField label="Border Color">
              <div className="tbe-color-row">
                <input type="color" className="tbe-color-swatch" value={block.borderColor} onChange={e => onUpdate({ borderColor: e.target.value })} />
                <input className="tbe-color-hex" value={block.borderColor} onChange={e => onUpdate({ borderColor: e.target.value })} maxLength={7} />
              </div>
            </InspField>
            <InspField label="Padding">
              <div className="tbe-range-row">
                <input type="range" className="tbe-range" min={0} max={64} value={block.padding} onChange={e => onUpdate({ padding: Number(e.target.value) })} />
                <span className="tbe-range-val">{block.padding}px</span>
              </div>
            </InspField>
            <InspField label="Border Radius">
              <div className="tbe-range-row">
                <input type="range" className="tbe-range" min={0} max={32} value={block.borderRadius} onChange={e => onUpdate({ borderRadius: Number(e.target.value) })} />
                <span className="tbe-range-val">{block.borderRadius}px</span>
              </div>
            </InspField>
          </>
        )}

        {block.type === 'media' && (
          <>
            <InspField label="File / Media URL">
              <input className="tbe-text-input" value={block.src} onChange={e => onUpdate({ src: e.target.value })} placeholder="https://files.example.com/doc.pdf" />
            </InspField>
            <InspField label="Display Filename">
              <input className="tbe-text-input" value={block.filename} onChange={e => onUpdate({ filename: e.target.value })} placeholder="document.pdf" />
            </InspField>
            <InspField label="Alignment">
              <div className="tbe-align-group">
                {[{val:'left',label:'Left'},{val:'center',label:'Center'},{val:'right',label:'Right'}].map(({val,label}) => (
                  <button key={val} className={`tbe-align-btn ${block.align === val ? 'active' : ''}`} onClick={() => onUpdate({ align: val })}>
                    {label}
                  </button>
                ))}
              </div>
            </InspField>
          </>
        )}

        {block.type === 'unsubscribe' && (
          <>
            <InspField label="Link Text">
              <input className="tbe-text-input" value={block.text} onChange={e => onUpdate({ text: e.target.value })} />
            </InspField>
            <InspField label="Color">
              <div className="tbe-color-row">
                <input type="color" className="tbe-color-swatch" value={block.color} onChange={e => onUpdate({ color: e.target.value })} />
                <input className="tbe-color-hex" value={block.color} onChange={e => onUpdate({ color: e.target.value })} maxLength={7} />
              </div>
            </InspField>
            <InspField label="Font Size">
              <div className="tbe-range-row">
                <input type="range" className="tbe-range" min={10} max={18} value={block.fontSize} onChange={e => onUpdate({ fontSize: Number(e.target.value) })} />
                <span className="tbe-range-val">{block.fontSize}px</span>
              </div>
            </InspField>
          </>
        )}
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

// ─── Image Upload Modal ─────────────────────────────────────────────────────────

function ImageUploadModal({ onClose, onConfirm }) {
  const [tab, setTab] = useState('url'); // 'url' | 'upload' | 'drive'
  const [urlValue, setUrlValue] = useState('');
  const [driveUrl, setDriveUrl] = useState('');
  const [preview, setPreview] = useState('');
  const fileRef = useRef(null);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      setPreview(ev.target.result);
      setUrlValue(ev.target.result);
    };
    reader.readAsDataURL(file);
  };

  const handleUrlChange = (v) => {
    setUrlValue(v);
    setPreview(v);
  };

  const handleDriveChange = (v) => {
    setDriveUrl(v);
    // Convert Google Drive share URLs to direct image URL
    const match = v.match(/\/d\/([a-zA-Z0-9_-]+)/);
    if (match) {
      const direct = `https://drive.google.com/uc?export=view&id=${match[1]}`;
      setPreview(direct);
      setUrlValue(direct);
    } else {
      setPreview(v);
      setUrlValue(v);
    }
  };

  return (
    <motion.div
      className="tbe-modal-backdrop"
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="tbe-modal tbe-modal-md"
        initial={{ opacity: 0, scale: 0.95, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }}
        onClick={e => e.stopPropagation()}
      >
        <div className="tbe-modal-header">
          <h2>Add Image or File</h2>
          <button className="tbe-btn-icon" onClick={onClose}><HiOutlineX /></button>
        </div>

        <div className="tbe-upload-tabs">
          {[{id:'url',label:'URL / Link'},{id:'upload',label:'Upload from Device'},{id:'drive',label:'Google Drive'}].map(t => (
            <button
              key={t.id}
              className={`tbe-upload-tab ${tab === t.id ? 'active' : ''}`}
              onClick={() => setTab(t.id)}
            >
              {t.label}
            </button>
          ))}
        </div>

        <div className="tbe-modal-body">
          {tab === 'url' && (
            <label className="tbe-form-label">
              Image or File URL
              <input
                className="tbe-form-input"
                value={urlValue}
                onChange={e => handleUrlChange(e.target.value)}
                placeholder="https://example.com/image.png"
                autoFocus
              />
            </label>
          )}

          {tab === 'upload' && (
            <div className="tbe-upload-area" onClick={() => fileRef.current?.click()}>
              <HiOutlineUpload style={{ fontSize: 28, opacity: 0.5 }} />
              <p>Click to select an image from your device</p>
              <span style={{ fontSize: 12, opacity: 0.5 }}>PNG, JPG, GIF, WebP, SVG</span>
              <input
                ref={fileRef}
                type="file"
                accept="image/*,.pdf,.doc,.docx,.xls,.xlsx"
                style={{ display: 'none' }}
                onChange={handleFileChange}
              />
            </div>
          )}

          {tab === 'drive' && (
            <div>
              <label className="tbe-form-label" style={{ marginBottom: 10 }}>
                Google Drive Share Link
                <input
                  className="tbe-form-input"
                  value={driveUrl}
                  onChange={e => handleDriveChange(e.target.value)}
                  placeholder="https://drive.google.com/file/d/..."
                />
              </label>
              <p style={{ fontSize: 12, color: 'var(--text-tertiary)', marginTop: 8, fontFamily: 'var(--font-sans)' }}>
                Share your file in Google Drive → Share → Anyone with link → Copy link
              </p>
            </div>
          )}

          {preview && (
            <div className="tbe-upload-preview">
              <div className="tbe-upload-preview-label">Preview</div>
              <img
                src={preview}
                alt="Preview"
                style={{ maxHeight: 180, maxWidth: '100%', borderRadius: 8, objectFit: 'contain' }}
                onError={e => { e.target.style.display = 'none'; }}
              />
            </div>
          )}
        </div>

        <div className="tbe-modal-footer">
          <button className="tbe-btn-secondary" onClick={onClose}>Cancel</button>
          <button
            className="tbe-btn-primary"
            onClick={() => urlValue && onConfirm(urlValue)}
            disabled={!urlValue}
          >
            <HiOutlineCheck /> Insert
          </button>
        </div>
      </motion.div>
    </motion.div>
  );
}

function initBlocks(template) {
  if (!template || (!template.htmlBody && !template.contentHtml)) {
    return [createBlock('heading'), createBlock('text')];
  }
  const html = template.htmlBody || template.contentHtml || '';
  return [{ id: crypto.randomUUID(), type: 'text', content: html, color: '#374151', fontSize: 15, align: 'left', lineHeight: 1.6 }];
}
