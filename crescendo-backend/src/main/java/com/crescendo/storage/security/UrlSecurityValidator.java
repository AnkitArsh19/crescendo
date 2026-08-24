package com.crescendo.storage.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Validates external URLs against Server-Side Request Forgery (SSRF),
 * private network access, cloud metadata endpoints, and protocol abuse.
 */
@Component
public class UrlSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(UrlSecurityValidator.class);

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    // Cloud metadata endpoints & well-known internal hostnames
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost",
            "127.0.0.1",
            "169.254.169.254", // AWS/GCP/Azure IMDS
            "metadata.google.internal",
            "instance-data",
            "0.0.0.0"
    );

    /**
     * Validates that a URL is well-formed, uses an allowed protocol (HTTP/HTTPS),
     * and does not resolve to private, loopback, link-local, or cloud metadata IP addresses.
     *
     * @param urlStr the URL string to validate
     * @throws SecurityException if the URL violates security rules
     */
    public void validateUrl(String urlStr) throws SecurityException {
        if (urlStr == null || urlStr.isBlank()) {
            throw new SecurityException("URL cannot be empty");
        }

        URI uri;
        try {
            uri = new URI(urlStr.trim());
        } catch (URISyntaxException e) {
            throw new SecurityException("Malformed URL: " + e.getMessage());
        }

        // 1. Protocol Scheme Check
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            log.warn("Blocked URL with disallowed scheme: {}", scheme);
            throw new SecurityException("Only HTTP and HTTPS URLs are allowed");
        }

        // 2. Hostname Check
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SecurityException("URL must include a valid hostname");
        }

        String hostLower = host.toLowerCase();
        if (BLOCKED_HOSTNAMES.contains(hostLower) || hostLower.endsWith(".internal") || hostLower.endsWith(".local")) {
            log.warn("Blocked URL with forbidden hostname: {}", host);
            throw new SecurityException("Access to internal hostnames is prohibited");
        }

        // 3. Userinfo Check (e.g. http://user:pass@evil.com)
        if (uri.getUserInfo() != null) {
            log.warn("Blocked URL containing userinfo/credentials in URI");
            throw new SecurityException("Embedded credentials in URLs are not permitted");
        }

        // 4. DNS Resolution & SSRF IP Address Checks
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateOrRestrictedIp(addr)) {
                    log.warn("SSRF Block: Host '{}' resolved to restricted IP: {}", host, addr.getHostAddress());
                    throw new SecurityException("URL resolves to a private or restricted network address");
                }
            }
        } catch (UnknownHostException e) {
            log.warn("DNS resolution failed for host: {}", host);
            throw new SecurityException("Unable to resolve host: " + host);
        }
    }

    /**
     * Checks if an IP address is private, loopback, link-local, multicast, or cloud metadata.
     */
    public boolean isPrivateOrRestrictedIp(InetAddress addr) {
        if (addr.isLoopbackAddress()
                || addr.isSiteLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = addr.getAddress();

        // IPv4 Checks
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 10.0.0.0/8
            if (b0 == 10) return true;
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true;
            // 169.254.0.0/16 (Link-local & AWS IMDS)
            if (b0 == 169 && b1 == 254) return true;
            // 127.0.0.0/8 (Loopback)
            if (b0 == 127) return true;
            // 0.0.0.0/8 (Current network)
            if (b0 == 0) return true;
            // 100.64.0.0/10 (Carrier-grade NAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true;
            // 198.18.0.0/15 (Benchmarking)
            if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;
        }

        // IPv6 Checks
        if (bytes.length == 16) {
            // fc00::/7 (Unique Local Address)
            if ((bytes[0] & 0xFE) == 0xFC) return true;
            // fe80::/10 (Link-Local)
            if ((bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xC0) == 0x80) return true;
            // ::1 (Loopback)
            boolean allZero = true;
            for (int i = 0; i < 15; i++) {
                if (bytes[i] != 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero && bytes[15] == 1) return true;
        }

        return false;
    }
}
