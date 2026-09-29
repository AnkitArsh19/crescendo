import { Link } from 'react-router-dom';
import './LegalPage.css';

export default function SecurityPage() {
    return (
        <div className="legal-page">
            <nav className="legal-page-nav">
                <Link to="/" className="legal-nav-brand">Crescendo</Link>
                <span className="legal-nav-sep">/</span>
                <span className="legal-nav-title">Security &amp; Trust Center</span>
                <Link to="/" className="legal-nav-back">← Back to Home</Link>
            </nav>

            <div className="legal-container">
                <div className="legal-header">
                    <div className="legal-badge">Trust &amp; Engineering</div>
                    <h1 className="legal-title">Security Architecture &amp; Trust</h1>
                    <p className="legal-subtitle">
                        How Crescendo safeguards developer secrets, protects workflow execution pipelines,
                        and enforces defense-in-depth across our cloud platform.
                    </p>
                    <div className="legal-meta">
                        <span className="legal-meta-item">
                            <span className="legal-meta-dot" />
                            Security Posture: Continuous Monitoring
                        </span>
                        <span className="legal-meta-item">
                            <span className="legal-meta-dot" />
                            Standard: RFC 9116 Compliant
                        </span>
                    </div>
                </div>

                <div className="legal-highlight-box">
                    <strong>Zero-Knowledge Secrets Philosophy:</strong> When you connect third-party accounts or supply your
                    own API keys (BYOK), your plaintext credentials are never written to permanent storage or exposed in execution logs.
                    Decrypted tokens exist strictly in volatile runtime memory during active external API handshakes.
                </div>

                <div className="legal-content">
                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">1</span>
                            Data Protection &amp; Cryptography
                        </h2>
                        <p>
                            We employ modern cryptographic protocols to secure data both in flight and at rest:
                        </p>
                        <div className="legal-grid-cards">
                            <div className="legal-card">
                                <div className="legal-card-title">AES-256-GCM Envelope Encryption</div>
                                <p className="legal-card-desc">
                                    All OAuth access tokens, refresh tokens, and BYOK credentials are sealed using Galois/Counter Mode
                                    (GCM) authenticated envelope encryption. Ciphertext cannot be tampered with without invalidating authentication tags.
                                </p>
                            </div>
                            <div className="legal-card">
                                <div className="legal-card-title">TLS 1.3 Transport Security</div>
                                <p className="legal-card-desc">
                                    All API requests, webhook deliveries, and dashboard interactions are encrypted in transit using enforced
                                    TLS 1.3 with Perfect Forward Secrecy (PFS) and strict HSTS headers.
                                </p>
                            </div>
                            <div className="legal-card">
                                <div className="legal-card-title">Volatile RAM Scoping</div>
                                <p className="legal-card-desc">
                                    When an automation node executes, secrets are decrypted directly in volatile heap memory, dispatched over
                                    an encrypted socket, and immediately scheduled for zeroization. Plaintext keys are never logged.
                                </p>
                            </div>
                            <div className="legal-card">
                                <div className="legal-card-title">Immediate Cryptographic Purge</div>
                                <p className="legal-card-desc">
                                    When you delete an integration connection or close your account, cryptographic keys and ciphertext payloads
                                    undergo immediate permanent erasure across our primary persistence clusters.
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">2</span>
                            Authentication &amp; Identity Security
                        </h2>
                        <p>
                            Crescendo provides enterprise-grade identity primitives designed to eliminate password compromise and credential stuffing:
                        </p>
                        <ul>
                            <li>
                                <strong>WebAuthn &amp; FIDO2 Passkeys:</strong> Full support for biometric logins (Touch ID, Face ID, Windows Hello)
                                and hardware security tokens (YubiKeys), offering complete phishing immunity.
                            </li>
                            <li>
                                <strong>Multi-Factor Authentication (MFA):</strong> Time-based One-Time Passwords (TOTP) supported via standard
                                authenticator apps (1Password, Google Authenticator) with cryptographically hashed one-time backup recovery codes.
                            </li>
                            <li>
                                <strong>Strict Refresh Token Rotation &amp; Reuse Detection:</strong> Every refresh operation generates an entirely new
                                cryptographic token pair. If an expired or previously consumed refresh token is presented, our backend immediately
                                triggers reuse detection, invalidates the entire token lineage, and terminates all active sessions for that account.
                            </li>
                            <li>
                                <strong>PKCE Enforced OAuth 2.0:</strong> All OAuth authorization flows require Proof Key for Code Exchange (RFC 7636),
                                ensuring authorization codes cannot be intercepted or redeemed by malicious intermediaries.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">3</span>
                            Network Defense &amp; Rate Limiting Architecture
                        </h2>
                        <p>
                            To prevent denial-of-service, brute-force attacks, and abusive scraping, Crescendo employs high-performance,
                            distributed rate limiting:
                        </p>
                        <ul>
                            <li>
                                <strong>Redis Lua Token Bucket Limiting:</strong> High-throughput, atomic token bucket algorithms implemented via Redis
                                Lua scripts protect all public authentication endpoints, webhook listeners, and developer API routes.
                            </li>
                            <li>
                                <strong>Tiered Quotas:</strong> Fine-grained rate limits are enforced dynamically across IP addresses, authenticated
                                user IDs, and issued API keys.
                            </li>
                            <li>
                                <strong>Intelligent Geolocation Anomaly Detection:</strong> Powered by MaxMind GeoLite2 IP intelligence, our login
                                pipelines calculate geographic velocity and detect unfamiliar countries or networks, instantly dispatching security
                                verification emails before granting access.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">4</span>
                            AI Safety &amp; Provider Isolation
                        </h2>
                        <p>
                            When orchestrating workflows that utilize generative artificial intelligence capabilities:
                        </p>
                        <ul>
                            <li>
                                <strong>Zero-Training Guarantee:</strong> Crescendo guarantees that your automation schemas, prompt instructions,
                                input parameters, and Audience subscriber data are <strong>never</strong> used to train, fine-tune, or reinforce foundation
                                models operated by Google, OpenAI, or any third party.
                            </li>
                            <li>
                                <strong>Direct Provider Scoping:</strong> Generative AI calls are executed via stateless API endpoints with zero data
                                retention on upstream provider infrastructure.
                            </li>
                        </ul>
                    </div>

                    <div className="legal-divider" />

                    <div className="legal-section">
                        <h2 className="legal-section-heading">
                            <span className="legal-section-number">5</span>
                            Vulnerability Disclosure &amp; Bug Bounty (RFC 9116)
                        </h2>
                        <p>
                            We appreciate the contributions of the ethical security research community. If you believe you have discovered
                            a security vulnerability in Crescendo, please review our responsible disclosure policy:
                        </p>
                        <div className="legal-contact-box">
                            <span className="contact-label">Reporting Channel</span>
                            <a href="mailto:security@crescendo.run">security@crescendo.run</a>
                            <span className="contact-label" style={{ marginTop: 8 }}>Standard Security Policy</span>
                            <a href="/.well-known/security.txt" target="_blank" rel="noreferrer">/.well-known/security.txt</a>
                        </div>
                        <p style={{ marginTop: 12 }}>
                            <strong>Safe Harbor:</strong> We will not pursue legal action against researchers who:
                        </p>
                        <ul>
                            <li>Conduct testing without disrupting production workflows or degrading platform performance.</li>
                            <li>Do not access, modify, or exfiltrate another user&rsquo;s personal data or automation credentials.</li>
                            <li>Give our engineering team reasonable time to investigate and patch the issue before making public disclosure.</li>
                        </ul>
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
