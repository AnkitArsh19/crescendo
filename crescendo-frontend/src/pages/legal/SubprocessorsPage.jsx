import { Link } from 'react-router-dom';
import './LegalPage.css';

export default function SubprocessorsPage() {
    return (
        <div className="legal-page">
            <nav className="legal-page-nav">
                <Link to="/" className="legal-nav-brand">Crescendo</Link>
                <span className="legal-nav-sep">/</span>
                <span className="legal-nav-title">Sub-processors</span>
                <Link to="/" className="legal-nav-back">← Back to Home</Link>
            </nav>

            <div className="legal-container">
                <div className="legal-header">
                    <div className="legal-badge">Compliance &amp; GDPR Art. 28</div>
                    <h1 className="legal-title">Authorized Sub-processors</h1>
                    <p className="legal-subtitle">
                        An itemized disclosure of third-party service providers authorized to process personal data
                        on behalf of Crescendo and our enterprise customers.
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
                    <strong>Vendor Due Diligence:</strong> Before engaging any sub-processor, Crescendo conducts rigorous technical
                    and legal due diligence to evaluate their security posture, SOC 2 / ISO 27001 certifications, and data protection compliance.
                    All sub-processors are bound by written data processing agreements containing obligations substantially equivalent to
                    our own Data Processing Addendum (DPA).
                </div>

                <div className="legal-content">
                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">1</span>
                            What Is a Sub-processor?
                        </h2>
                        <p>
                            A sub-processor is a third-party vendor engaged by Crescendo who has or may have access to, or processes,
                            Customer Personal Data on our behalf. These vendors provide essential underlying infrastructure, computing power,
                            email delivery pipelines, or security telemetry necessary to operate the Crescendo platform.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">2</span>
                            Infrastructure Sub-processors
                        </h2>
                        <p>
                            These vendors provide the core hosting, database storage, and high-performance caching infrastructure for our platform:
                        </p>
                        <div className="legal-table-container">
                            <table className="legal-table">
                                <thead>
                                    <tr>
                                        <th>Sub-processor</th>
                                        <th>Service Description</th>
                                        <th>Processing Location</th>
                                        <th>Transfer Mechanism</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td><strong>Amazon Web Services, Inc. (AWS)</strong></td>
                                        <td>Cloud hosting, PostgreSQL persistence, and encrypted volume storage</td>
                                        <td>United States / European Union</td>
                                        <td>EU-US Data Privacy Framework / Standard Contractual Clauses (SCCs)</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Redis Ltd. (Redis Cloud)</strong></td>
                                        <td>In-memory distributed rate limiting queue state and ephemeral session caches</td>
                                        <td>United States / European Union</td>
                                        <td>Standard Contractual Clauses (SCCs)</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">3</span>
                            Feature &amp; Communications Sub-processors
                        </h2>
                        <p>
                            These vendors provide specialized functionality such as transactional email delivery, AI step orchestration, and fraud prevention:
                        </p>
                        <div className="legal-table-container">
                            <table className="legal-table">
                                <thead>
                                    <tr>
                                        <th>Sub-processor</th>
                                        <th>Service Description</th>
                                        <th>Processing Location</th>
                                        <th>Transfer Mechanism</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td><strong>Brevo (Sendinblue SAS)</strong></td>
                                        <td>Outbound transactional and broadcast email transmission gateway</td>
                                        <td>France (European Union)</td>
                                        <td>Direct EU Processing / GDPR Adequacy</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Google Cloud Platform (Gemini AI)</strong></td>
                                        <td>Stateless AI prompt completion nodes (Zero-data-retention enforcement)</td>
                                        <td>United States / Global</td>
                                        <td>EU-US Data Privacy Framework / SCCs</td>
                                    </tr>
                                    <tr>
                                        <td><strong>MaxMind, Inc.</strong></td>
                                        <td>IP geolocation intelligence for detecting unauthorized or anomalous logins</td>
                                        <td>United States</td>
                                        <td>Standard Contractual Clauses (SCCs)</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">4</span>
                            Sub-processors vs. User-Connected Integrations
                        </h2>
                        <p>
                            It is important to distinguish between <strong>Sub-processors</strong> and <strong>User-Connected Integrations</strong>:
                        </p>
                        <ul>
                            <li>
                                <strong>Sub-processors:</strong> Vendors engaged systematically by Crescendo to operate the core platform for all users
                                (e.g. AWS or Redis).
                            </li>
                            <li>
                                <strong>User-Connected Integrations:</strong> Third-party platforms (such as your Google Workspace, Slack team,
                                GitHub organization, or custom SMTP server) that you deliberately authorize and connect via OAuth or BYOK credentials.
                                When data is transferred to these services, Crescendo acts strictly as your agent executing your explicit automation
                                workflow instructions. Your relationship with those services is governed by your independent agreements with them.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">5</span>
                            Notification of New Sub-processors
                        </h2>
                        <p>
                            In accordance with our <Link to="/dpa" style={{ color: 'var(--text-accent, #fafafa)' }}>Data Processing Addendum</Link>,
                            Crescendo provides customers with at least <strong>30 calendar days&rsquo; prior notice</strong> before authorizing any new
                            sub-processor to handle Customer Personal Data.
                        </p>
                        <p>
                            To subscribe to proactive sub-processor change notifications, contact our privacy compliance team at{' '}
                            <a href="mailto:privacy@crescendo.run" style={{ color: 'var(--text-accent, #fafafa)' }}>
                                privacy@crescendo.run
                            </a> with the subject line <em>&ldquo;Subscribe to Sub-processor Notifications&rdquo;</em>.
                        </p>
                    </div>
                </div>

                <div className="legal-footer">
                    <span className="legal-footer-text">© 2026 Crescendo. All rights reserved.</span>
                    <div className="legal-footer-links">
                        <Link to="/privacy">Privacy Policy</Link>
                        <Link to="/terms">Terms of Service</Link>
                        <Link to="/cookies">Cookie Policy</Link>
                        <Link to="/dpa">DPA</Link>
                        <Link to="/security">Security</Link>
                        <Link to="/subprocessors">Sub-processors</Link>
                        <Link to="/acceptable-use">Acceptable Use</Link>
                        <Link to="/">Home</Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
