/**
 * TemplateBlockEditor.jsx
 *
 * Crescendo Email Template Studio (Resend 1:1 Architecture & UI/UX)
 *
 * Enhancements:
 * - Text Selection Formatting & Editing: Selection retention with onMouseDown preventDefault on all bubble actions
 * - Inline Rich Text Formatting: Bold, Italic, Underline, Strikethrough, Text Color, Highlight Color, Lists, Links, Tags
 * - Auto-Close Open Drawers: Selecting any block or clicking canvas automatically switches back to inspector & closes flyouts
 * - Zero Horizontal Scrollbars: 100% strict box-sizing & width containment across all property rows
 * - Multi-Tab Image Dialog: Drag & drop device upload (FileReader base64), Unsplash presets, direct URL
 * - Robust Auto-Save to PostgreSQL with non-LOB TEXT columns & live status feedback
 */

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { createPortal } from 'react-dom';
import {
  HiOutlineChevronRight,
  HiOutlineChevronDown,
  HiOutlineColorSwatch,
  HiOutlineCode,
  HiOutlineDocumentText,
  HiOutlineVariable,
  HiOutlineTrash,
  HiOutlineDuplicate,
  HiOutlinePhotograph,
  HiOutlinePlusSm,
  HiOutlineX,
  HiOutlineCheck,
  HiOutlineClipboard,
  HiArrowLeft,
  HiOutlineViewGrid,
  HiOutlineInformationCircle,
  HiOutlineLink,
  HiOutlineArrowUp,
  HiOutlineArrowDown,
  HiOutlineUpload
} from 'react-icons/hi';
import {
  MdFormatAlignLeft,
  MdFormatAlignCenter,
  MdFormatAlignRight,
  MdFormatBold,
  MdFormatItalic,
  MdFormatUnderlined,
  MdFormatStrikethrough,
  MdFormatClear,
  MdFormatListBulleted,
  MdFormatListNumbered,
  MdUndo,
  MdRedo,
  MdLink,
  MdLinkOff,
  MdFormatColorText,
  MdFormatColorFill
} from 'react-icons/md';
import { templatesApi } from '../../api/emailServiceApi';
import './TemplateBlockEditor.css';

// ─── Font & Token Constants ──────────────────────────────────────────────────

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
  { label: 'Commit Mono (Code)', value: '"Commit Mono", monospace' }
];

const FONT_WEIGHT_OPTIONS = [
  { label: 'Light (300)', value: '300' },
  { label: 'Regular (400)', value: '400' },
  { label: 'Medium (500)', value: '500' },
  { label: 'Semibold (600)', value: '600' },
  { label: 'Bold (700)', value: '700' },
  { label: 'Extrabold (800)', value: '800' }
];

const THEME_DEFAULTS = {
  minimal: {
    bodyBg: '#ffffff',
    bodyPadding: { all: 24, top: 24, right: 24, bottom: 24, left: 24 },
    containerBg: '#ffffff',
    containerAlign: 'center',
    containerWidth: 600,
    containerWidthUnit: 'px',
    containerHeight: 'auto',
    containerHeightUnit: 'auto',
    containerPadding: { all: 32, top: 32, right: 32, bottom: 32, left: 32 },
    containerMargin: { all: 0, top: 0, right: 0, bottom: 0, left: 0 },
    containerRadius: { all: 8, topLeft: 8, topRight: 8, bottomRight: 8, bottomLeft: 8 },
    containerBorder: { all: 0, top: 0, right: 0, bottom: 0, left: 0 },
    containerBorderColor: '#e4e4e7',
    text: { color: '#18181b', size: 14, weight: '400', height: 155, fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif' },
    title: { color: '#09090b', size: 24, weight: '700', height: 130, fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif' },
    subtitle: { color: '#71717a', size: 16, weight: '500', height: 140, fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif' },
    button: { bg: '#000000', color: '#ffffff', radius: 8, paddingX: 20, paddingY: 10 },
    link: { color: '#000000' }
  },
  basic: {
    bodyBg: '#f4f4f5',
    bodyPadding: { all: 32, top: 32, right: 32, bottom: 32, left: 32 },
    containerBg: '#ffffff',
    containerAlign: 'center',
    containerWidth: 600,
    containerWidthUnit: 'px',
    containerHeight: 'auto',
    containerHeightUnit: 'auto',
    containerPadding: { all: 40, top: 40, right: 40, bottom: 40, left: 40 },
    containerMargin: { all: 0, top: 0, right: 0, bottom: 0, left: 0 },
    containerRadius: { all: 12, topLeft: 12, topRight: 12, bottomRight: 12, bottomLeft: 12 },
    containerBorder: { all: 1, top: 1, right: 1, bottom: 1, left: 1 },
    containerBorderColor: '#e4e4e7',
    text: { color: '#27272a', size: 15, weight: '400', height: 160, fontFamily: '"Inter", sans-serif' },
    title: { color: '#09090b', size: 26, weight: '700', height: 130, fontFamily: '"Inter", sans-serif' },
    subtitle: { color: '#71717a', size: 17, weight: '500', height: 145, fontFamily: '"Inter", sans-serif' },
    button: { bg: '#09090b', color: '#ffffff', radius: 6, paddingX: 24, paddingY: 12 },
    link: { color: '#2563eb' }
  },
  modern: {
    bodyBg: '#09090b',
    bodyPadding: { all: 32, top: 32, right: 32, bottom: 32, left: 32 },
    containerBg: '#121215',
    containerAlign: 'center',
    containerWidth: 600,
    containerWidthUnit: 'px',
    containerHeight: 'auto',
    containerHeightUnit: 'auto',
    containerPadding: { all: 36, top: 36, right: 36, bottom: 36, left: 36 },
    containerMargin: { all: 0, top: 0, right: 0, bottom: 0, left: 0 },
    containerRadius: { all: 16, topLeft: 16, topRight: 16, bottomRight: 16, bottomLeft: 16 },
    containerBorder: { all: 1, top: 1, right: 1, bottom: 1, left: 1 },
    containerBorderColor: '#27272a',
    text: { color: '#a1a1aa', size: 14, weight: '400', height: 160, fontFamily: '"Inter", sans-serif' },
    title: { color: '#fafafa', size: 24, weight: '700', height: 130, fontFamily: '"Inter", sans-serif' },
    subtitle: { color: '#71717a', size: 16, weight: '500', height: 140, fontFamily: '"Inter", sans-serif' },
    button: { bg: '#ffffff', color: '#000000', radius: 8, paddingX: 20, paddingY: 10 },
    link: { color: '#60a5fa' }
  },
  elegant: {
    bodyBg: '#faf9f6',
    bodyPadding: { all: 40, top: 40, right: 40, bottom: 40, left: 40 },
    containerBg: '#ffffff',
    containerAlign: 'center',
    containerWidth: 580,
    containerWidthUnit: 'px',
    containerHeight: 'auto',
    containerHeightUnit: 'auto',
    containerPadding: { all: 44, top: 44, right: 44, bottom: 44, left: 44 },
    containerMargin: { all: 0, top: 0, right: 0, bottom: 0, left: 0 },
    containerRadius: { all: 0, topLeft: 0, topRight: 0, bottomRight: 0, bottomLeft: 0 },
    containerBorder: { all: 1, top: 1, right: 1, bottom: 1, left: 1 },
    containerBorderColor: '#e5e5e5',
    text: { color: '#2c2c2c', size: 15, weight: '400', height: 170, fontFamily: 'Georgia, serif' },
    title: { color: '#111111', size: 28, weight: '700', height: 130, fontFamily: '"Playfair Display", serif' },
    subtitle: { color: '#666666', size: 16, weight: '400', height: 150, fontFamily: 'Georgia, serif' },
    button: { bg: '#111111', color: '#ffffff', radius: 0, paddingX: 24, paddingY: 12 },
    link: { color: '#111111' }
  }
};

const RESERVED_VARIABLES = [
  { key: 'FIRST_NAME', desc: "Recipient's first name" },
  { key: 'LAST_NAME', desc: "Recipient's last name" },
  { key: 'EMAIL', desc: "Recipient's email address" },
  { key: 'COMPANY_NAME', desc: 'Your workspace or company' },
  { key: 'CRESCENDO_UNSUBSCRIBE_URL', desc: 'Secure 1-click unsubscribe URL' },
  { key: 'CURRENT_YEAR', desc: 'Current 4-digit calendar year' }
];

const IMAGE_PRESETS = [
  { label: 'Geometric Gradient', url: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80' },
  { label: 'Minimal Dark Texture', url: 'https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800&auto=format&fit=crop&q=80' },
  { label: 'Abstract Wave', url: 'https://images.unsplash.com/photo-1604076913837-52ab5629fba9?w=800&auto=format&fit=crop&q=80' },
  { label: 'Clean Architecture', url: 'https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800&auto=format&fit=crop&q=80' },
  { label: 'Modern Studio', url: 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=800&auto=format&fit=crop&q=80' },
  { label: 'Tech Workspace', url: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&auto=format&fit=crop&q=80' }
];

// ─── Spacing and Typography Helpers ──────────────────────────────────────────

function normalizeLineHeight(val, defaultPercent = 150) {
  if (val === undefined || val === null || val === '') return `${defaultPercent}%`;
  const num = typeof val === 'number' ? val : parseFloat(val);
  if (Number.isNaN(num)) return `${defaultPercent}%`;
  if (num <= 3) return `${Math.round(num * 100)}%`;
  return `${num}%`;
}

function normalizeMargin(margin, defaultBottom = 16) {
  if (!margin) return `0 0 ${defaultBottom}px 0`;
  if (margin.all !== undefined && margin.linked !== false) {
    return `${margin.all}px`;
  }
  const top = margin.top ?? 0;
  const right = margin.right ?? 0;
  const bottom = margin.bottom ?? defaultBottom;
  const left = margin.left ?? 0;
  return `${top}px ${right}px ${bottom}px ${left}px`;
}

// ─── Sleek Custom Controls ───────────────────────────────────────────────────

/**
 * DimensionBox - Monochromatic dimension input with drag-to-scrub support
 */
function DimensionBox({
  value,
  onChange,
  unit = 'px',
  prefix,
  placeholder = '0',
  min = 0,
  max = 9999,
  step = 1
}) {
  const [isDragging, setIsDragging] = useState(false);
  const startXRef = useRef(0);
  const startValRef = useRef(0);

  const numericValue = typeof value === 'number' ? value : (parseInt(value, 10) || 0);

  const handleMouseDown = (e) => {
    setIsDragging(true);
    startXRef.current = e.clientX;
    startValRef.current = numericValue;

    const handleMouseMove = (moveEvent) => {
      const deltaX = moveEvent.clientX - startXRef.current;
      const stepMultiplier = moveEvent.shiftKey ? 10 : 1;
      const nextVal = Math.min(max, Math.max(min, Math.round(startValRef.current + deltaX * step * stepMultiplier)));
      onChange(nextVal);
    };

    const handleMouseUp = () => {
      setIsDragging(false);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
  };

  return (
    <div className={`re-dim-box ${isDragging ? 'scrubbing' : ''}`}>
      {prefix && <span className="re-dim-icon">{prefix}</span>}
      <input
        type="text"
        inputMode="numeric"
        className="re-dim-input"
        value={value !== undefined && value !== null ? value : ''}
        placeholder={placeholder}
        onChange={(e) => {
          const raw = e.target.value.trim();
          if (raw === '' || raw === 'auto') {
            onChange(raw);
          } else {
            const num = parseFloat(raw);
            if (!Number.isNaN(num)) onChange(num);
          }
        }}
      />
      {unit && (
        <span
          className="re-dim-unit"
          title="Drag horizontally to scrub value"
          onMouseDown={handleMouseDown}
        >
          {unit}
        </span>
      )}
    </div>
  );
}

/**
 * FourGridControl - Resend-style 4-way padding/margin/border/radius control
 */
function FourGridControl({ label, values = {}, onChange, isRadius = false, unit = 'px' }) {
  const isLinked = values?.linked !== false;

  const toggleLinked = (linked) => {
    if (linked) {
      const val = isRadius ? (values?.all ?? values?.topLeft ?? 0) : (values?.all ?? values?.top ?? 0);
      if (isRadius) {
        onChange({ linked: true, all: val, topLeft: val, topRight: val, bottomRight: val, bottomLeft: val });
      } else {
        onChange({ linked: true, all: val, top: val, right: val, bottom: val, left: val });
      }
    } else {
      const currentAll = values?.all ?? 0;
      if (isRadius) {
        onChange({
          linked: false,
          topLeft: values?.topLeft ?? currentAll,
          topRight: values?.topRight ?? currentAll,
          bottomRight: values?.bottomRight ?? currentAll,
          bottomLeft: values?.bottomLeft ?? currentAll
        });
      } else {
        onChange({
          linked: false,
          top: values?.top ?? currentAll,
          right: values?.right ?? currentAll,
          bottom: values?.bottom ?? currentAll,
          left: values?.left ?? currentAll
        });
      }
    }
  };

  const updateSingle = (key, val) => {
    if (isLinked) {
      if (isRadius) {
        onChange({ linked: true, all: val, topLeft: val, topRight: val, bottomRight: val, bottomLeft: val });
      } else {
        onChange({ linked: true, all: val, top: val, right: val, bottom: val, left: val });
      }
    } else {
      onChange({ ...values, linked: false, [key]: val });
    }
  };

  return (
    <div className="re-spacing-group">
      <div className="re-prop-row">
        <label className="re-prop-label">{label}</label>
        <div className="re-spacing-control-flex">
          <div className="re-spacing-input-wrap">
            {isLinked ? (
              <DimensionBox
                value={isRadius ? (values?.all ?? values?.topLeft ?? 0) : (values?.all ?? values?.top ?? 0)}
                onChange={(v) => updateSingle('all', v)}
                unit={unit}
              />
            ) : (
              <div className="re-spacing-4grid">
                {isRadius ? (
                  <>
                    <DimensionBox prefix="TL" value={values?.topLeft ?? 0} onChange={(v) => updateSingle('topLeft', v)} unit={unit} />
                    <DimensionBox prefix="TR" value={values?.topRight ?? 0} onChange={(v) => updateSingle('topRight', v)} unit={unit} />
                    <DimensionBox prefix="BL" value={values?.bottomLeft ?? 0} onChange={(v) => updateSingle('bottomLeft', v)} unit={unit} />
                    <DimensionBox prefix="BR" value={values?.bottomRight ?? 0} onChange={(v) => updateSingle('bottomRight', v)} unit={unit} />
                  </>
                ) : (
                  <>
                    <DimensionBox prefix="T" value={values?.top ?? 0} onChange={(v) => updateSingle('top', v)} unit={unit} />
                    <DimensionBox prefix="R" value={values?.right ?? 0} onChange={(v) => updateSingle('right', v)} unit={unit} />
                    <DimensionBox prefix="B" value={values?.bottom ?? 0} onChange={(v) => updateSingle('bottom', v)} unit={unit} />
                    <DimensionBox prefix="L" value={values?.left ?? 0} onChange={(v) => updateSingle('left', v)} unit={unit} />
                  </>
                )}
              </div>
            )}
          </div>
          <div className="re-linked-radiogroup" role="radiogroup">
            <button
              type="button"
              className={`re-linked-radio-btn ${isLinked ? 'active' : ''}`}
              onClick={() => toggleLinked(true)}
              title="Link all sides"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
              </svg>
            </button>
            <button
              type="button"
              className={`re-linked-radio-btn ${!isLinked ? 'active' : ''}`}
              onClick={() => toggleLinked(false)}
              title="Independent sides"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="18" height="18" rx="2" strokeDasharray="3 3" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * ColorPickerRow - Monochromatic color swatch with custom hex text input
 */
function ColorPickerRow({ label, value, onChange, onClear, id }) {
  const inputRef = useRef(null);
  const colorVal = (value || '').toUpperCase();
  const isClear = !value || value === 'transparent';

  return (
    <div className="re-prop-row">
      {label && <label className="re-prop-label" htmlFor={id}>{label}</label>}
      <div className="re-color-picker-wrap">
        <button
          type="button"
          className="re-color-swatch-btn"
          style={{ backgroundColor: isClear ? 'transparent' : colorVal }}
          onClick={() => inputRef.current?.click()}
          title="Pick color"
        >
          {isClear && <div className="re-color-empty-diagonal" />}
        </button>
        <input
          ref={inputRef}
          type="color"
          className="re-hidden-color-input"
          value={isClear ? '#ffffff' : (colorVal.startsWith('#') ? colorVal : '#000000')}
          onChange={(e) => onChange(e.target.value)}
        />
        <input
          id={id}
          type="text"
          className="re-color-hex-input font-mono"
          value={isClear ? '' : colorVal}
          placeholder="None"
          onChange={(e) => onChange(e.target.value)}
        />
        {onClear && !isClear && (
          <button type="button" className="re-color-clear-btn" onClick={onClear} title="Clear color">
            <HiOutlineX />
          </button>
        )}
      </div>
    </div>
  );
}

/**
 * CustomSelect - Sleek dark dropdown
 */
function CustomSelect({ options = [], value, onChange, placeholder = 'Select…' }) {
  const [open, setOpen] = useState(false);
  const selectedOption = options.find((o) => o.value === value) || { label: placeholder, value };

  return (
    <div className="re-custom-select-wrap">
      <button
        type="button"
        className="re-custom-select-btn"
        onClick={() => setOpen(!open)}
      >
        <span className="re-select-label">{selectedOption.label}</span>
        <HiOutlineChevronDown className={`re-select-chevron ${open ? 'open' : ''}`} />
      </button>
      {open && (
        <div className="re-custom-select-menu">
          {options.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={`re-select-option ${opt.value === value ? 'active' : ''}`}
              onClick={() => {
                onChange(opt.value);
                setOpen(false);
              }}
            >
              <span>{opt.label}</span>
              {opt.value === value && <HiOutlineCheck className="re-option-check" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

/**
 * RichTextBlock - Inline rich text editor with solid cursor retention & individual text selection formatting
 */
function RichTextBlock({
  html,
  onCommit,
  className = '',
  style = {},
  tagName = 'div'
}) {
  const ref = useRef(null);
  const isInternalTypingRef = useRef(false);
  const [bubblePos, setBubblePos] = useState(null);
  const [linkInputOpen, setLinkInputOpen] = useState(false);
  const [linkUrl, setLinkUrl] = useState('');
  const [savedRange, setSavedRange] = useState(null);
  const textColorInputRef = useRef(null);
  const highlightColorInputRef = useRef(null);

  // Initialize and sync external changes (e.g. from sidebar textarea, undo/redo) without wiping internal cursor
  useEffect(() => {
    if (ref.current && !isInternalTypingRef.current && ref.current.innerHTML !== (html || '')) {
      ref.current.innerHTML = html || '';
    }
  }, [html]);

  const handleSelection = useCallback(() => {
    const sel = window.getSelection();
    if (!sel || sel.isCollapsed || !ref.current || !ref.current.contains(sel.anchorNode)) {
      if (!linkInputOpen) {
        setBubblePos(null);
        setSavedRange(null);
      }
      return;
    }
    const range = sel.getRangeAt(0);
    setSavedRange(range.cloneRange());
    const rect = range.getBoundingClientRect();
    const containerRect = ref.current.getBoundingClientRect();
    setBubblePos({
      top: rect.top - containerRect.top - 46,
      left: Math.max(90, Math.min(containerRect.width - 90, rect.left - containerRect.left + (rect.width / 2))),
      visible: true
    });
  }, [linkInputOpen]);

  useEffect(() => {
    document.addEventListener('selectionchange', handleSelection);
    return () => document.removeEventListener('selectionchange', handleSelection);
  }, [handleSelection]);

  const execCommand = (cmd, val = null) => {
    if (savedRange) {
      const sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(savedRange);
    }
    document.execCommand(cmd, false, val);
    if (ref.current) {
      onCommit(ref.current.innerHTML);
    }
  };

  const applyLink = () => {
    if (!linkUrl.trim()) return;
    if (savedRange) {
      const sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(savedRange);
    }
    document.execCommand('createLink', false, linkUrl.trim());
    if (ref.current) {
      onCommit(ref.current.innerHTML);
    }
    setLinkInputOpen(false);
    setLinkUrl('');
    setBubblePos(null);
  };

  const insertVariable = (varKey) => {
    if (savedRange) {
      const sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(savedRange);
    }
    document.execCommand('insertHTML', false, `{{${varKey}}}`);
    if (ref.current) {
      onCommit(ref.current.innerHTML);
    }
    setBubblePos(null);
  };

  return (
    <div style={{ position: 'relative', width: '100%', minHeight: '1.2em' }}>
      {bubblePos?.visible && (
        <div
          className="rs-bubble-toolbar"
          style={{
            top: `${bubblePos.top}px`,
            left: `${bubblePos.left}px`,
            transform: 'translateX(-50%)'
          }}
          onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); }}
        >
          {!linkInputOpen ? (
            <>
              {/* Individual Text Formatting actions */}
              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); execCommand('bold'); }} title="Bold selection"><MdFormatBold /></button>
              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); execCommand('italic'); }} title="Italic selection"><MdFormatItalic /></button>
              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); execCommand('underline'); }} title="Underline selection"><MdFormatUnderlined /></button>
              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); execCommand('strikeThrough'); }} title="Strikethrough selection"><MdFormatStrikethrough /></button>
              <div className="rs-bubble-sep" />
              
              {/* Text Color for selected words */}
              <button
                type="button"
                className="rs-bubble-btn"
                onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); textColorInputRef.current?.click(); }}
                title="Color for selected text"
              >
                <MdFormatColorText />
              </button>
              <input
                ref={textColorInputRef}
                type="color"
                style={{ display: 'none' }}
                onChange={(e) => execCommand('foreColor', e.target.value)}
              />

              {/* Highlight / Background Color for selected words */}
              <button
                type="button"
                className="rs-bubble-btn"
                onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); highlightColorInputRef.current?.click(); }}
                title="Highlight selected text"
              >
                <MdFormatColorFill />
              </button>
              <input
                ref={highlightColorInputRef}
                type="color"
                style={{ display: 'none' }}
                onChange={(e) => execCommand('hiliteColor', e.target.value)}
              />

              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); execCommand('removeFormat'); }} title="Clear Formatting on selection"><MdFormatClear /></button>
              <div className="rs-bubble-sep" />
              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); execCommand('insertUnorderedList'); }} title="Bullet List"><MdFormatListBulleted /></button>
              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); execCommand('insertOrderedList'); }} title="Numbered List"><MdFormatListNumbered /></button>
              <div className="rs-bubble-sep" />
              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); setLinkInputOpen(true); }} title="Insert Link to selected text"><MdLink /></button>
              <button type="button" className="rs-bubble-btn" onMouseDown={(e) => { e.preventDefault(); e.stopPropagation(); execCommand('unlink'); }} title="Remove Link"><MdLinkOff /></button>
              <div className="rs-bubble-sep" />
              <select
                className="rs-bubble-select font-mono"
                defaultValue=""
                onMouseDown={(e) => e.stopPropagation()}
                onChange={(e) => {
                  if (e.target.value) insertVariable(e.target.value);
                  e.target.value = '';
                }}
              >
                <option value="" disabled>{'{{Tag}}'}</option>
                {RESERVED_VARIABLES.map((v) => (
                  <option key={v.key} value={v.key}>{v.key}</option>
                ))}
              </select>
            </>
          ) : (
            <div className="rs-bubble-link-box" onMouseDown={(e) => e.stopPropagation()}>
              <input
                type="text"
                className="rs-bubble-link-input font-mono"
                placeholder="https://..."
                value={linkUrl}
                onChange={(e) => setLinkUrl(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && applyLink()}
                autoFocus
              />
              <button type="button" className="rs-btn-primary" onClick={applyLink}>Apply</button>
              <button type="button" className="rs-btn-outline" onClick={() => setLinkInputOpen(false)}>✕</button>
            </div>
          )}
        </div>
      )}

      {React.createElement(tagName, {
        ref,
        contentEditable: true,
        suppressContentEditableWarning: true,
        className: `rs-editable-block ${className}`,
        style: {
          outline: 'none',
          wordBreak: 'break-word',
          minHeight: '1.2em',
          cursor: 'text',
          userSelect: 'text',
          WebkitUserSelect: 'text',
          ...style
        },
        onFocus: () => {
          isInternalTypingRef.current = true;
        },
        onInput: () => {
          if (ref.current) {
            isInternalTypingRef.current = true;
            onCommit(ref.current.innerHTML);
          }
        },
        onBlur: () => {
          isInternalTypingRef.current = false;
          if (ref.current) {
            onCommit(ref.current.innerHTML);
          }
          setTimeout(() => {
            if (!linkInputOpen) setBubblePos(null);
          }, 250);
        }
      })}
    </div>
  );
}

// ─── Email HTML Compiler ──────────────────────────────────────────────────────

function blocksToHtml(blocks = [], theme = THEME_DEFAULTS.minimal, globalCss = '', previewText = '') {
  const containerAlign = theme.containerAlign || 'center';
  const tableAlign = containerAlign === 'left' ? 'left' : containerAlign === 'right' ? 'right' : 'center';

  const containerPadding = theme.containerPadding?.linked !== false
    ? `${theme.containerPadding?.all ?? 32}px`
    : `${theme.containerPadding?.top ?? 32}px ${theme.containerPadding?.right ?? 32}px ${theme.containerPadding?.bottom ?? 32}px ${theme.containerPadding?.left ?? 32}px`;

  const containerRadius = theme.containerRadius?.linked !== false
    ? `${theme.containerRadius?.all ?? 8}px`
    : `${theme.containerRadius?.topLeft ?? 8}px ${theme.containerRadius?.topRight ?? 8}px ${theme.containerRadius?.bottomRight ?? 8}px ${theme.containerRadius?.bottomLeft ?? 8}px`;

  const containerBorder = theme.containerBorder?.linked !== false
    ? `${theme.containerBorder?.all ?? 0}px solid ${theme.containerBorderColor || '#e4e4e7'}`
    : `${theme.containerBorder?.top ?? 0}px solid ${theme.containerBorderColor || '#e4e4e7'}`;

  const renderBlockHtml = (b) => {
    switch (b.type) {
      case 'heading': {
        const level = b.level || 'h1';
        const fontSize = b.fontSize || (level === 'h1' ? 26 : level === 'h2' ? 22 : 18);
        const marginStr = normalizeMargin(b.margin, 16);
        const lh = normalizeLineHeight(b.lineHeight, 130);
        return `<${level} style="margin:${marginStr};font-family:${b.fontFamily || theme.title?.fontFamily || 'sans-serif'};font-size:${fontSize}px;font-weight:${b.fontWeight || '700'};line-height:${lh};color:${b.color || theme.title?.color || '#09090b'};text-align:${b.align || 'left'};text-transform:${b.textTransform || 'none'};letter-spacing:${b.letterSpacing || 0}px;">${b.content || ''}</${level}>`;
      }
      case 'text': {
        const marginStr = normalizeMargin(b.margin, 16);
        const lh = normalizeLineHeight(b.lineHeight, 155);
        return `<p style="margin:${marginStr};font-family:${b.fontFamily || theme.text?.fontFamily || 'sans-serif'};font-size:${b.fontSize || 14}px;font-weight:${b.fontWeight || '400'};line-height:${lh};color:${b.color || theme.text?.color || '#18181b'};text-align:${b.align || 'left'};letter-spacing:${b.letterSpacing || 0}px;">${b.content || ''}</p>`;
      }
      case 'button': {
        const align = b.align || 'left';
        const radius = b.borderRadius || 8;
        const padX = b.paddingX || 20;
        const padY = b.paddingY || 10;
        return `<table border="0" cellpadding="0" cellspacing="0" role="presentation" style="margin:16px 0;width:${b.isFullWidth ? '100%' : 'auto'};text-align:${align};">
          <tr>
            <td align="${align}">
              <a href="${b.url || '#'}" target="_blank" style="display:${b.isFullWidth ? 'block' : 'inline-block'};background-color:${b.bgColor || '#000000'};color:${b.textColor || '#ffffff'};padding:${padY}px ${padX}px;border-radius:${radius}px;text-decoration:none;font-weight:600;font-size:14px;font-family:${theme.text?.fontFamily || 'sans-serif'};text-align:center;">
                ${b.text || 'Button'}
              </a>
            </td>
          </tr>
        </table>`;
      }
      case 'image': {
        const width = b.widthMode === 'custom' ? `${b.customWidth || 600}px` : '100%';
        const imgTag = `<img src="${b.src}" alt="${b.alt || ''}" style="max-width:100%;width:${width};border-radius:${b.borderRadius || 0}px;display:inline-block;" />`;
        return `<div style="text-align:${b.align || 'center'};margin:16px 0;">
          ${b.linkUrl ? `<a href="${b.linkUrl}" target="_blank">${imgTag}</a>` : imgTag}
          ${b.caption ? `<p style="font-size:12px;color:#71717a;margin-top:6px;text-align:${b.align || 'center'};">${b.caption}</p>` : ''}
        </div>`;
      }
      case 'badge': {
        return `<div style="text-align:${b.align || 'left'};margin:8px 0 16px 0;">
          <span style="display:inline-block;background-color:${b.bgColor || '#f4f4f5'};color:${b.textColor || '#000000'};padding:${b.paddingY || 4}px ${b.paddingX || 10}px;border-radius:${b.borderRadius || 999}px;border:${b.borderWidth || 1}px solid ${b.borderColor || '#e4e4e7'};font-size:${b.fontSize || 12}px;font-weight:${b.fontWeight || '600'};letter-spacing:0.04em;text-transform:uppercase;">
            ${b.text || 'BADGE'}
          </span>
        </div>`;
      }
      case 'quote': {
        return `<blockquote style="border-left:${b.borderWidth || 3}px solid ${b.borderColor || '#000000'};background-color:${b.bgColor || '#f8fafc'};padding:12px 18px;margin:16px 0;border-radius:0 6px 6px 0;">
          <p style="font-style:italic;margin:0;color:${b.color || '#374151'};font-size:${b.fontSize || 15}px;">${b.content || ''}</p>
          ${b.citation ? `<small style="display:block;margin-top:4px;font-weight:600;color:#71717a;">— ${b.citation}</small>` : ''}
        </blockquote>`;
      }
      case 'code': {
        return `<pre style="background-color:${b.bgColor || '#09090b'};color:${b.textColor || '#f4f4f5'};padding:${b.padding || 16}px;border-radius:${b.borderRadius || 8}px;margin:16px 0;font-family:'Commit Mono',monospace;font-size:${b.fontSize || 13}px;overflow-x:auto;"><code>${b.code || ''}</code></pre>`;
      }
      case 'columns': {
        const cols = b.columns || [];
        return `<table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;margin:16px 0;">
          <tr>
            ${cols.map((c) => `<td valign="top" style="width:${c.widthRatio || 50}%;padding:0 ${b.gap ? b.gap / 2 : 10}px;">
              ${(c.blocks || []).map(renderBlockHtml).join('')}
            </td>`).join('')}
          </tr>
        </table>`;
      }
      case '3columns': {
        const cols = b.columns || [];
        return `<table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;margin:16px 0;">
          <tr>
            ${cols.map((c) => `<td valign="top" style="width:33.33%;padding:0 ${b.gap ? b.gap / 2 : 8}px;">
              ${(c.blocks || []).map(renderBlockHtml).join('')}
            </td>`).join('')}
          </tr>
        </table>`;
      }
      case '4columns': {
        const cols = b.columns || [];
        return `<table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;margin:16px 0;">
          <tr>
            ${cols.map((c) => `<td valign="top" style="width:25%;padding:0 ${b.gap ? b.gap / 2 : 6}px;">
              ${(c.blocks || []).map(renderBlockHtml).join('')}
            </td>`).join('')}
          </tr>
        </table>`;
      }
      case 'section': {
        return `<div style="background-color:${b.bgColor || '#f9fafb'};border:${b.borderWidth || 1}px solid ${b.borderColor || '#e5e7eb'};border-radius:${b.borderRadius || 8}px;padding:${b.padding?.all || 20}px;margin:16px 0;">
          ${(b.blocks || []).map(renderBlockHtml).join('')}
        </div>`;
      }
      case 'divider': {
        return `<div style="margin:${b.margin?.top || 24}px 0 ${b.margin?.bottom || 24}px 0;"><hr style="border:none;border-top:${b.thickness || 1}px ${b.style || 'solid'} ${b.color || '#e4e4e7'};" /></div>`;
      }
      case 'spacer': {
        return `<div style="height:${b.height || 28}px;line-height:${b.height || 28}px;">&nbsp;</div>`;
      }
      case 'social': {
        const links = b.links || [];
        return `<div style="text-align:${b.align || 'center'};margin:20px 0;">
          ${links.map((l) => `<a href="${l.url || '#'}" target="_blank" style="display:inline-block;margin:0 6px;padding:6px 12px;background-color:${b.bgColor || '#18181b'};color:${b.textColor || '#fafafa'};border-radius:6px;font-size:12px;font-weight:600;text-decoration:none;">${l.platform}</a>`).join('')}
        </div>`;
      }
      case 'unsubscribe': {
        return `<div style="text-align:${b.align || 'center'};margin:32px 0 16px 0;font-size:${b.fontSize || 12}px;color:${b.color || '#71717a'};">
          <p style="margin:0 0 4px 0;">${b.text || 'You are receiving this email because you subscribed to our updates.'}</p>
          <a href="${b.url || '{{CRESCENDO_UNSUBSCRIBE_URL}}'}" style="color:${b.color || '#71717a'};text-decoration:underline;">${b.linkText || 'Unsubscribe'}</a>
        </div>`;
      }
      default:
        return '';
    }
  };

  const bodyContent = blocks.map(renderBlockHtml).join('\n');

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Email</title>
  <style>
    body { margin: 0; padding: 0; background-color: ${theme.bodyBg || '#ffffff'}; -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
    table { border-collapse: collapse; }
    img { border: 0; outline: none; text-decoration: none; }
    ${globalCss}
  </style>
</head>
<body style="background-color:${theme.bodyBg || '#ffffff'};margin:0;padding:0;">
  ${previewText ? `<div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;mso-hide:all;">${previewText}</div>` : ''}
  <table width="100%" border="0" cellpadding="0" cellspacing="0" role="presentation" style="background-color:${theme.bodyBg || '#ffffff'};">
    <tr>
      <td align="${tableAlign}" style="padding:24px 0;">
        <table width="${theme.containerWidth || 600}" border="0" cellpadding="0" cellspacing="0" role="presentation" style="max-width:${theme.containerWidth || 600}px;width:100%;background-color:${theme.containerBg || '#ffffff'};border-radius:${containerRadius};border:${containerBorder};padding:${containerPadding};">
          <tr>
            <td>
              ${bodyContent}
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

// ─── Main TemplateBlockEditor Component ──────────────────────────────────────

export default function TemplateBlockEditor({ template, onClose, onSaved }) {
  const storedDocument = (() => {
    try { return template?.editorDocument ? JSON.parse(template.editorDocument) : null; } catch { return null; }
  })();

  // Navigation & Viewport
  const [editorMode, setEditorMode] = useState('visual'); // 'visual' | 'code' | 'plain'
  const [viewport, setViewport] = useState('desktop'); // 'desktop' | 'mobile'

  // Sidebars & Flyouts
  const [activeDockTool, setActiveDockTool] = useState(null); // 'text' | 'components' | 'image' | 'variables'
  const [activeSidebar, setActiveSidebar] = useState('inspector'); // 'inspector' | 'theme' | 'css' | 'json' | 'variables'
  const [sidebarPinned, setSidebarPinned] = useState(true);

  // Template Metadata
  const [title, setTitle] = useState(storedDocument?.title || template?.name || 'My First Template');
  const [subject, setSubject] = useState(storedDocument?.subject || template?.subject || 'Welcome to Crescendo');
  const [previewText, setPreviewText] = useState(storedDocument?.previewText || template?.previewText || '');
  const [showPreviewRow, setShowPreviewRow] = useState(Boolean(storedDocument?.previewText || template?.previewText));
  const [fromAddress, setFromAddress] = useState(storedDocument?.fromAddress || template?.fromAddress || '');
  const [replyTo, setReplyTo] = useState(storedDocument?.replyTo || template?.replyTo || '');
  const [showReplyToRow, setShowReplyToRow] = useState(Boolean(storedDocument?.replyTo || template?.replyTo));
  const [plainText, setPlainText] = useState(storedDocument?.plainText || template?.textBody || '');
  const [variables, setVariables] = useState(storedDocument?.variables || template?.variables || []);
  const [savedTemplate, setSavedTemplate] = useState(template || null);
  const [htmlOverride, setHtmlOverride] = useState(
    storedDocument?.hasHtmlOverride || storedDocument?.htmlOverride
      ? template?.htmlBody || ''
      : ''
  );

  // Theme & Global Styles
  const [theme, setTheme] = useState(storedDocument?.theme || THEME_DEFAULTS.minimal);
  const [globalCss, setGlobalCss] = useState(storedDocument?.globalCss || `/* Custom Email CSS */\n.node-paragraph {\n  line-height: 1.6;\n}\na:hover {\n  text-decoration: underline;\n}`);

  // Blocks
  const [blocks, setBlocks] = useState(() => {
    if (storedDocument?.blocks && Array.isArray(storedDocument.blocks)) return storedDocument.blocks;
    return [
      { id: 'b1', type: 'badge', text: 'UPDATE', bgColor: '#f4f4f5', textColor: '#000000', fontSize: 12, fontWeight: '600', paddingX: 10, paddingY: 4, borderRadius: 999, align: 'left' },
      { id: 'b2', type: 'heading', level: 'h1', content: 'Welcome to Crescendo Studio', fontSize: 26, fontWeight: '700', color: '#09090b', align: 'left', lineHeight: 130, margin: { top: 0, bottom: 16 } },
      { id: 'b3', type: 'text', content: 'Design emails with professional typography, individual block inspectors, and real-time variable support like {{FIRST_NAME}}.', fontSize: 14, fontWeight: '400', color: '#18181b', align: 'left', lineHeight: 155, margin: { top: 0, bottom: 20 } },
      { id: 'b4', type: 'button', text: 'Get Started', url: 'https://crescendo.app', bgColor: '#000000', textColor: '#ffffff', borderRadius: 8, paddingX: 20, paddingY: 10, align: 'left' },
      { id: 'b5', type: 'divider', thickness: 1, color: '#e4e4e7', style: 'solid', margin: { top: 28, bottom: 24 } },
      { id: 'b6', type: 'unsubscribe', text: 'You are receiving this email because you signed up.', linkText: 'Unsubscribe', url: '{{CRESCENDO_UNSUBSCRIBE_URL}}', color: '#71717a', fontSize: 12, align: 'center' }
    ];
  });

  const [selectedBlockId, setSelectedBlockId] = useState(null);
  const selectedBlock = blocks.find((b) => b.id === selectedBlockId);

  // History for Undo / Redo
  const [history, setHistory] = useState([]);
  const [future, setFuture] = useState([]);

  // Variables Mock
  const [testVariablesMock, setTestVariablesMock] = useState({
    FIRST_NAME: 'Sarah',
    LAST_NAME: 'Connor',
    EMAIL: 'sarah@crescendo.app',
    COMPANY_NAME: 'Crescendo AI',
    CRESCENDO_UNSUBSCRIBE_URL: 'https://crescendo.app/unsubscribe?token=preview_123',
    CURRENT_YEAR: '2026'
  });

  // Modals & Image Upload
  const [showImageModal, setShowImageModal] = useState(false);
  const [imageModalTab, setImageModalTab] = useState('upload'); // 'upload' | 'presets' | 'url'
  const [customImageUrl, setCustomImageUrl] = useState('');
  const [customImageAlt, setCustomImageAlt] = useState('');
  const [isUploadingImage, setIsUploadingImage] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const imageFileInputRef = useRef(null);

  // Test Email Modal
  const [showTestModal, setShowTestModal] = useState(false);
  const [testEmailAddress, setTestEmailAddress] = useState('');
  const [isSendingTest, setIsSendingTest] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveState, setSaveState] = useState('saved'); // saved | saving | error
  const [toastMsg, setToastMsg] = useState(null);
  const lastSavedFingerprintRef = useRef(null);
  const lastAutosaveAttemptRef = useRef(null);
  const creationAttemptedRef = useRef(false);
  const saveSequenceRef = useRef(0);

  const addToast = (msg, type = 'info') => {
    setToastMsg({ msg, type });
    setTimeout(() => setToastMsg(null), 3000);
  };

  const pushHistory = useCallback((newBlocks) => {
    setHistory((prev) => [...prev.slice(-30), blocks]);
    setFuture([]);
    setBlocks(newBlocks);
  }, [blocks]);

  const undo = () => {
    if (history.length === 0) return;
    const previous = history[history.length - 1];
    setFuture((f) => [blocks, ...f]);
    setBlocks(previous);
    setHistory((h) => h.slice(0, -1));
  };

  const redo = () => {
    if (future.length === 0) return;
    const next = future[0];
    setHistory((h) => [...h, blocks]);
    setBlocks(next);
    setFuture((f) => f.slice(1));
  };

  // Keyboard Shortcuts (Cmd+Z / Cmd+Shift+Z / Escape to close)
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && !e.shiftKey && e.key === 'z') {
        e.preventDefault();
        undo();
      } else if ((e.metaKey || e.ctrlKey) && ((e.shiftKey && e.key === 'z') || e.key === 'y')) {
        e.preventDefault();
        redo();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [history, future, blocks]);

  // Image Upload File Handler
  const handleImageFileUpload = (file) => {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      addToast('Please select a valid image file (PNG, JPG, SVG, WEBP)', 'error');
      return;
    }
    setIsUploadingImage(true);
    const reader = new FileReader();
    reader.onload = (e) => {
      const dataUrl = e.target.result;
      setCustomImageUrl(dataUrl);
      if (!customImageAlt) setCustomImageAlt(file.name.replace(/\.[^/.]+$/, ''));
      setIsUploadingImage(false);
      addToast('Image uploaded successfully', 'success');
    };
    reader.onerror = () => {
      setIsUploadingImage(false);
      addToast('Failed to read image file', 'error');
    };
    reader.readAsDataURL(file);
  };

  // Block Operations
  const addBlock = (type, extra = {}) => {
    const newId = `b_${Date.now()}`;
    let newBlock = { id: newId, type, ...extra };

    switch (type) {
      case 'heading':
        newBlock = { id: newId, type: 'heading', level: 'h1', content: 'New Heading', fontSize: 24, fontWeight: '700', color: theme.title?.color || '#09090b', align: 'left', lineHeight: 130, margin: { top: 0, bottom: 16 } };
        break;
      case 'text':
        newBlock = { id: newId, type: 'text', content: 'New paragraph text. You can format this directly on the canvas or change properties in the sidebar.', fontSize: 14, fontWeight: '400', color: theme.text?.color || '#18181b', align: 'left', lineHeight: 155, margin: { top: 0, bottom: 16 } };
        break;
      case 'button':
        newBlock = { id: newId, type: 'button', text: 'Click Here', url: 'https://', bgColor: theme.button?.bg || '#000000', textColor: theme.button?.color || '#ffffff', borderRadius: theme.button?.radius || 8, paddingX: 20, paddingY: 10, align: 'left' };
        break;
      case 'image':
        newBlock = { id: newId, type: 'image', src: extra.src || IMAGE_PRESETS[0].url, alt: extra.alt || 'Banner', widthMode: '100%', align: 'center', borderRadius: 8 };
        break;
      case 'badge':
        newBlock = { id: newId, type: 'badge', text: 'NEW', bgColor: '#f4f4f5', textColor: '#000000', fontSize: 12, fontWeight: '600', paddingX: 10, paddingY: 4, borderRadius: 999, align: 'left' };
        break;
      case 'quote':
        newBlock = { id: newId, type: 'quote', content: 'Simplicity is prerequisite for reliability.', citation: 'Edsger W. Dijkstra', borderColor: '#000000', bgColor: '#f8fafc', color: '#374151', fontSize: 15 };
        break;
      case 'code':
        newBlock = { id: newId, type: 'code', code: 'const crescendo = new Crescendo({\n  apiKey: "cr_live_..."\n});', language: 'javascript', bgColor: '#09090b', textColor: '#f4f4f5', fontSize: 13, borderRadius: 8 };
        break;
      case 'social':
        newBlock = { id: newId, type: 'social', links: [{ platform: '𝕏 Twitter', url: 'https://x.com' }, { platform: 'GitHub', url: 'https://github.com' }, { platform: 'LinkedIn', url: 'https://linkedin.com' }], align: 'center', bgColor: '#18181b', textColor: '#fafafa' };
        break;
      case 'columns':
        newBlock = { id: newId, type: 'columns', gap: 20, columns: [{ id: 'c1', widthRatio: 50, blocks: [{ id: 'c1_b1', type: 'text', content: 'Column 1 content' }] }, { id: 'c2', widthRatio: 50, blocks: [{ id: 'c2_b1', type: 'text', content: 'Column 2 content' }] }] };
        break;
      case '3columns':
        newBlock = { id: newId, type: '3columns', gap: 16, columns: [{ id: 'c1', blocks: [{ id: 'c1_b1', type: 'text', content: 'Col 1' }] }, { id: 'c2', blocks: [{ id: 'c2_b1', type: 'text', content: 'Col 2' }] }, { id: 'c3', blocks: [{ id: 'c3_b1', type: 'text', content: 'Col 3' }] }] };
        break;
      case '4columns':
        newBlock = { id: newId, type: '4columns', gap: 12, columns: [{ id: 'c1', blocks: [{ id: 'c1_b1', type: 'text', content: 'Col 1' }] }, { id: 'c2', blocks: [{ id: 'c2_b1', type: 'text', content: 'Col 2' }] }, { id: 'c3', blocks: [{ id: 'c3_b1', type: 'text', content: 'Col 3' }] }, { id: 'c4', blocks: [{ id: 'c4_b1', type: 'text', content: 'Col 4' }] }] };
        break;
      case 'section':
        newBlock = { id: newId, type: 'section', bgColor: '#f9fafb', borderColor: '#e5e7eb', borderWidth: 1, borderRadius: 8, padding: { all: 20 }, blocks: [{ id: 's_b1', type: 'text', content: 'Inside section container' }] };
        break;
      case 'divider':
        newBlock = { id: newId, type: 'divider', thickness: 1, color: '#e4e4e7', style: 'solid', margin: { top: 24, bottom: 24 } };
        break;
      case 'spacer':
        newBlock = { id: newId, type: 'spacer', height: 28 };
        break;
      case 'unsubscribe':
        newBlock = { id: newId, type: 'unsubscribe', text: 'Unsubscribe from our emails', linkText: 'Unsubscribe', url: '{{CRESCENDO_UNSUBSCRIBE_URL}}', color: '#71717a', fontSize: 12, align: 'center' };
        break;
      default:
        break;
    }

    pushHistory([...blocks, newBlock]);
    setSelectedBlockId(newId);
    setActiveDockTool(null);
    setActiveSidebar('inspector');
  };

  const updateBlock = (id, patch) => {
    setBlocks((prev) => prev.map((b) => (b.id === id ? { ...b, ...patch } : b)));
  };

  const deleteBlock = (id) => {
    pushHistory(blocks.filter((b) => b.id !== id));
    if (selectedBlockId === id) setSelectedBlockId(null);
  };

  const duplicateBlock = (id) => {
    const idx = blocks.findIndex((b) => b.id === id);
    if (idx === -1) return;
    const original = blocks[idx];
    const clone = { ...JSON.parse(JSON.stringify(original)), id: `b_${Date.now()}` };
    const next = [...blocks];
    next.splice(idx + 1, 0, clone);
    pushHistory(next);
    setSelectedBlockId(clone.id);
    setActiveSidebar('inspector');
    setActiveDockTool(null);
  };

  const moveBlock = (id, dir) => {
    const idx = blocks.findIndex((b) => b.id === id);
    if (idx === -1) return;
    const targetIdx = dir === 'up' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= blocks.length) return;
    const next = [...blocks];
    const temp = next[idx];
    next[idx] = next[targetIdx];
    next[targetIdx] = temp;
    pushHistory(next);
  };

  // Dynamic Variable Scanner
  const scanVariables = () => {
    const fullText = JSON.stringify(blocks) + subject + previewText + plainText + htmlOverride;
    const matches = fullText.match(/\{\{([a-zA-Z_][a-zA-Z0-9_]*)\}\}/g) || [];
    return Array.from(new Set(matches.map((m) => m.replace(/[\{\}]/g, ''))));
  };
  const detectedVariables = scanVariables();
  const reservedVariableNames = new Set(RESERVED_VARIABLES.map((variable) => variable.key));
  const undeclaredVariables = detectedVariables.filter(
    (name) => !reservedVariableNames.has(name) && !variables.some((variable) => variable.name === name)
  );

  const addVariable = () => {
    setVariables((current) => [...current, { name: '', type: 'STRING', fallbackValue: '' }]);
  };

  const updateVariable = (index, patch) => {
    setVariables((current) => current.map((variable, currentIndex) => (
      currentIndex === index ? { ...variable, ...patch } : variable
    )));
  };

  const removeVariable = (index) => {
    setVariables((current) => current.filter((_, currentIndex) => currentIndex !== index));
  };

  // Document Compilation & Save
  const doc = {
    blocks,
    theme,
    globalCss,
    title,
    subject,
    previewText,
    fromAddress,
    replyTo,
    plainText,
    variables,
    hasHtmlOverride: Boolean(htmlOverride)
  };
  const compiledHtml = blocksToHtml(blocks, theme, globalCss, previewText);
  const htmlBody = htmlOverride || compiledHtml;
  const draftFingerprint = JSON.stringify({
    title, subject, previewText, fromAddress, replyTo, plainText, variables, blocks, theme, globalCss, htmlOverride
  });

  const handleSave = async (publish = false, { quiet = false } = {}) => {
    const invalidVariable = variables.find((variable) => !/^[A-Z][A-Z0-9_]*$/.test(variable.name || ''));
    if (publish && invalidVariable) {
      addToast('Custom variable names must use uppercase letters, numbers, and underscores.', 'error');
      setActiveSidebar('variables');
      return null;
    }
    if (publish && undeclaredVariables.length > 0) {
      addToast(`Declare ${undeclaredVariables.map((name) => `{{${name}}}`).join(', ')} before publishing.`, 'error');
      setActiveSidebar('variables');
      return null;
    }
    const saveSequence = ++saveSequenceRef.current;
    setIsSaving(true);
    setSaveState('saving');
    try {
      const payload = {
        name: title,
        subject,
        previewText,
        fromAddress,
        replyTo,
        textBody: plainText || htmlBody.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim(),
        variables,
        htmlBody,
        editorDocument: JSON.stringify(doc)
      };

      let saved;
      if (savedTemplate?.id) {
        saved = await templatesApi.update(savedTemplate.id, payload);
      } else {
        saved = await templatesApi.create(payload);
      }

      if (publish) {
        saved = await templatesApi.publish(saved.id);
      }

      lastSavedFingerprintRef.current = draftFingerprint;
      lastAutosaveAttemptRef.current = draftFingerprint;
      setSavedTemplate(saved);
      setSaveState('saved');
      if (!quiet || publish) addToast(publish ? 'Template published successfully' : 'Draft saved', 'success');
      if (onSaved) onSaved(saved);
      return saved;
    } catch (e) {
      setSaveState('error');
      if (!quiet) addToast(e.response?.data?.message || e.message || 'Failed to save template', 'error');
      return null;
    } finally {
      if (saveSequence === saveSequenceRef.current) setIsSaving(false);
    }
  };

  // Immediate UUID persistence for new drafts
  useEffect(() => {
    if (savedTemplate?.id || creationAttemptedRef.current) return;
    creationAttemptedRef.current = true;
    void handleSave(false, { quiet: true });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [savedTemplate?.id]);

  // Debounced auto-save
  useEffect(() => {
    if (!savedTemplate?.id) return undefined;
    if (lastSavedFingerprintRef.current === null) {
      lastSavedFingerprintRef.current = draftFingerprint;
      return undefined;
    }
    if (lastSavedFingerprintRef.current === draftFingerprint
      || lastAutosaveAttemptRef.current === draftFingerprint || isSaving) return undefined;

    const timer = window.setTimeout(() => {
      lastAutosaveAttemptRef.current = draftFingerprint;
      void handleSave(false, { quiet: true });
    }, 700);
    return () => window.clearTimeout(timer);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draftFingerprint, savedTemplate?.id, isSaving]);

  const resetThemeSection = (sec) => {
    const defaults = THEME_DEFAULTS.minimal;
    if (defaults[sec]) {
      setTheme({ ...theme, [sec]: JSON.parse(JSON.stringify(defaults[sec])) });
      addToast(`Reset ${sec} to default`, 'info');
    }
  };

  const autoGeneratePlainText = () => {
    const text = htmlBody
      .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
      .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
      .replace(/<br\s*[\/]?>/gi, '\n')
      .replace(/<\/p>/gi, '\n\n')
      .replace(/<\/h[1-6]>/gi, '\n\n')
      .replace(/<a[^>]*href="([^"]*)"[^>]*>([\s\S]*?)<\/a>/gi, '$2 ($1)')
      .replace(/<[^>]+>/g, '')
      .replace(/&nbsp;/g, ' ')
      .replace(/\n\s+\n/g, '\n\n')
      .trim();
    setPlainText(text);
    addToast('Plain text generated from HTML', 'success');
  };

  return createPortal(
    <div className="resend-studio-root">
      {/* Toast Notification */}
      {toastMsg && (
        <div style={{ position: 'fixed', bottom: 20, right: 20, zIndex: 99999, backgroundColor: toastMsg.type === 'error' ? '#ef4444' : '#18181b', color: '#ffffff', border: '1px solid #27272a', padding: '8px 16px', borderRadius: 8, fontSize: 13, boxShadow: '0 8px 24px rgba(0,0,0,0.5)' }}>
          {toastMsg.msg}
        </div>
      )}

      {/* ─── Top Bar Navigation (Resend Header) ─────────────────────────────── */}
      <header className="rs-topbar">
        <div className="rs-topbar-left">
          <button type="button" className="rs-icon-square-btn" onClick={onClose} title="Back to Templates">
            <HiArrowLeft />
          </button>
          <div className="rs-breadcrumbs">
            <span className="rs-breadcrumb-item">Templates</span>
            <span className="rs-breadcrumb-sep">/</span>
            <input
              type="text"
              className="rs-title-input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Template name"
            />
            <span className={`rs-status-pill ${savedTemplate?.status === 'PUBLISHED' ? 'published' : ''}`}>
              {savedTemplate?.status || 'Draft'}
            </span>
          </div>
        </div>

        <div className="rs-topbar-center">
          {/* Editor Mode Switcher [Editor | Code | Plain text] */}
          <div className="rs-seg-group" role="tablist">
            <button
              type="button"
              role="tab"
              aria-selected={editorMode === 'visual'}
              className={`rs-seg-btn ${editorMode === 'visual' ? 'active' : ''}`}
              onClick={() => {
                setEditorMode('visual');
                setActiveSidebar('inspector');
                setActiveDockTool(null);
              }}
            >
              <HiOutlineDocumentText />
              <span>Editor</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={editorMode === 'code'}
              className={`rs-seg-btn ${editorMode === 'code' ? 'active' : ''}`}
              onClick={() => {
                setEditorMode('code');
                setActiveSidebar('inspector');
                setActiveDockTool(null);
              }}
            >
              <HiOutlineCode />
              <span>Code</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={editorMode === 'plain'}
              className={`rs-seg-btn ${editorMode === 'plain' ? 'active' : ''}`}
              onClick={() => {
                setEditorMode('plain');
                setActiveSidebar('inspector');
                setActiveDockTool(null);
              }}
            >
              <HiOutlineDocumentText />
              <span>Plain text</span>
            </button>
          </div>

          {/* Viewport Switcher [Desktop | Mobile] */}
          {editorMode === 'visual' && (
            <div className="rs-seg-group" role="radiogroup">
              <button
                type="button"
                className={`rs-seg-btn ${viewport === 'desktop' ? 'active' : ''}`}
                onClick={() => setViewport('desktop')}
                title="Desktop View (600px)"
              >
                Desktop
              </button>
              <button
                type="button"
                className={`rs-seg-btn ${viewport === 'mobile' ? 'active' : ''}`}
                onClick={() => setViewport('mobile')}
                title="Mobile View (375px)"
              >
                Mobile
              </button>
            </div>
          )}
        </div>

        <div className="rs-topbar-right">
          {/* Undo / Redo */}
          <button type="button" className="rs-icon-square-btn" onClick={undo} disabled={history.length === 0} title="Undo (Ctrl+Z)">
            <MdUndo />
          </button>
          <button type="button" className="rs-icon-square-btn" onClick={redo} disabled={future.length === 0} title="Redo (Ctrl+Y)">
            <MdRedo />
          </button>

          {/* Drawers: Variables */}
          <button
            type="button"
            className={`rs-icon-btn ${activeSidebar === 'variables' ? 'active' : ''}`}
            onClick={() => {
              setActiveSidebar(activeSidebar === 'variables' ? 'inspector' : 'variables');
              setActiveDockTool(null);
            }}
            title="Manage dynamic variables"
          >
            <HiOutlineVariable />
            <span>Variables ({detectedVariables.length})</span>
          </button>

          <span className={`rs-save-state ${saveState}`} aria-live="polite">
            {saveState === 'saving' ? 'Saving…' : saveState === 'error' ? 'Changes not saved' : 'All changes saved'}
          </span>

          {/* Send Test Email */}
          <button
            type="button"
            className="rs-icon-btn"
            onClick={() => setShowTestModal(true)}
          >
            <span>Send test</span>
          </button>

          {saveState === 'error' && (
            <button type="button" className="rs-save-draft-btn" disabled={isSaving} onClick={() => handleSave(false)}>
              Retry save
            </button>
          )}
          <button
            type="button"
            className="rs-publish-btn"
            disabled={isSaving}
            onClick={() => handleSave(true)}
          >
            <span className="rs-publish-dot" />
            <span>{isSaving ? 'Saving…' : 'Publish'}</span>
          </button>
        </div>
      </header>

      {/* ─── Studio Stage Layout ─────────────────────────────────────────────── */}
      <div className="rs-studio-stage">
        {/* Floating Left Tool Palette */}
        {editorMode === 'visual' && (
          <aside className="rs-floating-tool-palette">
            <ul className="rs-palette-list">
              {/* Text / Typography */}
              <li className="rs-palette-item">
                <button
                  type="button"
                  className={`rs-dock-btn ${activeDockTool === 'text' ? 'active' : ''}`}
                  onClick={() => setActiveDockTool(activeDockTool === 'text' ? null : 'text')}
                  title="Text & Typography"
                >
                  T
                </button>
                {activeDockTool === 'text' && (
                  <div className="rs-flyout-menu">
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('heading', { level: 'h1' })}>
                      <span className="rs-flyout-icon font-mono font-bold">H1</span>
                      <span>Heading 1</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('heading', { level: 'h2' })}>
                      <span className="rs-flyout-icon font-mono font-bold">H2</span>
                      <span>Heading 2</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('heading', { level: 'h3' })}>
                      <span className="rs-flyout-icon font-mono font-bold">H3</span>
                      <span>Heading 3</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('text')}>
                      <span className="rs-flyout-icon">¶</span>
                      <span>Paragraph</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('quote')}>
                      <span className="rs-flyout-icon">❝</span>
                      <span>Blockquote</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('badge')}>
                      <span className="rs-flyout-icon">#</span>
                      <span>Badge / Tag</span>
                    </button>
                  </div>
                )}
              </li>

              {/* Image */}
              <li className="rs-palette-item">
                <button
                  type="button"
                  className={`rs-dock-btn ${activeDockTool === 'image' ? 'active' : ''}`}
                  onClick={() => setActiveDockTool(activeDockTool === 'image' ? null : 'image')}
                  title="Image & Media"
                >
                  <HiOutlinePhotograph />
                </button>
                {activeDockTool === 'image' && (
                  <div className="rs-flyout-menu">
                    <button
                      type="button"
                      className="rs-flyout-item"
                      onClick={() => {
                        setImageModalTab('upload');
                        setShowImageModal(true);
                        setActiveDockTool(null);
                      }}
                    >
                      <span className="rs-flyout-icon"><HiOutlineUpload /></span>
                      <span>Upload from computer…</span>
                    </button>
                    <button
                      type="button"
                      className="rs-flyout-item"
                      onClick={() => {
                        setImageModalTab('presets');
                        setShowImageModal(true);
                        setActiveDockTool(null);
                      }}
                    >
                      <span className="rs-flyout-icon"><HiOutlinePhotograph /></span>
                      <span>Unsplash Presets…</span>
                    </button>
                    <button
                      type="button"
                      className="rs-flyout-item"
                      onClick={() => {
                        setImageModalTab('url');
                        setShowImageModal(true);
                        setActiveDockTool(null);
                      }}
                    >
                      <span className="rs-flyout-icon"><HiOutlineLink /></span>
                      <span>Insert from URL…</span>
                    </button>
                  </div>
                )}
              </li>

              {/* Components */}
              <li className="rs-palette-item">
                <button
                  type="button"
                  className={`rs-dock-btn ${activeDockTool === 'components' ? 'active' : ''}`}
                  onClick={() => setActiveDockTool(activeDockTool === 'components' ? null : 'components')}
                  title="Modular Layout Components"
                >
                  <HiOutlineViewGrid />
                </button>
                {activeDockTool === 'components' && (
                  <div className="rs-flyout-menu">
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('button')}>
                      <span className="rs-flyout-icon">▶</span>
                      <span>Button</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('columns')}>
                      <span className="rs-flyout-icon">⊞</span>
                      <span>2 Columns</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('3columns')}>
                      <span className="rs-flyout-icon">☰</span>
                      <span>3 Columns</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('4columns')}>
                      <span className="rs-flyout-icon">☷</span>
                      <span>4 Columns</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('section')}>
                      <span className="rs-flyout-icon">▢</span>
                      <span>Section Card</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('social')}>
                      <span className="rs-flyout-icon"><HiOutlineLink /></span>
                      <span>Social Links</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('code')}>
                      <span className="rs-flyout-icon">&lt;&gt;</span>
                      <span>Code Block</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('divider')}>
                      <span className="rs-flyout-icon">—</span>
                      <span>Divider</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('spacer')}>
                      <span className="rs-flyout-icon">↕</span>
                      <span>Spacer</span>
                    </button>
                    <button type="button" className="rs-flyout-item" onClick={() => addBlock('unsubscribe')}>
                      <span className="rs-flyout-icon">↗</span>
                      <span>Unsubscribe</span>
                    </button>
                  </div>
                )}
              </li>

              {/* Variables Tool (No text overflowing) */}
              <li className="rs-palette-item">
                <button
                  type="button"
                  className={`rs-dock-btn ${activeDockTool === 'variables' ? 'active' : ''}`}
                  onClick={() => setActiveDockTool(activeDockTool === 'variables' ? null : 'variables')}
                  title="Insert Variable Tag"
                >
                  <span className="font-mono text-xs font-bold">{'{x}'}</span>
                </button>
                {activeDockTool === 'variables' && (
                  <div className="rs-flyout-menu rs-variables-flyout">
                    <div className="rs-flyout-header-tag">
                      <span>Click to copy & insert</span>
                    </div>
                    {RESERVED_VARIABLES.map((v) => (
                      <button
                        key={v.key}
                        type="button"
                        className="rs-flyout-item rs-var-flyout-item"
                        onClick={() => {
                          navigator.clipboard.writeText(`{{${v.key}}}`);
                          addToast(`Copied {{${v.key}}} to clipboard`, 'success');
                          setActiveDockTool(null);
                        }}
                      >
                        <span className="rs-flyout-icon font-mono font-bold text-xs">{`{}`}</span>
                        <div className="rs-var-flyout-content">
                          <span className="rs-var-flyout-name font-mono">{`{{${v.key}}}`}</span>
                          <small className="rs-var-flyout-desc">{v.desc}</small>
                        </div>
                      </button>
                    ))}
                    {variables.map((cv, ci) => (
                      <button
                        key={ci}
                        type="button"
                        className="rs-flyout-item rs-var-flyout-item"
                        onClick={() => {
                          navigator.clipboard.writeText(`{{${cv.name}}}`);
                          addToast(`Copied {{${cv.name}}} to clipboard`, 'success');
                          setActiveDockTool(null);
                        }}
                      >
                        <span className="rs-flyout-icon font-mono font-bold text-xs">{`{}`}</span>
                        <div className="rs-var-flyout-content">
                          <span className="rs-var-flyout-name font-mono">{`{{${cv.name}}}`}</span>
                          <small className="rs-var-flyout-desc">Custom {cv.type?.toLowerCase() || 'string'} variable</small>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </li>
            </ul>
          </aside>
        )}

        {/* ─── Canvas Workspace (Clicking closes open drawers & deselects block) ─ */}
        <main
          className="rs-canvas-workspace"
          onClick={() => {
            setSelectedBlockId(null);
            setActiveSidebar('inspector');
            setActiveDockTool(null);
          }}
        >
          {editorMode === 'visual' ? (
            <div
              className={`rs-canvas-container ${viewport === 'mobile' ? 'mobile-view' : ''}`}
              style={{
                backgroundColor: theme.containerBg || '#ffffff',
                textAlign: theme.containerAlign || 'center'
              }}
              onClick={(e) => {
                e.stopPropagation();
                setActiveDockTool(null);
              }}
            >
              {/* Subject Line & Preview Header (Resend compact meta bar) */}
              <div className="rs-canvas-subject-bar">
                <div className="rs-subject-row">
                  <span className="rs-subject-label">From</span>
                  <input
                    type="text"
                    className="rs-subject-input"
                    value={fromAddress}
                    onChange={(e) => setFromAddress(e.target.value)}
                    placeholder="Crescendo <hello@yourdomain.com>"
                  />
                  <button
                    type="button"
                    className={`rs-meta-toggle-btn ${showReplyToRow ? 'active' : ''}`}
                    onClick={() => setShowReplyToRow(!showReplyToRow)}
                    title="Toggle Reply-To field"
                  >
                    Reply-to
                  </button>
                </div>

                {showReplyToRow && (
                  <div className="rs-subject-row rs-meta-expanded-row">
                    <span className="rs-subject-label">Reply-to</span>
                    <input
                      type="email"
                      className="rs-subject-input"
                      value={replyTo}
                      onChange={(e) => setReplyTo(e.target.value)}
                      placeholder="Optional reply-to address"
                    />
                  </div>
                )}

                <div className="rs-subject-row">
                  <span className="rs-subject-label">Subject</span>
                  <input
                    type="text"
                    className="rs-subject-input"
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    placeholder="Subject line…"
                  />
                  <button
                    type="button"
                    className={`rs-meta-toggle-btn ${showPreviewRow ? 'active' : ''}`}
                    onClick={() => setShowPreviewRow(!showPreviewRow)}
                    title="Toggle Preview text in inbox"
                  >
                    Preview
                  </button>
                </div>

                {showPreviewRow && (
                  <div className="rs-subject-row rs-meta-expanded-row">
                    <span className="rs-subject-label">Preview</span>
                    <input
                      type="text"
                      className="rs-subject-input"
                      value={previewText}
                      onChange={(e) => setPreviewText(e.target.value)}
                      placeholder="Preview text in inbox…"
                    />
                  </div>
                )}
              </div>

              {/* Rendered Block List with zero overlapping and solid inline editing */}
              <div
                className="rs-canvas-blocks-area"
                style={{
                  padding: theme.containerPadding?.linked !== false
                    ? `${theme.containerPadding?.all ?? 32}px`
                    : `${theme.containerPadding?.top ?? 32}px ${theme.containerPadding?.right ?? 32}px ${theme.containerPadding?.bottom ?? 32}px ${theme.containerPadding?.left ?? 32}px`
                }}
              >
                {htmlOverride ? (
                  <div className="rs-html-override-preview">
                    <div className="rs-html-override-notice">
                      <span>This template is using custom HTML. Return to the visual document to edit blocks again.</span>
                      <button type="button" onClick={() => setHtmlOverride('')}>Use visual document</button>
                    </div>
                    <iframe title="Custom HTML email preview" sandbox="" srcDoc={htmlOverride} />
                  </div>
                ) : blocks.map((block) => (
                  <div
                    key={block.id}
                    className={`rs-canvas-block-wrap ${selectedBlockId === block.id ? 'selected' : ''}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedBlockId(block.id);
                      setActiveSidebar('inspector');
                      setActiveDockTool(null);
                    }}
                  >
                    {/* Quick block hover toolbar */}
                    <div className="rs-block-hover-bar" onClick={(e) => e.stopPropagation()}>
                      <button type="button" onClick={() => moveBlock(block.id, 'up')} title="Move Up"><HiOutlineArrowUp /></button>
                      <button type="button" onClick={() => moveBlock(block.id, 'down')} title="Move Down"><HiOutlineArrowDown /></button>
                      <button type="button" onClick={() => duplicateBlock(block.id)} title="Duplicate"><HiOutlineDuplicate /></button>
                      <button type="button" onClick={() => deleteBlock(block.id)} title="Delete"><HiOutlineTrash /></button>
                    </div>

                    {/* Heading Block */}
                    {block.type === 'heading' && (
                      <RichTextBlock
                        html={block.content}
                        onCommit={(val) => updateBlock(block.id, { content: val })}
                        tagName={block.level || 'h1'}
                        style={{
                          margin: normalizeMargin(block.margin, 16),
                          fontFamily: block.fontFamily || theme.title?.fontFamily || 'sans-serif',
                          fontSize: `${block.fontSize || 24}px`,
                          fontWeight: block.fontWeight || '700',
                          lineHeight: normalizeLineHeight(block.lineHeight, 130),
                          color: block.color || theme.title?.color || '#09090b',
                          textAlign: block.align || 'left',
                          letterSpacing: `${block.letterSpacing || 0}px`,
                          textTransform: block.textTransform || 'none'
                        }}
                      />
                    )}

                    {/* Paragraph / Text Block */}
                    {block.type === 'text' && (
                      <RichTextBlock
                        html={block.content}
                        onCommit={(val) => updateBlock(block.id, { content: val })}
                        style={{
                          margin: normalizeMargin(block.margin, 16),
                          fontFamily: block.fontFamily || theme.text?.fontFamily || 'sans-serif',
                          fontSize: `${block.fontSize || 14}px`,
                          fontWeight: block.fontWeight || '400',
                          lineHeight: normalizeLineHeight(block.lineHeight, 155),
                          color: block.color || theme.text?.color || '#18181b',
                          textAlign: block.align || 'left',
                          letterSpacing: `${block.letterSpacing || 0}px`
                        }}
                      />
                    )}

                    {/* Button Block */}
                    {block.type === 'button' && (
                      <div style={{ textAlign: block.align || 'left', margin: '16px 0' }}>
                        <a
                          href={block.url || '#'}
                          onClick={(e) => e.preventDefault()}
                          style={{
                            display: block.isFullWidth ? 'block' : 'inline-block',
                            backgroundColor: block.bgColor || theme.button?.bg || '#000000',
                            color: block.textColor || theme.button?.color || '#ffffff',
                            padding: `${block.paddingY || 10}px ${block.paddingX || 20}px`,
                            borderRadius: `${block.borderRadius || 8}px`,
                            textDecoration: 'none',
                            fontWeight: '600',
                            fontSize: '14px',
                            textAlign: 'center'
                          }}
                        >
                          {block.text || 'Button'}
                        </a>
                      </div>
                    )}

                    {/* Badge Block */}
                    {block.type === 'badge' && (
                      <div style={{ textAlign: block.align || 'left', margin: '8px 0 16px 0' }}>
                        <span
                          className="rs-canvas-badge"
                          style={{
                            backgroundColor: block.bgColor || '#f4f4f5',
                            color: block.textColor || '#000000',
                            padding: `${block.paddingY || 4}px ${block.paddingX || 10}px`,
                            borderRadius: `${block.borderRadius || 999}px`,
                            border: `${block.borderWidth || 1}px solid ${block.borderColor || '#e4e4e7'}`,
                            fontSize: `${block.fontSize || 12}px`,
                            fontWeight: block.fontWeight || '600'
                          }}
                        >
                          {block.text || 'BADGE'}
                        </span>
                      </div>
                    )}

                    {/* Image Block */}
                    {block.type === 'image' && (
                      <div style={{ textAlign: block.align || 'center', margin: '16px 0' }}>
                        <img
                          src={block.src}
                          alt={block.alt || ''}
                          style={{
                            width: block.widthMode === 'custom' ? `${block.customWidth || 600}px` : '100%',
                            maxWidth: '100%',
                            borderRadius: `${block.borderRadius || 0}px`,
                            objectFit: block.objectFit || 'cover'
                          }}
                        />
                        {block.caption && (
                          <p style={{ fontSize: 12, color: '#71717a', marginTop: 6, textAlign: block.align || 'center' }}>
                            {block.caption}
                          </p>
                        )}
                      </div>
                    )}

                    {/* Blockquote */}
                    {block.type === 'quote' && (
                      <blockquote
                        style={{
                          borderLeft: `${block.borderWidth || 3}px solid ${block.borderColor || '#000000'}`,
                          backgroundColor: block.bgColor || '#f8fafc',
                          padding: '12px 18px',
                          margin: '16px 0',
                          borderRadius: '0 6px 6px 0'
                        }}
                      >
                        <p style={{ fontStyle: 'italic', margin: 0, color: block.color || '#374151', fontSize: `${block.fontSize || 15}px` }}>{block.content}</p>
                        {block.citation && <small style={{ display: 'block', marginTop: 4, fontWeight: 600, color: '#71717a' }}>— {block.citation}</small>}
                      </blockquote>
                    )}

                    {/* Code Block */}
                    {block.type === 'code' && (
                      <pre
                        style={{
                          backgroundColor: block.bgColor || '#09090b',
                          color: block.textColor || '#f4f4f5',
                          padding: `${block.padding || 16}px`,
                          borderRadius: `${block.borderRadius || 8}px`,
                          margin: '16px 0',
                          fontFamily: 'Commit Mono, monospace',
                          fontSize: `${block.fontSize || 13}px`
                        }}
                      >
                        <code>{block.code}</code>
                      </pre>
                    )}

                    {/* 2 Columns */}
                    {block.type === 'columns' && (
                      <div
                        style={{
                          display: 'flex',
                          gap: `${block.gap || 20}px`,
                          margin: '16px 0'
                        }}
                      >
                        {(block.columns || []).map((col) => (
                          <div key={col.id} style={{ flex: col.widthRatio ? `${col.widthRatio}%` : '1' }}>
                            {(col.blocks || []).map((cb) => (
                              <div key={cb.id} style={{ marginBottom: 8 }}>
                                <p style={{ fontWeight: cb.fontWeight || '400', fontSize: `${cb.fontSize || 14}px`, margin: 0 }}>{cb.content || cb.text}</p>
                              </div>
                            ))}
                          </div>
                        ))}
                      </div>
                    )}

                    {/* 3 Columns */}
                    {block.type === '3columns' && (
                      <div
                        style={{
                          display: 'flex',
                          gap: `${block.gap || 16}px`,
                          margin: '16px 0'
                        }}
                      >
                        {(block.columns || []).map((col) => (
                          <div key={col.id} style={{ flex: '1' }}>
                            {(col.blocks || []).map((cb) => (
                              <div key={cb.id}>
                                <p style={{ fontWeight: cb.fontWeight || '600', fontSize: `${cb.fontSize || 14}px`, margin: 0 }}>{cb.content || cb.text}</p>
                              </div>
                            ))}
                          </div>
                        ))}
                      </div>
                    )}

                    {/* 4 Columns */}
                    {block.type === '4columns' && (
                      <div
                        style={{
                          display: 'flex',
                          gap: `${block.gap || 12}px`,
                          margin: '16px 0'
                        }}
                      >
                        {(block.columns || []).map((col) => (
                          <div key={col.id} style={{ flex: '1' }}>
                            {(col.blocks || []).map((cb) => (
                              <div key={cb.id}>
                                <p style={{ fontWeight: cb.fontWeight || '600', fontSize: `${cb.fontSize || 13}px`, margin: 0 }}>{cb.content || cb.text}</p>
                              </div>
                            ))}
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Section Container */}
                    {block.type === 'section' && (
                      <div
                        style={{
                          backgroundColor: block.bgColor || '#f9fafb',
                          border: `${block.borderWidth || 1}px solid ${block.borderColor || '#e5e7eb'}`,
                          borderRadius: `${block.borderRadius || 8}px`,
                          padding: `${block.padding?.all || 20}px`,
                          margin: '16px 0'
                        }}
                      >
                        {(block.blocks || []).map((sb) => (
                          <div key={sb.id} style={{ marginBottom: 8 }}>
                            <p style={{ margin: 0, fontWeight: sb.fontWeight || '400' }}>{sb.content || sb.text}</p>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Social Links */}
                    {block.type === 'social' && (
                      <div style={{ textAlign: block.align || 'center', margin: '20px 0' }}>
                        {(block.links || []).map((l, i) => (
                          <span
                            key={i}
                            style={{
                              display: 'inline-block',
                              margin: '0 6px',
                              padding: '6px 12px',
                              backgroundColor: block.bgColor || '#18181b',
                              color: block.textColor || '#fafafa',
                              borderRadius: 6,
                              fontSize: 12,
                              fontWeight: 600
                            }}
                          >
                            {l.platform}
                          </span>
                        ))}
                      </div>
                    )}

                    {/* Divider */}
                    {block.type === 'divider' && (
                      <div style={{ margin: `${block.margin?.top || 24}px 0 ${block.margin?.bottom || 24}px 0` }}>
                        <hr style={{ border: 'none', borderTop: `${block.thickness || 1}px ${block.style || 'solid'} ${block.color || '#e4e4e7'}` }} />
                      </div>
                    )}

                    {/* Spacer */}
                    {block.type === 'spacer' && (
                      <div style={{ height: `${block.height || 28}px` }} />
                    )}

                    {/* Unsubscribe */}
                    {block.type === 'unsubscribe' && (
                      <div style={{ textAlign: block.align || 'center', margin: '32px 0 16px 0', color: block.color || '#71717a', fontSize: `${block.fontSize || 12}px` }}>
                        <p style={{ margin: '0 0 4px 0' }}>{block.text}</p>
                        <a href={block.url || '#'} onClick={(e) => e.preventDefault()} style={{ color: block.color || '#71717a', textDecoration: 'underline' }}>{block.linkText}</a>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ) : editorMode === 'plain' ? (
            <div className="rs-code-editor-wrapper rs-plain-text-editor">
              <div className="rs-code-editor-heading">
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <strong>Plain-text fallback</strong>
                  <span>Shown by email clients that cannot display HTML.</span>
                </div>
                <button type="button" className="rs-btn-outline" onClick={autoGeneratePlainText}>
                  Auto-generate from HTML
                </button>
              </div>
              <textarea
                className="rs-code-textarea font-mono"
                value={plainText}
                onChange={(event) => setPlainText(event.target.value)}
                placeholder="Write a clear plain-text version of this email…"
                spellCheck
              />
            </div>
          ) : (
            <div className="rs-code-editor-wrapper">
              <div className="rs-code-editor-heading">
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <strong>HTML source</strong>
                  <span>Changes are sanitized before saving and shown in an isolated preview.</span>
                </div>
                <button type="button" className="rs-btn-outline" onClick={() => setHtmlOverride('')}>
                  Reset from visual document
                </button>
              </div>
              <textarea
                className="rs-code-textarea font-mono"
                value={htmlOverride || compiledHtml}
                onChange={(event) => setHtmlOverride(event.target.value)}
                spellCheck={false}
                aria-label="Email HTML source"
              />
            </div>
          )}
        </main>

        {/* ─── Right Menus & Sidebars Suite (Resend Inspector Architecture) ─── */}
        <div
          className="rs-inspector-container"
          style={{ width: sidebarPinned ? '320px' : '60px' }}
        >
          {/* 1. INSPECTOR-SIDEBAR */}
          {activeSidebar === 'inspector' && (
            <div className="rs-sidebar-panel">
              <div className="rs-sidebar-header">
                {selectedBlock ? (
                  <button
                    type="button"
                    className="rs-back-to-page-style"
                    onClick={() => setSelectedBlockId(null)}
                  >
                    <HiArrowLeft />
                    <span>Page style</span>
                  </button>
                ) : (
                  <div className="rs-sidebar-title flex items-center gap-2">
                    <span>Page style</span>
                  </div>
                )}
                <div className="rs-sidebar-header-actions">
                  {selectedBlock && (
                    <>
                      <button type="button" onClick={() => moveBlock(selectedBlock.id, 'up')} title="Move block up">↑</button>
                      <button type="button" onClick={() => moveBlock(selectedBlock.id, 'down')} title="Move block down">↓</button>
                      <button type="button" onClick={() => duplicateBlock(selectedBlock.id)} title="Duplicate block"><HiOutlineDuplicate /></button>
                      <button type="button" onClick={() => deleteBlock(selectedBlock.id)} title="Delete block"><HiOutlineTrash /></button>
                    </>
                  )}
                  <button
                    type="button"
                    className="rs-pin-btn"
                    onClick={() => setSidebarPinned(!sidebarPinned)}
                    title={sidebarPinned ? 'Collapse sidebar' : 'Pin sidebar'}
                  >
                    <svg fill="none" height="16" viewBox="0 0 24 24" width="16" xmlns="http://www.w3.org/2000/svg">
                      <path fill="currentColor" d="M17 3a1 1 0 0 1 0 2 1 1 0 0 0-1 1v6.172a1 1 0 0 0 .293.707l1.414 1.414c.63.63.184 1.707-.707 1.707h-3a1 1 0 0 0-1 1v4a1 1 0 0 1-2 0v-4a1 1 0 0 0-1-1H7c-.89 0-1.337-1.077-.707-1.707l1.414-1.414A1 1 0 0 0 8 12.172V6a1 1 0 0 0-1-1 1 1 0 0 1 0-2zm-7 10.293a.3.3 0 0 1-.086.207.293.293 0 0 0 .207.5h3.758a.293.293 0 0 0 .207-.5.3.3 0 0 1-.086-.207V6a1 1 0 0 0-1-1h-2a1 1 0 0 0-1 1z" />
                    </svg>
                  </button>
                </div>
              </div>

              <div className="rs-sidebar-content">
                {!selectedBlock ? (
                  /* ─── Page Style Inspector (from docs_archive/resendrightmenu.txt) ─── */
                  <>
                    <div className="rs-section-block">
                      <ColorPickerRow
                        label="Background"
                        value={theme.bodyBg}
                        onChange={(v) => setTheme({ ...theme, bodyBg: v })}
                        onClear={() => setTheme({ ...theme, bodyBg: '#ffffff' })}
                      />
                      <FourGridControl
                        label="Padding"
                        values={theme.bodyPadding}
                        onChange={(v) => setTheme({ ...theme, bodyPadding: v })}
                      />
                    </div>

                    <div className="rs-section-block">
                      <div className="rs-section-header-title">Body Container</div>

                      {/* Alignment */}
                      <div className="rs-seg-group full-width" role="radiogroup">
                        <button
                          type="button"
                          className={`rs-seg-btn flex-1 justify-center ${theme.containerAlign === 'left' ? 'active' : ''}`}
                          onClick={() => setTheme({ ...theme, containerAlign: 'left' })}
                          title="Align Left"
                        >
                          <MdFormatAlignLeft />
                        </button>
                        <button
                          type="button"
                          className={`rs-seg-btn flex-1 justify-center ${theme.containerAlign === 'center' ? 'active' : ''}`}
                          onClick={() => setTheme({ ...theme, containerAlign: 'center' })}
                          title="Align Center"
                        >
                          <MdFormatAlignCenter />
                        </button>
                        <button
                          type="button"
                          className={`rs-seg-btn flex-1 justify-center ${theme.containerAlign === 'right' ? 'active' : ''}`}
                          onClick={() => setTheme({ ...theme, containerAlign: 'right' })}
                          title="Align Right"
                        >
                          <MdFormatAlignRight />
                        </button>
                      </div>

                      <ColorPickerRow
                        label="Text Color"
                        value={theme.text?.color}
                        onChange={(v) => setTheme({ ...theme, text: { ...theme.text, color: v } })}
                        onClear={() => setTheme({ ...theme, text: { ...theme.text, color: '#000000' } })}
                      />
                      <ColorPickerRow
                        label="Container Bg"
                        value={theme.containerBg}
                        onChange={(v) => setTheme({ ...theme, containerBg: v })}
                        onClear={() => setTheme({ ...theme, containerBg: '#ffffff' })}
                      />

                      {/* Width */}
                      <div className="re-prop-row">
                        <label className="re-prop-label">Width</label>
                        <DimensionBox
                          value={theme.containerWidth}
                          onChange={(v) => setTheme({ ...theme, containerWidth: v })}
                          placeholder="600"
                          unit="px"
                        />
                      </div>

                      <FourGridControl
                        label="Padding"
                        values={theme.containerPadding}
                        onChange={(v) => setTheme({ ...theme, containerPadding: v })}
                      />
                      <FourGridControl
                        label="Corner radius"
                        values={theme.containerRadius}
                        onChange={(v) => setTheme({ ...theme, containerRadius: v })}
                        isRadius
                      />
                      <FourGridControl
                        label="Border"
                        values={theme.containerBorder}
                        onChange={(v) => setTheme({ ...theme, containerBorder: v })}
                      />
                      <ColorPickerRow
                        label="Border color"
                        value={theme.containerBorderColor}
                        onChange={(v) => setTheme({ ...theme, containerBorderColor: v })}
                        onClear={() => setTheme({ ...theme, containerBorderColor: '#e4e4e7' })}
                      />
                    </div>

                    {/* Bottom Links */}
                    <div className="rs-sidebar-bottom-links">
                      <button
                        type="button"
                        className="rs-bottom-link-row"
                        onClick={() => {
                          setActiveSidebar('theme');
                          setActiveDockTool(null);
                        }}
                      >
                        <div className="flex items-center gap-2">
                          <HiOutlineColorSwatch />
                          <span>Edit theme</span>
                        </div>
                        <HiOutlineChevronRight />
                      </button>

                      <button
                        type="button"
                        className="rs-bottom-link-row"
                        onClick={() => {
                          setActiveSidebar('css');
                          setActiveDockTool(null);
                        }}
                      >
                        <div className="flex items-center gap-2">
                          <HiOutlineCode />
                          <span>Global CSS</span>
                        </div>
                        <HiOutlineChevronRight />
                      </button>

                      <button
                        type="button"
                        className="rs-bottom-link-row"
                        onClick={() => {
                          setActiveSidebar('json');
                          setActiveDockTool(null);
                        }}
                      >
                        <div className="flex items-center gap-2">
                          <HiOutlineDocumentText />
                          <span>Editor JSON</span>
                        </div>
                        <HiOutlineChevronRight />
                      </button>
                    </div>
                  </>
                ) : (
                  /* ─── Individual Contextual Block Inspector ─────────────── */
                  <div className="rs-block-inspector-content">
                    <div className="rs-block-type-badge">
                      <span className="capitalize">{selectedBlock.type}</span> Block
                    </div>

                    {/* 1. Heading Block Inspector */}
                    {selectedBlock.type === 'heading' && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Level</label>
                          <div className="rs-seg-group full-width">
                            {['h1', 'h2', 'h3'].map((lvl) => (
                              <button
                                key={lvl}
                                type="button"
                                className={`rs-seg-btn flex-1 justify-center ${(selectedBlock.level || 'h1') === lvl ? 'active' : ''}`}
                                onClick={() => updateBlock(selectedBlock.id, { level: lvl, fontSize: lvl === 'h1' ? 26 : lvl === 'h2' ? 22 : 18 })}
                              >
                                {lvl.toUpperCase()}
                              </button>
                            ))}
                          </div>
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Text Content</label>
                          <textarea
                            className="re-textarea-clean"
                            rows={3}
                            value={selectedBlock.content || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { content: e.target.value })}
                            placeholder="Heading text…"
                          />
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Font Family</label>
                          <CustomSelect
                            options={GOOGLE_FONTS}
                            value={selectedBlock.fontFamily || theme.title?.fontFamily || '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'}
                            onChange={(v) => updateBlock(selectedBlock.id, { fontFamily: v })}
                          />
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Font Size</label>
                          <DimensionBox
                            value={selectedBlock.fontSize || 24}
                            onChange={(v) => updateBlock(selectedBlock.id, { fontSize: v })}
                            unit="px"
                          />
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Font Weight</label>
                          <CustomSelect
                            options={FONT_WEIGHT_OPTIONS}
                            value={selectedBlock.fontWeight || '700'}
                            onChange={(v) => updateBlock(selectedBlock.id, { fontWeight: v })}
                          />
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Line Height</label>
                          <DimensionBox
                            value={selectedBlock.lineHeight ? (selectedBlock.lineHeight <= 3 ? Math.round(selectedBlock.lineHeight * 100) : selectedBlock.lineHeight) : 130}
                            onChange={(v) => updateBlock(selectedBlock.id, { lineHeight: v })}
                            unit="%"
                          />
                        </div>

                        <ColorPickerRow
                          label="Text Color"
                          value={selectedBlock.color}
                          onChange={(v) => updateBlock(selectedBlock.id, { color: v })}
                          onClear={() => updateBlock(selectedBlock.id, { color: '#09090b' })}
                        />

                        {/* Alignment */}
                        <div className="re-prop-row">
                          <label className="re-prop-label">Alignment</label>
                          <div className="rs-seg-group full-width">
                            <button
                              type="button"
                              className={`rs-seg-btn flex-1 justify-center ${(selectedBlock.align || 'left') === 'left' ? 'active' : ''}`}
                              onClick={() => updateBlock(selectedBlock.id, { align: 'left' })}
                            >
                              <MdFormatAlignLeft />
                            </button>
                            <button
                              type="button"
                              className={`rs-seg-btn flex-1 justify-center ${(selectedBlock.align || 'left') === 'center' ? 'active' : ''}`}
                              onClick={() => updateBlock(selectedBlock.id, { align: 'center' })}
                            >
                              <MdFormatAlignCenter />
                            </button>
                            <button
                              type="button"
                              className={`rs-seg-btn flex-1 justify-center ${(selectedBlock.align || 'left') === 'right' ? 'active' : ''}`}
                              onClick={() => updateBlock(selectedBlock.id, { align: 'right' })}
                            >
                              <MdFormatAlignRight />
                            </button>
                          </div>
                        </div>

                        <FourGridControl
                          label="Margin"
                          values={selectedBlock.margin || { bottom: 16 }}
                          onChange={(v) => updateBlock(selectedBlock.id, { margin: v })}
                        />
                      </>
                    )}

                    {/* 2. Text Block Inspector */}
                    {selectedBlock.type === 'text' && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Paragraph Text</label>
                          <textarea
                            className="re-textarea-clean"
                            rows={4}
                            value={selectedBlock.content || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { content: e.target.value })}
                            placeholder="Type paragraph text…"
                          />
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Font Family</label>
                          <CustomSelect
                            options={GOOGLE_FONTS}
                            value={selectedBlock.fontFamily || theme.text?.fontFamily || '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'}
                            onChange={(v) => updateBlock(selectedBlock.id, { fontFamily: v })}
                          />
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Font Size</label>
                          <DimensionBox
                            value={selectedBlock.fontSize || 14}
                            onChange={(v) => updateBlock(selectedBlock.id, { fontSize: v })}
                            unit="px"
                          />
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Weight</label>
                          <CustomSelect
                            options={FONT_WEIGHT_OPTIONS}
                            value={selectedBlock.fontWeight || '400'}
                            onChange={(v) => updateBlock(selectedBlock.id, { fontWeight: v })}
                          />
                        </div>

                        <div className="re-prop-row">
                          <label className="re-prop-label">Line Height</label>
                          <DimensionBox
                            value={selectedBlock.lineHeight ? (selectedBlock.lineHeight <= 3 ? Math.round(selectedBlock.lineHeight * 100) : selectedBlock.lineHeight) : 155}
                            onChange={(v) => updateBlock(selectedBlock.id, { lineHeight: v })}
                            unit="%"
                          />
                        </div>

                        <ColorPickerRow
                          label="Text Color"
                          value={selectedBlock.color}
                          onChange={(v) => updateBlock(selectedBlock.id, { color: v })}
                          onClear={() => updateBlock(selectedBlock.id, { color: '#18181b' })}
                        />

                        {/* Alignment */}
                        <div className="re-prop-row">
                          <label className="re-prop-label">Alignment</label>
                          <div className="rs-seg-group full-width">
                            <button
                              type="button"
                              className={`rs-seg-btn flex-1 justify-center ${(selectedBlock.align || 'left') === 'left' ? 'active' : ''}`}
                              onClick={() => updateBlock(selectedBlock.id, { align: 'left' })}
                            >
                              <MdFormatAlignLeft />
                            </button>
                            <button
                              type="button"
                              className={`rs-seg-btn flex-1 justify-center ${(selectedBlock.align || 'left') === 'center' ? 'active' : ''}`}
                              onClick={() => updateBlock(selectedBlock.id, { align: 'center' })}
                            >
                              <MdFormatAlignCenter />
                            </button>
                            <button
                              type="button"
                              className={`rs-seg-btn flex-1 justify-center ${(selectedBlock.align || 'left') === 'right' ? 'active' : ''}`}
                              onClick={() => updateBlock(selectedBlock.id, { align: 'right' })}
                            >
                              <MdFormatAlignRight />
                            </button>
                          </div>
                        </div>

                        <FourGridControl
                          label="Margin"
                          values={selectedBlock.margin || { bottom: 16 }}
                          onChange={(v) => updateBlock(selectedBlock.id, { margin: v })}
                        />
                      </>
                    )}

                    {/* 3. Button Inspector */}
                    {selectedBlock.type === 'button' && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Label</label>
                          <input
                            type="text"
                            className="re-input-clean"
                            value={selectedBlock.text || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { text: e.target.value })}
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Target URL</label>
                          <input
                            type="text"
                            className="re-input-clean font-mono"
                            value={selectedBlock.url || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { url: e.target.value })}
                            placeholder="https://..."
                          />
                        </div>
                        <ColorPickerRow
                          label="Background"
                          value={selectedBlock.bgColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { bgColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { bgColor: '#000000' })}
                        />
                        <ColorPickerRow
                          label="Text Color"
                          value={selectedBlock.textColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { textColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { textColor: '#ffffff' })}
                        />
                        <div className="re-prop-row">
                          <label className="re-prop-label">Corner radius</label>
                          <DimensionBox
                            value={selectedBlock.borderRadius || 8}
                            onChange={(v) => updateBlock(selectedBlock.id, { borderRadius: v })}
                            unit="px"
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Padding X</label>
                          <DimensionBox
                            value={selectedBlock.paddingX || 20}
                            onChange={(v) => updateBlock(selectedBlock.id, { paddingX: v })}
                            unit="px"
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Padding Y</label>
                          <DimensionBox
                            value={selectedBlock.paddingY || 10}
                            onChange={(v) => updateBlock(selectedBlock.id, { paddingY: v })}
                            unit="px"
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Full Width</label>
                          <input
                            type="checkbox"
                            checked={Boolean(selectedBlock.isFullWidth)}
                            onChange={(e) => updateBlock(selectedBlock.id, { isFullWidth: e.target.checked })}
                          />
                        </div>
                      </>
                    )}

                    {/* 4. Image Inspector with Real-Time Upload Button */}
                    {selectedBlock.type === 'image' && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Image Source</label>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, width: '100%' }}>
                            {selectedBlock.src && (
                              <div style={{ width: '100%', height: 100, borderRadius: 6, overflow: 'hidden', border: '1px solid #27272a' }}>
                                <img src={selectedBlock.src} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                              </div>
                            )}
                            <button
                              type="button"
                              className="rs-btn-outline w-full flex items-center justify-center gap-2"
                              onClick={() => {
                                setImageModalTab('upload');
                                setShowImageModal(true);
                              }}
                            >
                              <HiOutlineUpload />
                              <span>Replace / Upload Image…</span>
                            </button>
                          </div>
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Alt Text</label>
                          <input
                            type="text"
                            className="re-input-clean"
                            value={selectedBlock.alt || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { alt: e.target.value })}
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Caption</label>
                          <input
                            type="text"
                            className="re-input-clean"
                            value={selectedBlock.caption || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { caption: e.target.value })}
                            placeholder="Optional image caption"
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Link URL</label>
                          <input
                            type="text"
                            className="re-input-clean font-mono"
                            value={selectedBlock.linkUrl || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { linkUrl: e.target.value })}
                            placeholder="https://... (click destination)"
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Corner radius</label>
                          <DimensionBox
                            value={selectedBlock.borderRadius || 0}
                            onChange={(v) => updateBlock(selectedBlock.id, { borderRadius: v })}
                            unit="px"
                          />
                        </div>
                      </>
                    )}

                    {/* 5. Badge Inspector */}
                    {selectedBlock.type === 'badge' && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Badge Text</label>
                          <input
                            type="text"
                            className="re-input-clean"
                            value={selectedBlock.text || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { text: e.target.value })}
                          />
                        </div>
                        <ColorPickerRow
                          label="Background"
                          value={selectedBlock.bgColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { bgColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { bgColor: '#f4f4f5' })}
                        />
                        <ColorPickerRow
                          label="Text Color"
                          value={selectedBlock.textColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { textColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { textColor: '#000000' })}
                        />
                        <ColorPickerRow
                          label="Border Color"
                          value={selectedBlock.borderColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { borderColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { borderColor: '#e4e4e7' })}
                        />
                        <div className="re-prop-row">
                          <label className="re-prop-label">Font Size</label>
                          <DimensionBox
                            value={selectedBlock.fontSize || 12}
                            onChange={(v) => updateBlock(selectedBlock.id, { fontSize: v })}
                            unit="px"
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Corner radius</label>
                          <DimensionBox
                            value={selectedBlock.borderRadius || 999}
                            onChange={(v) => updateBlock(selectedBlock.id, { borderRadius: v })}
                            unit="px"
                          />
                        </div>
                      </>
                    )}

                    {/* 6. Quote Inspector */}
                    {selectedBlock.type === 'quote' && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Quote Text</label>
                          <textarea
                            className="re-textarea-clean"
                            rows={3}
                            value={selectedBlock.content || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { content: e.target.value })}
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Citation</label>
                          <input
                            type="text"
                            className="re-input-clean"
                            value={selectedBlock.citation || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { citation: e.target.value })}
                            placeholder="e.g., Steve Jobs, Apple"
                          />
                        </div>
                        <ColorPickerRow
                          label="Border Color"
                          value={selectedBlock.borderColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { borderColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { borderColor: '#000000' })}
                        />
                        <ColorPickerRow
                          label="Background"
                          value={selectedBlock.bgColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { bgColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { bgColor: '#f8fafc' })}
                        />
                      </>
                    )}

                    {/* 7. Code Block Inspector */}
                    {selectedBlock.type === 'code' && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Language</label>
                          <CustomSelect
                            options={[
                              { label: 'JavaScript', value: 'javascript' },
                              { label: 'TypeScript', value: 'typescript' },
                              { label: 'Python', value: 'python' },
                              { label: 'Bash', value: 'bash' },
                              { label: 'JSON', value: 'json' },
                              { label: 'HTML', value: 'html' },
                              { label: 'CSS', value: 'css' }
                            ]}
                            value={selectedBlock.language || 'javascript'}
                            onChange={(v) => updateBlock(selectedBlock.id, { language: v })}
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Code Content</label>
                          <textarea
                            className="re-textarea-clean font-mono"
                            rows={5}
                            value={selectedBlock.code || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { code: e.target.value })}
                          />
                        </div>
                        <ColorPickerRow
                          label="Background"
                          value={selectedBlock.bgColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { bgColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { bgColor: '#09090b' })}
                        />
                      </>
                    )}

                    {/* 8. Columns (2, 3, 4) Inspector */}
                    {(selectedBlock.type === 'columns' || selectedBlock.type === '3columns' || selectedBlock.type === '4columns') && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Column Gap</label>
                          <DimensionBox
                            value={selectedBlock.gap || 16}
                            onChange={(v) => updateBlock(selectedBlock.id, { gap: v })}
                            unit="px"
                          />
                        </div>
                        <p style={{ fontSize: 11, color: '#71717a', margin: '4px 0' }}>
                          Columns automatically stack vertically on mobile devices.
                        </p>
                      </>
                    )}

                    {/* 9. Section Card Inspector */}
                    {selectedBlock.type === 'section' && (
                      <>
                        <ColorPickerRow
                          label="Background"
                          value={selectedBlock.bgColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { bgColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { bgColor: '#f9fafb' })}
                        />
                        <ColorPickerRow
                          label="Border Color"
                          value={selectedBlock.borderColor}
                          onChange={(v) => updateBlock(selectedBlock.id, { borderColor: v })}
                          onClear={() => updateBlock(selectedBlock.id, { borderColor: '#e5e7eb' })}
                        />
                        <div className="re-prop-row">
                          <label className="re-prop-label">Border Width</label>
                          <DimensionBox
                            value={selectedBlock.borderWidth || 1}
                            onChange={(v) => updateBlock(selectedBlock.id, { borderWidth: v })}
                            unit="px"
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Corner Radius</label>
                          <DimensionBox
                            value={selectedBlock.borderRadius || 8}
                            onChange={(v) => updateBlock(selectedBlock.id, { borderRadius: v })}
                            unit="px"
                          />
                        </div>
                      </>
                    )}

                    {/* 10. Social Links Inspector */}
                    {selectedBlock.type === 'social' && (
                      <>
                        <div className="rs-section-title-bold">Platforms</div>
                        <div className="rs-social-list">
                          {(selectedBlock.links || []).map((l, i) => (
                            <div key={i} className="rs-social-item">
                              <input
                                className="rs-social-platform-input"
                                value={l.platform}
                                onChange={(e) => {
                                  const updated = (selectedBlock.links || []).map((link, li) => (li === i ? { ...link, platform: e.target.value } : link));
                                  updateBlock(selectedBlock.id, { links: updated });
                                }}
                              />
                              <input
                                className="rs-social-url-input font-mono"
                                value={l.url}
                                onChange={(e) => {
                                  const updated = (selectedBlock.links || []).map((link, li) => (li === i ? { ...link, url: e.target.value } : link));
                                  updateBlock(selectedBlock.id, { links: updated });
                                }}
                                placeholder="https://..."
                              />
                              <button
                                type="button"
                                className="rs-btn-outline"
                                onClick={() => updateBlock(selectedBlock.id, { links: (selectedBlock.links || []).filter((_, li) => li !== i) })}
                              >
                                <HiOutlineTrash />
                              </button>
                            </div>
                          ))}
                          <button
                            type="button"
                            className="rs-btn-outline"
                            onClick={() => updateBlock(selectedBlock.id, { links: [...(selectedBlock.links || []), { platform: 'Website', url: 'https://' }] })}
                          >
                            + Add Link
                          </button>
                        </div>
                      </>
                    )}

                    {/* 11. Divider Inspector */}
                    {selectedBlock.type === 'divider' && (
                      <>
                        <ColorPickerRow
                          label="Line Color"
                          value={selectedBlock.color}
                          onChange={(v) => updateBlock(selectedBlock.id, { color: v })}
                          onClear={() => updateBlock(selectedBlock.id, { color: '#e4e4e7' })}
                        />
                        <div className="re-prop-row">
                          <label className="re-prop-label">Thickness</label>
                          <DimensionBox
                            value={selectedBlock.thickness || 1}
                            onChange={(v) => updateBlock(selectedBlock.id, { thickness: v })}
                            unit="px"
                          />
                        </div>
                        <FourGridControl
                          label="Margin"
                          values={selectedBlock.margin || { top: 24, bottom: 24 }}
                          onChange={(v) => updateBlock(selectedBlock.id, { margin: v })}
                        />
                      </>
                    )}

                    {/* 12. Spacer Inspector */}
                    {selectedBlock.type === 'spacer' && (
                      <div className="re-prop-row">
                        <label className="re-prop-label">Height</label>
                        <DimensionBox
                          value={selectedBlock.height || 28}
                          onChange={(v) => updateBlock(selectedBlock.id, { height: v })}
                          unit="px"
                        />
                      </div>
                    )}

                    {/* 13. Unsubscribe Inspector */}
                    {selectedBlock.type === 'unsubscribe' && (
                      <>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Notice Text</label>
                          <textarea
                            className="re-textarea-clean"
                            rows={2}
                            value={selectedBlock.text || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { text: e.target.value })}
                          />
                        </div>
                        <div className="re-prop-row">
                          <label className="re-prop-label">Link Label</label>
                          <input
                            type="text"
                            className="re-input-clean"
                            value={selectedBlock.linkText || ''}
                            onChange={(e) => updateBlock(selectedBlock.id, { linkText: e.target.value })}
                          />
                        </div>
                        <ColorPickerRow
                          label="Text Color"
                          value={selectedBlock.color}
                          onChange={(v) => updateBlock(selectedBlock.id, { color: v })}
                          onClear={() => updateBlock(selectedBlock.id, { color: '#71717a' })}
                        />
                      </>
                    )}

                    {/* Delete Block Action */}
                    <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #18181b' }}>
                      <button
                        type="button"
                        className="rs-btn-danger w-full"
                        onClick={() => deleteBlock(selectedBlock.id)}
                      >
                        <HiOutlineTrash />
                        <span>Delete Block</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 2. THEME-SIDEBAR */}
          {activeSidebar === 'theme' && (
            <div className="rs-sidebar-panel">
              <div className="rs-sidebar-header">
                <div className="rs-sidebar-title flex items-center gap-2">
                  <HiOutlineColorSwatch />
                  <span>Theme</span>
                </div>
                <button
                  type="button"
                  className="rs-close-btn"
                  onClick={() => setActiveSidebar('inspector')}
                >
                  <HiOutlineX />
                </button>
              </div>

              <div className="rs-sidebar-content">
                {/* Presets Switcher (minimal / basic / modern / elegant) */}
                <div className="rs-seg-group full-width">
                  {['minimal', 'basic', 'modern', 'elegant'].map((p) => (
                    <button
                      key={p}
                      type="button"
                      className={`rs-seg-btn flex-1 justify-center ${(theme.preset || 'minimal') === p ? 'active' : ''}`}
                      onClick={() => setTheme({ ...THEME_DEFAULTS[p], preset: p })}
                    >
                      {p}
                    </button>
                  ))}
                </div>

                {/* Section: Text */}
                <div className="rs-section-block">
                  <div className="rs-section-header-row">
                    <span className="rs-section-title-bold">Body Text</span>
                    <button type="button" className="rs-undo-btn" onClick={() => resetThemeSection('text')} title="Reset to default">
                      <MdUndo />
                    </button>
                  </div>
                  <ColorPickerRow label="Color" value={theme.text?.color} onChange={(v) => setTheme({ ...theme, text: { ...theme.text, color: v } })} />
                  <div className="re-prop-row">
                    <label className="re-prop-label">Size</label>
                    <DimensionBox value={theme.text?.size} onChange={(v) => setTheme({ ...theme, text: { ...theme.text, size: v } })} unit="px" />
                  </div>
                  <div className="re-prop-row">
                    <label className="re-prop-label">Weight</label>
                    <CustomSelect options={FONT_WEIGHT_OPTIONS} value={theme.text?.weight || '400'} onChange={(v) => setTheme({ ...theme, text: { ...theme.text, weight: v } })} />
                  </div>
                </div>

                {/* Section: Title */}
                <div className="rs-section-block">
                  <div className="rs-section-header-row">
                    <span className="rs-section-title-bold">Title</span>
                    <button type="button" className="rs-undo-btn" onClick={() => resetThemeSection('title')} title="Reset to default">
                      <MdUndo />
                    </button>
                  </div>
                  <ColorPickerRow label="Color" value={theme.title?.color} onChange={(v) => setTheme({ ...theme, title: { ...theme.title, color: v } })} />
                  <div className="re-prop-row">
                    <label className="re-prop-label">Size</label>
                    <DimensionBox value={theme.title?.size} onChange={(v) => setTheme({ ...theme, title: { ...theme.title, size: v } })} unit="px" />
                  </div>
                </div>

                {/* Section: Button */}
                <div className="rs-section-block">
                  <div className="rs-section-header-row">
                    <span className="rs-section-title-bold">Button</span>
                    <button type="button" className="rs-undo-btn" onClick={() => resetThemeSection('button')} title="Reset to default">
                      <MdUndo />
                    </button>
                  </div>
                  <ColorPickerRow label="Background" value={theme.button?.bg} onChange={(v) => setTheme({ ...theme, button: { ...theme.button, bg: v } })} />
                  <ColorPickerRow label="Text Color" value={theme.button?.color} onChange={(v) => setTheme({ ...theme, button: { ...theme.button, color: v } })} />
                  <div className="re-prop-row">
                    <label className="re-prop-label">Corner radius</label>
                    <DimensionBox value={theme.button?.radius} onChange={(v) => setTheme({ ...theme, button: { ...theme.button, radius: v } })} unit="px" />
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 3. CSS-SIDEBAR */}
          {activeSidebar === 'css' && (
            <div className="rs-sidebar-panel">
              <div className="rs-sidebar-header">
                <div className="rs-sidebar-title flex items-center gap-2">
                  <HiOutlineCode />
                  <span>Global CSS</span>
                </div>
                <div className="rs-sidebar-header-actions">
                  <button
                    type="button"
                    onClick={() => {
                      navigator.clipboard.writeText(globalCss);
                      addToast('Copied CSS to clipboard', 'success');
                    }}
                    title="Copy CSS"
                  >
                    <HiOutlineClipboard />
                  </button>
                  <button
                    type="button"
                    className="rs-close-btn"
                    onClick={() => setActiveSidebar('inspector')}
                  >
                    <HiOutlineX />
                  </button>
                </div>
              </div>

              <div className="rs-sidebar-content">
                <p className="rs-sidebar-subtext">Custom CSS rules injected directly into the template's <code>&lt;style&gt;</code> block.</p>
                <textarea
                  className="rs-code-textarea font-mono"
                  rows={15}
                  value={globalCss}
                  onChange={(e) => setGlobalCss(e.target.value)}
                  spellCheck={false}
                />
              </div>
            </div>
          )}

          {/* 4. JSON-SIDEBAR */}
          {activeSidebar === 'json' && (
            <div className="rs-sidebar-panel">
              <div className="rs-sidebar-header">
                <div className="rs-sidebar-title flex items-center gap-2">
                  <HiOutlineDocumentText />
                  <span>Editor JSON</span>
                </div>
                <div className="rs-sidebar-header-actions">
                  <button
                    type="button"
                    onClick={() => {
                      navigator.clipboard.writeText(JSON.stringify(doc, null, 2));
                      addToast('Copied AST JSON to clipboard', 'success');
                    }}
                    title="Copy JSON"
                  >
                    <HiOutlineClipboard />
                  </button>
                  <button
                    type="button"
                    className="rs-close-btn"
                    onClick={() => setActiveSidebar('inspector')}
                  >
                    <HiOutlineX />
                  </button>
                </div>
              </div>

              <div className="rs-sidebar-content">
                <pre className="rs-json-viewer font-mono">
                  {JSON.stringify(doc, null, 2)}
                </pre>
              </div>
            </div>
          )}

          {/* 5. VARIABLES-SIDEBAR */}
          {activeSidebar === 'variables' && (
            <div className="rs-sidebar-panel">
              <div className="rs-sidebar-header">
                <div className="rs-sidebar-title flex items-center gap-2">
                  <HiOutlineVariable />
                  <span>Variables</span>
                </div>
                <button
                  type="button"
                  className="rs-close-btn"
                  onClick={() => setActiveSidebar('inspector')}
                >
                  <HiOutlineX />
                </button>
              </div>

              <div className="rs-sidebar-content">
                <div className="rs-vars-list">
                  {detectedVariables.length === 0 ? (
                    <div className="rs-empty-vars">
                      <HiOutlineInformationCircle className="text-xl" />
                      <p>No dynamic variables detected yet. Add tags in the format <code>{'{{FIRST_NAME}}'}</code>.</p>
                    </div>
                  ) : (
                    detectedVariables.map((v) => (
                      <div key={v} className="rs-var-card">
                        <span className="rs-var-tag font-mono">{`{{${v}}}`}</span>
                        <input
                          type="text"
                          className="re-input-clean font-mono"
                          placeholder="Test value…"
                          value={testVariablesMock[v] || ''}
                          onChange={(e) => setTestVariablesMock({ ...testVariablesMock, [v]: e.target.value })}
                        />
                      </div>
                    ))
                  )}
                </div>

                <div className="rs-section-title-bold rs-variable-heading">
                  Custom variables
                  <button type="button" className="rs-inline-add" onClick={addVariable}>
                    <HiOutlinePlusSm /> Add
                  </button>
                </div>
                <p className="rs-variable-help">Declare every non-system tag before publishing. Values can be supplied at send time or use a fallback.</p>
                {undeclaredVariables.length > 0 && (
                  <div className="rs-variable-warning">
                    Undeclared: {undeclaredVariables.map((name) => `{{${name}}}`).join(', ')}
                  </div>
                )}
                <div className="rs-custom-vars-list">
                  {variables.length === 0 ? (
                    <p className="rs-variable-help">No custom variables declared.</p>
                  ) : variables.map((variable, index) => (
                    <div className="rs-custom-var-card" key={`${variable.name || 'new'}-${index}`}>
                      <input
                        type="text"
                        className="re-input-clean font-mono"
                        value={variable.name || ''}
                        onChange={(event) => updateVariable(index, { name: event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })}
                        placeholder="ORDER_NUMBER"
                        aria-label="Variable name"
                      />
                      <select
                        className="re-input-clean"
                        value={variable.type || 'STRING'}
                        onChange={(event) => updateVariable(index, { type: event.target.value })}
                        aria-label="Variable type"
                      >
                        <option value="STRING">Text</option>
                        <option value="NUMBER">Number</option>
                      </select>
                      <input
                        type="text"
                        className="re-input-clean"
                        value={variable.fallbackValue || ''}
                        onChange={(event) => updateVariable(index, { fallbackValue: event.target.value })}
                        placeholder="Fallback (optional)"
                        aria-label="Fallback value"
                      />
                      <button type="button" className="rs-remove-variable" onClick={() => removeVariable(index)} title="Remove variable"><HiOutlineTrash /></button>
                    </div>
                  ))}
                </div>

                <div className="rs-section-title-bold" style={{ marginTop: 20, marginBottom: 8 }}>
                  Reserved System Tags
                </div>
                <div className="rs-reserved-tags-list">
                  {RESERVED_VARIABLES.map((r) => (
                    <button
                      key={r.key}
                      type="button"
                      className="rs-reserved-tag-item"
                      onClick={() => {
                        navigator.clipboard.writeText(`{{${r.key}}}`);
                        addToast(`Copied {{${r.key}}}`, 'success');
                      }}
                    >
                      <span className="font-mono">{`{{${r.key}}}`}</span>
                      <small>{r.desc}</small>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ─── Modern Multi-Tab Image Upload & Select Modal ────────────────────── */}
      {showImageModal && (
        <div className="rs-modal-backdrop" onClick={() => setShowImageModal(false)}>
          <div className="rs-modal-card rs-image-modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="rs-modal-header">
              <div className="rs-modal-header-tabs">
                <button
                  type="button"
                  className={`rs-modal-tab-btn ${imageModalTab === 'upload' ? 'active' : ''}`}
                  onClick={() => setImageModalTab('upload')}
                >
                  <HiOutlineUpload />
                  <span>Upload Image</span>
                </button>
                <button
                  type="button"
                  className={`rs-modal-tab-btn ${imageModalTab === 'presets' ? 'active' : ''}`}
                  onClick={() => setImageModalTab('presets')}
                >
                  <HiOutlinePhotograph />
                  <span>Presets</span>
                </button>
                <button
                  type="button"
                  className={`rs-modal-tab-btn ${imageModalTab === 'url' ? 'active' : ''}`}
                  onClick={() => setImageModalTab('url')}
                >
                  <HiOutlineLink />
                  <span>Direct URL</span>
                </button>
              </div>
              <button type="button" className="rs-close-btn" onClick={() => setShowImageModal(false)}><HiOutlineX /></button>
            </div>

            <div className="rs-modal-body">
              {/* TAB 1: UPLOAD FROM DEVICE */}
              {imageModalTab === 'upload' && (
                <div className="rs-image-upload-tab">
                  <div
                    className={`rs-image-dropzone ${dragActive ? 'dragging' : ''}`}
                    onDragOver={(e) => { e.preventDefault(); setDragActive(true); }}
                    onDragLeave={() => setDragActive(false)}
                    onDrop={(e) => {
                      e.preventDefault();
                      setDragActive(false);
                      if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                        handleImageFileUpload(e.dataTransfer.files[0]);
                      }
                    }}
                    onClick={() => imageFileInputRef.current?.click()}
                  >
                    <input
                      ref={imageFileInputRef}
                      type="file"
                      accept="image/*"
                      style={{ display: 'none' }}
                      onChange={(e) => {
                        if (e.target.files && e.target.files[0]) {
                          handleImageFileUpload(e.target.files[0]);
                        }
                      }}
                    />
                    {customImageUrl ? (
                      <div className="rs-image-dropzone-preview">
                        <img src={customImageUrl} alt="Preview" />
                        <div className="rs-image-dropzone-overlay">
                          <HiOutlineUpload className="text-xl" />
                          <span>Click or drop new file to replace</span>
                        </div>
                      </div>
                    ) : (
                      <div className="rs-image-dropzone-empty">
                        <div className="rs-dropzone-icon">
                          <HiOutlineUpload />
                        </div>
                        <p className="rs-dropzone-title">Click to upload or drag & drop</p>
                        <p className="rs-dropzone-sub">PNG, JPG, SVG, GIF or WEBP</p>
                      </div>
                    )}
                  </div>

                  <div className="re-prop-row" style={{ marginTop: 16 }}>
                    <label className="re-prop-label">Alt Description</label>
                    <input
                      type="text"
                      className="re-input-clean"
                      style={{ width: '100%' }}
                      placeholder="Image alt description (for accessibility)"
                      value={customImageAlt}
                      onChange={(e) => setCustomImageAlt(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {/* TAB 2: UNSPLASH PRESETS */}
              {imageModalTab === 'presets' && (
                <div className="rs-image-presets-tab">
                  <div className="rs-image-presets-grid">
                    {IMAGE_PRESETS.map((p) => (
                      <button
                        key={p.label}
                        type="button"
                        className={`rs-preset-card ${customImageUrl === p.url ? 'active' : ''}`}
                        onClick={() => {
                          setCustomImageUrl(p.url);
                          setCustomImageAlt(p.label);
                        }}
                      >
                        <img src={p.url} alt={p.label} />
                        <span>{p.label}</span>
                        {customImageUrl === p.url && <div className="rs-preset-check"><HiOutlineCheck /></div>}
                      </button>
                    ))}
                  </div>

                  <div className="re-prop-row" style={{ marginTop: 16 }}>
                    <label className="re-prop-label">Alt Description</label>
                    <input
                      type="text"
                      className="re-input-clean"
                      style={{ width: '100%' }}
                      placeholder="Image alt description"
                      value={customImageAlt}
                      onChange={(e) => setCustomImageAlt(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {/* TAB 3: DIRECT URL */}
              {imageModalTab === 'url' && (
                <div className="rs-image-url-tab">
                  <div className="re-prop-row">
                    <label className="re-prop-label">Image URL</label>
                    <input
                      type="text"
                      className="re-input-clean font-mono"
                      style={{ width: '100%' }}
                      placeholder="https://images.unsplash.com/..."
                      value={customImageUrl}
                      onChange={(e) => setCustomImageUrl(e.target.value)}
                    />
                  </div>
                  <div className="re-prop-row">
                    <label className="re-prop-label">Alt Description</label>
                    <input
                      type="text"
                      className="re-input-clean"
                      style={{ width: '100%' }}
                      placeholder="Image alt description"
                      value={customImageAlt}
                      onChange={(e) => setCustomImageAlt(e.target.value)}
                    />
                  </div>

                  {customImageUrl && (
                    <div style={{ marginTop: 12 }}>
                      <label className="re-prop-label" style={{ marginBottom: 6, display: 'block' }}>Preview</label>
                      <div style={{ maxHeight: 180, borderRadius: 8, overflow: 'hidden', border: '1px solid #27272a' }}>
                        <img src={customImageUrl} alt="" style={{ width: '100%', height: 180, objectFit: 'cover' }} />
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className="rs-modal-footer">
              <button type="button" className="rs-btn-outline" onClick={() => setShowImageModal(false)}>Cancel</button>
              <button
                type="button"
                className="rs-btn-primary"
                disabled={!customImageUrl || isUploadingImage}
                onClick={() => {
                  if (selectedBlock?.type === 'image') {
                    updateBlock(selectedBlock.id, { src: customImageUrl, alt: customImageAlt });
                  } else {
                    addBlock('image', { src: customImageUrl, alt: customImageAlt });
                  }
                  setShowImageModal(false);
                }}
              >
                {selectedBlock?.type === 'image' ? 'Apply Changes' : 'Insert Image'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── Send Test Email Modal ───────────────────────────────────────────── */}
      {showTestModal && (
        <div className="rs-modal-backdrop" onClick={() => setShowTestModal(false)}>
          <div className="rs-modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="rs-modal-header">
              <h3>Send Test Email</h3>
              <button type="button" onClick={() => setShowTestModal(false)}><HiOutlineX /></button>
            </div>
            <div className="rs-modal-body">
              <div className="re-prop-row">
                <label className="re-prop-label">Recipient Email</label>
                <input
                  type="email"
                  className="re-input-clean font-mono"
                  style={{ width: '100%' }}
                  placeholder="you@domain.com"
                  value={testEmailAddress}
                  onChange={(e) => setTestEmailAddress(e.target.value)}
                />
              </div>
              <p style={{ fontSize: 12, color: '#71717a', margin: '8px 0 0 0' }}>
                For safety, test emails are delivered only to your verified account email. Variables in the subject and body use the mock values configured in the studio.
              </p>
            </div>
            <div className="rs-modal-footer">
              <button type="button" className="rs-btn-outline" onClick={() => setShowTestModal(false)}>Cancel</button>
              <button
                type="button"
                className="rs-btn-primary"
                disabled={!testEmailAddress || isSendingTest}
                onClick={async () => {
                  let currentTemplate = savedTemplate;
                  setIsSendingTest(true);
                  try {
                    if (!currentTemplate?.id) {
                      currentTemplate = await handleSave(false);
                    }
                    if (!currentTemplate?.id) {
                      throw new Error('Save the draft first, then send a test.');
                    }
                    await templatesApi.testSend(currentTemplate.id, {
                      toAddress: testEmailAddress,
                      variables: testVariablesMock
                    });
                    addToast(`Test email sent to ${testEmailAddress}`, 'success');
                    setShowTestModal(false);
                  } catch (e) {
                    addToast(e.message || 'Failed to send test email', 'error');
                  } finally {
                    setIsSendingTest(false);
                  }
                }}
              >
                {isSendingTest ? 'Sending…' : 'Send Test'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>,
    document.body
  );
}
