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
}
