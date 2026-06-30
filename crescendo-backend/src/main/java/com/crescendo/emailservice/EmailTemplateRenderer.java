package com.crescendo.emailservice;

public class EmailTemplateRenderer {

    private static final String BASE_LAYOUT = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    background-color: #0d0d0d;
                    color: #ffffff;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    -webkit-font-smoothing: antialiased;
                }
                .container {
                    max-width: 600px;
                    margin: 40px auto;
                    background: rgba(255, 255, 255, 0.03);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    border-radius: 12px;
                    padding: 32px;
                    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
                }
                .header {
                    text-align: center;
                    margin-bottom: 32px;
                }
                .logo {
                    font-size: 24px;
                    font-weight: 700;
                    letter-spacing: -0.5px;
                    color: #ffffff;
                    text-decoration: none;
                }
                .logo span {
                    color: #3b82f6; /* Accent color matching frontend */
                }
                .content {
                    font-size: 16px;
                    line-height: 1.6;
                    color: #d1d5db;
                }
                .content h2 {
                    color: #ffffff;
                    font-size: 20px;
                    margin-top: 0;
                    margin-bottom: 16px;
                    font-weight: 600;
                }
                .btn {
                    display: inline-block;
                    background-color: #3b82f6;
                    color: #ffffff !important;
                    text-decoration: none;
                    padding: 12px 24px;
                    border-radius: 6px;
                    font-weight: 500;
                    margin-top: 24px;
                    margin-bottom: 24px;
                    text-align: center;
                }
                .code-block {
                    background: rgba(255, 255, 255, 0.05);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    padding: 16px;
                    border-radius: 6px;
                    font-family: monospace;
                    font-size: 20px;
                    text-align: center;
                    letter-spacing: 4px;
                    margin: 24px 0;
                    color: #ffffff;
                }
                .footer {
                    margin-top: 40px;
                    padding-top: 24px;
                    border-top: 1px solid rgba(255, 255, 255, 0.1);
                    font-size: 13px;
                    color: #9ca3af;
                    text-align: center;
                }
                .footer a {
                    color: #9ca3af;
                    text-decoration: underline;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">Crescendo<span>.</span></div>
                </div>
                <div class="content">
                    {{CONTENT}}
                </div>
                <div class="footer">
                    <p>&copy; 2026 Crescendo Inc. All rights reserved.</p>
                    <p>If you didn't request this email, you can safely ignore it.</p>
                </div>
            </div>
        </body>
        </html>
        """;

    public static String renderPasswordReset(String resetUrl) {
        String content = """
            <h2>Reset your password</h2>
            <p>We received a request to reset the password for your Crescendo account. Click the button below to choose a new password.</p>
            <a href="{{url}}" class="btn">Reset Password</a>
            <p>This link will expire in 1 hour.</p>
            """;
        return BASE_LAYOUT.replace("{{CONTENT}}", content.replace("{{url}}", resetUrl));
    }

    public static String renderEmailVerification(String verifyUrl) {
        String content = """
            <h2>Verify your email</h2>
            <p>Welcome to Crescendo! Please verify your email address to activate your account and start building workflows.</p>
            <a href="{{url}}" class="btn">Verify Email</a>
            <p>This link will expire in 24 hours.</p>
            """;
        return BASE_LAYOUT.replace("{{CONTENT}}", content.replace("{{url}}", verifyUrl));
    }

    public static String renderWelcome(String name) {
        String content = """
            <h2>Welcome to Crescendo, {{name}}!</h2>
            <p>Your account is fully set up. You can now start automating your workflows and integrating your favorite apps seamlessly.</p>
            <p>Head over to your dashboard to create your first workflow.</p>
            <a href="https://app.crescendo.run/dashboard" class="btn">Go to Dashboard</a>
            """;
        return BASE_LAYOUT.replace("{{CONTENT}}", content.replace("{{name}}", name != null ? name : "there"));
    }

    public static String renderDeleteAccount() {
        String content = """
            <h2>Account Deleted</h2>
            <p>Your Crescendo account and all associated data have been permanently deleted as requested.</p>
            <p>We're sorry to see you go. If you ever want to return, you can always create a new account.</p>
            """;
        return BASE_LAYOUT.replace("{{CONTENT}}", content);
    }

    public static String renderPasswordChanged() {
        String content = """
            <h2>Password Changed Successfully</h2>
            <p>The password for your Crescendo account has been updated.</p>
            <p>If you did not make this change, please contact support immediately to secure your account.</p>
            """;
        return BASE_LAYOUT.replace("{{CONTENT}}", content);
    }

    public static String renderTotpEnabled() {
        String content = """
            <h2>Two-Factor Authentication Enabled</h2>
            <p>You have successfully enabled Two-Factor Authentication (TOTP) on your Crescendo account.</p>
            <p>Your account is now more secure. You will be required to enter a code from your authenticator app when signing in.</p>
            """;
        return BASE_LAYOUT.replace("{{CONTENT}}", content);
    }

    public static String renderTotpDisabled() {
        String content = """
            <h2>Two-Factor Authentication Disabled</h2>
            <p>Two-Factor Authentication (TOTP) has been removed from your Crescendo account.</p>
            <p>Your account is now less secure. If you did not authorize this change, please reset your password immediately.</p>
            """;
        return BASE_LAYOUT.replace("{{CONTENT}}", content);
    }

    public static String renderLoginAlert(String device, String location) {
        String content = """
            <h2>New Login Detected</h2>
            <p>We noticed a new login to your Crescendo account from an unrecognized device.</p>
            <ul>
                <li><strong>Device:</strong> {{device}}</li>
                <li><strong>Location:</strong> {{location}}</li>
            </ul>
            <p>If this was you, you can safely ignore this email. If not, please change your password immediately.</p>
            """;
        return BASE_LAYOUT.replace("{{CONTENT}}", content.replace("{{device}}", device).replace("{{location}}", location));
    }
}
