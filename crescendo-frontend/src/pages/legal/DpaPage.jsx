import { Link } from 'react-router-dom';
import './LegalPage.css';

export default function DpaPage() {
    return (
        <div className="legal-page">
            <nav className="legal-page-nav">
                <Link to="/" className="legal-nav-brand">Crescendo</Link>
                <span className="legal-nav-sep">/</span>
                <span className="legal-nav-title">Data Processing Addendum (DPA)</span>
                <Link to="/" className="legal-nav-back">← Back to Home</Link>
            </nav>

            <div className="legal-container">
                <div className="legal-header">
                    <div className="legal-badge">Enterprise &amp; Compliance</div>
                    <h1 className="legal-title">Data Processing Addendum</h1>
                    <p className="legal-subtitle">
                        Standard contractual terms governing the processing of personal data under the European General Data Protection
                        Regulation (GDPR), UK GDPR, and California Consumer Privacy Act (CCPA/CPRA).
                    </p>
                    <div className="legal-meta">
                        <span className="legal-meta-item">
                            <span className="legal-meta-dot" />
                            Effective: January 1, 2026
                        </span>
                        <span className="legal-meta-item">
                            <span className="legal-meta-dot" />
                            Version: 2.1 (Incorporating EU SCCs &amp; UK Addendum)
                        </span>
                    </div>
                </div>

                <div className="legal-highlight-box">
                    <strong>Contractual Summary:</strong> This Data Processing Addendum (&ldquo;DPA&rdquo;) supplements the Crescendo
                    Terms of Service when you use Crescendo to process Personal Data on behalf of European, UK, or California individuals
                    (e.g., storing audience contact rosters, delivering email campaigns, or orchestrating webhooks). It legally binds
                    Crescendo as a Data Processor under Article 28 of the GDPR.
                </div>

                <div className="legal-content">
                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">1</span>
                            Definitions and Scope
                        </h2>
                        <p>
                            Unless otherwise defined herein, capitalized terms shall have the meanings ascribed in our Terms of Service
                            or under applicable Data Protection Laws:
                        </p>
                        <ul>
                            <li>
                                <strong>&ldquo;Data Protection Laws&rdquo;</strong> means all global privacy legislation applicable to the
                                processing of Personal Data, including EU Regulation 2016/679 (GDPR), the UK Data Protection Act 2018,
                                the Swiss Federal Act on Data Protection (FADP), and the California Consumer Privacy Act as amended by CPRA.
                            </li>
                            <li>
                                <strong>&ldquo;Customer Personal Data&rdquo;</strong> means any Personal Data uploaded to Crescendo by or
                                on behalf of the Customer (including Audience subscriber lists, campaign logs, and workflow step parameters).
                            </li>
                            <li>
                                <strong>&ldquo;Controller&rdquo;</strong> and <strong>&ldquo;Processor&rdquo;</strong> (or &ldquo;Service Provider&rdquo;)
                                have the meanings established in the GDPR and CCPA.
                            </li>
                            <li>
                                <strong>&ldquo;Standard Contractual Clauses (SCCs)&rdquo;</strong> means the standard contractual clauses
                                approved by the European Commission Decision (EU) 2021/914 for Controller-to-Processor international transfers.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">2</span>
                            Roles and Documented Instructions
                        </h2>
                        <p>
                            The parties agree that Customer is the <strong>Data Controller</strong> (or Business) and Crescendo is the
                            <strong>Data Processor</strong> (or Service Provider) of Customer Personal Data.
                        </p>
                        <p>
                            Crescendo shall process Customer Personal Data exclusively in accordance with Customer&rsquo;s documented
                            instructions, as embodied in the Terms of Service, this DPA, and your configuration of workflows, API endpoints,
                            and email broadcasts within the platform. Crescendo shall never:
                        </p>
                        <ul>
                            <li>Sell, rent, or lease Customer Personal Data to any third party.</li>
                            <li>Retain, use, or disclose Customer Personal Data outside the direct business relationship with Customer.</li>
                            <li>Cross-reference or combine Customer Personal Data with personal data obtained from separate clients.</li>
                            <li>Mine or ingest Customer Personal Data to train or refine artificial intelligence foundation models.</li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">3</span>
                            Technical and Organizational Security Measures (TOMs)
                        </h2>
                        <p>
                            In compliance with Article 32 of the GDPR, Crescendo maintains rigorous technical and organizational controls
                            designed to protect Customer Personal Data against accidental destruction, alteration, unauthorized disclosure, or access:
                        </p>
                        <div className="legal-grid-cards">
                            <div className="legal-card">
                                <div className="legal-card-title">Authenticated Envelope Encryption</div>
                                <p className="legal-card-desc">
                                    All sensitive secrets, database records, and third-party credentials are encrypted at rest using authenticated
                                    AES-256-GCM. Primary disk volumes enforce full physical encryption.
                                </p>
                            </div>
                            <div className="legal-card">
                                <div className="legal-card-title">TLS 1.3 in Transit</div>
                                <p className="legal-card-desc">
                                    All external traffic and inter-service communications enforce modern TLS 1.3 transport security with Perfect
                                    Forward Secrecy and Strict Transport Security (HSTS).
                                </p>
                            </div>
                            <div className="legal-card">
                                <div className="legal-card-title">Volatile Runtime Execution</div>
                                <p className="legal-card-desc">
                                    Integration credentials reside strictly in ephemeral memory during live node execution and are automatically
                                    redacted from operational logs and telemetry pipelines.
                                </p>
                            </div>
                            <div className="legal-card">
                                <div className="legal-card-title">Strict Access &amp; PKCE Controls</div>
                                <p className="legal-card-desc">
                                    Enforced Multi-Factor Authentication (MFA), WebAuthn passkeys, refresh token rotation with reuse detection,
                                    and Redis Token Bucket rate limiting.
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">4</span>
                            Sub-processors and Authorized Infrastructure
                        </h2>
                        <p>
                            Customer provides general written authorization for Crescendo to engage sub-processors to assist in delivering
                            the Service. Our current list of authorized sub-processors is maintained on our dedicated{' '}
                            <Link to="/subprocessors" style={{ color: 'var(--text-accent, #fafafa)' }}>
                                Sub-processors Page
                            </Link>.
                        </p>
                        <ul>
                            <li>
                                <strong>Contractual Flow-Down:</strong> Crescendo imposes data protection obligations on every sub-processor
                                that are no less protective than those contained in this DPA.
                            </li>
                            <li>
                                <strong>Notification of Changes:</strong> Crescendo will provide at least 30 calendar days&rsquo; advance notice
                                of any planned addition or replacement of an existing sub-processor via email or dashboard notification.
                            </li>
                            <li>
                                <strong>Right to Object:</strong> If Customer reasonably objects to a new sub-processor on grounds related to
                                data protection, Customer may terminate the affected service tier without penalty prior to the new sub-processor&rsquo;s engagement.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">5</span>
                            Assistance with Data Subject Requests
                        </h2>
                        <p>
                            Taking into account the nature of processing, Crescendo shall assist Customer through appropriate technical features
                            (including Audience contact management tools, export mechanisms, and account deletion cascades) to fulfill obligations
                            regarding Data Subject Requests under Chapter III of the GDPR:
                        </p>
                        <ul>
                            <li>Right to Access and Data Portability (JSON/CSV export).</li>
                            <li>Right to Rectification and Modification.</li>
                            <li>Right to Erasure (&ldquo;Right to Be Forgotten&rdquo;) via automated suppression and hard-deletion workflows.</li>
                            <li>Right to Restriction and Objection of automated processing.</li>
                        </ul>
                        <p>
                            If Crescendo receives a request directly from a data subject regarding Customer Personal Data, Crescendo shall promptly
                            redirect the data subject to submit their inquiry directly to the Customer.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">6</span>
                            Personal Data Breach Notification
                        </h2>
                        <p>
                            In the event of a confirmed <strong>Personal Data Breach</strong> affecting Customer Personal Data processed by
                            Crescendo or its sub-processors, Crescendo shall:
                        </p>
                        <ul>
                            <li>
                                Notify Customer without undue delay, and in any event within <strong>48 hours</strong> of becoming aware of the breach.
                            </li>
                            <li>
                                Provide Customer with comprehensive incident analysis, including the categories of data affected, estimated number
                                of data subjects, likely consequences, and remediation actions taken.
                            </li>
                            <li>
                                Cooperate reasonably with Customer in mitigating adverse effects and fulfilling Customer&rsquo;s reporting
                                obligations to supervisory authorities (e.g. EU DPAs, UK ICO).
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">7</span>
                            International Data Transfers &amp; EU Standard Contractual Clauses
                        </h2>
                        <p>
                            Where the transfer of Customer Personal Data involves a transfer from the European Economic Area (EEA), Switzerland,
                            or the United Kingdom to countries not deemed to provide an adequate level of data protection:
                        </p>
                        <ul>
                            <li>
                                The parties agree to enter into Module 2 (Controller-to-Processor) of the <strong>Standard Contractual Clauses (SCCs)</strong>.
                            </li>
                            <li>
                                For transfers originating from the UK, the parties adopt the UK International Data Transfer Addendum issued by the Information Commissioner&rsquo;s Office (ICO).
                            </li>
                            <li>
                                The technical measures specified in Section 3 of this DPA and on our Security page serve as Annex II of the SCCs.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">8</span>
                            Deletion and Return of Personal Data
                        </h2>
                        <p>
                            Upon termination of Customer&rsquo;s Crescendo account or upon written request, Crescendo shall delete all Customer
                            Personal Data from primary production databases within <strong>30 calendar days</strong>, including associated
                            OAuth credentials, contact lists, and execution logs, except where retention is strictly required by statutory law.
                            Cryptographic key rotation ensures encrypted backup payloads become permanently unrecoverable upon standard retention expiry.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">9</span>
                            Audits and Compliance Inquiries
                        </h2>
                        <p>
                            Crescendo shall provide Customer with documentation, compliance summaries, and security attestations necessary
                            to demonstrate adherence to this DPA. For enterprise customers requiring custom on-site audits, inquiries may be
                            submitted to our legal department at <a href="mailto:legal@crescendo.run">legal@crescendo.run</a>.
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
