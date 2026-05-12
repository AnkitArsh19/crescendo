import { motion } from 'framer-motion';
import {
    HiOutlineCube,
    HiOutlineLightningBolt,
    HiOutlineClock,
    HiOutlineLink,
    HiOutlineChartBar,
    HiOutlineTemplate,
} from 'react-icons/hi';
import './Features.css';

const features = [
    {
        icon: <HiOutlineCube />,
        title: 'Visual Workflow Builder',
        desc: 'Design complex workflows with an intuitive drag-and-drop canvas. No code required to get started.',
    },
    {
        icon: <HiOutlineLightningBolt />,
        title: 'Webhook Triggers',
        desc: 'Listen for events from any service via webhooks. Trigger workflows instantly when data arrives.',
    },
    {
        icon: <HiOutlineClock />,
        title: 'Scheduled Execution',
        desc: 'Run workflows on a schedule — cron expressions, intervals, or specific dates. Set it and forget it.',
    },
    {
        icon: <HiOutlineLink />,
        title: 'Integrations',
        desc: 'Connect with 50+ services out of the box. REST APIs, databases, messaging queues, and more.',
    },
    {
        icon: <HiOutlineChartBar />,
        title: 'Real-time Monitoring',
        desc: 'Track every execution with detailed logs, metrics, and alerts. Debug issues before they escalate.',
    },
    {
        icon: <HiOutlineTemplate />,
        title: 'Pre-built Templates',
        desc: 'Start fast with battle-tested templates for common automation patterns. Customize to fit your needs.',
    },
];

const cardVariants = {
    hidden: { opacity: 0, y: 50, rotateX: -8 },
    visible: (i) => ({
        opacity: 1,
        y: 0,
        rotateX: 0,
        transition: {
            duration: 0.8,
            delay: i * 0.08,
            ease: [0.22, 1, 0.36, 1],
        },
    }),
};

export default function Features() {
    return (
        <section className="features" id="features">
            <div className="features-header">
                <motion.p
                    className="section-label"
                    initial={{ opacity: 0 }}
                    whileInView={{ opacity: 1 }}
                    viewport={{ once: true, margin: '-50px' }}
                >
                    Features
                </motion.p>
                <motion.h2
                    className="section-title"
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true, margin: '-50px' }}
                    transition={{ duration: 0.6 }}
                >
                    Everything you need to <span className="font-serif" style={{ fontStyle: 'italic' }}>automate</span>
                </motion.h2>
                <motion.p
                    className="section-subtitle"
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true, margin: '-50px' }}
                    transition={{ duration: 0.6, delay: 0.1 }}
                >
                    Powerful primitives that compose into any workflow.
                    From simple data transforms to complex orchestrations.
                </motion.p>
            </div>

            <div className="features-grid">
                {features.map((f, i) => (
                    <motion.div
                        className="feature-card"
                        key={f.title}
                        custom={i}
                        variants={cardVariants}
                        initial="hidden"
                        whileInView="visible"
                        viewport={{ once: true, margin: '-30px' }}
                    >
                        <div className="feature-card-corner" />
                        <div className="feature-icon">{f.icon}</div>
                        <div className="feature-title">{f.title}</div>
                        <div className="feature-desc">{f.desc}</div>
                    </motion.div>
                ))}
            </div>
        </section>
    );
}
