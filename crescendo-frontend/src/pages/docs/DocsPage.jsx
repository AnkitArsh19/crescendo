import { useState } from 'react';
import { Routes, Route, Link, useLocation, Navigate } from 'react-router-dom';
import { 
    HiOutlineBookOpen, 
    HiOutlineLightningBolt, 
    HiOutlineViewGrid, 
    HiOutlineCode,
    HiOutlineMenu,
    HiOutlineX,
    HiOutlineShieldCheck,
    HiOutlineMail,
    HiOutlineUsers,
    HiOutlineChartBar,
    HiOutlineTemplate,
    HiOutlineSparkles,
    HiOutlineAdjustments,
    HiOutlineCog,
    HiOutlineTerminal
} from 'react-icons/hi';
import './DocsPage.css';

import DocsSearch from './DocsSearch';
import MarkdownRenderer from './MarkdownRenderer';
import OpenApiRenderer from './OpenApiRenderer';

// Import raw markdown content
import gettingStartedMd from './content/getting-started.md?raw';
import emailBroadcastingMd from './content/email-broadcasting.md?raw';
import audiencesContactsMd from './content/audiences-contacts.md?raw';
import analyticsInsightsMd from './content/analytics-insights.md?raw';
import workflowCanvasMd from './content/workflow-canvas.md?raw';
import naturalLanguageMd from './content/natural-language.md?raw';
import workflowRunsMd from './content/workflow-runs.md?raw';
import appsIntegrationsMd from './content/apps-integrations.md?raw';
import appsCatalogDeepdiveMd from './content/apps-catalog-deepdive.md?raw';
import byokVsOauthMd from './content/byok-vs-oauth.md?raw';
import settingsSecurityMd from './content/settings-security.md?raw';
import publicApiMd from './content/public-api.md?raw';
import sdkNodeMd from './content/sdk-node.md?raw';
import sdkPythonMd from './content/sdk-python.md?raw';
import sdkMultiLanguageMd from './content/sdk-multi-language.md?raw';
import authenticationMd from './content/authentication.md?raw';
import governanceMd from './content/api-governance.md?raw';

// Index all markdown guides and REST endpoints for instantaneous Fuse.js search
const CONTENT_INDEX = [
    { title: 'Getting Started & Overview', path: '/docs', contentSnippet: gettingStartedMd },
    { title: 'Email Marketing & Broadcasting', path: '/docs/email-broadcasting', contentSnippet: emailBroadcastingMd },
    { title: 'Audience & Contact Management', path: '/docs/audiences-contacts', contentSnippet: audiencesContactsMd },
    { title: 'Analytics & Insights Dashboards', path: '/docs/analytics-insights', contentSnippet: analyticsInsightsMd },
    { title: 'Workflow Studio & Canvas', path: '/docs/workflow-canvas', contentSnippet: workflowCanvasMd },
    { title: 'AI Builder (Natural Language)', path: '/docs/ai-builder', contentSnippet: naturalLanguageMd },
    { title: 'Workflow Runs & Diagnostic Logs', path: '/docs/workflow-runs', contentSnippet: workflowRunsMd },
    { title: 'App Catalog & Integrations Overview', path: '/docs/apps-integrations', contentSnippet: appsIntegrationsMd },
    { title: '113+ Backend Apps Catalog Index', path: '/docs/apps-catalog-deepdive', contentSnippet: appsCatalogDeepdiveMd },
    { title: 'BYOK & OAuth 2.0 Security Architecture', path: '/docs/byok-vs-oauth', contentSnippet: byokVsOauthMd },
    { title: 'Settings, Security & Passkeys', path: '/docs/settings-security', contentSnippet: settingsSecurityMd },
    { title: 'Node.js & TypeScript SDK (@crescendo/email)', path: '/docs/sdk-node', contentSnippet: sdkNodeMd },
    { title: 'Python SDK (crescendo-sdk-python)', path: '/docs/sdk-python', contentSnippet: sdkPythonMd },
    { title: 'Multi-Language SDKs (Java, Go, Rust, C#, CLI)', path: '/docs/sdk-multi-language', contentSnippet: sdkMultiLanguageMd },
    { title: 'Developer API Overview', path: '/docs/public-api', contentSnippet: publicApiMd },
    { title: 'API Authentication', path: '/docs/authentication', contentSnippet: authenticationMd },
    { title: 'API Governance & Idempotency', path: '/docs/api-governance', contentSnippet: governanceMd },
    { title: 'Workflows OpenAPI Spec', path: '/docs/api/workflows', contentSnippet: 'Manage and trigger workflows programmatically via OpenAPI v3 definitions.' },
    { title: 'Connections OpenAPI Spec', path: '/docs/api/connections', contentSnippet: 'Manage third-party app credentials via interactive specification.' },
    { title: 'Domains OpenAPI Spec', path: '/docs/api/domains', contentSnippet: 'Manage email sender domains and DNS via OpenAPI v3 definitions.' },
    { title: 'Audiences OpenAPI Spec', path: '/docs/api/audiences', contentSnippet: 'Manage contacts and audience segments via REST endpoints.' },
    { title: 'Suppressions OpenAPI Spec', path: '/docs/api/suppressions', contentSnippet: 'Manage suppressed emails and bounces via REST endpoints.' },
];

const NAV_GROUPS = [
    {
        title: 'User Guides & Operations',
        items: [
            { id: '', title: 'Getting Started', icon: <HiOutlineBookOpen />, type: 'md', content: gettingStartedMd },
            { id: 'email-broadcasting', title: 'Email Broadcasting', icon: <HiOutlineMail />, type: 'md', content: emailBroadcastingMd },
            { id: 'audiences-contacts', title: 'Audience & Contacts', icon: <HiOutlineUsers />, type: 'md', content: audiencesContactsMd },
            { id: 'analytics-insights', title: 'Analytics & Insights', icon: <HiOutlineChartBar />, type: 'md', content: analyticsInsightsMd },
        ]
    },
    {
        title: 'Workflow Automation & AI',
        items: [
            { id: 'workflow-canvas', title: 'Workflow Studio', icon: <HiOutlineTemplate />, type: 'md', content: workflowCanvasMd },
            { id: 'ai-builder', title: 'AI Builder (NL Prompts)', icon: <HiOutlineSparkles />, type: 'md', content: naturalLanguageMd },
            { id: 'workflow-runs', title: 'Runs & Diagnostic Logs', icon: <HiOutlineAdjustments />, type: 'md', content: workflowRunsMd },
            { id: 'apps-integrations', title: 'App Catalog Overview', icon: <HiOutlineViewGrid />, type: 'md', content: appsIntegrationsMd },
            { id: 'apps-catalog-deepdive', title: '113+ Apps Directory', icon: <HiOutlineViewGrid />, type: 'md', content: appsCatalogDeepdiveMd },
            { id: 'byok-vs-oauth', title: 'BYOK & OAuth Security', icon: <HiOutlineShieldCheck />, type: 'md', content: byokVsOauthMd },
        ]
    },
    {
        title: 'Official Client SDKs',
        items: [
            { id: 'sdk-node', title: 'Node.js / TypeScript SDK', icon: <HiOutlineTerminal />, type: 'md', content: sdkNodeMd },
            { id: 'sdk-python', title: 'Python SDK', icon: <HiOutlineTerminal />, type: 'md', content: sdkPythonMd },
            { id: 'sdk-multi-language', title: 'Multi-Language SDKs & CLI', icon: <HiOutlineTerminal />, type: 'md', content: sdkMultiLanguageMd },
        ]
    },
    {
        title: 'Administration & Security',
        items: [
            { id: 'settings-security', title: 'Settings & Passkeys', icon: <HiOutlineCog />, type: 'md', content: settingsSecurityMd },
        ]
    },
    {
        title: 'Developer REST API',
        items: [
            { id: 'public-api', title: 'API Overview & Auth', icon: <HiOutlineTerminal />, type: 'md', content: publicApiMd },
            { id: 'authentication', title: 'Authentication', icon: <HiOutlineShieldCheck />, type: 'md', content: authenticationMd },
            { id: 'api-governance', title: 'API Governance', icon: <HiOutlineLightningBolt />, type: 'md', content: governanceMd },
        ]
    },
    {
        title: 'OpenAPI Reference',
        items: [
            { id: 'api/workflows', title: 'Workflows', icon: <HiOutlineCode />, type: 'openapi', tag: 'Workflows' },
            { id: 'api/runs', title: 'Workflow Runs', icon: <HiOutlineCode />, type: 'openapi', tag: 'Workflow Runs' },
            { id: 'api/connections', title: 'Connections', icon: <HiOutlineCode />, type: 'openapi', tag: 'Connections' },
            { id: 'api/domains', title: 'Domains', icon: <HiOutlineCode />, type: 'openapi', tag: 'Domains' },
            { id: 'api/audiences', title: 'Audiences (Contacts)', icon: <HiOutlineCode />, type: 'openapi', tag: 'Audiences (Contacts)' },
            { id: 'api/suppressions', title: 'Suppressions', icon: <HiOutlineCode />, type: 'openapi', tag: 'Suppressions' },
            { id: 'api/apps', title: 'App Catalog Schema', icon: <HiOutlineCode />, type: 'openapi', tag: 'App Catalog' },
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
                            {(() => {
                                const flatNavItems = NAV_GROUPS.flatMap(g => g.items);
                                return flatNavItems.map((sec, idx) => {
                                    const path = sec.id;
                                    const prevItem = idx > 0 ? flatNavItems[idx - 1] : null;
                                    const nextItem = idx < flatNavItems.length - 1 ? flatNavItems[idx + 1] : null;

                                    if (sec.type === 'md') {
                                        return (
                                            <Route 
                                                key={sec.id} 
                                                path={path} 
                                                element={<MarkdownRenderer content={sec.content} prevItem={prevItem} nextItem={nextItem} />} 
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
                                });
                            })()}
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
