package com.crescendo.emailservice.tracking;

import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrackingInjectorTest {

    @Test
    void instrumentsOnlyHttpLinks_andPlacesOpenPixelBeforeBodyClose() {
        UUID emailId = UUID.randomUUID();
        String baseUrl = "https://api.example.test";
        String html = "<body><a href=\"https://example.com/a?b=1\">Read</a>"
                + "<a href=\"mailto:hello@example.com\">Email</a></body>";

        String withClicks = TrackingInjector.rewriteClickLinks(html, emailId, baseUrl);
        String instrumented = TrackingInjector.injectOpenPixel(withClicks, emailId, baseUrl);

        String expectedUrl = URLEncoder.encode("https://example.com/a?b=1", StandardCharsets.UTF_8);
        assertTrue(instrumented.contains(baseUrl + "/t/c/" + emailId + "?url=" + expectedUrl));
        assertTrue(instrumented.contains("href=\"mailto:hello@example.com\""));
        assertTrue(instrumented.indexOf("/t/o/" + emailId) < instrumented.indexOf("</body>"));
    }
}
