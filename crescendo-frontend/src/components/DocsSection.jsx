import { motion } from 'framer-motion';
import {
    HiOutlineBookOpen,
    HiOutlineAcademicCap,
    HiOutlineCode,
    HiOutlinePlay,
} from 'react-icons/hi';
import { HiArrowRight } from 'react-icons/hi';
import './DocsSection.css';

const docs = [
    {
        icon: <HiOutlinePlay />,
        title: 'Quick Start',
        desc: 'Get up and running in under 5 minutes with your first workflow.',
    },
    {
        icon: <HiOutlineBookOpen />,
        title: 'Documentation',
        desc: 'Comprehensive guides covering every feature and configuration option.',
    },
    {
        icon: <HiOutlineAcademicCap />,
        title: 'Tutorials',
        desc: 'Step-by-step tutorials from basic automations to advanced patterns.',
    },
    {
        icon: <HiOutlineCode />,
        title: 'API Reference',
        desc: 'Complete API documentation with examples for every endpoint.',
    },
];

const cardVariant = {
    hidden: { opacity: 0, y: 30 },
    visible: (i) => ({
        opacity: 1,
        y: 0,
        transition: {
            duration: 0.6,
            delay: i * 0.1,
            ease: [0.22, 1, 0.36, 1],
        },
    }),
};

export default function DocsSection() {
    return (
        <section className="docs-section" id="docs">
            <div className="docs-header">
                <motion.p
                    className="section-label"
                    initial={{ opacity: 0 }}
                    whileInView={{ opacity: 1 }}
                    viewport={{ once: true }}
                >
                    Resources
                </motion.p>
                <motion.h2
                    className="section-title"
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                >
                    Learn &amp; <span className="font-serif" style={{ fontStyle: 'italic' }}>explore</span>
                </motion.h2>
                <motion.p
                    className="section-subtitle"
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6, delay: 0.1 }}
                >
                    Everything you need to master Crescendo, from first steps to advanced patterns.
                </motion.p>
            </div>

            <div className="docs-grid">
                {docs.map((d, i) => (
                    <motion.div
                        className="docs-card"
                        key={d.title}
                        custom={i}
                        variants={cardVariant}
                        initial="hidden"
                        whileInView="visible"
                        viewport={{ once: true, margin: '-20px' }}
                    >
                        <div className="docs-icon">{d.icon}</div>
                        <div className="docs-card-content">
                            <div className="docs-card-title">{d.title}</div>
                            <div className="docs-card-desc">{d.desc}</div>
                        </div>
                    </motion.div>
                ))}
            </div>

            <motion.div
                className="docs-cta"
                initial={{ opacity: 0 }}
                whileInView={{ opacity: 1 }}
                viewport={{ once: true }}
                transition={{ delay: 0.4 }}
            >
                <a href="#" className="docs-cta-link">
                    Browse all resources <HiArrowRight />
                </a>
            </motion.div>
        </section>
    );
}
