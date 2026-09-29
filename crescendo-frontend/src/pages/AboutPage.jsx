import { Link } from 'react-router-dom';
import { FaGithub, FaLinkedin, FaMediumM, FaBolt, FaLock, FaShieldAlt, FaCheck } from 'react-icons/fa';
import { FaXTwitter } from 'react-icons/fa6';
import { HiOutlineMail } from 'react-icons/hi';
import './AboutPage.css';

export default function AboutPage() {
    return (
        <div className="about-page">
            <nav className="about-nav">
                <Link to="/" className="about-nav-brand">Crescendo</Link>
                <span className="about-nav-sep">/</span>
                <span className="about-nav-title">About &amp; Story</span>
                <Link to="/" className="about-nav-back">← Back to Home</Link>
            </nav>

            <main className="about-container">
                <header className="about-hero">
                    <div className="about-badge">Our Story &amp; Vision</div>
                    <h1 className="about-title">Why We Built Crescendo</h1>
                    <p className="about-subtitle">
                        Workflow automation shouldn&rsquo;t be an overpriced black box. We engineered Crescendo to deliver
                        enterprise-grade concurrency, zero-knowledge secret security, and transparent execution for every developer.
                    </p>
                </header>

                <div className="about-grid-manifesto">
                    <div className="about-manifesto-card">
                        <span className="about-manifesto-icon"><FaBolt style={{ color: '#fbbf24' }} /></span>
                        <h3 className="about-manifesto-title">Java 25 &amp; Virtual Threads</h3>
                        <p className="about-manifesto-desc">
                            High-throughput non-blocking architecture using JDK 25 Project Loom virtual threads and Redis Streams,
                            processing thousands of concurrent events without memory exhaustion.
                        </p>
                    </div>
                    <div className="about-manifesto-card">
                        <span className="about-manifesto-icon"><FaLock style={{ color: '#a78bfa' }} /></span>
                        <h3 className="about-manifesto-title">Zero-Knowledge Secrets</h3>
                        <p className="about-manifesto-desc">
                            Authenticated AES-256-GCM envelope encryption for all BYOK credentials. Decrypted secrets exist solely in
                            volatile RAM during live API step dispatches and are never logged.
                        </p>
                    </div>
                    <div className="about-manifesto-card">
                        <span className="about-manifesto-icon"><FaShieldAlt style={{ color: '#60a5fa' }} /></span>
                        <h3 className="about-manifesto-title">Atomic Distributed Limiting</h3>
                        <p className="about-manifesto-desc">
                            Redis Lua Token Bucket rate limiting ensures equitable traffic distribution, preventing denial-of-service
                            and eliminating distributed concurrency race conditions.
                        </p>
                    </div>
                </div>

                <div className="about-story-section">
                    <h2>The Genesis</h2>
                    <p>
                        Crescendo began with a simple frustration: modern workflow automation platforms either charge exorbitant
                        subscription fees for basic task executions or require maintaining complicated, brittle multi-container
                        setups with zero built-in email or DNS authentication capabilities.
                    </p>
                    <p>
                        We set out to build a platform that bridges this divide: an intuitive, catalog-driven visual canvas paired
                        with an uncompromising systems engineering foundation. Built from India with a global engineering standard,
                        Crescendo incorporates 114+ native app integrations, a dual CQRS database architecture, and native passkey
                        authentication (WebAuthn/FIDO2).
                    </p>

                    <div className="about-guarantee-box">
                        <h3 className="about-guarantee-title">
                            <FaShieldAlt style={{ color: '#a78bfa' }} /> The Crescendo Platform Guarantees
                        </h3>
                        <ul className="about-guarantee-list">
                            <li className="about-guarantee-item">
                                <FaCheck className="about-guarantee-check" />
                                <div><strong>Zero AI Model Training:</strong> Your automation DAG schemas, prompt inputs, and execution logs are never used to train or refine generative AI foundation models.</div>
                            </li>
                            <li className="about-guarantee-item">
                                <FaCheck className="about-guarantee-check" />
                                <div><strong>Zero Data Lock-In:</strong> Export your full workspace definitions, workflows, contact rosters, and execution logbooks in open JSON and CSV formats at any time.</div>
                            </li>
                            <li className="about-guarantee-item">
                                <FaCheck className="about-guarantee-check" />
                                <div><strong>No Advertising Trackers:</strong> We deploy strictly essential first-party cookies for session security and zero third-party cross-site advertising trackers.</div>
                            </li>
                            <li className="about-guarantee-item">
                                <FaCheck className="about-guarantee-check" />
                                <div><strong>High Reliability Target:</strong> Clustered execution queue consumers with manual Redis Stream acknowledgment guarantee that workflow events are never dropped silently.</div>
                            </li>
                        </ul>
                    </div>

                    <h2>The Engineering Team</h2>
                    <p>
                        Crescendo is architected and built by independent Indian software engineers passionate about distributed systems,
                        clean code, and developer ergonomics:
                    </p>
                </div>

                <div className="footer-team-grid" style={{ marginBottom: 48 }}>
                    <div className="footer-dev-card">
                        <div className="footer-dev-meta">
                            <span className="footer-dev-name">Ankit Arsh</span>
                            <span className="footer-dev-role">Core Platform, Distributed Backend &amp; Architecture</span>
                        </div>
                        <div className="footer-dev-links">
                            <a href="https://github.com/AnkitArsh19" className="footer-dev-icon-btn" aria-label="Ankit on GitHub" target="_blank" rel="noreferrer" title="GitHub">
                                <FaGithub />
                            </a>
                            <a href="https://www.linkedin.com/in/ankitarsh19/" className="footer-dev-icon-btn" aria-label="Ankit on LinkedIn" target="_blank" rel="noreferrer" title="LinkedIn">
                                <FaLinkedin />
                            </a>
                            <a href="https://x.com/AnkitArsh19" className="footer-dev-icon-btn" aria-label="Ankit on X (Twitter)" target="_blank" rel="noreferrer" title="X (Twitter)">
                                <FaXTwitter />
                            </a>
                            <a href="https://ankitarsh19.medium.com/" className="footer-dev-icon-btn" aria-label="Ankit on Medium" target="_blank" rel="noreferrer" title="Medium">
                                <FaMediumM />
                            </a>
                            <a href="mailto:ankitarsh19@gmail.com" className="footer-dev-icon-btn" aria-label="Email Ankit" title="Email">
                                <HiOutlineMail />
                            </a>
                        </div>
                    </div>

                    <div className="footer-dev-card">
                        <div className="footer-dev-meta">
                            <span className="footer-dev-name">Rishika Sarma</span>
                            <span className="footer-dev-role">AI / ML &amp; Conversational Synthesis</span>
                        </div>
                        <div className="footer-dev-links">
                            <a href="https://github.com/Reql75" className="footer-dev-icon-btn" aria-label="Rishika on GitHub" target="_blank" rel="noreferrer" title="GitHub">
                                <FaGithub />
                            </a>
                            <a href="https://www.linkedin.com/in/rishikasarma75/" className="footer-dev-icon-btn" aria-label="Rishika on LinkedIn" target="_blank" rel="noreferrer" title="LinkedIn">
                                <FaLinkedin />
                            </a>
                        </div>
                    </div>
                </div>

                <div className="legal-footer">
                    <span className="legal-footer-text">© 2026 Crescendo. All rights reserved.</span>
                    <div className="legal-footer-links">
                        <Link to="/about">About</Link>
                        <Link to="/privacy">Privacy Policy</Link>
                        <Link to="/terms">Terms of Service</Link>
                        <Link to="/cookies">Cookie Policy</Link>
                        <Link to="/security">Security</Link>
                        <Link to="/dpa">DPA</Link>
                        <Link to="/subprocessors">Sub-processors</Link>
                        <Link to="/acceptable-use">Acceptable Use</Link>
                        <Link to="/">Home</Link>
                    </div>
                </div>
            </main>
        </div>
    );
}
