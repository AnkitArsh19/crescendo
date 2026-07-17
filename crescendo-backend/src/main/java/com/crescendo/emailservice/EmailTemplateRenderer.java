package com.crescendo.emailservice;

/**
 * Renders transactional HTML email templates for Crescendo.
 *
 * Design principles:
 *  - Dual-mode: works beautifully in both dark and light mode email clients.
 *  - Inline SVG logo: no image hosting required, renders everywhere.
 *  - Google Fonts (Inter): same font as the frontend, loaded with a @import fallback.
 *  - Table-based layout: maximum compatibility with legacy email clients (Outlook, Yahoo).
 *  - All styles inlined or in a single <style> block; no external CSS files.
 */
public class EmailTemplateRenderer {

    // Inline SVG logo paths — neutral color filled dynamically per mode via CSS class
    private static final String LOGO_SVG_PATHS =
        "<path d=\"M60.5 98.1878L134.5 132.688V93.6878L60.5 59.1878V98.1878Z\"/>" +
        "<path d=\"M86.5 193.688L51.5 217.188L4.5 187.188V143.188L86.5 193.688Z\"/>" +
        "<path d=\"M24 144.688L7 134.688L56 104.688L75.5 114.188L24 144.688Z\"/>" +
        "<path d=\"M140 86.1879L63 51.6878L114 30.6878L187.5 59.1878L140 86.1879Z\"/>" +
        "<path d=\"M56 267.688L157 285.188L128 221.188L92 244.438L56 267.688Z\"/>" +
        "<path d=\"M119.5 25.1878L191.5 53.1879L182.5 0.687897L119.5 25.1878Z\"/>" +
        "<path d=\"M124 178.188L53 225.688V257.688L124 211.688V178.188Z\"/>" +
        "<path d=\"M122 170.188L94.5 187.188L31.5 149.188L58 134.188L122 170.188Z\"/>";

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
            <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
            <style>
                /* ── Reset ── */
                * { box-sizing: border-box; }
                body, table, td, p, a, li, blockquote {
                    -webkit-text-size-adjust: 100%;
                    -ms-text-size-adjust: 100%;
                }
                table, td { border-collapse: collapse; }
                img { border: 0; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; }

                /* ── Base (light mode default) ── */
                body {
                    margin: 0; padding: 0;
                    background-color: #f0f2f5;
                    color: #1a1a2e;
                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                    -webkit-font-smoothing: antialiased;
                }
                .email-wrapper {
                    background-color: #f0f2f5;
                    padding: 40px 16px;
                }
                .email-card {
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 16px;
                    border: 1px solid #e2e8f0;
                    overflow: hidden;
                    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
                }
                .email-header {
                    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 60%, #0f3460 100%);
                    padding: 36px 40px;
                    text-align: center;
                }
                .logo-container {
                    display: inline-flex;
                    align-items: center;
                    gap: 12px;
                    text-decoration: none;
                }
                .logo-svg {
                    width: 32px;
                    height: auto;
                    fill: #ffffff;
                }
                .logo-text {
                    font-family: 'Inter', sans-serif;
                    font-size: 22px;
                    font-weight: 700;
                    letter-spacing: -0.5px;
                    color: #ffffff;
                    vertical-align: middle;
                }
                .logo-text span {
                    color: #60a5fa;
                }
                .email-body {
                    padding: 40px;
                }
                .email-body h2 {
                    font-family: 'Inter', sans-serif;
                    font-size: 22px;
                    font-weight: 700;
                    color: #0f172a;
                    margin: 0 0 16px;
                    letter-spacing: -0.3px;
                    line-height: 1.3;
                }
                .email-body p {
                    font-size: 15px;
                    line-height: 1.7;
                    color: #475569;
                    margin: 0 0 16px;
                }
                .email-body p:last-child { margin-bottom: 0; }
                .email-body a {
                    color: #3b82f6;
                    text-decoration: underline;
                }
                .email-body ul {
                    padding-left: 20px;
                    color: #475569;
                    font-size: 15px;
                    line-height: 1.7;
                }
                .email-body ul li { margin-bottom: 6px; }
                .email-body ul li strong { color: #0f172a; }
                .btn-container {
                    text-align: center;
                    margin: 32px 0;
                }
                .btn {
                    display: inline-block;
                    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
                    color: #ffffff !important;
                    text-decoration: none !important;
                    padding: 14px 32px;
                    border-radius: 10px;
                    font-family: 'Inter', sans-serif;
                    font-size: 15px;
                    font-weight: 600;
                    letter-spacing: 0.2px;
                    box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);
                }
                .code-block {
                    background: #f8fafc;
                    border: 1.5px solid #e2e8f0;
                    border-radius: 10px;
                    padding: 20px;
                    text-align: center;
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 28px;
                    font-weight: 700;
                    letter-spacing: 8px;
                    color: #1a1a2e;
                    margin: 24px 0;
                }
                .divider {
                    border: none;
                    border-top: 1px solid #e2e8f0;
                    margin: 32px 0;
                }
                .email-footer {
                    background-color: #f8fafc;
                    border-top: 1px solid #e2e8f0;
                    padding: 24px 40px;
                    text-align: center;
                }
                .email-footer p {
                    font-size: 12px;
                    color: #94a3b8;
                    margin: 0 0 4px;
                    line-height: 1.6;
                }
                .email-footer a {
                    color: #94a3b8;
                    text-decoration: underline;
                }
                .info-box {
                    background: #eff6ff;
                    border-left: 4px solid #3b82f6;
                    border-radius: 0 8px 8px 0;
                    padding: 14px 18px;
                    margin: 20px 0;
                }
                .info-box p {
                    font-size: 13px;
                    color: #1e40af;
                    margin: 0;
                    line-height: 1.5;
                }
                .warning-box {
                    background: #fefce8;
                    border-left: 4px solid #eab308;
                    border-radius: 0 8px 8px 0;
                    padding: 14px 18px;
                    margin: 20px 0;
                }
                .warning-box p {
                    font-size: 13px;
                    color: #713f12;
                    margin: 0;
                    line-height: 1.5;
                }

                /* ── Dark mode overrides ── */
                @media (prefers-color-scheme: dark) {
                    body, .email-wrapper { background-color: #0d0d0d !important; }
                    .email-card {
                        background-color: #141414 !important;
                        border-color: rgba(255,255,255,0.08) !important;
                        box-shadow: 0 4px 32px rgba(0,0,0,0.6) !important;
                    }
                    .email-header {
                        background: linear-gradient(135deg, #0d0d0d 0%, #111827 60%, #1a2744 100%) !important;
                    }
                    .email-body h2 { color: #f1f5f9 !important; }
                    .email-body p { color: #94a3b8 !important; }
                    .email-body ul { color: #94a3b8 !important; }
                    .email-body ul li strong { color: #f1f5f9 !important; }
                    .email-body a { color: #60a5fa !important; }
                    .code-block {
                        background: rgba(255,255,255,0.05) !important;
                        border-color: rgba(255,255,255,0.12) !important;
                        color: #f1f5f9 !important;
                    }
                    .divider { border-color: rgba(255,255,255,0.08) !important; }
                    .email-footer {
                        background-color: #0a0a0a !important;
                        border-color: rgba(255,255,255,0.06) !important;
                    }
                    .email-footer p, .email-footer a { color: #475569 !important; }
                    .info-box {
                        background: rgba(59,130,246,0.1) !important;
                        border-color: #3b82f6 !important;
                    }
                    .info-box p { color: #93c5fd !important; }
                    .warning-box {
                        background: rgba(234,179,8,0.1) !important;
                        border-color: #eab308 !important;
                    }
                    .warning-box p { color: #fde68a !important; }
                }

                /* ── Responsive ── */
                @media only screen and (max-width: 620px) {
                    .email-header { padding: 28px 24px !important; }
                    .email-body { padding: 28px 24px !important; }
                    .email-footer { padding: 20px 24px !important; }
                    .email-body h2 { font-size: 19px !important; }
                    .btn { padding: 12px 24px !important; font-size: 14px !important; }
                }
            </style>
        </head>
        <body>
            <div class="email-wrapper">
                <div class="email-card">
                    <!-- Header -->
                    <div class="email-header">
                        <a href="https://app.crescendo.run" class="logo-container">
                            <svg class="logo-svg" viewBox="0 0 197 294" fill="none" xmlns="http://www.w3.org/2000/svg">
                                %s
                            </svg>
                            <span class="logo-text">Crescendo<span>.</span></span>
                        </a>
                    </div>

                    <!-- Body -->
                    <div class="email-body">
                        {{CONTENT}}
                    </div>

                    <!-- Footer -->
                    <div class="email-footer">
                        <p>&copy; 2026 Crescendo Inc. All rights reserved.</p>
                        <p>If you didn't request this email, you can safely ignore it — no changes have been made to your account.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(LOGO_SVG_PATHS);

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
                <a href="https://app.crescendo.run/dashboard" class="btn">Go to Dashboard</a>
            </div>
            <div class="divider"></div>
            <p>Need help getting started? Visit our
               <a href="https://docs.crescendo.run">documentation</a> or reach out to us at
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
                <a href="%s" class="btn" style="background-color: #dc2626; border-color: #dc2626;">Revoke Access</a>
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
                <a href="%s" class="btn" style="background-color: #dc2626; border-color: #dc2626;">Revoke Session</a>
            </div>
            """.formatted(originalIp, newIp, revokeUrl);
        return render(content);
    }
}
