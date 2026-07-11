package com.crescendo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crescendo Public API — OpenAPI (Swagger) configuration.
 *
 * Defines a single "public" API group that covers:
 *   - com.crescendo.publicapi          (Workflows, Runs, Connections, App Catalog;
 *                                        springdoc prefix-matches sub-packages like publicapi.email)
 *   - com.crescendo.emailservice.email_send    (POST /api/v1/emails)
 *   - com.crescendo.emailservice.email_log     (GET /api/v1/emails)
 *   - com.crescendo.emailservice.customevent   (POST /api/v1/email/custom-events)
 *
 * Internal controllers (DMARC ingestion, unsubscribe handler, dashboard
 * settings, auth) are deliberately excluded — they live in separate packages
 * not listed here.
 *
 * API key authentication is declared globally so generated clients produce
 * a clean new Client(apiKey) constructor.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Crescendo API",
        version = "v1",
        description = "The Crescendo API powers workflow automation and transactional email at scale. " +
                      "Authenticate with a Bearer API key (prefix: `re_...`) obtained from the dashboard.",
        contact = @Contact(
            name = "Crescendo Developer Support",
            url = "https://crescendo.run/docs"
        ),
        license = @License(
            name = "MIT",
            url = "https://github.com/AnkitArsh19/crescendo/blob/main/LICENSE"
        )
    ),
    servers = {
        @Server(url = "https://api.crescendo.run", description = "Production"),
        @Server(url = "http://localhost:8080", description = "Local Development")
    }
)
@SecurityScheme(
    name = "ApiKeyAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "API Key",
    in = SecuritySchemeIn.HEADER,
    description = "Your Crescendo API key. Obtain one from the dashboard Settings → API Keys. " +
                  "All API keys are prefixed with `re_`."
)
@Configuration
public class OpenApiConfig {

    /**
     * The single public-facing API group. Only controllers in the explicitly listed
     * packages are included in the generated spec — any future internal controller
     * must be placed outside these packages to remain hidden.
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("crescendo-public-api-v1")
                .packagesToScan(
                        // Workflow automation + publicapi.email (webhooks, templates, metrics, domains,
                        // suppressions, contacts) — springdoc uses prefix matching so sub-packages
                        // are automatically included without explicit listing.
                        "com.crescendo.publicapi",
                        // Email service — public send and log endpoints
                        "com.crescendo.emailservice.email_send",
                        "com.crescendo.emailservice.email_log",
                        // Custom events API
                        "com.crescendo.emailservice.customevent"
                        // NOTE: com.crescendo.emailservice.outboundwebhook was removed —
                        // WebhookSubscriptionController (the only public class) was deleted.
                        // Outbound webhook management is now under com.crescendo.publicapi.email
                        // (PublicWebhookController at /api/v1/webhooks).
                )
                .build();
    }
}
