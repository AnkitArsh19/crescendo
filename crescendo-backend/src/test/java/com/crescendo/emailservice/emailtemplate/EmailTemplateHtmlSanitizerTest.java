package com.crescendo.emailservice.emailtemplate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateHtmlSanitizerTest {

    private final EmailTemplateHtmlSanitizer sanitizer = new EmailTemplateHtmlSanitizer();

    @Test
    void sanitize_keepsEmailMarkupButRemovesExecutableContent() {
        String sanitized = sanitizer.sanitize("""
                <html><body onload="alert(1)">
                  <script>alert(1)</script><form action="/steal"><input></form>
                  <a href="javascript:alert(1)" onclick="alert(1)">Read</a>
                  <img src="https://images.example/logo.png" onerror="alert(1)">
                  <p style="color: #111">Safe content</p>
                </body></html>
                """);

        assertThat(sanitized)
                .contains("Safe content")
                .contains("https://images.example/logo.png")
                .doesNotContain("<script")
                .doesNotContain("<form")
                .doesNotContain("onload")
                .doesNotContain("onclick")
                .doesNotContain("onerror")
                .doesNotContain("javascript:");
    }
}
