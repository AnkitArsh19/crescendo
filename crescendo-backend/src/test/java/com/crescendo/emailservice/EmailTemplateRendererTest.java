package com.crescendo.emailservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateRendererTest {

    @Test
    @DisplayName("Password reset email contains resetUrl and expiry notice")
    void passwordResetTemplate() {
        String url = "https://app.crescendo.run/reset-password?token=abc123token";
        String html = EmailTemplateRenderer.renderPasswordReset(url);

        assertNotNull(html);
        assertTrue(html.contains(url), "HTML should contain the reset URL");
        assertTrue(html.contains("Reset your password"), "HTML should contain the title header");
        assertTrue(html.contains("1 hour"), "HTML should state link expiry time");
    }

    @Test
    @DisplayName("Email verification email contains verifyUrl")
    void emailVerificationTemplate() {
        String url = "https://app.crescendo.run/verify-email?token=xyz789";
        String html = EmailTemplateRenderer.renderEmailVerification(url);

        assertNotNull(html);
        assertTrue(html.contains(url), "HTML should contain verification URL");
        assertTrue(html.contains("Verify your email address"), "HTML should contain title header");
        assertTrue(html.contains("24 hours"), "HTML should state 24 hours expiry");
    }

    @Test
    @DisplayName("Passwordless OTP email contains OTP code")
    void passwordlessSignupOtpTemplate() {
        String otp = "849201";
        String html = EmailTemplateRenderer.renderPasswordlessSignupOtp(otp);

        assertNotNull(html);
        assertTrue(html.contains(otp), "HTML should contain OTP code");
        assertTrue(html.contains("Your verification code"), "HTML should contain title header");
    }

    @Test
    @DisplayName("Welcome email substitutes recipient name")
    void welcomeTemplateSubstitutesName() {
        String html = EmailTemplateRenderer.renderWelcome("Alice");

        assertNotNull(html);
        assertTrue(html.contains("Welcome to Crescendo, Alice!"), "HTML should greet Alice by name");
    }

    @Test
    @DisplayName("Welcome email uses default fallback name when blank")
    void welcomeTemplateFallback() {
        String html = EmailTemplateRenderer.renderWelcome("");

        assertNotNull(html);
        assertTrue(html.contains("Welcome to Crescendo, there!"), "HTML should fallback to 'there'");
    }

    @Test
    @DisplayName("Login alert contains device and location")
    void loginAlertTemplate() {
        String html = EmailTemplateRenderer.renderLoginAlert("Chrome / macOS", "London, UK");

        assertNotNull(html);
        assertTrue(html.contains("Chrome / macOS"), "HTML should contain device name");
        assertTrue(html.contains("London, UK"), "HTML should contain location");
    }

    @Test
    @DisplayName("Base layout incorporates Funnel Display, hosted logo, dark mode styles, and transactional footer")
    void baseLayoutFeatures() {
        String html = EmailTemplateRenderer.renderWelcome("Alice");

        // Landing page font alignment
        assertTrue(html.contains("Funnel+Display"), "HTML should link to Funnel Display Google Font");
        assertTrue(html.contains("Inter"), "HTML should link to Inter Google Font");

        // Hosted PNG logo for 100% email client compatibility
        assertTrue(html.contains("https://app.crescendo.run/logo-app-light.png"), "HTML should include hosted logo image URL");
        assertTrue(html.contains("class=\"logo-img\""), "HTML should style logo img");

        // Light & dark mode support
        assertTrue(html.contains("@media (prefers-color-scheme: dark)"), "HTML should include dark mode styles");
        assertTrue(html.contains("#09090b"), "HTML should use Crescendo zinc brand dark color");

        // Transactional compliance footer
        assertTrue(html.contains("https://app.crescendo.run/settings/notifications"), "Footer should link to notification preferences");
        assertTrue(html.contains("https://app.crescendo.run/settings/security"), "Footer should link to security settings");
        assertTrue(html.contains("https://app.crescendo.run/privacy"), "Footer should link to privacy policy");
        assertTrue(html.contains("https://app.crescendo.run/terms"), "Footer should link to terms of service");
    }

    @Test
    @DisplayName("Suspicious activity email contains original/new locations, IPs, and mobile-safe fixed table layout")
    void suspiciousActivityTemplate() {
        String origLoc = "New Delhi, Delhi, India";
        String origIp = "2401:4900:7160:f461:5823:d42d:e784";
        String newLoc = "Bengaluru, Karnataka, India";
        String newIp = "2401:4900:3e80:f430:ed81:afed:d138";
        String revokeUrl = "https://app.crescendo.run/auth/revoke-session?token=test-token";

        String html = EmailTemplateRenderer.renderSuspiciousActivity(origLoc, origIp, newLoc, newIp, "Rapid Geo-IP Shift", revokeUrl);

        assertNotNull(html);
        assertTrue(html.contains(origLoc), "HTML should contain original location");
        assertTrue(html.contains(origIp), "HTML should contain original IP");
        assertTrue(html.contains(newLoc), "HTML should contain new location");
        assertTrue(html.contains(newIp), "HTML should contain new IP");
        assertTrue(html.contains("Rapid Geo-IP Shift"), "HTML should contain activity type");
        assertTrue(html.contains(revokeUrl), "HTML should contain revoke URL");
        assertTrue(html.contains("table-layout: fixed"), "HTML table must enforce fixed layout to prevent mobile overflow");
        assertTrue(html.contains("word-break: break-all"), "Monospace IP must have word-break: break-all for IPv6 wrapping");
    }

    @Test
    @DisplayName("Smart login alert email cleanly splits location and IP address and applies mobile-safe styling")
    void smartLoginAlertTemplate() {
        String locWithIp = "New Delhi, Delhi, India (IP: 2401:4900:7160:f461:5823:d42d:e784)";
        String revokeUrl = "https://app.crescendo.run/auth/revoke-session?token=test-token";

        String html = EmailTemplateRenderer.renderSmartLoginAlert("Chrome on Windows", locWithIp, "IN", revokeUrl);

        assertNotNull(html);
        assertTrue(html.contains("Chrome on Windows"), "HTML should contain device");
        assertTrue(html.contains("New Delhi, Delhi, India"), "HTML should contain location");
        assertTrue(html.contains("2401:4900:7160:f461:5823:d42d:e784"), "HTML should contain IP address");
        assertTrue(html.contains("table-layout: fixed"), "HTML table must enforce fixed layout");
    }
}
