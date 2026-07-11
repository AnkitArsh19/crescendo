package com.crescendo.emailservice.suppression;

import com.crescendo.emailservice.email_log.EmailLogRepository;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailSuppressionServiceTest {

    @Test
    void testVerifyUnsubscribeToken_Valid() {
        EmailSuppressionRepository mockRepo = mock(EmailSuppressionRepository.class);
        EmailLogRepository mockLogRepo = mock(EmailLogRepository.class);
        String secret = "test-secret-key-12345678901234567890";
        EmailSuppressionService service = new EmailSuppressionService(mockRepo, mockLogRepo, secret);

        UUID logId = UUID.randomUUID();
        String token = service.generateUnsubscribeToken(logId);
        
        assertNotNull(token);
        assertTrue(token.contains("."));
        
        UUID verified = service.verifyUnsubscribeToken(token);
        assertEquals(logId, verified);
    }

    @Test
    void testVerifyUnsubscribeToken_Tampered() {
        EmailSuppressionRepository mockRepo = mock(EmailSuppressionRepository.class);
        EmailLogRepository mockLogRepo = mock(EmailLogRepository.class);
        String secret = "test-secret-key-12345678901234567890";
        EmailSuppressionService service = new EmailSuppressionService(mockRepo, mockLogRepo, secret);

        UUID logId = UUID.randomUUID();
        String token = service.generateUnsubscribeToken(logId);
        
        // Tamper with the token data
        String[] parts = token.split("\\.");
        String tamperedToken = UUID.randomUUID().toString() + "." + parts[1];
        
        UUID verified = service.verifyUnsubscribeToken(tamperedToken);
        assertNull(verified);
        
        // Tamper with signature
        String tamperedToken2 = parts[0] + "." + parts[1].substring(0, parts[1].length() - 1) + "X";
        UUID verified2 = service.verifyUnsubscribeToken(tamperedToken2);
        assertNull(verified2);
    }
}
