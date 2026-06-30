package com.crescendo.emailservice.provider;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SendGridEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailProvider.class);
    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SendGridEmailProvider(String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public EmailSendResult send(EmailMessage message) {
        if (apiKey == null || apiKey.isBlank()) {
            return new EmailSendResult(false, null, "SendGrid API key is not configured.");
        }

        try {
            // SendGrid v3 payload structure
            Map<String, Object> personalizations = new java.util.HashMap<>();
            personalizations.put("to", List.of(Map.of("email", message.to())));
            
            // Handle idempotency / custom headers
            Map<String, String> headers = new java.util.HashMap<>();
            if (message.idempotencyKey() != null) {
                headers.put("Idempotency-Key", message.idempotencyKey());
            }

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("personalizations", List.of(personalizations));
            payload.put("from", Map.of("email", message.from()));
            payload.put("subject", message.subject());
            
            String htmlContent = message.htmlBody();
            String textContent = message.textBody();

            if (message.emailType() == com.crescendo.enums.EmailType.MARKETING) {
                String logId = message.idempotencyKey() != null ? message.idempotencyKey() : "unknown";
                String unsubUrl = "https://app.crescendo.run/api/v1/unsubscribe/" + logId;
                
                headers.put("List-Unsubscribe", "<" + unsubUrl + ">");
                headers.put("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
                
                String unsubFooterHtml = "<br><br><p style='font-size:12px;color:#666;'>You received this email because you are subscribed to updates. <a href='" + unsubUrl + "'>Unsubscribe</a></p>";
                String unsubFooterText = "\n\nTo unsubscribe, visit: " + unsubUrl;

                if (htmlContent != null) {
                    htmlContent += unsubFooterHtml;
                }
                if (textContent != null) {
                    textContent += unsubFooterText;
                }
            }

            if (!headers.isEmpty()) {
                personalizations.put("headers", headers);
            }

            List<Map<String, String>> content = new java.util.ArrayList<>();
            if (textContent != null) {
                content.add(Map.of("type", "text/plain", "value", textContent));
            }
            if (htmlContent != null) {
                content.add(Map.of("type", "text/html", "value", htmlContent));
            }
            payload.put("content", content);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(SENDGRID_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // SendGrid returns 202 Accepted. The X-Message-Id header contains the message ID.
                String messageId = response.headers().firstValue("x-message-id").orElse(UUID.randomUUID().toString());
                return new EmailSendResult(true, messageId, null);
            } else {
                log.error("[SendGrid] API Error: HTTP {} - {}", response.statusCode(), response.body());
                return new EmailSendResult(false, null, "SendGrid API Error: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("[SendGrid] Exception sending email", e);
            return new EmailSendResult(false, null, e.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "sendgrid";
    }
}
