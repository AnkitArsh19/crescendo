package com.crescendo.emailservice.domain;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves the Domain Connect template definition.
 *
 * IMPORTANT: Domain Connect is a PRE-REGISTRATION model. DNS providers (Cloudflare, GoDaddy, etc.)
 * do NOT fetch this template live at runtime. Instead:
 *  1. The template JSON is submitted to github.com/domain-connect/templates
 *  2. Each DNS provider manually onboards / pre-loads it into their system
 *  3. At runtime, the DNS provider uses their pre-loaded copy
 *
 * This endpoint is kept for completeness and for DNS providers that DO fetch the template
 * dynamically (some implementations check the URL). The template content matches
 * the crescendo.run.email.json file exactly.
 *
 * The runtime flow is:
 *   1. Crescendo discovers user's DNS provider via _domainconnect TXT record
 *   2. Crescendo builds a signed redirect URL to the DNS provider's sync UX
 *   3. DNS provider shows confirmation to user (using pre-loaded template)
 *   4. User approves → DNS records created automatically
 *   5. User redirected back to app.crescendo.run
 *
 * See: domain-connect/SETUP.md for full setup instructions.
 */
@RestController
@RequestMapping("/v2/domainTemplates/providers")
public class DomainConnectTemplateController {

    // Must match DomainConnectService.PROVIDER_ID and the filename crescendo.run.email.json
    private static final String PROVIDER_ID = "crescendo.run";
    private static final String SERVICE_ID = "email";

    /**
     * Serves the template JSON at the standard Domain Connect path.
     * Some DNS providers fetch this URL to validate template variables at apply-time.
     */
    @GetMapping(
            value = {"/{providerId}/services/{serviceId}", "/{providerId}/services/{serviceId}/{version}"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> getTemplate(
            @PathVariable String providerId,
            @PathVariable String serviceId,
            @PathVariable(required = false) String version) {

        if (!PROVIDER_ID.equals(providerId) || !SERVICE_ID.equals(serviceId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildTemplate());
    }

    private Map<String, Object> buildTemplate() {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("providerId", PROVIDER_ID);
        t.put("providerName", "Crescendo");
        t.put("serviceId", SERVICE_ID);
        t.put("serviceName", "Crescendo Email Service");
        t.put("version", 2);
        t.put("logoUrl", "https://crescendo.run/logo.svg");
        t.put("description", "Configures domain ownership verification, SPF, DKIM, and DMARC for Crescendo email delivery.");
        t.put("variableDescription", "%token%: verification token for domain ownership; %dkim_pub_key%: Public key for DKIM signature");
        t.put("syncBlock", false);
        t.put("syncPubKeyDomain", "keys.crescendo.run"); // DNS TXT at key1._domainconnect.keys.crescendo.run
        t.put("syncRedirectDomain", "app.crescendo.run");

        t.put("records", List.of(
                // Verification TXT
                Map.of(
                        "type", "TXT",
                        "host", "_crescendo-verify",
                        "ttl", 3600,
                        "data", "crescendo-verify=%token%",
                        "txtConflictMatchingMode", "Prefix",
                        "txtConflictMatchingPrefix", "crescendo-verify="
                ),
                // SPF merge (SPFM = safe merge, does not overwrite existing SPF)
                Map.of(
                        "type", "SPFM",
                        "host", "@",
                        "ttl", 3600,
                        "spfRules", "include:spf.crescendo.run"
                ),
                // DKIM
                Map.of(
                        "type", "TXT",
                        "host", "crescendo._domainkey",
                        "ttl", 3600,
                        "data", "v=DKIM1; k=rsa; p=%dkim_pub_key%",
                        "txtConflictMatchingMode", "Prefix",
                        "txtConflictMatchingPrefix", "v=DKIM1"
                ),
                // DMARC - optional (only applied if none exists)
                Map.of(
                        "type", "TXT",
                        "host", "_dmarc",
                        "ttl", 3600,
                        "data", "v=DMARC1; p=none;",
                        "essential", "OnApply",
                        "txtConflictMatchingMode", "Prefix",
                        "txtConflictMatchingPrefix", "v=DMARC1"
                )
        ));

        return t;
    }
}
