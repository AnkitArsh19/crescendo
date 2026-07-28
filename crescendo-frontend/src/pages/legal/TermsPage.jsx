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
                            Developer Applications &amp; OAuth Provider API
                        </h2>
                        <p>
                            Crescendo enables developers to register applications and utilize Crescendo as an
                            OAuth 2.0 Authorization Server to access permitted user data and workflow primitives.
                            By registering a Developer Application or invoking public APIs, you agree to the following:
                        </p>
                        <ul>
                            <li>
                                <strong>Lawful User Consent:</strong> You must explicitly disclose all requested scopes
                                (&ldquo;workflow:read&rdquo;, &ldquo;email:send&rdquo;, etc.) on an authentic consent screen
                                and obtain explicit, transparent user authorization before performing actions on their behalf.
                            </li>
                            <li>
                                <strong>Security &amp; PKCE Enforcement:</strong> All authorization flows must implement Proof
                                Key for Code Exchange (PKCE). You are strictly responsible for protecting issued client secrets
                                and user access tokens against leakage or unauthorized transfer.
                            </li>
                            <li>
                                <strong>Token Revocation &amp; Reuse Detection:</strong> Users reserve the right to revoke an
                                application&rsquo;s authorization at any time. Crescendo enforces strict refresh token rotation;
                                any detected reuse of a consumed refresh token immediately terminates all active grants for that
                                session to protect user security.
                            </li>
                            <li>
                                <strong>Abuse &amp; Rate Limits:</strong> We reserve the right to audit, rate-limit, throttle,
                                suspend, or terminate developer applications that exhibit abusive traffic, excessive polling, or
                                security violations without prior notice.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">11</span>
                            Email Service &amp; Anti-Spam Policies
                        </h2>
                        <p>
                            Crescendo provides an embedded transactional email sending engine, custom domain verification,
                            audience contact management, and marketing broadcast capabilities. When utilizing our Email Service,
                            you strictly agree to adhere to the following acceptable use and deliverability policies:
                        </p>
                        <ul>
                            <li>
                                <strong>Lawful Opt-In &amp; Anti-Spam (CAN-SPAM / GDPR):</strong> You may only transmit broadcast or marketing
                                emails to recipients who have explicitly opted-in or given demonstrated consent to receive correspondence.
                                All promotional campaigns must include an operable, conspicuous unsubscribe mechanism.
                            </li>
                            <li>
                                <strong>Prohibited Contact Lists:</strong> The use of purchased, rented, scraped, or third-party harvested
                                contact lists in Crescendo Audience / Contacts is strictly forbidden.
                            </li>
                            <li>
                                <strong>Deliverability &amp; Reputation Protections:</strong> Crescendo actively monitors domain bounce rates,
                                spam complaints, and suppression lists. To safeguard overall system deliverability, we reserve the right to
                                automatically throttle, quarantine, or suspend email sending privileges for accounts exceeding allowable bounce
                                or complaint thresholds without prior notification.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">12</span>
                            Bring Your Own Key (BYOK) &amp; Custom OAuth Credentials
                        </h2>
                        <p>
                            Crescendo enables users to connect third-party integrations using their own developer API keys
                            (e.g., OpenAI, SendGrid, Stripe) or by supplying custom OAuth Client ID and Client Secret configurations
                            (&ldquo;BYOK&rdquo; or &ldquo;BYOA&rdquo;). When supplying your own third-party credentials:
                        </p>
                        <ul>
                            <li>
                                <strong>Third-Party Compliance &amp; Rate Limits:</strong> You represent that you are the lawful owner or authorized
                                licensee of the supplied credentials. You remain solely responsible for abiding by the respective third-party
                                developer platform&rsquo;s Terms of Service, rate limiting tiers, and Acceptable Use Policies.
                            </li>
                            <li>
                                <strong>Financial Liability &amp; Overage Disclaimer:</strong> Crescendo operates as a workflow orchestration tool
                                executing automations under your provided API keys. Crescendo explicitly disclaims all operational and financial
                                liability for third-party billing costs, usage overages, API token exhaustion, or developer account suspensions
                                resulting from normal or retried workflow executions.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">13</span>
                            Regulated Sensitive Data &amp; AI Usage Rules
                        </h2>
                        <p>
                            To ensure adherence to international cyber law and statutory industry protections, users must abide by the following operational boundaries when configuring automations:
                        </p>
                        <ul>
                            <li>
                                <strong>Prohibited Sensitive Data (HIPAA &amp; PCI-DSS):</strong> Unless operating under an explicitly executed enterprise Business Associate Agreement (BAA) on a dedicated single-tenant infrastructure tier, users are strictly prohibited from utilizing standard workflow queues, email broadcasts, or webhook endpoints to transmit Protected Health Information (PHI regulated by HIPAA), unencrypted credit card Primary Account Numbers (regulated by PCI-DSS), government biometric identifiers, or classified defense data.
                            </li>
                            <li>
                                <strong>Artificial Intelligence &amp; Automated Decisions (EU AI Act):</strong> When integrating generative artificial intelligence nodes (e.g., Google Gemini, OpenAI, Anthropic) within automation pathways, users acknowledge that AI outputs may occasionally exhibit inaccuracies, hallucinations, or unverified assertions. You assume full operational responsibility for human verification and transparency disclosures required by law prior to deploying AI outputs for consumer decision-making, financial underwriting, or automated public broadcasts.
                            </li>
                            <li>
                                <strong>Data Processing Addendum (DPA):</strong> For commercial organizations processing Personal Data of European Economic Area (EEA), UK, or California consumers, Crescendo&rsquo;s standard Data Processing Addendum incorporating European Standard Contractual Clauses (SCCs) forms an enforceable, binding component of these Terms by reference.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">14</span>
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
                            <span className="legal-section-number">15</span>
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
