package com.crescendo.emailservice.queue;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.enums.EmailStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class EmailPendingReaper {

    private static final Logger logger = LoggerFactory.getLogger(EmailPendingReaper.class);

    private final EmailLogRepository emailLogRepo;

    public EmailPendingReaper(EmailLogRepository emailLogRepo) {
        this.emailLogRepo = emailLogRepo;
    }

    /**
     * Finds PENDING emails that were created more than 15 minutes ago
     * and transitions them to FAILED, since the queue worker either
     * dropped them or crashed before acknowledging.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000)
    public void reapStalePendingEmails() {
        Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<EmailLog> staleLogs = emailLogRepo.findByStatusAndCreatedAtBefore(EmailStatus.PENDING, cutoff);
        
        if (staleLogs.isEmpty()) {
            return;
        }

        logger.info("[email-reaper] Found {} stale PENDING emails. Marking as FAILED.", staleLogs.size());

        for (EmailLog log : staleLogs) {
            log.setStatus(EmailStatus.FAILED);
            log.setError("Email timed out in queue before processing completed.");
            emailLogRepo.save(log);
        }
    }
}
