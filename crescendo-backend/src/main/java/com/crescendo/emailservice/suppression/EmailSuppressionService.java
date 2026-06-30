package com.crescendo.emailservice.suppression;

import com.crescendo.emailservice.email_log.EmailLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Manages the per-user suppression list.
 *
 * Before every send, EmailSendService calls isSuppressed() to skip addresses
 * that have unsubscribed or previously hard-bounced.
 */
@Service
public class EmailSuppressionService {

    private final EmailSuppressionRepository suppressionRepo;
    private final EmailLogRepository emailLogRepo;

    public EmailSuppressionService(EmailSuppressionRepository suppressionRepo,
                                   EmailLogRepository emailLogRepo) {
        this.suppressionRepo = suppressionRepo;
        this.emailLogRepo = emailLogRepo;
    }

    /**
     * Returns true if the recipient address is suppressed for this user/account.
     */
    public boolean isSuppressed(UUID userId, String email) {
        return suppressionRepo.existsByUserIdAndNormalizedEmail(userId, email.toLowerCase().trim());
    }

    /**
     * Adds an address to the suppression list (idempotent — safe to call multiple times).
     */
    public void suppress(UUID userId, String email, String reason) {
        String normalized = email.toLowerCase().trim();
        EmailSuppression suppression = suppressionRepo.findByUserIdAndNormalizedEmail(userId, normalized).orElse(null);
        if (suppression == null) {
            suppressionRepo.save(EmailSuppression.of(userId, email, reason));
        } else {
            suppression.setReason(reason);
            suppressionRepo.save(suppression);
        }
    }

    /**
     * Bulk imports addresses to the suppression list.
     */
    public void importSuppressions(List<String> emails, String reason, UUID userId) {
        for (String email : emails) {
            if (email != null && !email.isBlank()) {
                suppress(userId, email, reason);
            }
        }
    }

    public void handleHardBounce(UUID userId, String email, String reason) {
        suppress(userId, email, reason != null ? reason : "This address does not exist or is permanently unavailable. It has been removed from your list.");
    }

    public void handleComplaint(UUID userId, String email, String reason) {
        suppress(userId, email, reason != null ? reason : "The recipient marked this email as spam.");
    }

    public void handleSoftBounce(UUID userId, String email, String reason) {
        String normalized = email.toLowerCase().trim();
        EmailSuppression suppression = suppressionRepo.findByUserIdAndNormalizedEmail(userId, normalized).orElse(null);
        if (suppression == null) {
            suppression = EmailSuppression.of(userId, email, reason != null ? reason : "Temporary delivery issue (e.g., inbox full). We will retry, but repeated failures will result in suppression.");
            suppression.setConsecutiveSoftBounces(1);
            suppressionRepo.save(suppression);
        } else if (!suppression.getReason().equals("HARD_BOUNCE") && !suppression.getReason().equals("COMPLAINED") && !suppression.getReason().equals("UNSUBSCRIBED")) {
            suppression.setConsecutiveSoftBounces(suppression.getConsecutiveSoftBounces() + 1);
            if (suppression.getConsecutiveSoftBounces() >= 3) {
                suppression.setReason("SOFT_BOUNCE_THRESHOLD");
            }
            suppressionRepo.save(suppression);
        }
    }

    public void recordDelivery(UUID userId, String email) {
        String normalized = email.toLowerCase().trim();
        suppressionRepo.findByUserIdAndNormalizedEmail(userId, normalized).ifPresent(suppression -> {
            // Only reset if they aren't permanently suppressed.
            if (!suppression.getReason().equals("HARD_BOUNCE") && !suppression.getReason().equals("COMPLAINED") && !suppression.getReason().equals("UNSUBSCRIBED")) {
                suppression.setConsecutiveSoftBounces(0);
                suppressionRepo.save(suppression);
                
                // If it was previously SOFT_BOUNCE_THRESHOLD but a delivery succeeded (e.g. manual retry), un-suppress
                if (suppression.getReason().equals("SOFT_BOUNCE_THRESHOLD")) {
                    suppressionRepo.delete(suppression);
                }
            }
        });
    }

    public List<EmailSuppression> list(UUID userId) {
        return suppressionRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void remove(UUID userId, UUID suppressionId) {
        EmailSuppression s = suppressionRepo.findById(suppressionId)
                .filter(sup -> sup.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suppression not found"));
        suppressionRepo.delete(s);
    }

    /**
     * Unsubscribe via email log ID — resolves the recipient from the email that was sent
     * and adds them to the suppression list as UNSUBSCRIBED.
     * Used by the public /unsubscribe endpoint linked from the List-Unsubscribe header.
     *
     * @return true if found and suppressed, false if the email ID was not found
     */
    public boolean unsubscribeByEmailLogId(UUID emailLogId) {
        return emailLogRepo.findById(emailLogId).map(log -> {
            suppress(log.getUserId(), log.getToAddress(), "UNSUBSCRIBED");
            return true;
        }).orElse(false);
    }
}
