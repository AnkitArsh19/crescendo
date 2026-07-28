import { useState } from 'react';
import { Link } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { HiOutlineDuplicate, HiCheck } from 'react-icons/hi';
import { motion } from 'framer-motion';

function CopyButton({ text }) {
    const [copied, setCopied] = useState(false);
    
    const handleCopy = () => {
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <button className="docs-copy-btn" onClick={handleCopy} aria-label="Copy code">
            {copied ? <HiCheck className="text-green-400" /> : <HiOutlineDuplicate />}
        </button>
    );
}

export default function MarkdownRenderer({ content, prevItem, nextItem }) {
    return (
        <motion.div 
            className="docs-markdown-body"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.3 }}
        >
            <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                    blockquote: ({ ...props }) => {
                        const str = props.children?.[1]?.props?.children?.[0] || '';
                        let type = 'info';
                        let cleanStr = str;
                        if (typeof str === 'string') {
                            if (str.includes('[!TIP]')) { type = 'tip'; cleanStr = str.replace('[!TIP]', ''); }
                            if (str.includes('[!WARNING]')) { type = 'warning'; cleanStr = str.replace('[!WARNING]', ''); }
                            if (str.includes('[!CAUTION]')) { type = 'caution'; cleanStr = str.replace('[!CAUTION]', ''); }
                            if (str.includes('[!IMPORTANT]')) { type = 'important'; cleanStr = str.replace('[!IMPORTANT]', ''); }
                            if (str.includes('[!NOTE]')) { type = 'note'; cleanStr = str.replace('[!NOTE]', ''); }
                        }
                        
                        return (
                            <div className={`docs-alert docs-alert-${type}`}>
                                {typeof str === 'string' ? cleanStr : str}
                                {props.children?.slice ? props.children.slice(2) : props.children}
                            </div>
                        );
                    },
                    code(props) {
                        const { className, children, ...rest } = props;
                        const match = /language-(\w+)/.exec(className || '');
                        const isBlock = match || String(children).includes('\n');
                        
                        if (isBlock) {
                            const language = match ? match[1] : 'text';
                            const codeString = String(children).replace(/\n$/, '');
                            return (
                                <div className="docs-code-container">
                                    <div className="docs-code-header">
                                        <span className="docs-code-lang">{language}</span>
                                        <CopyButton text={codeString} />
                                    </div>
                                    <SyntaxHighlighter
                                        {...rest}
                                        style={vscDarkPlus}
                                        language={language}
                                        PreTag="div"
                                        customStyle={{ margin: 0, borderRadius: '0 0 8px 8px', background: 'var(--bg-card)', border: 'none' }}
                                    >
                                        {codeString}
                                    </SyntaxHighlighter>
                                </div>
                            );
                        }
                        return (
                            <code {...rest} className={`docs-inline-code ${className || ''}`}>
                                {children}
                            </code>
                        );
                    }
                }}
            >
                {content}
            </ReactMarkdown>

            {(prevItem || nextItem) && (
                <nav className="docs-nav-footer" aria-label="Documentation Page Navigation">
                    {prevItem ? (
                        <Link to={`/docs${prevItem.id ? '/' + prevItem.id : ''}`} className="docs-nav-btn prev">
                            <span className="docs-nav-btn-label">&larr; Previous</span>
                            <span className="docs-nav-btn-title">{prevItem.title}</span>
                        </Link>
                    ) : <div />}
                    {nextItem && (
                        <Link to={`/docs${nextItem.id ? '/' + nextItem.id : ''}`} className="docs-nav-btn next">
                            <span className="docs-nav-btn-label">Next &rarr;</span>
                            <span className="docs-nav-btn-title">{nextItem.title}</span>
                        </Link>
                    )}
                </nav>
            )}
        </motion.div>
    );
}
