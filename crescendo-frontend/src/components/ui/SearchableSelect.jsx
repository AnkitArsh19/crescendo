import { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { HiCheck, HiChevronDown, HiRefresh } from 'react-icons/hi';
import './SearchableSelect.css';

/**
 * SearchableSelect — Portal-based dropdown that never gets clipped.
 * Dropdown renders at document.body level and auto-positions itself.
 */
export default function SearchableSelect({
    options = [],
    value = '',
    onChange,
    placeholder = 'Select…',
    loading = false,
    error = null,
    disabled = false,
    onRefresh,
    searchable = true,
    emptyMessage = 'No options found',
    className = '',
}) {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [focusIdx, setFocusIdx] = useState(-1);
    const [dropdownPos, setDropdownPos] = useState({ top: 0, left: 0, width: 0, direction: 'down' });
    const containerRef = useRef(null);
    const triggerRef = useRef(null);
    const searchRef = useRef(null);
    const optionsRef = useRef(null);
    const dropdownRef = useRef(null);

    // Filter options by search
    const filtered = useMemo(() => {
        if (!search) return options;
        const q = search.toLowerCase();
        return options.filter(
            (o) =>
                o.label?.toLowerCase().includes(q) ||
                o.description?.toLowerCase().includes(q) ||
                o.id?.toLowerCase().includes(q)
        );
    }, [options, search]);

    const selectedOption = options.find((o) => o.id === value);
    const displayText = selectedOption ? selectedOption.label : null;

    // Calculate dropdown position relative to viewport
    const updatePosition = useCallback(() => {
        if (!triggerRef.current) return;
        const rect = triggerRef.current.getBoundingClientRect();
        const spaceBelow = window.innerHeight - rect.bottom;
        const spaceAbove = rect.top;
        const dropdownHeight = Math.min(320, (filtered.length * 42) + (searchable ? 52 : 0) + 16);
        const direction = spaceBelow < dropdownHeight && spaceAbove > spaceBelow ? 'up' : 'down';

        setDropdownPos({
            top: direction === 'down' ? rect.bottom + 4 : rect.top - dropdownHeight - 4,
            left: rect.left,
            width: rect.width,
            direction,
        });
    }, [filtered.length, searchable]);

    // Reposition on scroll/resize
    useEffect(() => {
        if (!open) return;
        updatePosition();
        const handleScroll = () => updatePosition();
        const handleResize = () => updatePosition();
        window.addEventListener('scroll', handleScroll, true);
        window.addEventListener('resize', handleResize);
        return () => {
            window.removeEventListener('scroll', handleScroll, true);
            window.removeEventListener('resize', handleResize);
        };
    }, [open, updatePosition]);

    // Click outside to close
    useEffect(() => {
        if (!open) return;
        const handler = (e) => {
            if (
                containerRef.current && !containerRef.current.contains(e.target) &&
                dropdownRef.current && !dropdownRef.current.contains(e.target)
            ) {
                setOpen(false);
                setSearch('');
                setFocusIdx(-1);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, [open]);

    // Focus search on open
    useEffect(() => {
        if (open && searchable && searchRef.current) {
            setTimeout(() => searchRef.current?.focus(), 0);
        }
    }, [open, searchable]);

    // Scroll focused option into view
    useEffect(() => {
        if (focusIdx >= 0 && optionsRef.current) {
            const el = optionsRef.current.children[focusIdx];
            if (el) el.scrollIntoView({ block: 'nearest' });
        }
    }, [focusIdx]);

    const handleSelect = useCallback(
        (id) => {
            onChange?.(id);
            setOpen(false);
            setSearch('');
            setFocusIdx(-1);
        },
        [onChange]
    );

    const handleKeyDown = useCallback(
        (e) => {
            if (!open) {
                if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
                    e.preventDefault();
                    setOpen(true);
                }
                return;
            }
            switch (e.key) {
                case 'ArrowDown':
                    e.preventDefault();
                    setFocusIdx((prev) => Math.min(prev + 1, filtered.length - 1));
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    setFocusIdx((prev) => Math.max(prev - 1, 0));
                    break;
                case 'Enter':
                    e.preventDefault();
                    if (focusIdx >= 0 && focusIdx < filtered.length) {
                        const opt = filtered[focusIdx];
                        if (!opt.disabled) {
                            handleSelect(opt.id);
                        }
                    }
                    break;
                case 'Escape':
                    e.preventDefault();
                    setOpen(false);
                    setSearch('');
                    setFocusIdx(-1);
                    break;
                default:
                    break;
            }
        },
        [open, filtered, focusIdx, handleSelect]
    );

    const handleRefresh = (e) => {
        e.stopPropagation();
        onRefresh?.();
    };

    const handleToggle = () => {
        if (disabled) return;
        if (!open) updatePosition();
        setOpen(!open);
    };

    const dropdown = open ? (
        <div
            ref={dropdownRef}
            className={`ss-dropdown ss-dropdown--portal ss-dropdown--${dropdownPos.direction}`}
            style={{
                position: 'fixed',
                top: `${dropdownPos.top}px`,
                left: `${dropdownPos.left}px`,
                width: `${dropdownPos.width}px`,
                zIndex: 9999,
            }}
        >
            {searchable && (
                <div className="ss-search-wrap">
                    <input
                        ref={searchRef}
                        className="ss-search-input"
                        type="text"
                        placeholder="Search…"
                        value={search}
                        onChange={(e) => {
                            setSearch(e.target.value);
                            setFocusIdx(-1);
                        }}
                        onKeyDown={handleKeyDown}
                    />
                </div>
            )}
            <div className="ss-options" ref={optionsRef}>
                {loading ? (
                    <div className="ss-loading">
                        <span className="ss-loading-dot" />
                        <span className="ss-loading-dot" />
                        <span className="ss-loading-dot" />
                    </div>
                ) : error ? (
                    <div className="ss-error">{error}</div>
                ) : filtered.length === 0 ? (
                    <div className="ss-empty">{search ? 'No matches' : emptyMessage}</div>
                ) : (
                    filtered.map((opt, idx) => (
                        <button
                            key={opt.id}
                            type="button"
                            className={`ss-option ${value === opt.id ? 'selected' : ''} ${focusIdx === idx ? 'focused' : ''} ${opt.disabled ? 'disabled' : ''}`}
                            onClick={(e) => {
                                if (opt.disabled) {
                                    e.preventDefault();
                                    return;
                                }
                                handleSelect(opt.id);
                            }}
                            onMouseEnter={() => {
                                if (!opt.disabled) setFocusIdx(idx);
                            }}
                            title={opt.tooltip || ''}
                            style={opt.disabled ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
                        >
                            <div className="ss-option-content">
                                <div className="ss-option-label">{opt.label}</div>
                                {opt.description && (
                                    <div className="ss-option-desc">{opt.description}</div>
                                )}
                            </div>
                            {value === opt.id && (
                                <HiCheck className="ss-option-check" />
                            )}
                        </button>
                    ))
                )}
            </div>
        </div>
    ) : null;

    return (
        <div
            ref={containerRef}
            className={`ss-container ${className}`}
            onKeyDown={handleKeyDown}
        >
            <button
                ref={triggerRef}
                type="button"
                className={`ss-trigger ${open ? 'open' : ''} ${disabled ? 'disabled' : ''}`}
                onClick={handleToggle}
                tabIndex={0}
            >
                <span className={`ss-trigger-text ${!displayText ? 'placeholder' : ''}`}>
                    {displayText || placeholder}
                </span>
                <span className="ss-trigger-actions">
                    {onRefresh && !disabled && (
                        <span
                            className="ss-trigger-refresh"
                            onClick={handleRefresh}
                            title="Refresh"
                        >
                            <HiRefresh />
                        </span>
                    )}
                    <HiChevronDown className={`ss-trigger-chevron ${open ? 'open' : ''}`} />
                </span>
            </button>
            {createPortal(dropdown, document.body)}
        </div>
    );
}
