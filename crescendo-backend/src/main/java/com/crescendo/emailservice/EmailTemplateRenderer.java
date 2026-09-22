package com.crescendo.emailservice;

/**
 * Renders transactional HTML email templates for Crescendo.
 *
 * Design principles:
 *  - Landing Page Identity: adheres to the Funnel Display + Inter typography,
 *    zinc palette (#09090b, #141416, #fafafa), and sleek micro-borders of crescendo.run.
 *  - Strictly monochrome: black, white, and zinc tones only — zero accent colors.
 *  - Enterprise craftsmanship: hero badge status pills, spec metadata grid tables,
 *    full-pill CTA buttons with trailing indicators, and monospace security blocks.
 *  - Dual-mode: flawless rendering in both light mode and dark mode email clients
 *    via @media (prefers-color-scheme: dark).
 *  - Hosted PNG logo: hosted at https://app.crescendo.run/logo-app-light.png for 100%
 *    rendering reliability across Gmail, Outlook, Apple Mail, and Yahoo.
 *  - Table-compatible structure with fluid container: maximum cross-client compatibility.
 *  - Compliant transactional footer with security origin dispatch badge, direct access
 *    to Notification Preferences, Security Settings, Privacy Policy, and Terms.
 */
public class EmailTemplateRenderer {

    private static final String BASE_LAYOUT = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta name="color-scheme" content="light dark">
            <meta name="supported-color-schemes" content="light dark">
            <title>Crescendo</title>
            <link rel="preconnect" href="https://fonts.googleapis.com">
            <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
            <link href="https://fonts.googleapis.com/css2?family=Funnel+Display:wght@500;600;700&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
            <style>
                /* ── Reset ── */
                * { box-sizing: border-box; }
                body, table, td, p, a, li, blockquote {
                    -webkit-text-size-adjust: 100%;
                    -ms-text-size-adjust: 100%;
                }
                table, td { border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
                img { border: 0; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; }

                /* ── Base (light mode default) ── */
                body {
                    margin: 0;
                    padding: 0;
                    background-color: #f4f4f5;
                    color: #18181b;
                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                    -webkit-font-smoothing: antialiased;
                }
                .email-wrapper {
                    background-color: #f4f4f5;
                    padding: 48px 16px;
                    width: 100%;
                }
                .email-card {
                    max-width: 580px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 16px;
                    border: 1px solid #e4e4e7;
                    overflow: hidden;
                    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
                }
                .card-top-bar {
                    height: 3px;
                    background-color: #18181b;
                    width: 100%;
                }
                .email-header {
                    background-color: #ffffff;
                    padding: 30px 40px 24px;
                    text-align: center;
                    border-bottom: 1px solid #f0f0f2;
                }
                .logo-container {
                    display: inline-flex;
                    align-items: center;
                    gap: 12px;
                    text-decoration: none;
                }
                .logo-img {
                    width: 32px;
                    height: 32px;
                    display: inline-block;
                    vertical-align: middle;
                    border: 0;
                    outline: none;
                }
                .logo-text {
                    font-family: 'Funnel Display', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                    font-size: 22px;
                    font-weight: 700;
                    letter-spacing: -0.5px;
                    color: #09090b;
                    vertical-align: middle;
                }
                .logo-dot {
                    color: #71717a;
                }
                .email-body {
                    padding: 36px 40px 40px;
                }

                /* ── Landing Page Micro-Badge ── */
                .badge {
                    display: inline-block;
                    padding: 5px 13px;
                    border: 1px solid #e4e4e7;
                    border-radius: 100px;
                    background-color: #fafafa;
                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
                    font-size: 11px;
                    font-weight: 600;
                    letter-spacing: 0.6px;
                    text-transform: uppercase;
                    color: #71717a;
                    margin-bottom: 20px;
                }
                .badge-dot {
                    display: inline-block;
                    width: 5px;
                    height: 5px;
                    border-radius: 50%;
                    background-color: #18181b;
                    vertical-align: middle;
                    margin-right: 6px;
                    margin-top: -1px;
                }

                .email-body h2 {
                    font-family: 'Funnel Display', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                    font-size: 24px;
                    font-weight: 700;
                    color: #09090b;
                    margin: 0 0 16px;
                    letter-spacing: -0.025em;
                    line-height: 1.25;
                }
                .email-body p {
                    font-size: 15px;
                    line-height: 1.65;
                    color: #52525b;
                    margin: 0 0 16px;
                }
                .email-body p:last-child { margin-bottom: 0; }
                .email-body a {
                    color: #09090b;
                    font-weight: 500;
                    text-decoration: underline;
                    text-underline-offset: 3px;
                }

                /* ── Metadata Spec Grid (replaces plain <ul>) ── */
                .meta-table {
                    width: 100%;
                    border: 1px solid #e4e4e7;
                    border-radius: 10px;
                    background-color: #fafafa;
                    margin: 22px 0;
                    border-collapse: separate;
                    border-spacing: 0;
                    overflow: hidden;
                }
                .meta-table tr td {
                    padding: 12px 16px;
                    border-bottom: 1px solid #e4e4e7;
                    font-size: 13px;
                }
                .meta-table tr:last-child td {
                    border-bottom: none;
                }
                .meta-key {
                    font-size: 11px;
                    font-weight: 600;
                    letter-spacing: 0.6px;
                    text-transform: uppercase;
                    color: #71717a;
                    width: 30%;
                    vertical-align: middle;
                }
                .meta-val {
                    font-weight: 500;
                    color: #09090b;
                    vertical-align: middle;
                }
                .meta-val-mono {
                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
                    font-size: 12px;
                    font-weight: 600;
                    color: #18181b;
                    vertical-align: middle;
                }

                /* ── Landing Page Full-Pill CTA Button ── */
                .btn-container {
                    text-align: center;
                    margin: 32px 0;
                }
                .btn {
                    display: inline-block;
                    background-color: #09090b;
                    color: #ffffff !important;
                    text-decoration: none !important;
                    padding: 13px 32px;
                    border-radius: 100px;
                    font-family: 'Funnel Display', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                    font-size: 14px;
                    font-weight: 600;
                    letter-spacing: -0.01em;
                    border: 1px solid #09090b;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
                }
                .btn-danger {
                    background-color: #18181b !important;
                    border-color: #27272a !important;
                    color: #ffffff !important;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2) !important;
                }

                /* ── OTP Code Card ── */
                .otp-card {
                    background: #fafafa;
                    border: 1px solid #e4e4e7;
                    border-radius: 12px;
                    padding: 22px 24px;
                    text-align: center;
                    margin: 24px 0;
                }
                .otp-label {
                    font-size: 10px;
                    font-weight: 600;
                    letter-spacing: 1px;
                    text-transform: uppercase;
                    color: #71717a;
                    margin-bottom: 10px;
                }
                .code-block {
                    background: transparent;
                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
                    font-size: 32px;
                    font-weight: 700;
                    letter-spacing: 10px;
                    color: #09090b;
                    margin: 0;
                    padding: 6px 0;
                }
                .otp-sub {
                    font-size: 12px;
                    color: #71717a;
                    margin-top: 10px;
                }

                .divider {
                    border: none;
                    border-top: 1px solid #e4e4e7;
                    margin: 28px 0;
                }

                /* ── Micro-Notice Callouts ── */
                .info-box {
                    background: #fafafa;
                    border: 1px solid #e4e4e7;
                    border-left: 3px solid #18181b;
                    border-radius: 8px;
                    padding: 14px 18px;
                    margin: 20px 0;
                }
                .info-box p {
                    font-size: 13px;
                    color: #3f3f46;
                    margin: 0;
                    line-height: 1.55;
                }
                .info-box p strong {
                    color: #09090b;
                }
                .warning-box {
                    background: #fafafa;
                    border: 1px solid #e4e4e7;
                    border-left: 3px solid #52525b;
                    border-radius: 8px;
                    padding: 14px 18px;
                    margin: 20px 0;
                }
                .warning-box p {
                    font-size: 13px;
                    color: #3f3f46;
                    margin: 0;
                    line-height: 1.55;
                }
                .warning-box p strong {
                    color: #09090b;
                }

                /* ── Fallback URL row ── */
                .url-fallback {
                    margin-top: 24px;
                    padding-top: 18px;
                    border-top: 1px dashed #e4e4e7;
                }
                .url-fallback p {
                    font-size: 12px;
                    color: #71717a;
                    margin: 0 0 6px;
                    line-height: 1.5;
                }
                .url-fallback a {
                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
                    font-size: 11px;
                    color: #71717a;
                    word-break: break-all;
                    text-decoration: underline;
                }

                /* ── Transactional Compliance Footer ── */
                .email-footer {
                    background-color: #fafafa;
                    border-top: 1px solid #e4e4e7;
                    padding: 28px 40px;
                    text-align: center;
                }
                .footer-badge {
                    display: inline-block;
                    padding: 4px 11px;
                    border: 1px solid #e4e4e7;
                    border-radius: 100px;
                    font-size: 10px;
                    font-weight: 600;
                    letter-spacing: 0.6px;
                    text-transform: uppercase;
                    color: #71717a;
                    margin-bottom: 14px;
                }
                .footer-badge-dot {
                    display: inline-block;
                    width: 4px;
                    height: 4px;
                    border-radius: 50%;
                    background-color: #71717a;
                    vertical-align: middle;
                    margin-right: 5px;
                    margin-top: -1px;
                }
                .email-footer p {
                    font-size: 12px;
                    color: #71717a;
                    margin: 0 0 8px;
                    line-height: 1.6;
                }
                .email-footer p:last-child { margin-bottom: 0; }
                .email-footer a {
                    color: #71717a;
                    text-decoration: underline;
                    text-underline-offset: 2px;
                }
                .footer-links a {
                    margin: 0 4px;
                }

                /* ── Dark mode overrides ── */
                @media (prefers-color-scheme: dark) {
                    body, .email-wrapper { background-color: #09090b !important; }
                    .email-card {
                        background-color: #141416 !important;
                        border-color: rgba(255, 255, 255, 0.08) !important;
                        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6) !important;
                    }
                    .card-top-bar {
                        background-color: #ffffff !important;
                    }
                    .email-header {
                        background-color: #09090b !important;
                        border-bottom-color: rgba(255, 255, 255, 0.06) !important;
                    }
                    .logo-img {
                        filter: brightness(0) invert(1) !important;
                    }
                    .logo-text {
                        color: #fafafa !important;
                    }
                    .badge {
                        background-color: #18181b !important;
                        border-color: rgba(255, 255, 255, 0.12) !important;
                        color: #a1a1aa !important;
                    }
                    .badge-dot {
                        background-color: #fafafa !important;
                    }
                    .email-body h2 { color: #fafafa !important; }
                    .email-body p { color: #a1a1aa !important; }
                    .email-body a { color: #ffffff !important; }

                    .meta-table {
                        background-color: #18181b !important;
                        border-color: rgba(255, 255, 255, 0.08) !important;
                    }
                    .meta-table tr td {
                        border-bottom-color: rgba(255, 255, 255, 0.06) !important;
                    }
                    .meta-key { color: #71717a !important; }
                    .meta-val { color: #fafafa !important; }
                    .meta-val-mono { color: #fafafa !important; }

                    .btn {
                        background-color: #ffffff !important;
                        color: #09090b !important;
                        border-color: #ffffff !important;
                        box-shadow: 0 4px 14px rgba(255, 255, 255, 0.15) !important;
                    }
                    .btn-danger {
                        background-color: #27272a !important;
                        border-color: #3f3f46 !important;
                        color: #ffffff !important;
                        box-shadow: 0 4px 14px rgba(255, 255, 255, 0.1) !important;
                    }

                    .otp-card {
                        background: #18181b !important;
                        border-color: rgba(255, 255, 255, 0.1) !important;
                    }
                    .otp-label { color: #71717a !important; }
                    .code-block { color: #fafafa !important; }
                    .otp-sub { color: #71717a !important; }

                    .divider { border-top-color: rgba(255, 255, 255, 0.08) !important; }

                    .info-box {
                        background: rgba(255, 255, 255, 0.03) !important;
                        border-color: rgba(255, 255, 255, 0.08) !important;
                        border-left-color: #ffffff !important;
                    }
                    .info-box p { color: #a1a1aa !important; }
                    .info-box p strong { color: #fafafa !important; }
                    .warning-box {
                        background: rgba(255, 255, 255, 0.03) !important;
                        border-color: rgba(255, 255, 255, 0.08) !important;
                        border-left-color: #a1a1aa !important;
                    }
                    .warning-box p { color: #a1a1aa !important; }
                    .warning-box p strong { color: #fafafa !important; }

                    .url-fallback { border-top-color: rgba(255, 255, 255, 0.08) !important; }
                    .url-fallback p, .url-fallback a { color: #71717a !important; }

                    .email-footer {
                        background-color: #0f0f11 !important;
                        border-top-color: rgba(255, 255, 255, 0.06) !important;
                    }
                    .footer-badge {
                        background-color: #141416 !important;
                        border-color: rgba(255, 255, 255, 0.08) !important;
                        color: #71717a !important;
                    }
                    .footer-badge-dot { background-color: #71717a !important; }
                    .email-footer p, .email-footer a { color: #71717a !important; }
                    .email-footer a:hover { color: #a1a1aa !important; }
                }

                /* ── Responsive ── */
                @media only screen and (max-width: 620px) {
                    .email-wrapper { padding: 24px 12px !important; }
                    .email-header { padding: 24px 20px 20px !important; }
                    .email-body { padding: 28px 20px 32px !important; }
                    .email-footer { padding: 24px 20px !important; }
                    .email-body h2 { font-size: 21px !important; }
                    .btn { padding: 12px 26px !important; font-size: 13px !important; }
                    .code-block { font-size: 26px !important; letter-spacing: 6px !important; }
                }
            </style>
        </head>
        <body>
            <div class="email-wrapper">
                <div class="email-card">
                    <!-- Top hairline accent -->
                    <div class="card-top-bar"></div>

                    <!-- Header -->
                    <div class="email-header">
                        <a href="https://app.crescendo.run" target="_blank" class="logo-container">
                            <img src="https://app.crescendo.run/logo-app-light.png" width="32" height="32" alt="Crescendo" class="logo-img" />
                            <span class="logo-text">Crescendo<span class="logo-dot">.</span></span>
                        </a>
                    </div>

                    <!-- Body -->
                    <div class="email-body">
                        {{CONTENT}}
                    </div>

                    <!-- Footer -->
                    <div class="email-footer">
                        <div class="footer-badge">
                            <span class="footer-badge-dot"></span>
                            <span>Secure Transactional Dispatch</span>
                        </div>
                        <p>You received this mandatory security and account notification because you have a Crescendo account.</p>
                        <p class="footer-links">
                            <a href="https://app.crescendo.run/settings/notifications">Notification Preferences</a> &bull;
                            <a href="https://app.crescendo.run/settings/security">Security Settings</a> &bull;
                            <a href="https://app.crescendo.run/privacy">Privacy Policy</a> &bull;
                            <a href="https://app.crescendo.run/terms">Terms</a>
                        </p>
                        <p>&copy; 2026 Crescendo Inc. All rights reserved.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """;

    private static String render(String content) {
        return BASE_LAYOUT.replace("{{CONTENT}}", content);
    }

    public static String renderPasswordReset(String resetUrl) {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Authentication</div>
            <h2>Reset your password</h2>
            <p>We received a request to reset the password for your Crescendo account.
               Click the button below to choose a new password.</p>
            <div class="btn-container">
                <a href="%s" class="btn">Reset Password &rarr;</a>
            </div>
            <div class="info-box">
                <p>This link expires in <strong>1 hour</strong> and can only be used once.</p>
            </div>
            <div class="url-fallback">
                <p>Button not working? Copy and paste this URL into your browser:</p>
                <a href="%s">%s</a>
            </div>
            <div class="divider"></div>
            <p>If you didn't request this, no action is needed — your password hasn't changed.
               If you're concerned, please contact us at <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            """.formatted(resetUrl, resetUrl, resetUrl);
        return render(content);
    }

    public static String renderEmailVerification(String verifyUrl) {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Account Activation</div>
            <h2>Verify your email address</h2>
            <p>Welcome to Crescendo! You're one step away from getting started.
               Please verify your email address to activate your account and start building workflows.</p>
            <div class="btn-container">
                <a href="%s" class="btn">Verify Email Address &rarr;</a>
            </div>
            <div class="info-box">
                <p>This link expires in <strong>24 hours</strong>.</p>
            </div>
            <div class="url-fallback">
                <p>Button not working? Copy and paste this URL into your browser:</p>
                <a href="%s">%s</a>
            </div>
            <div class="divider"></div>
            <p>If you didn't create a Crescendo account, you can safely ignore this email.</p>
            """.formatted(verifyUrl, verifyUrl, verifyUrl);
        return render(content);
    }

    public static String renderPasskeyRecovery(String recoveryUrl) {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Security Credentials</div>
            <h2>Recover your passkey</h2>
            <p>You requested a passkey recovery link for your Crescendo account.
               Use the button below to add a replacement passkey.</p>
            <div class="btn-container">
                <a href="%s" class="btn">Recover Passkey &rarr;</a>
            </div>
            <div class="warning-box">
                <p>This link expires in <strong>10 minutes</strong>. It cannot be used to access your account
                   or change your password — only to register a new passkey.</p>
            </div>
            <div class="url-fallback">
                <p>Button not working? Copy and paste this URL into your browser:</p>
                <a href="%s">%s</a>
            </div>
            <div class="divider"></div>
            <p>If you didn't request this, please contact support immediately at
               <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            """.formatted(recoveryUrl, recoveryUrl, recoveryUrl);
        return render(content);
    }

    public static String renderPasswordlessSignupOtp(String otp) {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Verification Code</div>
            <h2>Your verification code</h2>
            <p>Enter this single-use code to authenticate and continue setting up your passwordless Crescendo account.</p>
            <div class="otp-card">
                <div class="otp-label">One-Time Passcode</div>
                <div class="code-block">%s</div>
                <div class="otp-sub">Valid for <strong>10 minutes</strong> &bull; Single-use only</div>
            </div>
            <div class="warning-box">
                <p>Never share this code with anyone. Crescendo security staff will never ask for it.</p>
            </div>
            """.formatted(otp);
        return render(content);
    }

    /**
     * Sent when an email that already belongs to an account is used in the
     * passwordless sign-up flow. The caller always sees a success response so that
     * this email is the only signal — the API reveals nothing about account existence.
     */
    public static String renderAccountExists() {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Account Notice</div>
            <h2>You already have an account</h2>
            <p>Someone (hopefully you) just tried to sign up for Crescendo using this email address,
               but an account already exists with this email.</p>
            <p>If that was you, simply sign in to your existing account. You can then add a passkey at
               any time from your security settings.</p>
            <div class="btn-container">
                <a href="https://app.crescendo.run/login" class="btn">Sign In &rarr;</a>
            </div>
            <div class="divider"></div>
            <p>If you didn't attempt to sign up, you can safely ignore this email — no changes have been
               made to your account.</p>
            """;
        return render(content);
    }

    public static String renderWelcome(String name) {
        String safeName = (name != null && !name.isBlank()) ? name : "there";
        String content = """
            <div class="badge"><span class="badge-dot"></span>Getting Started</div>
            <h2>Welcome to Crescendo, %s!</h2>
            <p>Your account is set up and ready to go. You can now start automating your workflows
               and connecting your favourite apps — all in one place.</p>
            <div class="btn-container">
                <a href="https://app.crescendo.run/dashboard" class="btn">Open Workflow Studio &rarr;</a>
            </div>
            <div class="divider"></div>
            <p>Need help getting started? Explore our
               <a href="https://app.crescendo.run/docs">Documentation Portal</a> or reach out to us at
               <a href="mailto:hello@crescendo.run">hello@crescendo.run</a>.</p>
            """.formatted(safeName);
        return render(content);
    }

    public static String renderDeleteAccount() {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Account Status</div>
            <h2>Account deleted</h2>
            <p>Your Crescendo account and all associated data have been permanently deleted
               as requested. We're sorry to see you go.</p>
            <p>If you ever change your mind, you're always welcome to create a new account at
               <a href="https://app.crescendo.run">app.crescendo.run</a>.</p>
            <div class="divider"></div>
            <p>If you believe this was a mistake, please contact us immediately at
               <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            """;
        return render(content);
    }

    public static String renderPasswordChanged() {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Security Notification</div>
            <h2>Your password was changed</h2>
            <p>The password for your Crescendo account was successfully updated.</p>
            <div class="info-box">
                <p>This change was completed at the time you received this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>If you did <strong>not</strong> make this change, please reset your password immediately
                   and contact us at <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            </div>
            """;
        return render(content);
    }

    public static String renderTotpEnabled() {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Two-Factor Authentication</div>
            <h2>Two-factor authentication enabled</h2>
            <p>Two-factor authentication (TOTP) has been successfully enabled on your Crescendo account.
               You will now need your authenticator app each time you sign in.</p>
            <div class="info-box">
                <p>Your account is now significantly more secure. Make sure to save your backup codes
                   in a safe place in case you lose access to your authenticator app.</p>
            </div>
            <div class="divider"></div>
            <p>If you didn't enable this yourself, please contact support immediately at
               <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            """;
        return render(content);
    }

    public static String renderTotpDisabled() {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Security Warning</div>
            <h2>Two-factor authentication disabled</h2>
            <p>Two-factor authentication (TOTP) has been removed from your Crescendo account.</p>
            <div class="warning-box">
                <p>Your account is now less protected. If you did <strong>not</strong> authorize this change,
                   please reset your password immediately and contact us at
                   <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            </div>
            """;
        return render(content);
    }

    public static String renderLoginAlert(String device, String location) {
        String content = """
            <div class="badge"><span class="badge-dot"></span>New Sign-In</div>
            <h2>New sign-in detected</h2>
            <p>We noticed a new sign-in to your Crescendo account from an unrecognized device or location.</p>
            <table class="meta-table" width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                    <td class="meta-key">Device</td>
                    <td class="meta-val">%s</td>
                </tr>
                <tr>
                    <td class="meta-key">Location</td>
                    <td class="meta-val">%s</td>
                </tr>
                <tr>
                    <td class="meta-key">Time</td>
                    <td class="meta-val">Just now</td>
                </tr>
            </table>
            <div class="info-box">
                <p>If this was you, you can safely ignore this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>If you don't recognise this sign-in, please change your password immediately and contact
                   us at <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            </div>
            """.formatted(device, location);
        return render(content);
    }

    public static String renderPasskeyAdded(String passkeyName) {
        String safeName = (passkeyName != null && !passkeyName.isBlank()) ? passkeyName : "a new passkey";
        String content = """
            <div class="badge"><span class="badge-dot"></span>Credential Registered</div>
            <h2>A passkey was added to your account</h2>
            <p>A new passkey (<strong>%s</strong>) was just registered to your Crescendo account.
               You can now use this passkey to sign in securely.</p>
            <div class="info-box">
                <p>This change was made at the time you received this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>If you did <strong>not</strong> add this passkey, please sign in, remove it from your security settings immediately,
                   and contact us at <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            </div>
            """.formatted(safeName);
        return render(content);
    }

    public static String renderSmartLoginAlert(String device, String location, String country, String revokeUrl) {
        String locationDisplay = (location != null && !location.isBlank()) ? location : "Unknown Location";
        if (country != null && !country.isBlank() && !locationDisplay.contains(country) && locationDisplay.startsWith("IP: ")) {
            locationDisplay = locationDisplay + " (" + country + ")";
        }
        String content = """
            <div class="badge"><span class="badge-dot"></span>Security Alert</div>
            <h2>New sign-in detected</h2>
            <p>We noticed a new sign-in to your Crescendo account from an unrecognized device or location.</p>
            <table class="meta-table" width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                    <td class="meta-key">Device</td>
                    <td class="meta-val">%s</td>
                </tr>
                <tr>
                    <td class="meta-key">Location</td>
                    <td class="meta-val">%s</td>
                </tr>
                <tr>
                    <td class="meta-key">Status</td>
                    <td class="meta-val">Active Session</td>
                </tr>
            </table>
            <div class="info-box">
                <p>If this was you, you can safely ignore this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>If you don't recognise this sign-in, revoke access immediately using the button below, then change your password.</p>
            </div>
            <div class="btn-container" style="margin-top: 28px;">
                <a href="%s" class="btn btn-danger">Revoke Access &rarr;</a>
            </div>
            """.formatted(device, locationDisplay, revokeUrl);
        return render(content);
    }

    public static String renderSuspiciousActivity(String originalIp, String newIp, String revokeUrl) {
        String content = """
            <div class="badge"><span class="badge-dot"></span>Security Incident</div>
            <h2>Suspicious session activity detected</h2>
            <p>We detected that one of your active sessions suddenly changed IP addresses across a large geographic distance.</p>
            <table class="meta-table" width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                    <td class="meta-key">Original IP</td>
                    <td class="meta-val-mono">%s</td>
                </tr>
                <tr>
                    <td class="meta-key">New IP</td>
                    <td class="meta-val-mono">%s</td>
                </tr>
                <tr>
                    <td class="meta-key">Activity</td>
                    <td class="meta-val">Rapid Geo-IP Shift</td>
                </tr>
            </table>
            <p>While this can sometimes happen if you switch from Wi-Fi to cellular data or use a VPN, it can also indicate that your session was hijacked.</p>
            <div class="info-box">
                <p>If you are using a VPN or just travelled, you can safely ignore this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>If you have not changed locations or enabled a VPN, revoke access immediately using the button below.</p>
            </div>
            <div class="btn-container" style="margin-top: 28px;">
                <a href="%s" class="btn btn-danger">Revoke Session &rarr;</a>
            </div>
            """.formatted(originalIp, newIp, revokeUrl);
        return render(content);
    }
}
