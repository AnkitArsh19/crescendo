import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import {
    HiOutlineBookOpen,
    HiOutlineAcademicCap,
    HiOutlineCode,
    HiOutlinePlay,
    HiArrowRight,
} from 'react-icons/hi';
import './DocsSection.css';

const docs = [
    {
        icon: <HiOutlinePlay />,
        title: 'Getting Started',
        desc: 'Set up your account and make your first authenticated Crescendo API request.',
        to: '/docs',
    },
    {
        icon: <HiOutlineBookOpen />,
        title: 'Authentication',
        desc: 'Learn how JWTs, API keys, OAuth clients, scopes, and rate limits protect your requests.',
        to: '/docs/authentication',
    },
    {
        icon: <HiOutlineCode />,
        title: 'Public API Reference',
        desc: 'Browse the OpenAPI-backed workflow, connection, email, and resource endpoints.',
        to: '/docs/api/workflows',
    },
    {
        icon: <HiOutlineAcademicCap />,
        title: 'App Catalog',
        desc: 'See the supported apps, actions, triggers, and connection schemas exposed by Crescendo.',
        to: '/docs/api/apps',
    },
];

const cardVariant = {
    hidden: { opacity: 0, y: 30 },
    visible: (index) => ({
        opacity: 1,
        y: 0,
        transition: { duration: 0.6, delay: index * 0.1, ease: [0.22, 1, 0.36, 1] },
    }),
};

export default function DocsSection() {
    return (
        <section className="docs-section" id="docs">
            <div className="docs-header">
                <motion.p className="section-label" initial={{ opacity: 0 }} whileInView={{ opacity: 1 }} viewport={{ once: true }}>
                    Resources
                </motion.p>
                <motion.h2 className="section-title" initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ duration: 0.6 }}>
                    Learn &amp; <span className="font-serif" style={{ fontStyle: 'italic' }}>explore</span>
                </motion.h2>
                <motion.p className="section-subtitle" initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ duration: 0.6, delay: 0.1 }}>
                    Start with the guides, then use the live OpenAPI reference for the public surface.
                </motion.p>
            </div>

            <div className="docs-grid">
                {docs.map((doc, index) => (
                    <motion.div className="docs-card" key={doc.title} custom={index} variants={cardVariant} initial="hidden" whileInView="visible" viewport={{ once: true, margin: '-20px' }}>
                        <Link to={doc.to} className="docs-card-link" aria-label={`Open ${doc.title}`}>
                            <div className="docs-icon">{doc.icon}</div>
                            <div className="docs-card-content">
                                <div className="docs-card-title">{doc.title}</div>
                                <div className="docs-card-desc">{doc.desc}</div>
                            </div>
                        </Link>
                    </motion.div>
                ))}
            </div>

            <motion.div className="docs-cta" initial={{ opacity: 0 }} whileInView={{ opacity: 1 }} viewport={{ once: true }} transition={{ delay: 0.4 }}>
                <Link to="/docs" className="docs-cta-link">Browse documentation <HiArrowRight /></Link>
            </motion.div>
        </section>
    );
}
