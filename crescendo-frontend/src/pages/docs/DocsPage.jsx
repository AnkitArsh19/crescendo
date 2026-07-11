import { useState } from 'react';
import { Routes, Route, Link, useLocation, Navigate } from 'react-router-dom';
import { 
    HiOutlineBookOpen, 
    HiOutlineLightningBolt, 
    HiOutlineViewGrid, 
    HiOutlineCode,
    HiOutlineMenu,
    HiOutlineX,
    HiOutlineShieldCheck
} from 'react-icons/hi';
import './DocsPage.css';

import DocsSearch from './DocsSearch';
import MarkdownRenderer from './MarkdownRenderer';
import OpenApiRenderer from './OpenApiRenderer';

// Import raw markdown content
import gettingStartedMd from './content/getting-started.md?raw';
import authenticationMd from './content/authentication.md?raw';
import governanceMd from './content/api-governance.md?raw';

// We index this for Fuse.js
const CONTENT_INDEX = [
    { title: 'Getting Started', path: '/docs', contentSnippet: gettingStartedMd },
    { title: 'Authentication', path: '/docs/authentication', contentSnippet: authenticationMd },
    { title: 'API Governance', path: '/docs/api-governance', contentSnippet: governanceMd },
    { title: 'Workflows API', path: '/docs/api/workflows', contentSnippet: 'Manage and trigger workflows programmatically.' },
    { title: 'Connections API', path: '/docs/api/connections', contentSnippet: 'Manage third-party app credentials.' },
    { title: 'Domains API', path: '/docs/api/domains', contentSnippet: 'Manage email sender domains and DNS.' },
    { title: 'Audiences API', path: '/docs/api/audiences', contentSnippet: 'Manage contacts and audiences.' },
    { title: 'Suppressions API', path: '/docs/api/suppressions', contentSnippet: 'Manage suppressed emails and bounces.' },
];

const NAV_GROUPS = [
    {
        title: 'Guides',
        items: [
            { id: '', title: 'Getting Started', icon: <HiOutlineBookOpen />, type: 'md', content: gettingStartedMd },
            { id: 'authentication', title: 'Authentication', icon: <HiOutlineShieldCheck />, type: 'md', content: authenticationMd },
            { id: 'api-governance', title: 'API Governance', icon: <HiOutlineLightningBolt />, type: 'md', content: governanceMd },
        ]
    },
    {
        title: 'API Reference',
        items: [
            { id: 'api/workflows', title: 'Workflows', icon: <HiOutlineCode />, type: 'openapi', tag: 'Workflows' },
            { id: 'api/runs', title: 'Workflow Runs', icon: <HiOutlineCode />, type: 'openapi', tag: 'Workflow Runs' },
            { id: 'api/connections', title: 'Connections', icon: <HiOutlineCode />, type: 'openapi', tag: 'Connections' },
            { id: 'api/domains', title: 'Domains', icon: <HiOutlineCode />, type: 'openapi', tag: 'Domains' },
            { id: 'api/audiences', title: 'Audiences (Contacts)', icon: <HiOutlineCode />, type: 'openapi', tag: 'Audiences (Contacts)' },
            { id: 'api/suppressions', title: 'Suppressions', icon: <HiOutlineCode />, type: 'openapi', tag: 'Suppressions' },
            { id: 'api/apps', title: 'App Catalog', icon: <HiOutlineViewGrid />, type: 'openapi', tag: 'App Catalog' },
        ]
    }
];

function DocsSidebar({ isOpen, setIsOpen }) {
    const location = useLocation();
    
    return (
        <>
            <div className={`docs-sidebar-backdrop ${isOpen ? 'show' : ''}`} onClick={() => setIsOpen(false)} />
            <aside className={`docs-sidebar ${isOpen ? 'open' : ''}`}>
                <div className="docs-sidebar-header">
                    <div className="docs-logo">
                        <span>Crescendo</span> Docs
                    </div>
                    <button className="docs-sidebar-close" onClick={() => setIsOpen(false)}>
                        <HiOutlineX />
                    </button>
                </div>
                
                <nav className="docs-sidebar-nav">
                    {NAV_GROUPS.map((group, idx) => (
                        <div key={idx} className="docs-nav-group">
                            <div className="docs-nav-group-title">{group.title}</div>
                            {group.items.map(item => {
                                const linkPath = `/docs${item.id ? '/' + item.id : ''}`;
                                const isActive = location.pathname === linkPath || (item.id === '' && location.pathname === '/docs/');
                                
                                return (
                                    <Link 
                                        key={item.id} 
                                        to={linkPath} 
                                        className={`docs-nav-link ${isActive ? 'active' : ''}`}
                                        onClick={() => setIsOpen(false)}
                                    >
                                        {item.icon} {item.title}
                                    </Link>
                                );
                            })}
                        </div>
                    ))}
                </nav>
            </aside>
        </>
    );
}

// Extract h2 and h3 from markdown for TOC
function extractToc(markdown) {
    if (!markdown) return [];
    const lines = markdown.split('\n');
    const toc = [];
    lines.forEach(line => {
        const h2 = line.match(/^##\s+(.+)$/);
        if (h2) toc.push({ level: 2, text: h2[1] });
        const h3 = line.match(/^###\s+(.+)$/);
        if (h3) toc.push({ level: 3, text: h3[1] });
    });
    return toc;
}

export default function DocsPage() {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const location = useLocation();

    // Determine current content for TOC
    let currentToc = [];
    for (const group of NAV_GROUPS) {
        for (const item of group.items) {
            const linkPath = `/docs${item.id ? '/' + item.id : ''}`;
            if (location.pathname === linkPath && item.type === 'md') {
                currentToc = extractToc(item.content);
            }
        }
    }

    return (
        <div className="docs-layout">
            <DocsSidebar isOpen={sidebarOpen} setIsOpen={setSidebarOpen} />
            
            <div className="docs-main">
                <header className="docs-header-mobile">
                    <button className="docs-menu-btn" onClick={() => setSidebarOpen(true)}>
                        <HiOutlineMenu />
                    </button>
                    <div className="docs-logo-mobile">Crescendo Docs</div>
                </header>

                <div className="docs-topbar">
                    <DocsSearch contentIndex={CONTENT_INDEX} />
                </div>
                
                <div className="docs-content-wrapper">
                    <main className="docs-markdown-body">
                        <Routes location={location} key={location.pathname}>
                            {NAV_GROUPS.flatMap(g => g.items).map(sec => {
                                const path = sec.id;
                                if (sec.type === 'md') {
                                    return (
                                        <Route 
                                            key={sec.id} 
                                            path={path} 
                                            element={<MarkdownRenderer content={sec.content} />} 
                                        />
                                    );
                                } else {
                                    return (
                                        <Route 
                                            key={sec.id} 
                                            path={path} 
                                            element={<OpenApiRenderer targetTag={sec.tag} />} 
                                        />
                                    );
                                }
                            })}
                        </Routes>
                    </main>

                    {/* Right Table of Contents */}
                    <aside className="docs-toc">
                        <h4>On this page</h4>
                        {currentToc.length > 0 ? (
                            <ul>
                                {currentToc.map((item, idx) => (
                                    <li key={idx} style={{ paddingLeft: item.level === 3 ? '1rem' : '0' }}>
                                        <a href={`#${item.text.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`}>
                                            {item.text}
                                        </a>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                                Overview
                            </p>
                        )}
                    </aside>
                </div>
            </div>
        </div>
    );
}
