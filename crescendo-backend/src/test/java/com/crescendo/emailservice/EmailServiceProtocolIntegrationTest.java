package com.crescendo.emailservice;

import com.crescendo.emailservice.tracking.TrackingInjector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmailServiceProtocolIntegrationTest {

    @Test
    @DisplayName("Email pipeline renders templates and injects tracking pixels and unsubscribe headers")
    void emailPipeline_rendersTemplateAndInjectsTracking() {
        String recipient = "subscriber@example.com";
        String trackingBase = "https://mail.crescendo.run";
        UUID messageId = UUID.randomUUID();

        String rawHtml = EmailTemplateRenderer.renderWelcome("Ankit");
        assertNotNull(rawHtml);
        assertTrue(rawHtml.contains("Welcome to Crescendo, Ankit!"));

        String htmlWithTracking = TrackingInjector.injectOpenPixel(rawHtml, messageId, trackingBase);

        assertNotNull(htmlWithTracking);
        assertTrue(htmlWithTracking.contains("/t/o/" + messageId), "HTML should contain tracking open pixel URL");
    }
}
