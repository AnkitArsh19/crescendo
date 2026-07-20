package com.crescendo.emailservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@Service
public class DomainConnectService {

    private static final Logger log = LoggerFactory.getLogger(DomainConnectService.class);

    private static final String PROVIDER_ID = "crescendo.run";
    private static final String SERVICE_ID = "email";
    private static final String KEY_ID = "key1";
    // We will redirect users back to the frontend domains settings page
    private static final String REDIRECT_URI = "https://app.crescendo.run/dashboard/email/domains";

    @Value("${domainconnect.private-key-path:#{environment.DOMAIN_CONNECT_PRIVATE_KEY_PATH}}")
    private String privateKeyPath;

    private PrivateKey privateKey;
    private final RestTemplate restTemplate;

    public DomainConnectService() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            try {
                String keyContent = Files.readString(Paths.get(privateKeyPath))
                        .replaceAll("\\n", "")
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                        .replace("-----END RSA PRIVATE KEY-----", "")
                        .trim();
                
                byte[] keyBytes = Base64.getDecoder().decode(keyContent);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                this.privateKey = kf.generatePrivate(spec);
                log.info("Domain Connect private key loaded successfully from {}", privateKeyPath);
            } catch (Exception e) {
                log.error("Failed to load Domain Connect private key from path: {}", privateKeyPath, e);
            }
        } else {
            log.warn("DOMAIN_CONNECT_PRIVATE_KEY_PATH is not set. Domain Connect URLs will not be signed.");
        }
    }

    /**
     * Attempts to find the Domain Connect redirect URL for a given domain.
     * @param domainDto the domain configuration
     * @return Optional containing the URL to redirect the user to, or empty if unsupported.
     */
    public Optional<String> buildSyncUrl(DomainDto.DomainResponse domainDto) {
        String domain = domainDto.domainName();
        return discoverApiDomain(domain)
                .flatMap(this::fetchUrlSyncUX)
                .map(urlSyncUx -> constructApplyUrl(urlSyncUx, domainDto));
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
     * Step 3: Construct the final redirect URL to apply the template and digitally sign it.
     */
    String constructApplyUrl(String urlSyncUx, DomainDto.DomainResponse domainDto) {
        String domain = domainDto.domainName();
        String token = extractToken(domainDto);
        String dkimPubKey = "YOUR_PUBLIC_KEY"; // Placeholder to match DomainCommandService's DnsRecord generation

        try {
            // Construct the query string variables.
            // According to specification: The query string variables must be properly URL Encoded before signature generation.
            String queryString = String.format("domain=%s&redirect_uri=%s&token=%s&dkim_pub_key=%s",
                    URLEncoder.encode(domain, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(token, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(dkimPubKey, StandardCharsets.UTF_8.toString())
            );

            // Construct the base URL
            String baseUrl = String.format("%s/v2/domainTemplates/providers/%s/services/%s/apply",
                    urlSyncUx, PROVIDER_ID, SERVICE_ID);

            // Sign the exact query string
            if (privateKey != null) {
                Signature signer = Signature.getInstance("SHA256withRSA");
                signer.initSign(privateKey);
                signer.update(queryString.getBytes(StandardCharsets.UTF_8));
                byte[] signatureBytes = signer.sign();
                String sigBase64 = Base64.getEncoder().encodeToString(signatureBytes);
                
                // Append key and sig to the query string (url encoded sig)
                queryString += String.format("&key=%s&sig=%s",
                        URLEncoder.encode(KEY_ID, StandardCharsets.UTF_8.toString()),
                        URLEncoder.encode(sigBase64, StandardCharsets.UTF_8.toString())
                );
            } else {
                log.warn("Private key not loaded, returning unsigned URL for {}", domain);
            }

            return baseUrl + "?" + queryString;
        } catch (Exception e) {
            log.error("Error constructing Domain Connect URL for {}", domain, e);
            throw new RuntimeException("Failed to construct Domain Connect URL", e);
        }
    }

    String extractToken(DomainDto.DomainResponse domainDto) {
        // Extract the verification token from the required TXT records
        List<DomainDto.DnsRecord> records = domainDto.requiredDnsRecords();
        if (records != null) {
            for (DomainDto.DnsRecord record : records) {
                if (record.name() != null && record.name().startsWith(DnsVerificationService.TXT_RECORD_PREFIX)
                    && record.value() != null && record.value().startsWith(DnsVerificationService.TOKEN_VALUE_PREFIX)) {
                    return record.value().substring(DnsVerificationService.TOKEN_VALUE_PREFIX.length());
                }
            }
        }
        return "missing-token";
    }
}
