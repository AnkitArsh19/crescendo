package com.crescendo.emailservice.queue;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.enums.EmailStatus;
import com.crescendo.enums.EmailType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailPendingReaperTest {

    @Test
    void testReapStalePendingEmails() {
        EmailLogRepository mockRepo = mock(EmailLogRepository.class);
        EmailPendingReaper reaper = new EmailPendingReaper(mockRepo);

        EmailLog staleLog = new EmailLog(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "from@test.com", "to@test.com", "Subject", EmailStatus.PENDING, EmailType.TRANSACTIONAL);
        staleLog.setCreatedAt(Instant.now().minus(20, ChronoUnit.MINUTES));

        when(mockRepo.findByStatusAndCreatedAtBefore(eq(EmailStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(staleLog));

        reaper.reapStalePendingEmails();

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(mockRepo).save(captor.capture());

        EmailLog savedLog = captor.getValue();
        assertEquals(EmailStatus.FAILED, savedLog.getStatus());
        assertEquals("Email timed out in queue before processing completed.", savedLog.getError());
    }

    @Test
    void testReapStalePendingEmails_NoStaleLogs() {
        EmailLogRepository mockRepo = mock(EmailLogRepository.class);
        EmailPendingReaper reaper = new EmailPendingReaper(mockRepo);

        when(mockRepo.findByStatusAndCreatedAtBefore(eq(EmailStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of());

        reaper.reapStalePendingEmails();

        verify(mockRepo, never()).save(any(EmailLog.class));
    }
}
