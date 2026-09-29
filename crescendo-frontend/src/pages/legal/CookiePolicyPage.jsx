import { Link } from 'react-router-dom';
import './LegalPage.css';

export default function CookiePolicyPage() {
    return (
        <div className="legal-page">
            <nav className="legal-page-nav">
                <Link to="/" className="legal-nav-brand">Crescendo</Link>
                <span className="legal-nav-sep">/</span>
                <span className="legal-nav-title">Cookie Policy</span>
                <Link to="/" className="legal-nav-back">← Back to Home</Link>
            </nav>

            <div className="legal-container">
                <div className="legal-header">
                    <div className="legal-badge">Legal &amp; Compliance</div>
                    <h1 className="legal-title">Cookie Policy</h1>
                    <p className="legal-subtitle">
                        How Crescendo uses cookies, browser storage, and related technologies to provide
                        secure, reliable, and privacy-first workflow automation.
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
                    <strong>Zero Tracking Guarantee:</strong> Crescendo does not deploy third-party advertising cookies,
                    behavioral profiling trackers, or cross-site fingerprinting scripts. We exclusively use strictly necessary
                    first-party security cookies and local storage tokens required for platform authentication, session state,
                    and user interface preferences.
                </div>

                <div className="legal-content">
                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">1</span>
                            What Are Cookies and Local Storage?
                        </h2>
                        <p>
                            Cookies are small text files placed on your device by websites that you visit. They are widely used
                            to ensure websites function correctly, maintain authenticated sessions, enhance security, and remember
                            user preferences.
                        </p>
                        <p>
                            In addition to cookies, Crescendo utilizes HTML5 Local Storage and Session Storage. These browser-based
                            key-value stores allow client applications to retain workspace state, theme configurations, and volatile
                            UI views without transmitting them across the network on every HTTP request.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">2</span>
                            Categories of Cookies We Use
                        </h2>
                        <p>
                            Under the EU General Data Protection Regulation (GDPR) and the ePrivacy Directive (&ldquo;Cookie Law&rdquo;),
                            cookies are classified into functional categories. Crescendo strictly operates within two categories:
                        </p>
                        <div className="legal-grid-cards">
                            <div className="legal-card">
                                <div className="legal-card-title">Strictly Necessary Cookies</div>
                                <p className="legal-card-desc">
                                    Essential for the core security and operational functionality of our service. They power user
                                    authentication, PKCE OAuth exchanges, CSRF prevention, and token rotation. They cannot be disabled.
                                </p>
                            </div>
                            <div className="legal-card">
                                <div className="legal-card-title">Functional Preferences</div>
                                <p className="legal-card-desc">
                                    Used to remember settings such as your active dark/light mode theme, collapsed navigation sidebars,
                                    and last-opened workflow canvas layouts across browser sessions.
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">3</span>
                            Itemized Cookie Audit Table
                        </h2>
                        <p>
                            The following table lists every HTTP cookie issued by Crescendo services:
                        </p>
                        <div className="legal-table-container">
                            <table className="legal-table">
                                <thead>
                                    <tr>
                                        <th>Cookie Name</th>
                                        <th>Category</th>
                                        <th>Lifespan</th>
                                        <th>Flags</th>
                                        <th>Purpose &amp; Description</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td><span className="legal-code-badge">refreshToken</span></td>
                                        <td><span className="legal-tag legal-tag-essential">Strictly Necessary</span></td>
                                        <td>30 Days</td>
                                        <td>HttpOnly, Secure, SameSite=Lax</td>
                                        <td>
                                            Contains an opaque, cryptographically sealed refresh token used by the backend
                                            to renew short-lived access tokens. Protected by automatic reuse detection and token rotation.
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><span className="legal-code-badge">oauth2_auth_request</span></td>
                                        <td><span className="legal-tag legal-tag-essential">Strictly Necessary</span></td>
                                        <td>15 Minutes</td>
                                        <td>HttpOnly, Secure, SameSite=Lax</td>
                                        <td>
                                            Stores the OAuth 2.0 authorization state nonce, code challenge, and PKCE parameters
                                            to verify callbacks from third-party identity providers and prevent CSRF login attacks.
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><span className="legal-code-badge">link_user_id</span></td>
                                        <td><span className="legal-tag legal-tag-essential">Strictly Necessary</span></td>
                                        <td>10 Minutes</td>
                                        <td>HttpOnly, Secure, SameSite=Lax</td>
                                        <td>
                                            Temporary cryptographic token used exclusively when an authenticated user links a secondary
                                            social identity (e.g. GitHub or Google) to their existing workspace profile.
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><span className="legal-code-badge">crescendo_from_desktop</span></td>
                                        <td><span className="legal-tag legal-tag-essential">Strictly Necessary</span></td>
                                        <td>Session</td>
                                        <td>HttpOnly, Secure, SameSite=Lax</td>
                                        <td>
                                            Enables seamless authentication handoff when launching browser-based OAuth flows from the
                                            Crescendo native desktop application (Tauri client). Cleared immediately upon flow completion.
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">4</span>
                            Browser Local Storage Disclosures
                        </h2>
                        <p>
                            We store non-sensitive state elements in your client browser&rsquo;s Local Storage to optimize dashboard
                            performance and prevent visual flicker during page transitions:
                        </p>
                        <ul>
                            <li>
                                <strong><span className="legal-code-badge">crescendo_theme</span>:</strong> Persists your chosen visual
                                theme (dark mode, light mode, or system default).
                            </li>
                            <li>
                                <strong><span className="legal-code-badge">crescendo_active_workspace</span>:</strong> Remembers your
                                currently selected organization or project workspace ID.
                            </li>
                            <li>
                                <strong><span className="legal-code-badge">crescendo_sidebar_collapsed</span>:</strong> Stores UI layout
                                state for collapsing or expanding navigation sidebars.
                            </li>
                            <li>
                                <strong><span className="legal-code-badge">crescendo_workflow_zoom</span>:</strong> Stores canvas coordinates
                                and zoom scale for workflow node graph editing.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">5</span>
                            Third-Party &amp; Advertising Cookies
                        </h2>
                        <p>
                            <strong>We do not use third-party advertising or retargeting cookies.</strong> We believe software tools
                            should respect developer privacy. We do not integrate with Facebook Pixel, Google AdSense, Criteo, or
                            any third-party advertising network that tracks your browsing activity across other domains.
                        </p>
                        <p>
                            When you execute workflow actions connecting to third-party APIs (such as Google Drive, Slack, or GitHub),
                            API communication is initiated directly by our backend servers using encrypted OAuth access tokens, completely
                            isolated from client-side browser cookies.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">6</span>
                            Managing and Disabling Cookies
                        </h2>
                        <p>
                            You have the right to decide whether to accept or reject cookies. You can manage or delete cookies
                            directly through your browser settings:
                        </p>
                        <ul>
                            <li>
                                <strong>Google Chrome:</strong> Settings &gt; Privacy and security &gt; Third-party cookies &gt; See all site data and permissions.
                            </li>
                            <li>
                                <strong>Mozilla Firefox:</strong> Settings &gt; Privacy &amp; Security &gt; Cookies and Site Data &gt; Manage Data.
                            </li>
                            <li>
                                <strong>Apple Safari:</strong> Preferences &gt; Privacy &gt; Manage Website Data.
                            </li>
                            <li>
                                <strong>Microsoft Edge:</strong> Settings &gt; Cookies and site permissions &gt; Manage and delete cookies and site data.
                            </li>
                        </ul>
                        <p>
                            <em>Please note:</em> Because Crescendo&rsquo;s cookies are strictly essential for authentication and session
                            security, disabling or clearing cookies will immediately terminate your session and prevent you from logging
                            into your account.
                        </p>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">7</span>
                            Questions &amp; Contact
                        </h2>
                        <p>
                            For inquiries regarding our use of cookies or privacy practices, please contact our data protection team:
                        </p>
                        <div className="legal-contact-box">
                            <span className="contact-label">Privacy &amp; Cookie Inquiries</span>
                            <a href="mailto:privacy@crescendo.run">privacy@crescendo.run</a>
                            <span className="contact-label" style={{ marginTop: 8 }}>Security Officer</span>
                            <a href="mailto:security@crescendo.run">security@crescendo.run</a>
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
