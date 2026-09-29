import { Link } from 'react-router-dom';
import './LegalPage.css';

export default function AcceptableUsePage() {
    return (
        <div className="legal-page">
            <nav className="legal-page-nav">
                <Link to="/" className="legal-nav-brand">Crescendo</Link>
                <span className="legal-nav-sep">/</span>
                <span className="legal-nav-title">Acceptable Use &amp; Anti-Spam Policy</span>
                <Link to="/" className="legal-nav-back">← Back to Home</Link>
            </nav>

            <div className="legal-container">
                <div className="legal-header">
                    <div className="legal-badge">Policy &amp; Deliverability</div>
                    <h1 className="legal-title">Acceptable Use &amp; Anti-Spam Policy</h1>
                    <p className="legal-subtitle">
                        Rules, content boundaries, and email deliverability standards required to maintain a safe,
                        reputable platform for all Crescendo users and receiving networks.
                    </p>
                    <div className="legal-meta">
                        <span className="legal-meta-item">
                            <span className="legal-meta-dot" />
                            Effective: January 1, 2026
                        </span>
                        <span className="legal-meta-item">
                            <span className="legal-meta-dot" />
                            Standard: M3AAWG / CAN-SPAM / CASL
                        </span>
                    </div>
                </div>

                <div className="legal-highlight-box">
                    <strong>Zero-Tolerance Spam Policy:</strong> Crescendo does not permit the transmission of unsolicited bulk email.
                    You may only send campaigns and workflow notifications to recipients who have directly and verifiable opted in to receive
                    your communications. Violations result in immediate suspension of outbound sending privileges.
                </div>

                <div className="legal-content">
                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">1</span>
                            Scope and Application
                        </h2>
                        <p>
                            This Acceptable Use Policy (&ldquo;AUP&rdquo;) forms an integral part of your agreement with Crescendo.
                            It applies to all users, developer applications, webhook consumers, and outbound email services operated
                            under our domain verification system.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">2</span>
                            Email Deliverability &amp; Anti-Spam Standards
                        </h2>
                        <p>
                            When utilizing Crescendo to dispatch transactional emails, drip sequences, or broadcast campaigns:
                        </p>
                        <ul>
                            <li>
                                <strong>Lawful Opt-In Consent:</strong> Every contact in your Audience must have provided affirmative,
                                verifiable consent to receive messages from your specific brand (e.g. double opt-in web forms, customer checkouts).
                            </li>
                            <li>
                                <strong>Prohibited Contact Lists:</strong> You may not upload, import, or send to purchased, rented,
                                scraped, third-party appended, or publicly harvested email lists. The use of list brokers is grounds for termination.
                            </li>
                            <li>
                                <strong>Mandatory Unsubscribe Mechanism:</strong> All marketing broadcasts must include a clear, working,
                                one-click unsubscribe link in the email footer and honor RFC 8058 <code>List-Unsubscribe-Post</code> headers.
                                Opt-out requests must be processed immediately without requiring login credentials.
                            </li>
                            <li>
                                <strong>Accurate Sender Identity:</strong> Your &ldquo;From&rdquo; address, display name, and reply-to must
                                accurately represent your organization. Subject lines must reflect message content without deceptive clickbait.
                                A valid physical postal address must be included in outbound email footers (CAN-SPAM requirement).
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">3</span>
                            Deliverability Thresholds &amp; Automatic Throttling
                        </h2>
                        <p>
                            To protect shared sending IP pools, domain reputation, and deliverability rates across the platform, Crescendo
                            enforces automated deliverability thresholds:
                        </p>
                        <div className="legal-grid-cards">
                            <div className="legal-card">
                                <div className="legal-card-title">Bounce Rate Limit &lt; 5%</div>
                                <p className="legal-card-desc">
                                    Accounts with hard bounce rates exceeding 5% on active campaigns will be quarantined to protect sender IP
                                    hygiene and inbox placement.
                                </p>
                            </div>
                            <div className="legal-card">
                                <div className="legal-card-title">Spam Complaints &lt; 0.1%</div>
                                <p className="legal-card-desc">
                                    Spam complaint rates must remain strictly below 0.1% (1 complaint per 1,000 sent messages). Rates above 0.3%
                                    trigger immediate sending suspension.
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">4</span>
                            Prohibited Content &amp; Activities
                        </h2>
                        <p>
                            You agree not to use Crescendo to create, transmit, host, or execute workflows involving:
                        </p>
                        <ul>
                            <li>
                                <strong>Cybercrime &amp; Malware:</strong> Phishing campaigns, credential harvesting, malware distribution,
                                ransomware payload propagation, or command-and-control server orchestration.
                            </li>
                            <li>
                                <strong>Fraud &amp; Deception:</strong> Get-rich-quick programs, multi-level marketing (MLM), unlicensed cryptocurrency
                                pump-and-dump schemes, advance fee fraud, or deceptive identity spoofing.
                            </li>
                            <li>
                                <strong>Regulated Goods:</strong> Sale or promotion of illegal narcotics, unlicensed prescription pharmaceuticals,
                                counterfeit goods, or untraceable firearms.
                            </li>
                            <li>
                                <strong>Harassment &amp; Exploitation:</strong> Hate speech, doxxing, non-consensual surveillance, stalking,
                                or any content involving the sexual abuse or exploitation of minors (reported immediately to NCMEC and law enforcement).
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">5</span>
                            Platform &amp; API Integrity
                        </h2>
                        <p>
                            To ensure platform stability and equitable resource allocation across all tenants:
                        </p>
                        <ul>
                            <li>Do not attempt to circumvent or tamper with Redis Token Bucket rate limiters, token quotas, or tier limits.</li>
                            <li>Do not configure circular or recursive workflow loops that cause runaway resource exhaustion or unbounded queue spikes.</li>
                            <li>Do not perform automated security fuzzing, penetration tests, or volumetric load testing without advance written authorization.</li>
                            <li>Do not reverse-engineer, decompile, or harvest proprietary application logic from Crescendo binaries or services.</li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">6</span>
                            Reporting Violations &amp; Appeals
                        </h2>
                        <p>
                            If you detect abuse originating from Crescendo infrastructure or wish to appeal an automated deliverability quarantine:
                        </p>
                        <div className="legal-contact-box">
                            <span className="contact-label">Report Abuse &amp; Spam</span>
                            <a href="mailto:abuse@crescendo.run">abuse@crescendo.run</a>
                            <span className="contact-label" style={{ marginTop: 8 }}>Deliverability Appeals</span>
                            <a href="mailto:deliverability@crescendo.run">deliverability@crescendo.run</a>
                        </div>
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
