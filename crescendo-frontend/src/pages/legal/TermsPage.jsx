import { Link } from 'react-router-dom';
import './LegalPage.css';

export default function TermsPage() {
    return (
        <div className="legal-page">
            <nav className="legal-page-nav">
                <Link to="/" className="legal-nav-brand">Crescendo</Link>
                <span className="legal-nav-sep">/</span>
                <span className="legal-nav-title">Terms of Service</span>
                <Link to="/" className="legal-nav-back">← Back to Home</Link>
            </nav>

            <div className="legal-container">
                <div className="legal-header">
                    <div className="legal-badge">Legal</div>
                    <h1 className="legal-title">Terms of Service</h1>
                    <p className="legal-subtitle">
                        Please read these terms carefully before using the Crescendo platform.
                        By accessing or using our services, you agree to be bound by these terms.
                    </p>
                    <div className="legal-meta">
                        <span className="legal-meta-item">
                            <span className="legal-meta-dot" />
                            Effective: January 1, 2026
                        </span>
                        <span className="legal-meta-item">
                            <span className="legal-meta-dot" />
                            Last updated: March 1, 2026
                        </span>
                    </div>
                </div>

                <div className="legal-highlight-box">
                    <strong>Summary:</strong> Crescendo provides a workflow automation platform.
                    You must use it lawfully, keep your credentials secure, and respect the rights
                    of others. We may suspend accounts that violate these terms or applicable law.
                </div>

                <div className="legal-content">
                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">1</span>
                            Acceptance of Terms
                        </h2>
                        <p>
                            By creating an account or using any part of the Crescendo service
                            (&ldquo;Service&rdquo;), you agree to these Terms of Service
                            (&ldquo;Terms&rdquo;) and our Privacy Policy. If you are using the
                            Service on behalf of an organization, you represent that you have the
                            authority to bind that organization to these Terms.
                        </p>
                        <p>
                            We may update these Terms at any time. We will notify you of material
                            changes by email or via an in-app notice. Continued use of the Service
                            after the effective date of updated Terms constitutes acceptance.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">2</span>
                            Description of Service
                        </h2>
                        <p>
                            Crescendo is a workflow automation platform that allows you to connect
                            third-party applications and services, create automated workflows
                            (triggers and actions), send transactional and broadcast emails, and
                            monitor execution history.
                        </p>
                        <p>
                            The Service is provided on an &ldquo;as is&rdquo; and &ldquo;as
                            available&rdquo; basis. We reserve the right to modify, suspend, or
                            discontinue any part of the Service at any time with reasonable notice.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">3</span>
                            Accounts and Registration
                        </h2>
                        <ul>
                            <li>You must be at least 13 years of age to use the Service.</li>
                            <li>
                                You are responsible for maintaining the confidentiality of your
                                account credentials and for all activity that occurs under your
                                account.
                            </li>
                            <li>
                                You must provide accurate and complete registration information and
                                keep it up to date.
                            </li>
                            <li>
                                You must notify us immediately of any unauthorized use of your
                                account at <a href="mailto:support@crescendo.app">support@crescendo.app</a>.
                            </li>
                            <li>
                                You may not share your account credentials with third parties or
                                create accounts by automated means.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">4</span>
                            Acceptable Use
                        </h2>
                        <p>You agree not to use the Service to:</p>
                        <ul>
                            <li>Violate any applicable law or regulation.</li>
                            <li>
                                Send spam, unsolicited messages, or engage in any form of
                                unauthorized advertising.
                            </li>
                            <li>
                                Transmit malware, viruses, or any code of a destructive nature.
                            </li>
                            <li>
                                Interfere with or disrupt the integrity or performance of the
                                Service or its underlying infrastructure.
                            </li>
                            <li>
                                Attempt to gain unauthorized access to any part of the Service or
                                other users&rsquo; accounts.
                            </li>
                            <li>
                                Reverse engineer, decompile, or disassemble any portion of the
                                Service.
                            </li>
                            <li>
                                Use the Service to process or store data in a manner that violates
                                the rights of any third party, including intellectual property rights
                                and privacy rights.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">5</span>
                            Third-Party Integrations
                        </h2>
                        <p>
                            The Service allows you to connect third-party applications (e.g.,
                            Google, Microsoft, Twitter/X). By connecting such services, you
                            authorize Crescendo to interact with them on your behalf using the
                            permissions you grant.
                        </p>
                        <p>
                            Crescendo is not responsible for the availability, accuracy, or
                            policies of any third-party service. Your use of third-party services
                            is subject to their respective terms and privacy policies.
                        </p>
                        <p>
                            You may revoke access to connected accounts at any time through the
                            Connections settings page or directly through the third-party
                            service&rsquo;s account settings.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">6</span>
                            Intellectual Property
                        </h2>
                        <p>
                            The Service, including all software, design, text, and graphics, is
                            owned by Crescendo and protected by applicable intellectual property
                            laws. You are granted a limited, non-exclusive, non-transferable
                            license to use the Service solely for its intended purpose.
                        </p>
                        <p>
                            You retain ownership of any data, workflows, or content you create
                            using the Service (&ldquo;User Content&rdquo;). By using the Service,
                            you grant Crescendo a limited license to process your User Content
                            solely to provide and improve the Service.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">7</span>
                            Data and Privacy
                        </h2>
                        <p>
                            Your use of the Service is also governed by our{' '}
                            <Link to="/privacy" style={{ color: 'var(--text-accent, #fafafa)' }}>
                                Privacy Policy
                            </Link>
                            , which is incorporated into these Terms by reference. Please review
                            it carefully to understand how we collect, use, and protect your
                            information.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">8</span>
                            Disclaimers and Limitation of Liability
                        </h2>
                        <p>
                            THE SERVICE IS PROVIDED &ldquo;AS IS&rdquo; WITHOUT WARRANTIES OF ANY
                            KIND, EXPRESS OR IMPLIED. CRESCENDO DOES NOT WARRANT THAT THE SERVICE
                            WILL BE UNINTERRUPTED, ERROR-FREE, OR SECURE.
                        </p>
                        <p>
                            TO THE MAXIMUM EXTENT PERMITTED BY LAW, CRESCENDO SHALL NOT BE LIABLE
                            FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE
                            DAMAGES ARISING OUT OF OR RELATED TO YOUR USE OF THE SERVICE, EVEN IF
                            ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">9</span>
                            Termination
                        </h2>
                        <p>
                            You may terminate your account at any time by contacting us or through
                            your account settings. We may suspend or terminate your access to the
                            Service immediately if you violate these Terms or if we are required to
                            do so by law.
                        </p>
                        <p>
                            Upon termination, your right to use the Service ceases immediately.
                            Provisions that by their nature should survive termination will remain
                            in effect.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">10</span>
                            Governing Law
                        </h2>
                        <p>
                            These Terms are governed by and construed in accordance with applicable
                            law. Any disputes arising under these Terms shall be subject to the
                            exclusive jurisdiction of the competent courts, unless otherwise agreed
                            in writing.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">11</span>
                            Contact Us
                        </h2>
                        <p>If you have any questions about these Terms, please contact us:</p>
                        <div className="legal-contact-box">
                            <span className="contact-label">Email</span>
                            <a href="mailto:legal@crescendo.app">legal@crescendo.app</a>
                            <span className="contact-label" style={{ marginTop: 8 }}>Support</span>
                            <a href="mailto:support@crescendo.app">support@crescendo.app</a>
                        </div>
                    </div>
                </div>

                <div className="legal-footer">
                    <span className="legal-footer-text">© 2026 Crescendo. All rights reserved.</span>
                    <div className="legal-footer-links">
                        <Link to="/privacy">Privacy Policy</Link>
                        <Link to="/terms">Terms of Service</Link>
                        <Link to="/">Home</Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
