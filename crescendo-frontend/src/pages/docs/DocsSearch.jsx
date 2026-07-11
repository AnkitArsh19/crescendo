import { useState, useMemo, useEffect } from 'react';
import { HiOutlineSearch, HiOutlineX } from 'react-icons/hi';
import { useNavigate } from 'react-router-dom';
import Fuse from 'fuse.js';

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
            <button className="docs-search-trigger" onClick={() => setIsOpen(true)}>
                <HiOutlineSearch className="docs-search-icon" />
                <span className="docs-search-placeholder">Search documentation...</span>
                <span className="docs-search-shortcut">⌘K</span>
            </button>

            {isOpen && (
                <div className="docs-search-overlay" onClick={() => setIsOpen(false)}>
                    <div className="docs-search-modal" onClick={e => e.stopPropagation()}>
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
                            <button className="docs-search-close" onClick={() => setIsOpen(false)}>
                                <HiOutlineX />
                            </button>
                        </div>
                        
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
                                                {res.item.contentSnippet.substring(0, 80)}...
                                            </div>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                        {query && results.length === 0 && (
                            <div className="docs-search-empty">No results found for "{query}"</div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
