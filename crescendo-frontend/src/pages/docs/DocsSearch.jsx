import { useState, useMemo, useEffect } from 'react';
import { HiOutlineSearch, HiOutlineX } from 'react-icons/hi';
import { useNavigate } from 'react-router-dom';
import Fuse from 'fuse.js';

function searchExcerpt(content, query) {
    const plainText = content
        .replace(/```[\s\S]*?```/g, ' ')
        .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
        .replace(/[`#>*_|]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
    const matchAt = plainText.toLowerCase().indexOf(query.toLowerCase());
    const start = matchAt > 48 ? matchAt - 48 : 0;
    const excerpt = plainText.slice(start, start + 180).trim();
    return `${start > 0 ? '…' : ''}${excerpt}${start + 180 < plainText.length ? '…' : ''}`;
}

export default function DocsSearch({ contentIndex }) {
    const [query, setQuery] = useState('');
    const [isOpen, setIsOpen] = useState(false);
    const navigate = useNavigate();

    // contentIndex is an array of objects: { title, path, contentSnippet }
    const fuse = useMemo(() => new Fuse(contentIndex, {
        keys: ['title', 'contentSnippet'],
        threshold: 0.3,
        includeMatches: true
    }), [contentIndex]);

    const results = useMemo(() => {
        if (!query) return [];
        return fuse.search(query).slice(0, 5);
    }, [query, fuse]);

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key === 'k' && (e.metaKey || e.ctrlKey)) {
                e.preventDefault();
                setIsOpen(true);
            }
            if (e.key === 'Escape') {
                setIsOpen(false);
            }
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, []);

    const handleSelect = (path) => {
        navigate(path);
        setIsOpen(false);
        setQuery('');
    };

    return (
        <div className="docs-search-wrapper">
            <button type="button" className="docs-search-trigger" onClick={() => setIsOpen(true)}>
                <HiOutlineSearch className="docs-search-icon" />
                <span className="docs-search-placeholder">Search guides and API reference</span>
                <span className="docs-search-shortcut">Ctrl / Cmd K</span>
            </button>

            {isOpen && (
                <div className="docs-search-overlay" onClick={() => setIsOpen(false)} role="presentation">
                    <section className="docs-search-modal" role="dialog" aria-modal="true" aria-labelledby="docs-search-title" onClick={e => e.stopPropagation()}>
                        <div className="docs-search-modal-intro">
                            <p>Documentation search</p>
                            <h2 id="docs-search-title">Find an answer</h2>
                        </div>
                        <div className="docs-search-input-wrapper">
                            <HiOutlineSearch className="docs-search-icon-large" />
                            <input 
                                autoFocus
                                type="text"
                                placeholder="Search..."
                                value={query}
                                onChange={e => setQuery(e.target.value)}
                                className="docs-search-input"
                            />
                            <button type="button" className="docs-search-close" onClick={() => setIsOpen(false)} aria-label="Close search">
                                <HiOutlineX />
                            </button>
                        </div>

                        {!query && <p className="docs-search-hint">Search workflow setup, passkeys, email delivery, API endpoints, SDKs, and more.</p>}
                        {query && results.length > 0 && (
                            <ul className="docs-search-results">
                                {results.map((res) => (
                                    <li key={res.item.path}>
                                        <button 
                                            className="docs-search-result-btn"
                                            onClick={() => handleSelect(res.item.path)}
                                        >
                                            <div className="docs-search-result-title">{res.item.title}</div>
                                            <div className="docs-search-result-snippet">
                                                {searchExcerpt(res.item.contentSnippet, query)}
                                            </div>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                        {query && results.length === 0 && (
                            <div className="docs-search-empty">No results found for "{query}"</div>
                        )}
                    </section>
                </div>
            )}
        </div>
    );
}
