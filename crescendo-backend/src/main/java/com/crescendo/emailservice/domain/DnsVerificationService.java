package com.crescendo.emailservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Performs DNS TXT record lookups using JNDI (built into the JDK — no external library needed).
 *
 * Verification flow:
 *   1. Add domain → we generate a random token and ask the user to create a TXT record:
 *        _crescendo-verify.example.com  →  crescendo-verify=<token>
 *   2. Verify domain → we query TXT records for _crescendo-verify.<domain>
 *      and check whether any of them match the expected token value.
 */
@Component
public class DnsVerificationService {

    private static final Logger log = LoggerFactory.getLogger(DnsVerificationService.class);
    static final String TXT_RECORD_PREFIX = "_crescendo-verify.";
    static final String TOKEN_VALUE_PREFIX = "crescendo-verify=";

    /**
     * Queries TXT records for {@code _crescendo-verify.<domainName>} and returns true
     * if any record matches one of the expected tokens.
     */
    public boolean verifyDomainTxtRecord(String domainName, List<String> expectedTokens) {
        String lookupHost = TXT_RECORD_PREFIX + domainName;
        List<String> txtRecords = lookupTxtRecords(lookupHost);

        for (String record : txtRecords) {
            // TXT records may come wrapped in quotes
            String cleaned = record.replace("\"", "").trim();
            for (String token : expectedTokens) {
                if (cleaned.equals(TOKEN_VALUE_PREFIX + token)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean verifySpf(String subDomain) {
        List<String> txtRecords = lookupTxtRecords(subDomain);
        for (String record : txtRecords) {
            String cleaned = record.replace("\"", "").trim();
            if (cleaned.startsWith("v=spf1 ") && cleaned.contains("include:spf.crescendo.run")) {
                return true;
            }
        }
        return false;
    }

    public boolean verifyDkim(String selector, String subDomain) {
        String lookupHost = selector + "._domainkey." + subDomain;
        List<String> txtRecords = lookupTxtRecords(lookupHost);
        for (String record : txtRecords) {
            String cleaned = record.replace("\"", "").trim();
            if (cleaned.startsWith("v=DKIM1")) {
                return true;
            }
        }
        return false;
    }

    public boolean verifyDmarc(String subDomain) {
        String lookupHost = "_dmarc." + subDomain;
        List<String> txtRecords = lookupTxtRecords(lookupHost);
        for (String record : txtRecords) {
            String cleaned = record.replace("\"", "").trim();
            if (cleaned.startsWith("v=DMARC1")) {
                return true;
            }
        }
        return false;
    }

    /// Performs a DNS TXT lookup over the system resolver.
    /// Returns an empty list if the name has no TXT records or if the lookup fails.
    List<String> lookupTxtRecords(String hostname) {
        List<String> results = new ArrayList<>();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            InitialDirContext dirContext = new InitialDirContext(env);

            Attributes attrs = dirContext.getAttributes(hostname, new String[]{"TXT"});
            Attribute txtAttr = attrs.get("TXT");
            if (txtAttr != null) {
                NamingEnumeration<?> values = txtAttr.getAll();
                while (values.hasMore()) {
                    results.add(values.next().toString());
                }
            }
            dirContext.close();
        } catch (NamingException e) {
            log.debug("DNS TXT lookup failed for {}: {}", hostname, e.getMessage());
        }
        return results;
    }
}
