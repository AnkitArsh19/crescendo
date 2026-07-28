import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion, useMotionValue, useSpring } from 'framer-motion';
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
import { useTypewriter } from '../hooks/useTypewriter';
import { useTheme } from './ThemeContext';
import { AnimatedBeam } from './ui/AnimatedBeam';
import './Hero.css';

/* ── Animation variants ── */
const staggerContainer = {
    hidden: {},
    visible: { transition: { staggerChildren: 0.12, delayChildren: 0.2 } },
};

const fadeUp = {
    hidden: { opacity: 0, y: 32 },
    visible: {
        opacity: 1, y: 0,
        transition: { duration: 0.75, ease: [0.22, 1, 0.36, 1] },
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

/* ── Typewriter phrases ── */
const TYPEWRITER_PHRASES = [
    'workflows',
    'automations',
    'integrations',
    'pipelines',
    'campaigns',
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

/* ── Floating ambient orb ── */
function AmbientOrb({ className, delay = 0 }) {
    return (
        <motion.div
            className={`ambient-orb ${className}`}
            animate={{
                y: [0, -24, 0],
                x: [0, 12, -8, 0],
                scale: [1, 1.06, 0.96, 1],
            }}
            transition={{
                duration: 8 + delay,
                ease: 'easeInOut',
                repeat: Infinity,
                delay,
            }}
        />
    );
}

/* ── Cursor-follow glow on canvas ── */
function CanvasGlow({ containerRef }) {
    const mouseX = useMotionValue(0);
    const mouseY = useMotionValue(0);
    const springX = useSpring(mouseX, { damping: 30, stiffness: 200 });
    const springY = useSpring(mouseY, { damping: 30, stiffness: 200 });

    useEffect(() => {
        const el = containerRef.current;
        if (!el) return;
        const handleMove = (e) => {
            const rect = el.getBoundingClientRect();
            mouseX.set(e.clientX - rect.left);
            mouseY.set(e.clientY - rect.top);
        };
        el.addEventListener('mousemove', handleMove);
        return () => el.removeEventListener('mousemove', handleMove);
    }, [containerRef, mouseX, mouseY]);

    return (
        <motion.div
            className="canvas-cursor-glow"
            style={{ left: springX, top: springY }}
        />
    );
}

/* ── Animated counter stat ── */
function AnimatedStat({ target, suffix, label }) {
    const [count, setCount] = useState(0);
    const ref = useRef(null);
    const started = useRef(false);

    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting && !started.current) {
                    started.current = true;
                    const duration = 1800;
                    const steps = 60;
                    const increment = target / steps;
                    let current = 0;
                    const timer = setInterval(() => {
                        current = Math.min(current + increment, target);
                        setCount(Math.floor(current));
                        if (current >= target) clearInterval(timer);
                    }, duration / steps);
                }
            },
            { threshold: 0.5 }
        );
        if (ref.current) observer.observe(ref.current);
        return () => observer.disconnect();
    }, [target]);

    return (
        <div ref={ref} className="hero-stat">
            <span className="hero-stat-number">{count.toLocaleString()}{suffix}</span>
            <span className="hero-stat-label">{label}</span>
        </div>
    );
}

export default function Hero() {
    const wf = useWorkflowAnimation();
    const { displayText, isTyping } = useTypewriter(TYPEWRITER_PHRASES, 65, 35, 2000);
    const { theme } = useTheme();
    const canvasRef = useRef(null);
    const triggerRef = useRef(null);
    const action1Ref = useRef(null);
    const action2Ref = useRef(null);

    return (
        <section className="hero" id="hero">
            {/* ── Ambient background orbs ── */}
            <AmbientOrb className="orb-1" delay={0} />
            <AmbientOrb className="orb-2" delay={2.5} />
            <AmbientOrb className="orb-3" delay={1.2} />

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
                        <span className="hero-title-serif typewriter-word">
                            {displayText}
                            <span className={`typewriter-cursor ${isTyping ? 'blinking' : 'solid'}`}>|</span>
                        </span>
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

                    {/* ── Animated stats ── */}
                    <motion.div variants={fadeUp} className="hero-stats">
                        <AnimatedStat target={114} suffix="+" label="Integrations" />
                        <div className="hero-stat-divider" />
                        <AnimatedStat target={868} suffix="+" label="Actions" />
                        <div className="hero-stat-divider" />
                        <AnimatedStat target={99} suffix="%" label="Uptime SLA" />
                    </motion.div>
                </motion.div>

                {/* ── Right — Canvas Workflow ── */}
                <motion.div
                    className="hero-visual"
                    initial={{ opacity: 0, y: 30, scale: 0.97 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ duration: 1, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
                >
                    <div className="workflow-canvas" ref={canvasRef}>
                        {/* Cursor-follow glow */}
                        <CanvasGlow containerRef={canvasRef} />

                        {/* Dot grid */}
                        <div className="canvas-grid" />

                        {/* Header bar */}
                        <div className="canvas-header">
                            <div className="canvas-dots">
                                <span className="canvas-dot canvas-dot-red" />
                                <span className="canvas-dot canvas-dot-yellow" />
                                <span className="canvas-dot canvas-dot-green" />
                            </div>
                            <span className="canvas-filename">lead-follow-up.flow</span>
                            <span className="canvas-status">
                                <span className="canvas-status-dot" />
                                {wf.allDone ? 'Done' : 'Live'}
                            </span>
                        </div>

                        {/* Dynamic Animated Beams connecting workflow nodes in monochrome tones */}
                        <AnimatedBeam
                            containerRef={canvasRef}
                            fromRef={triggerRef}
                            toRef={action1Ref}
                            curvature={0}
                            duration={3.2}
                            gradientStartColor={theme === 'dark' ? '#71717a' : '#a1a1aa'}
                            gradientStopColor={theme === 'dark' ? '#ffffff' : '#18181b'}
                            pathColor={theme === 'dark' ? 'rgba(255, 255, 255, 0.14)' : 'rgba(0, 0, 0, 0.12)'}
                        />
                        <AnimatedBeam
                            containerRef={canvasRef}
                            fromRef={triggerRef}
                            toRef={action2Ref}
                            curvature={0}
                            duration={3.2}
                            delay={1.6}
                            gradientStartColor={theme === 'dark' ? '#71717a' : '#a1a1aa'}
                            gradientStopColor={theme === 'dark' ? '#ffffff' : '#18181b'}
                            pathColor={theme === 'dark' ? 'rgba(255, 255, 255, 0.14)' : 'rgba(0, 0, 0, 0.12)'}
                        />

                        {/* Nodes */}
                        <div className="canvas-nodes">
                            {/* Trigger — left */}
                            <div ref={triggerRef} className={`hero-wf-node hero-wf-node-trigger ${wf.triggerDone ? 'completed' : ''}`}>
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
                            <div ref={action1Ref} className={`hero-wf-node hero-wf-node-action-1 ${wf.action1Done ? 'completed' : ''}`}>
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
                            <div ref={action2Ref} className={`hero-wf-node hero-wf-node-action-2 ${wf.action2Done ? 'completed' : ''}`}>
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
