import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
    HiArrowRight,
    HiOutlineLightningBolt,
    HiOutlineMail,
    HiOutlineDatabase,
    HiOutlineClock,
    HiOutlineGlobe,
    HiCheck,
} from 'react-icons/hi';
import {
    SiDiscord, SiGmail, SiPostgresql, SiMongodb,
    SiGithub, SiRedis,
} from 'react-icons/si';
import { FaSlack } from 'react-icons/fa';
import './Hero.css';

/* ── Animation variants ── */
const staggerContainer = {
    hidden: {},
    visible: { transition: { staggerChildren: 0.12, delayChildren: 0.2 } },
};

const fadeUp = {
    hidden: { opacity: 0, y: 30 },
    visible: {
        opacity: 1, y: 0,
        transition: { duration: 0.7, ease: [0.22, 1, 0.36, 1] },
    },
};

/* ── Integration icons ── */
const integrations = [
    { icon: <FaSlack />, name: 'Slack' },
    { icon: <SiDiscord />, name: 'Discord' },
    { icon: <SiGmail />, name: 'Gmail' },
    { icon: <SiPostgresql />, name: 'PostgreSQL' },
    { icon: <SiMongodb />, name: 'MongoDB' },
    { icon: <SiGithub />, name: 'GitHub' },
    { icon: <HiOutlineMail />, name: 'CrescendoMail' },
    { icon: <SiRedis />, name: 'Redis' },
    { icon: <HiOutlineGlobe />, name: 'Webhooks' },
    { icon: <HiOutlineClock />, name: 'Schedules' },
];

/* ── Workflow execution cycle ── */
function useWorkflowAnimation() {
    const [step, setStep] = useState(0);
    useEffect(() => {
        const delays = [2200, 1800, 1400, 1400, 2800];
        const t = setTimeout(() => setStep(s => (s >= 4 ? 0 : s + 1)), delays[step]);
        return () => clearTimeout(t);
    }, [step]);
    return {
        triggerDone: step >= 1,
        action1Done: step >= 2,
        action2Done: step >= 3,
        allDone: step >= 4,
        flowActive: step >= 1 && step < 4,
    };
}

/* ── Animated dot traveling along an SVG path ── */
function TravelingDot({ pathId, active, delay = 0, duration = 1.5 }) {
    const dotRef = useRef(null);
    const reqRef = useRef(null);

    useEffect(() => {
        if (!active) {
            if (dotRef.current) dotRef.current.setAttribute('opacity', '0');
            return;
        }
        const path = document.getElementById(pathId);
        if (!path) return;
        const length = path.getTotalLength();
        let start = null;

        const animate = (timestamp) => {
            if (!start) start = timestamp;
            const elapsed = (timestamp - start - delay) / 1000;
            if (elapsed < 0) {
                reqRef.current = requestAnimationFrame(animate);
                return;
            }
            const progress = (elapsed % duration) / duration;
            const point = path.getPointAtLength(progress * length);
            if (dotRef.current) {
                dotRef.current.setAttribute('cx', point.x);
                dotRef.current.setAttribute('cy', point.y);
                dotRef.current.setAttribute('opacity', progress < 0.05 || progress > 0.95 ? '0' : '1');
            }
            reqRef.current = requestAnimationFrame(animate);
        };
        reqRef.current = requestAnimationFrame(animate);
        return () => cancelAnimationFrame(reqRef.current);
    }, [active, pathId, delay, duration]);

    return <circle ref={dotRef} className="flow-dot" r="3.5" opacity="0" />;
}

export default function Hero() {
    const wf = useWorkflowAnimation();

    // SVG path coordinates (within viewBox 560 x 350-ish, offset by header 36px)
    // Trigger center-right: ~180, 157 (center of canvas area)
    // Action1 center-left: ~385, 85
    // Action2 center-left: ~385, 229
    const pathTop = "M 180 157 C 260 157, 310 85, 385 85";
    const pathBottom = "M 180 157 C 260 157, 310 229, 385 229";

    return (
        <section className="hero" id="hero">
            <div className="hero-inner">
                {/* ── Left — Text ── */}
                <motion.div
                    className="hero-content"
                    variants={staggerContainer}
                    initial="hidden"
                    animate="visible"
                >
                    <motion.div variants={fadeUp} className="hero-badge">
                        <span className="hero-badge-dot" />
                        Workflow automation
                    </motion.div>

                    <motion.h1 variants={fadeUp} className="hero-title">
                        Automate your
                        <br />
                        <span className="hero-title-serif">workflows</span>
                        <br />
                        with confidence
                    </motion.h1>

                    <motion.p variants={fadeUp} className="hero-subtitle">
                        Build, run, and monitor workflows across your stack.
                    </motion.p>

                    <motion.div variants={fadeUp} className="hero-cta">
                        <Link to="/register" className="hero-btn-primary">
                            Get Started <HiArrowRight />
                        </Link>
                        <Link to="/docs" className="hero-btn-secondary">View Documentation</Link>
                    </motion.div>
                </motion.div>

                {/* ── Right — Canvas Workflow ── */}
                <motion.div
                    className="hero-visual"
                    initial={{ opacity: 0, y: 30, scale: 0.97 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ duration: 1, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
                >
                    <div className="workflow-canvas">
                        {/* Dot grid */}
                        <div className="canvas-grid" />

                        {/* Header bar */}
                        <div className="canvas-header">
                            <div className="canvas-dots">
                                <span className="canvas-dot" />
                                <span className="canvas-dot" />
                                <span className="canvas-dot" />
                            </div>
                            <span className="canvas-filename">lead-follow-up.flow</span>
                            <span className="canvas-status">
                                <span className="canvas-status-dot" />
                                {wf.allDone ? 'Done' : 'Live'}
                            </span>
                        </div>

                        {/* SVG connector paths */}
                        <svg className="canvas-connectors" viewBox="0 0 560 314" preserveAspectRatio="none">
                            {/* Static paths */}
                            <path d={pathTop} className="connector-path" />
                            <path d={pathBottom} className="connector-path" />

                            {/* Animated dash overlay */}
                            <path
                                d={pathTop}
                                className={`connector-pulse ${wf.flowActive ? 'active' : ''}`}
                            />
                            <path
                                d={pathBottom}
                                className={`connector-pulse ${wf.flowActive ? 'active' : ''}`}
                            />

                            {/* Traveling dots */}
                            <TravelingDot pathId="path-top-hidden" active={wf.flowActive} delay={0} duration={2} />
                            <TravelingDot pathId="path-bottom-hidden" active={wf.flowActive} delay={400} duration={2} />

                            {/* Hidden paths for getPointAtLength */}
                            <path id="path-top-hidden" d={pathTop} fill="none" stroke="none" />
                            <path id="path-bottom-hidden" d={pathBottom} fill="none" stroke="none" />
                        </svg>

                        {/* Nodes */}
                        <div className="canvas-nodes">
                            {/* Trigger — left */}
                            <div className={`hero-wf-node hero-wf-node-trigger ${wf.triggerDone ? 'completed' : ''}`}>
                                <div className="hero-wf-node-icon">
                                    <HiOutlineLightningBolt />
                                </div>
                                <div className="hero-wf-node-info">
                                    <div className="hero-wf-node-label">Trigger</div>
                                    <div className="hero-wf-node-name">Webhook</div>
                                    <div className="hero-wf-node-detail">POST /hooks/new-lead</div>
                                </div>
                                <div className={`hero-wf-node-check ${wf.triggerDone ? 'done' : ''}`}>
                                    {wf.triggerDone && <HiCheck />}
                                </div>
                            </div>

                            {/* Action 1 — top right */}
                            <div className={`hero-wf-node hero-wf-node-action-1 ${wf.action1Done ? 'completed' : ''}`}>
                                <div className="hero-wf-node-icon">
                                    <HiOutlineMail />
                                </div>
                                <div className="hero-wf-node-info">
                                    <div className="hero-wf-node-name">CrescendoMail</div>
                                    <div className="hero-wf-node-detail">
                                        {wf.action1Done ? 'Queued' : wf.triggerDone ? 'Sending...' : 'Idle'}
                                    </div>
                                </div>
                                <div className={`hero-wf-node-check ${wf.action1Done ? 'done' : ''}`}>
                                    {wf.action1Done && <HiCheck />}
                                </div>
                            </div>

                            {/* Action 2 — bottom right */}
                            <div className={`hero-wf-node hero-wf-node-action-2 ${wf.action2Done ? 'completed' : ''}`}>
                                <div className="hero-wf-node-icon">
                                    <HiOutlineDatabase />
                                </div>
                                <div className="hero-wf-node-info">
                                    <div className="hero-wf-node-name">Google Sheets</div>
                                    <div className="hero-wf-node-detail">
                                        {wf.action2Done ? 'Updated' : wf.action1Done ? 'Writing...' : 'Idle'}
                                    </div>
                                </div>
                                <div className={`hero-wf-node-check ${wf.action2Done ? 'done' : ''}`}>
                                    {wf.action2Done && <HiCheck />}
                                </div>
                            </div>
                        </div>
                    </div>
                </motion.div>
            </div>

            {/* ── Integration Marquee ── */}
            <motion.div
                className="integrations-strip"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 1.2, duration: 0.8 }}
            >
                <div className="integrations-label">Works with your stack</div>
                <div className="integrations-track">
                    {[...integrations, ...integrations].map((item, i) => (
                        <div className="integration-item" key={i}>
                            <span className="integration-icon">{item.icon}</span>
                            <span className="integration-name">{item.name}</span>
                        </div>
                    ))}
                </div>
            </motion.div>
        </section>
    );
}
