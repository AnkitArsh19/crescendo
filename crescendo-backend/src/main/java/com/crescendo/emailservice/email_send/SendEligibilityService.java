package com.crescendo.emailservice.email_send;

import com.crescendo.emailservice.domain.Domain;
import com.crescendo.emailservice.domain.DomainRepository;
import com.crescendo.enums.DomainSendReadiness;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.suppression.EmailSuppressionRepository;
import com.crescendo.enums.AllowedEmailType;
import com.crescendo.enums.EmailType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class SendEligibilityService {

    private final DomainRepository domainRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailSuppressionRepository suppressionRepository;

    public SendEligibilityService(DomainRepository domainRepository,
                                  EmailLogRepository emailLogRepository,
                                  EmailSuppressionRepository suppressionRepository) {
        this.domainRepository = domainRepository;
        this.emailLogRepository = emailLogRepository;
        this.suppressionRepository = suppressionRepository;
    }

    public EligibilityResult checkEligibility(UUID userId, String fromAddress, String toAddress, EmailType emailType) {
        // 1. Suppression Check
        boolean isSuppressed = suppressionRepository.findByUserIdAndNormalizedEmail(userId, toAddress.toLowerCase().trim()).isPresent();
        if (isSuppressed) {
            return new EligibilityResult(false, "SUPPRESSED", "Recipient is suppressed.");
        }

        // Extract exact domain from fromAddress
        int atIndex = fromAddress.indexOf('@');
        if (atIndex == -1 || atIndex == fromAddress.length() - 1) {
            return new EligibilityResult(false, "FAILED", "Invalid fromAddress format.");
        }
        String domainName = fromAddress.substring(atIndex + 1);

        // 2. Domain Readiness Check
        Domain domain = domainRepository.findByDomainNameAndUserId(domainName, userId).orElse(null);
        if (domain == null) {
            return new EligibilityResult(false, "FAILED", "Domain not registered.");
        }
        
        if (domain.getSendReadiness() != DomainSendReadiness.READY) {
            return new EligibilityResult(false, "FAILED", "Domain is not fully authenticated and READY.");
        }

        // 3. Rate Limit Check
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long todaySends = emailLogRepository.countTotalSendsSince(userId, domainName, startOfDay);
        if (todaySends >= domain.getDailySendCap()) {
            return new EligibilityResult(false, "FAILED", "Domain daily send cap reached.");
        }

        // 4. Usage-Type Enforcement
        if (emailType == EmailType.MARKETING && domain.getAllowedEmailType() == AllowedEmailType.TRANSACTIONAL_ONLY) {
            return new EligibilityResult(false, "FAILED", "Domain is restricted to TRANSACTIONAL_ONLY but email is MARKETING.");
        }

        return new EligibilityResult(true, "OK", null);
    }

    public record EligibilityResult(boolean eligible, String suggestedStatus, String reason) {}
}
