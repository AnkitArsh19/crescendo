import { motion } from 'framer-motion';
import { HiOutlineGlobeAlt } from 'react-icons/hi';
import { FaGithub, FaDiscord } from 'react-icons/fa';
import { FaXTwitter } from 'react-icons/fa6';
import './Footer.css';

export default function Footer() {
    return (
        <motion.footer
            className="footer"
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
        >
            <div className="footer-inner">
                <div className="footer-grid">
                    <div className="footer-brand">
                        <div className="footer-brand-name">Crescendo</div>
                        <p className="footer-brand-desc">
                            The workflow automation platform that grows with you.
                            Build, deploy, and monitor with confidence.
                        </p>
                        <div className="footer-social">
                            <a href="#" className="footer-social-link" aria-label="GitHub">
                                <FaGithub />
                            </a>
                            <a href="#" className="footer-social-link" aria-label="Twitter">
                                <FaXTwitter />
                            </a>
                            <a href="#" className="footer-social-link" aria-label="Discord">
                                <FaDiscord />
                            </a>
                            <a href="#" className="footer-social-link" aria-label="Website">
                                <HiOutlineGlobeAlt />
                            </a>
                        </div>
                    </div>

                    <div className="footer-column">
                        <div className="footer-column-title">Product</div>
                        <ul className="footer-links">
                            <li><a href="#features" className="footer-link">Features</a></li>
                            <li><a href="#api" className="footer-link">API</a></li>
                            <li><a href="#" className="footer-link">Integrations</a></li>
                            <li><a href="#" className="footer-link">Changelog</a></li>
                        </ul>
                    </div>

                    <div className="footer-column">
                        <div className="footer-column-title">Resources</div>
                        <ul className="footer-links">
                            <li><a href="#docs" className="footer-link">Documentation</a></li>
                            <li><a href="#" className="footer-link">Tutorials</a></li>
                            <li><a href="#" className="footer-link">Blog</a></li>
                            <li><a href="#" className="footer-link">Community</a></li>
                        </ul>
                    </div>

                    <div className="footer-column">
                        <div className="footer-column-title">Company</div>
                        <ul className="footer-links">
                            <li><a href="#" className="footer-link">About</a></li>
                            <li><a href="#" className="footer-link">Careers</a></li>
                            <li><a href="#" className="footer-link">Contact</a></li>
                            <li><a href="#" className="footer-link">Status</a></li>
                        </ul>
                    </div>
                </div>

                <div className="footer-bottom">
                    <div className="footer-copyright">
                        © 2026 Crescendo. All rights reserved.
                    </div>
                    <div className="footer-legal">
                        <a href="https://app.crescendo.run/privacy/">Privacy Policy</a>
                        <a href="https://app.crescendo.run/terms/">Terms of Service</a>
                    </div>
                </div>
            </div>
        </motion.footer>
    );
}
