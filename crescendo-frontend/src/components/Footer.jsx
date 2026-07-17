import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { FaGithub } from 'react-icons/fa';
import './Footer.css';

export default function Footer() {
    return (
        <motion.footer className="footer" initial={{ opacity: 0 }} whileInView={{ opacity: 1 }} viewport={{ once: true }} transition={{ duration: 0.8 }}>
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
                            <li><Link to="/settings/developer-api" className="footer-link">API keys</Link></li>
                            <li><Link to="/developer" className="footer-link">Developer profile</Link></li>
                        </ul>
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
