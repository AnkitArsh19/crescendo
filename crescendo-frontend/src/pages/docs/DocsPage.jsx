import { useState, useEffect } from 'react';
import { Routes, Route, Link, useLocation, Navigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import ReactMarkdown from 'react-markdown';
import { 
    HiOutlineBookOpen, 
    HiOutlineLightningBolt, 
    HiOutlineViewGrid, 
    HiOutlineCode,
    HiOutlineMenu,
    HiOutlineX
} from 'react-icons/hi';
import './DocsPage.css';

// Import raw markdown
import gettingStartedMd from './content/getting-started.md?raw';
import naturalLanguageMd from './content/natural-language.md?raw';
import appsIntegrationsMd from './content/apps-integrations.md?raw';
import publicApiMd from './content/public-api.md?raw';

const SECTIONS = [
    { id: 'getting-started', title: 'Getting Started', icon: <HiOutlineBookOpen />, content: gettingStartedMd },
    { id: 'natural-language', title: 'AI Builder', icon: <HiOutlineLightningBolt />, content: naturalLanguageMd },
    { id: 'apps-integrations', title: 'Apps & Integrations', icon: <HiOutlineViewGrid />, content: appsIntegrationsMd },
    { id: 'public-api', title: 'Public API', icon: <HiOutlineCode />, content: publicApiMd },
];

function MarkdownRenderer({ content }) {
    return (
        <motion.div 
            className="docs-markdown-body"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.3 }}
        >
            <ReactMarkdown
                components={{
                    blockquote: ({ node, ...props }) => {
                        const str = props.children[1]?.props?.children?.[0] || '';
                        let type = 'info';
                        let cleanStr = str;
                        if (str.includes('[!TIP]')) { type = 'tip'; cleanStr = str.replace('[!TIP]', ''); }
                        if (str.includes('[!WARNING]')) { type = 'warning'; cleanStr = str.replace('[!WARNING]', ''); }
                        if (str.includes('[!CAUTION]')) { type = 'caution'; cleanStr = str.replace('[!CAUTION]', ''); }
                        if (str.includes('[!IMPORTANT]')) { type = 'important'; cleanStr = str.replace('[!IMPORTANT]', ''); }
                        if (str.includes('[!NOTE]')) { type = 'note'; cleanStr = str.replace('[!NOTE]', ''); }
                        
                        return (
                            <div className={`docs-alert docs-alert-${type}`}>
                                {cleanStr}
                                {props.children.slice(2)}
                            </div>
                        );
                    }
                }}
            >
                {content}
            </ReactMarkdown>
        </motion.div>
    );
}

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
                    {SECTIONS.map(section => {
                        const path = `/docs/${section.id}`;
                        // If it's subdomain, path is just `/${section.id}`
                        // But since we use React Router, we just let it handle relative or exact paths.
                        // Actually, wait, if we are on docs.crescendo.com, the base path is `/`. If we are on crescendo.com/docs, the base is `/docs`.
                        // Let's use relative links if possible or infer from base.
                        // For now we'll hardcode based on location.
                        const isSubdomain = window.location.hostname.startsWith('docs.');
                        const linkPath = isSubdomain ? `/${section.id}` : `/docs/${section.id}`;
                        const isActive = location.pathname.includes(section.id);
                        
                        return (
                            <Link 
                                key={section.id} 
                                to={linkPath} 
                                className={`docs-nav-link ${isActive ? 'active' : ''}`}
                                onClick={() => setIsOpen(false)}
                            >
                                {section.icon} {section.title}
                            </Link>
                        );
                    })}
                </nav>
            </aside>
        </>
    );
}

export default function DocsPage() {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const location = useLocation();

    // Check if we're on the root path to redirect to the first section
    const isSubdomain = window.location.hostname.startsWith('docs.');
    const rootPath = isSubdomain ? '/' : '/docs';
    
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
                
                <main className="docs-content-container">
                    <AnimatePresence mode="wait">
                        <Routes location={location} key={location.pathname}>
                            <Route path="/" element={<Navigate to="getting-started" replace />} />
                            {SECTIONS.map(sec => (
                                <Route 
                                    key={sec.id} 
                                    path={sec.id} 
                                    element={<MarkdownRenderer content={sec.content} />} 
                                />
                            ))}
                        </Routes>
                    </AnimatePresence>
                </main>
            </div>
        </div>
    );
}
