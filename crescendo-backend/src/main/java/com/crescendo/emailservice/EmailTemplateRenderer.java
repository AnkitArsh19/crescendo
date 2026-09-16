package com.crescendo.emailservice;

/**
 * Renders transactional HTML email templates for Crescendo.
 *
 * Design principles:
 *  - Landing Page Identity: adheres to the Funnel Display + Inter typography,
 *    zinc palette (#09090b, #141416, #fafafa), and sleek micro-borders of crescendo.run.
 *  - Dual-mode: flawless rendering in both light mode and dark mode email clients
 *    via @media (prefers-color-scheme: dark).
 *  - Hosted PNG logo: hosted at https://app.crescendo.run/logo-white.png for 100%
 *    rendering reliability across Gmail, Outlook, Apple Mail, and Yahoo (which strip raw SVGs).
 *  - Table-compatible structure with fluid container: maximum cross-client compatibility.
 *  - Compliant transactional footer with direct access to Notification Preferences,
 *    Security Settings, Privacy Policy, and Terms.
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
                    padding: 44px 16px;
                    width: 100%;
                }
                .email-card {
                    max-width: 580px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 16px;
                    border: 1px solid #e4e4e7;
                    overflow: hidden;
                    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.05);
                }
                .email-header {
                    background-color: #ffffff;
                    padding: 32px 40px;
                    text-align: center;
                    border-bottom: 1px solid #e4e4e7;
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
                    color: #60a5fa;
                }
                .email-body {
                    padding: 38px 40px;
                }
                .email-body h2 {
                    font-family: 'Funnel Display', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                    font-size: 22px;
                    font-weight: 700;
                    color: #09090b;
                    margin: 0 0 16px;
                    letter-spacing: -0.02em;
                    line-height: 1.3;
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
                .email-body ul {
                    padding-left: 20px;
                    color: #52525b;
                    font-size: 15px;
                    line-height: 1.65;
                    margin: 0 0 16px;
                }
                .email-body ul li { margin-bottom: 6px; }
                .email-body ul li strong { color: #18181b; }
                .btn-container {
                    text-align: center;
                    margin: 32px 0;
                }
                .btn {
                    display: inline-block;
                    background-color: #09090b;
                    color: #ffffff !important;
                    text-decoration: none !important;
                    padding: 13px 30px;
                    border-radius: 10px;
                    font-family: 'Funnel Display', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                    font-size: 14px;
                    font-weight: 600;
                    letter-spacing: -0.01em;
                    border: 1px solid #27272a;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
                }
                .btn-danger {
                    background-color: #dc2626 !important;
                    border-color: #b91c1c !important;
                    color: #ffffff !important;
                    box-shadow: 0 2px 8px rgba(220, 38, 38, 0.25) !important;
                }
                .code-block {
                    background: #f4f4f5;
                    border: 1px solid #e4e4e7;
                    border-radius: 10px;
                    padding: 18px 24px;
                    text-align: center;
                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
                    font-size: 28px;
                    font-weight: 700;
                    letter-spacing: 8px;
                    color: #09090b;
                    margin: 24px 0;
                }
                .divider {
                    border: none;
                    border-top: 1px solid #e4e4e7;
                    margin: 28px 0;
                }
                .info-box {
                    background: #f8fafc;
                    border: 1px solid #e2e8f0;
                    border-left: 3px solid #3b82f6;
                    border-radius: 8px;
                    padding: 14px 18px;
                    margin: 20px 0;
                }
                .info-box p {
                    font-size: 13px;
                    color: #1e40af;
                    margin: 0;
                    line-height: 1.5;
                }
                .info-box p strong {
                    color: #1e3a8a;
                }
                .warning-box {
                    background: #fefce8;
                    border: 1px solid #fef08a;
                    border-left: 3px solid #eab308;
                    border-radius: 8px;
                    padding: 14px 18px;
                    margin: 20px 0;
                }
                .warning-box p {
                    font-size: 13px;
                    color: #854d0e;
                    margin: 0;
                    line-height: 1.5;
                }
                .warning-box p strong {
                    color: #713f12;
                }
                .email-footer {
                    background-color: #fafafa;
                    border-top: 1px solid #e4e4e7;
                    padding: 28px 40px;
                    text-align: center;
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
                    .email-header {
                        background-color: #09090b !important;
                        border-bottom-color: rgba(255, 255, 255, 0.08) !important;
                    }
                    .logo-img {
                        filter: brightness(0) invert(1) !important;
                    }
                    .logo-text {
                        color: #fafafa !important;
                    }
                    .email-body h2 { color: #fafafa !important; }
                    .email-body p { color: #a1a1aa !important; }
                    .email-body ul { color: #a1a1aa !important; }
                    .email-body ul li strong { color: #fafafa !important; }
                    .email-body a { color: #ffffff !important; }
                    .btn {
                        background-color: #ffffff !important;
                        color: #09090b !important;
                        border-color: #ffffff !important;
                        box-shadow: 0 4px 14px rgba(255, 255, 255, 0.15) !important;
                    }
                    .btn-danger {
                        background-color: #ef4444 !important;
                        border-color: #dc2626 !important;
                        color: #ffffff !important;
                        box-shadow: 0 4px 14px rgba(239, 68, 68, 0.4) !important;
                    }
                    .code-block {
                        background: #18181b !important;
                        border-color: rgba(255, 255, 255, 0.12) !important;
                        color: #fafafa !important;
                    }
                    .divider { border-top-color: rgba(255, 255, 255, 0.08) !important; }
                    .info-box {
                        background: rgba(59, 130, 246, 0.08) !important;
                        border-color: rgba(59, 130, 246, 0.2) !important;
                        border-left-color: #3b82f6 !important;
                    }
                    .info-box p { color: #93c5fd !important; }
                    .info-box p strong { color: #bfdbfe !important; }
                    .warning-box {
                        background: rgba(234, 179, 8, 0.08) !important;
                        border-color: rgba(234, 179, 8, 0.2) !important;
                        border-left-color: #eab308 !important;
                    }
                    .warning-box p { color: #fde047 !important; }
                    .warning-box p strong { color: #fef08a !important; }
                    .email-footer {
                        background-color: #0f0f11 !important;
                        border-top-color: rgba(255, 255, 255, 0.06) !important;
                    }
                    .email-footer p, .email-footer a { color: #71717a !important; }
                    .email-footer a:hover { color: #a1a1aa !important; }
                }

                /* ── Responsive ── */
                @media only screen and (max-width: 620px) {
                    .email-header { padding: 24px 20px !important; }
                    .email-body { padding: 28px 20px !important; }
                    .email-footer { padding: 22px 20px !important; }
                    .email-body h2 { font-size: 20px !important; }
                    .btn { padding: 12px 24px !important; font-size: 14px !important; }
                }
            </style>
        </head>
        <body>
            <div class="email-wrapper">
                <div class="email-card">
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
            <h2>Reset your password</h2>
            <p>We received a request to reset the password for your Crescendo account.
               Click the button below to choose a new password.</p>
            <div class="btn-container">
                <a href="%s" class="btn">Reset Password</a>
            </div>
            <div class="info-box">
                <p>&#128274; This link expires in <strong>1 hour</strong> and can only be used once.</p>
            </div>
            <div class="divider"></div>
            <p>If you didn't request this, no action is needed — your password hasn't changed.
               If you're concerned, please contact us at <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            """.formatted(resetUrl);
        return render(content);
    }

    public static String renderEmailVerification(String verifyUrl) {
        String content = """
            <h2>Verify your email address</h2>
            <p>Welcome to Crescendo! You're one step away from getting started.
               Please verify your email address to activate your account and start building workflows.</p>
            <div class="btn-container">
                <a href="%s" class="btn">Verify Email Address</a>
            </div>
            <div class="info-box">
                <p>&#128274; This link expires in <strong>24 hours</strong>.</p>
            </div>
            <div class="divider"></div>
            <p>If you didn't create a Crescendo account, you can safely ignore this email.</p>
            """.formatted(verifyUrl);
        return render(content);
    }

    public static String renderPasskeyRecovery(String recoveryUrl) {
        String content = """
            <h2>Recover your passkey</h2>
            <p>You requested a passkey recovery link for your Crescendo account.
               Use the button below to add a replacement passkey.</p>
            <div class="btn-container">
                <a href="%s" class="btn">Recover Passkey</a>
            </div>
            <div class="warning-box">
                <p>&#9888;&#65039; This link expires in <strong>10 minutes</strong>. It cannot be used to access your account
                   or change your password — only to register a new passkey.</p>
            </div>
            <div class="divider"></div>
            <p>If you didn't request this, please contact support immediately at
               <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            """.formatted(recoveryUrl);
        return render(content);
    }

    public static String renderPasswordlessSignupOtp(String otp) {
        String content = """
            <h2>Your verification code</h2>
            <p>Enter this code to continue creating your passwordless Crescendo account.
               The code is valid for <strong>10 minutes</strong>.</p>
            <div class="code-block">%s</div>
            <div class="warning-box">
                <p>&#128274; Never share this code with anyone. Crescendo will never ask for it.</p>
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
            <h2>You already have an account</h2>
            <p>Someone (hopefully you) just tried to sign up for Crescendo using this email address,
               but an account already exists with this email.</p>
            <p>If that was you, simply sign in to your existing account. You can then add a passkey at
               any time from your security settings.</p>
            <div class="btn-container">
                <a href="https://app.crescendo.run/login" class="btn">Sign In</a>
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
            <h2>Welcome to Crescendo, %s! &#127881;</h2>
            <p>Your account is set up and ready to go. You can now start automating your workflows
               and connecting your favourite apps — all in one place.</p>
            <div class="btn-container">
                <a href="https://app.crescendo.run/dashboard" class="btn">Open Workflow Studio</a>
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
            <h2>Your password was changed</h2>
            <p>The password for your Crescendo account was successfully updated.</p>
            <div class="info-box">
                <p>&#9989; This change was made at the time you received this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>&#9888;&#65039; If you did <strong>not</strong> make this change, please reset your password immediately
                   and contact us at <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            </div>
            """;
        return render(content);
    }

    public static String renderTotpEnabled() {
        String content = """
            <h2>Two-factor authentication enabled</h2>
            <p>Two-factor authentication (TOTP) has been successfully enabled on your Crescendo account.
               You will now need your authenticator app each time you sign in.</p>
            <div class="info-box">
                <p>&#128274; Your account is now significantly more secure. Make sure to save your backup codes
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
            <h2>Two-factor authentication disabled</h2>
            <p>Two-factor authentication (TOTP) has been removed from your Crescendo account.</p>
            <div class="warning-box">
                <p>&#9888;&#65039; Your account is now less protected. If you did <strong>not</strong> authorize this change,
                   please reset your password immediately and contact us at
                   <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            </div>
            """;
        return render(content);
    }

    public static String renderLoginAlert(String device, String location) {
        String content = """
            <h2>New sign-in detected</h2>
            <p>We noticed a new sign-in to your Crescendo account from an unrecognized device or location.</p>
            <ul>
                <li><strong>Device:</strong> %s</li>
                <li><strong>Location:</strong> %s</li>
            </ul>
            <div class="info-box">
                <p>&#10003; If this was you, you can safely ignore this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>&#9888;&#65039; If you don't recognise this sign-in, please change your password immediately and contact
                   us at <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            </div>
            """.formatted(device, location);
        return render(content);
    }
    public static String renderPasskeyAdded(String passkeyName) {
        String safeName = (passkeyName != null && !passkeyName.isBlank()) ? passkeyName : "a new passkey";
        String content = """
            <h2>A passkey was added to your account</h2>
            <p>A new passkey (<strong>%s</strong>) was just registered to your Crescendo account.
               You can now use this passkey to sign in securely.</p>
            <div class="info-box">
                <p>&#9989; This change was made at the time you received this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>&#9888;&#65039; If you did <strong>not</strong> add this passkey, please sign in, remove it from your security settings immediately,
                   and contact us at <a href="mailto:support@crescendo.run">support@crescendo.run</a>.</p>
            </div>
            """.formatted(safeName);
        return render(content);
    }
    public static String renderSmartLoginAlert(String device, String location, String country, String revokeUrl) {
        String locationDisplay = location;
        if (country != null && !country.isBlank() && !location.contains(country)) {
            locationDisplay = location + " (" + country + ")";
        }
        String content = """
            <h2>New sign-in detected</h2>
            <p>We noticed a new sign-in to your Crescendo account from an unrecognized device or location.</p>
            <ul>
                <li><strong>Device:</strong> %s</li>
                <li><strong>Location:</strong> %s</li>
            </ul>
            <div class="info-box">
                <p>&#10003; If this was you, you can safely ignore this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>&#9888;&#65039; If you don't recognise this sign-in, revoke access immediately using the button below, then change your password.</p>
            </div>
            <div class="btn-container" style="margin-top: 24px;">
                <a href="%s" class="btn btn-danger">Revoke Access</a>
            </div>
            """.formatted(device, locationDisplay, revokeUrl);
        return render(content);
    }

    public static String renderSuspiciousActivity(String originalIp, String newIp, String revokeUrl) {
        String content = """
            <h2>Suspicious session activity detected</h2>
            <p>We detected that one of your active sessions suddenly changed IP addresses across a large geographic distance.</p>
            <ul>
                <li><strong>Original IP:</strong> %s</li>
                <li><strong>New IP:</strong> %s</li>
            </ul>
            <p>While this can sometimes happen if you switch from Wi-Fi to cellular data or use a VPN, it can also indicate that your session was hijacked.</p>
            <div class="info-box">
                <p>&#10003; If you are using a VPN or just travelled, you can safely ignore this email.</p>
            </div>
            <div class="divider"></div>
            <div class="warning-box">
                <p>&#9888;&#65039; If you have not changed locations or enabled a VPN, revoke access immediately using the button below.</p>
            </div>
            <div class="btn-container" style="margin-top: 24px;">
                <a href="%s" class="btn btn-danger">Revoke Session</a>
            </div>
            """.formatted(originalIp, newIp, revokeUrl);
        return render(content);
    }
}
