package com.crescendo.emailservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

@Service
public class DomainConnectService {

    private static final Logger log = LoggerFactory.getLogger(DomainConnectService.class);

    private static final String PROVIDER_ID = "crescendo.run";
    private static final String SERVICE_ID = "email";
    // We will redirect users back to the frontend domains settings page
    private static final String REDIRECT_URI = "https://app.crescendo.run/dashboard/email/domains";

    private final RestTemplate restTemplate;

    public DomainConnectService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Attempts to find the Domain Connect redirect URL for a given domain.
     * @param domain e.g., "example.com"
     * @return Optional containing the URL to redirect the user to, or empty if unsupported.
     */
    public Optional<String> buildSyncUrl(String domain) {
        return discoverApiDomain(domain)
                .flatMap(this::fetchUrlSyncUX)
                .map(urlSyncUx -> constructApplyUrl(urlSyncUx, domain));
    }

    /**
     * Step 1: Perform a DNS TXT lookup on _domainconnect.<domain> to find the API endpoint.
     */
    private Optional<String> discoverApiDomain(String domain) {
        String queryDomain = "_domainconnect." + domain;
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(queryDomain, new String[]{"TXT"});
            Attribute attr = attrs.get("TXT");
            
            if (attr != null) {
                for (int i = 0; i < attr.size(); i++) {
                    String txt = (String) attr.get(i);
                    // Remove quotes if present
                    if (txt.startsWith("\"") && txt.endsWith("\"")) {
                        txt = txt.substring(1, txt.length() - 1);
                    }
                    if (txt.startsWith("domainconnectProviderName")) {
                        continue;
                    }
                    // It should just be the domain, e.g., "domainconnect.godaddy.com"
                    log.info("Domain Connect discovery for {}: Found API domain {}", domain, txt);
                    return Optional.of(txt);
                }
            }
        } catch (Exception e) {
            log.debug("Domain Connect discovery failed or unsupported for {}: {}", domain, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Step 2: Fetch the urlSyncUX from the provider's settings endpoint.
     */
    private Optional<String> fetchUrlSyncUX(String apiDomain) {
        String url = "https://" + apiDomain + "/v2/domainconnect/settings";
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("urlSyncUX")) {
                String syncUx = response.get("urlSyncUX");
                log.info("Domain Connect settings for {}: Found urlSyncUX {}", apiDomain, syncUx);
                return Optional.of(syncUx);
            }
        } catch (Exception e) {
            log.error("Failed to fetch Domain Connect settings from {}", url, e);
        }
        return Optional.empty();
    }

    /**
     * Step 3: Construct the final redirect URL to apply the template.
     */
    private String constructApplyUrl(String urlSyncUx, String domain) {
        // According to specification:
        // {urlSyncUX}/v2/domainTemplates/providers/{providerId}/services/{serviceId}/apply?domain={domain}&redirect_uri={redirect_uri}
        return String.format("%s/v2/domainTemplates/providers/%s/services/%s/apply?domain=%s&redirect_uri=%s",
                urlSyncUx, PROVIDER_ID, SERVICE_ID, domain, REDIRECT_URI);
    }
}
