package com.crescendo.emailservice.provider;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "crescendo.email.provider", havingValue = "brevo")
public class BrevoEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailProvider.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BrevoEmailProvider(@Value("${brevo.api.key:}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public EmailSendResult send(EmailMessage message) {
        if (apiKey == null || apiKey.isBlank()) {
            return new EmailSendResult(false, null, "Brevo API key is not configured.");
        }

        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("sender", Map.of("email", message.from()));
            payload.put("to", List.of(Map.of("email", message.to())));
            payload.put("subject", message.subject());
            if (message.emailType() == com.crescendo.enums.EmailType.MARKETING) {
                String logId = message.idempotencyKey() != null ? message.idempotencyKey() : "unknown";
                String unsubUrl = "https://app.crescendo.run/api/v1/unsubscribe/" + logId;
                payload.put("headers", Map.of(
                        "List-Unsubscribe", "<" + unsubUrl + ">",
                        "List-Unsubscribe-Post", "List-Unsubscribe=One-Click"
                ));
                
                String unsubFooterHtml = "<br><br><p style='font-size:12px;color:#666;'>You received this email because you are subscribed to updates. <a href='" + unsubUrl + "'>Unsubscribe</a></p>";
                String unsubFooterText = "\n\nTo unsubscribe, visit: " + unsubUrl;

                if (message.htmlBody() != null) {
                    payload.put("htmlContent", message.htmlBody() + unsubFooterHtml);
                }
                if (message.textBody() != null) {
                    payload.put("textContent", message.textBody() + unsubFooterText);
                }
            } else {
                if (message.htmlBody() != null) {
                    payload.put("htmlContent", message.htmlBody());
                }
                if (message.textBody() != null) {
                    payload.put("textContent", message.textBody());
                }
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            if (message.idempotencyKey() != null) {
                requestBuilder.header("Idempotency-Key", message.idempotencyKey());
            }

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Parse messageId from response
                // {"messageId":"<...>" }
                @SuppressWarnings("unchecked")
                Map<String, String> responseBody = objectMapper.readValue(response.body(), Map.class);
                String messageId = responseBody.get("messageId");
                return new EmailSendResult(true, messageId, null);
            } else {
                log.error("[Brevo] API Error: HTTP {} - {}", response.statusCode(), response.body());
                return new EmailSendResult(false, null, "Brevo API Error: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("[Brevo] Exception sending email", e);
            return new EmailSendResult(false, null, e.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "brevo";
    }
}
