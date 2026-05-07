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
        if (!suppressionRepo.existsByUserIdAndNormalizedEmail(userId, normalized)) {
            suppressionRepo.save(EmailSuppression.of(userId, email, reason));
        }
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
