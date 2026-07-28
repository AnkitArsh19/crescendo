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
        desc: 'Compose triggers and actions on the visual canvas, with each app exposing its own configuration schema.',
    },
    {
        icon: <HiOutlineLightningBolt />,
        title: 'Webhook Workflows',
        desc: 'Receive incoming HTTP requests, choose accepted methods and a response mode, then continue the workflow.',
    },
    {
        icon: <HiOutlineClock />,
        title: 'Schedule Triggers',
        desc: 'Start workflows with Spring cron expressions or fixed intervals for recurring jobs and checks.',
    },
    {
        icon: <HiOutlineLink />,
        title: 'Catalog-driven Apps',
        desc: 'Connect the 114 app definitions in the catalog, including Google, Microsoft, GitHub, databases, AI, and HTTP tools.',
    },
    {
        icon: <HiOutlineChartBar />,
        title: 'Execution Visibility',
        desc: 'Inspect workflow runs, step logs, statuses, and failures from the dashboard and public API.',
    },
    {
        icon: <HiOutlineTemplate />,
        title: 'CrescendoMail',
        desc: 'Send transactional or marketing email with verified domains, templates, audiences, suppressions, and delivery events.',
    },
];

const containerVariants = {
    hidden: {},
    visible: {
        transition: {
            staggerChildren: 0.08,
            delayChildren: 0.1,
        },
    },
};

const cardVariants = {
    hidden: { opacity: 0, y: 36, scale: 0.96 },
    visible: {
        opacity: 1,
        y: 0,
        scale: 1,
        transition: { duration: 0.65, ease: [0.22, 1, 0.36, 1] },
    },
};

export default function Features() {
    return (
        <section className="features" id="features">
            <div className="features-header">
                <motion.p
                    className="section-label"
                    initial={{ opacity: 0, y: 12 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: false, amount: 0.3 }}
                    transition={{ duration: 0.5 }}
                >
                    Features
                </motion.p>
                <motion.h2
                    className="section-title"
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: false, amount: 0.3 }}
                    transition={{ duration: 0.6, delay: 0.08 }}
                >
                    Everything you need to <span className="font-serif" style={{ fontStyle: 'italic' }}>automate</span>
                </motion.h2>
                <motion.p
                    className="section-subtitle"
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: false, amount: 0.3 }}
                    transition={{ duration: 0.6, delay: 0.16 }}
                >
                    A catalog-driven builder, dependable execution pipeline, and built-in email platform
                    for automations that need more than a single request.
                </motion.p>
            </div>

            <motion.div
                className="features-grid"
                variants={containerVariants}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: false, amount: 0.2 }}
            >
                {features.map((feature) => (
                    <motion.div
                        className="feature-card"
                        key={feature.title}
                        variants={cardVariants}
                        whileHover={{ y: -6, transition: { duration: 0.25, ease: 'easeOut' } }}
                    >
                        <div className="feature-card-corner" />
                        <div className="feature-icon">{feature.icon}</div>
                        <div className="feature-title">{feature.title}</div>
                        <div className="feature-desc">{feature.desc}</div>
                    </motion.div>
                ))}
            </motion.div>
        </section>
    );
}
