import { motion } from 'framer-motion';
import { HiOutlineCheck } from 'react-icons/hi';
import './ApiSection.css';

const apiFeatures = [
    { text: <><strong>RESTful endpoints</strong> — Full CRUD for workflows, executions, and integrations</> },
    { text: <><strong>Webhook registration</strong> — Programmatically create and manage webhook listeners</> },
    { text: <><strong>Execution logs</strong> — Query detailed run histories with filtering and pagination</> },
    { text: <><strong>SDK support</strong> — Official libraries for Node.js, Python, and Go</> },
];

export default function ApiSection() {
    return (
        <section className="api-section" id="api">
            <div className="api-inner">
                <motion.div
                    className="api-content"
                    initial={{ opacity: 0, x: -30 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true, margin: '-50px' }}
                    transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
                >
                    <p className="section-label">API</p>
                    <h2 className="section-title">
                        Built for <span className="font-serif" style={{ fontStyle: 'italic' }}>developers</span>
                    </h2>
                    <p className="section-subtitle">
                        A clean, well-documented REST API that lets you control every aspect
                        of your automation pipeline programmatically.
                    </p>
                    <div className="api-features">
                        {apiFeatures.map((f, i) => (
                            <motion.div
                                className="api-feature"
                                key={i}
                                initial={{ opacity: 0, x: -20 }}
                                whileInView={{ opacity: 1, x: 0 }}
                                viewport={{ once: true }}
                                transition={{ delay: 0.2 + i * 0.1, duration: 0.5 }}
                            >
                                <div className="api-feature-icon">
                                    <HiOutlineCheck />
                                </div>
                                <div className="api-feature-text">{f.text}</div>
                            </motion.div>
                        ))}
                    </div>
                </motion.div>

                <motion.div
                    className="api-code-wrapper"
                    initial={{ opacity: 0, x: 30, rotateY: 5 }}
                    whileInView={{ opacity: 1, x: 0, rotateY: 0 }}
                    viewport={{ once: true, margin: '-50px' }}
                    transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
                >
                    <div className="api-code-glow" />
                    <div className="api-code-block">
                        <div className="api-code-header">
                            <div className="api-code-dots">
                                <span className="api-code-dot" />
                                <span className="api-code-dot" />
                                <span className="api-code-dot" />
                            </div>
                            <span className="api-code-filename">workflow.js</span>
                        </div>
                        <div className="api-code-body">
                            <pre>{codeSnippet}</pre>
                        </div>
                    </div>
                </motion.div>
            </div>
        </section>
    );
}

const codeSnippet = (
    <>
        <span className="code-comment">{'// Create a new workflow'}</span>{'\n'}
        <span className="code-keyword">const</span>{' workflow = '}
        <span className="code-keyword">await</span>{' '}
        <span className="code-func">crescendo</span>
        {'.workflows.'}
        <span className="code-func">create</span>
        {'('}<span className="code-bracket">{'{'}</span>{'\n'}
        {'  name: '}<span className="code-string">{'"data-pipeline"'}</span>{','}{'\n'}
        {'  trigger: '}<span className="code-bracket">{'{'}</span>{'\n'}
        {'    type: '}<span className="code-string">{'"webhook"'}</span>{','}{'\n'}
        {'    config: '}<span className="code-bracket">{'{'}</span>{' path: '}<span className="code-string">{'"​/ingest"'}</span>{' '}<span className="code-bracket">{'}'}</span>{'\n'}
        {'  '}<span className="code-bracket">{'}'}</span>{','}{'\n'}
        {'  steps: '}<span className="code-bracket">{'['}</span>{'\n'}
        {'    '}<span className="code-bracket">{'{'}</span>{' action: '}<span className="code-string">{'"transform"'}</span>{', '}{'\n'}
        {'      fn: '}<span className="code-string">{'"parseJSON"'}</span>{' '}<span className="code-bracket">{'}'}</span>{','}{'\n'}
        {'    '}<span className="code-bracket">{'{'}</span>{' action: '}<span className="code-string">{'"store"'}</span>{', '}{'\n'}
        {'      target: '}<span className="code-string">{'"postgres"'}</span>{' '}<span className="code-bracket">{'}'}</span>{'\n'}
        {'  '}<span className="code-bracket">{']'}</span>{'\n'}
        <span className="code-bracket">{'}'}</span>{')'}{';'}{'\n'}
        {'\n'}
        <span className="code-comment">{'// Deploy it'}</span>{'\n'}
        <span className="code-keyword">await</span>{' workflow.'}
        <span className="code-func">deploy</span>{'()'}{';'}
    </>
);
