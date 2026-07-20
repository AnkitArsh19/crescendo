import { useState } from 'react';
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

// A custom component for CodeTabs that we can use inside MDX (or via some custom logic)
// Since we are using standard Markdown, we can't easily embed React components directly without MDX.
// BUT we can use a trick: if multiple code blocks are back-to-back, we can group them. 
// For simplicity in standard markdown, we will just render standard code blocks beautifully.
// To actually support Tabs in pure Markdown, we'd need a remark plugin or a complex AST transform.
// Alternatively, we can just render the language label nicely and developers can see all languages sequentially.
// Let's implement a clean code block with a top bar showing the language and copy button.

export default function MarkdownRenderer({ content }) {
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
                        
                        // If it has a language match, it's definitely a code block.
                        // If it doesn't, but its parent is a 'pre', it's also a code block.
                        // react-markdown v9+ passes node which we can inspect, but an easier way is to just 
                        // check if it has a match, or if it has newlines (which inline code rarely does).
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
        </motion.div>
    );
}
