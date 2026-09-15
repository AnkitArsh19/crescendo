import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { FaGithub, FaLinkedin, FaXTwitter } from 'react-icons/fa6';
import { FaMediumM } from 'react-icons/fa';
import { HiOutlineMail } from 'react-icons/hi';
import './Footer.css';

export default function Footer() {
    return (
        <motion.footer
            className="footer"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: false, amount: 0.1 }}
            transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        >
            <div className="footer-inner">
                <div className="footer-grid">
                    <div className="footer-brand">
                        <div className="footer-brand-name">Crescendo</div>
                        <p className="footer-brand-desc">
                            A workflow automation platform with a catalog-driven builder,
                            asynchronous execution, and built-in transactional email.
                        </p>
                        <div className="footer-social">
                            <a href="https://github.com/AnkitArsh19/crescendo" className="footer-social-link" aria-label="Crescendo on GitHub" target="_blank" rel="noreferrer">
                                <FaGithub />
                            </a>
                        </div>
                    </div>

                    <div className="footer-column">
                        <div className="footer-column-title">Product</div>
                        <ul className="footer-links">
                            <li><a href="#features" className="footer-link">Features</a></li>
                            <li><a href="#downloads" className="footer-link">Desktop App</a></li>
                            <li><a href="#api" className="footer-link">Public API</a></li>
                            <li><Link to="/docs/api/apps" className="footer-link">App Catalog</Link></li>
                            <li><Link to="/dashboard/email" className="footer-link">CrescendoMail</Link></li>
                        </ul>
                    </div>

                    <div className="footer-column">
                        <div className="footer-column-title">Resources</div>
                        <ul className="footer-links">
                            <li><Link to="/docs" className="footer-link">Documentation</Link></li>
                            <li><Link to="/docs/authentication" className="footer-link">Authentication</Link></li>
                            <li><Link to="/docs/api/workflows" className="footer-link">API Reference</Link></li>
                            <li><a href="https://github.com/AnkitArsh19/crescendo-sdk" className="footer-link" target="_blank" rel="noreferrer">SDKs</a></li>
                        </ul>
                    </div>

                    <div className="footer-column">
                        <div className="footer-column-title">Account</div>
                        <ul className="footer-links">
                            <li><Link to="/register" className="footer-link">Create account</Link></li>
                            <li><Link to="/login" className="footer-link">Log in</Link></li>
                            <li><Link to="/dashboard/settings/developer-api" className="footer-link">API keys</Link></li>
                            <li><Link to="/developer" className="footer-link">Developer profile</Link></li>
                        </ul>
                    </div>
                </div>

                <div className="footer-team-section">
                    <div className="footer-team-label">Built by</div>
                    <div className="footer-team-grid">
                        <div className="footer-dev-card">
                            <div className="footer-dev-meta">
                                <span className="footer-dev-name">Ankit Arsh</span>
                                <span className="footer-dev-role">Core Platform, Backend & Frontend</span>
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
                                <span className="footer-dev-role">AI / ML & Conversational Synthesis</span>
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
                </div>

                <div className="footer-bottom">
                    <div className="footer-copyright">© 2026 Crescendo. All rights reserved.</div>
                    <div className="footer-legal">
                        <Link to="/privacy">Privacy Policy</Link>
                        <Link to="/terms">Terms of Service</Link>
                    </div>
                </div>
            </div>
        </motion.footer>
    );
}
