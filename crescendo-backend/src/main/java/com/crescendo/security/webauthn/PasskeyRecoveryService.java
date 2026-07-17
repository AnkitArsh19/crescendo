package com.crescendo.security.webauthn;

import com.crescendo.emailservice.NotificationService;
import com.crescendo.security.JWTService;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import com.crescendo.user.user_command.webauthn.PasskeyCredential_commandRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Passwordless-account recovery is deliberately restricted to adding a new
 * passkey. Its signed token cannot be accepted by the normal JWT filter and is
 * tracked in Redis to make the link one-time use.
 */
@Service
public class PasskeyRecoveryService {

    private static final String RECOVERY_PREFIX = "webauthn:recovery:";
    private static final Duration RECOVERY_TTL = Duration.ofMinutes(10);

    private final User_commandRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasskeyCredential_commandRepository passkeyRepository;
    private final JWTService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    public PasskeyRecoveryService(User_commandRepository userRepository,
                                  UserCredentialRepository userCredentialRepository,
                                  PasskeyCredential_commandRepository passkeyRepository,
                                  JWTService jwtService,
                                  StringRedisTemplate redisTemplate,
                                  NotificationService notificationService) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passkeyRepository = passkeyRepository;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.notificationService = notificationService;
    }

    /** Always returns normally so callers do not disclose account existence. */
    public void requestMagicLink(String email) {
        if (email == null || email.isBlank()) return;
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            boolean passkeyOnly = user.isEmailVerified()
                    && userCredentialRepository.findByUser_Id(user.getId()).isEmpty()
                    && !passkeyRepository.findByUserId(user.getId()).isEmpty();
            if (!passkeyOnly) return;

            String token = jwtService.issuePasskeyRecoveryToken(user.getId());
            JWTService.PasskeyRecoveryClaims claims = jwtService.parsePasskeyRecoveryToken(token);
            redisTemplate.opsForValue().set(RECOVERY_PREFIX + claims.tokenId(), user.getId().toString(), RECOVERY_TTL);
            notificationService.sendPasskeyRecoveryLink(user.getEmailId(), token);
        });
    }

    public UUID validateActiveToken(String token) {
        JWTService.PasskeyRecoveryClaims claims = jwtService.parsePasskeyRecoveryToken(token);
        String storedUserId = redisTemplate.opsForValue().get(RECOVERY_PREFIX + claims.tokenId());
        if (!claims.userId().toString().equals(storedUserId)) {
            throw new IllegalArgumentException("Recovery link is expired or has already been used");
        }
        return claims.userId();
    }

    public void consume(String token, UUID expectedUserId) {
        JWTService.PasskeyRecoveryClaims claims = jwtService.parsePasskeyRecoveryToken(token);
        if (!claims.userId().equals(expectedUserId)) {
            throw new IllegalArgumentException("Invalid recovery link");
        }
        String consumedUserId = redisTemplate.opsForValue().getAndDelete(RECOVERY_PREFIX + claims.tokenId());
        if (!expectedUserId.toString().equals(consumedUserId)) {
            throw new IllegalArgumentException("Recovery link is expired or has already been used");
        }
    }
}
