package com.crescendo.shared.infrastructure.event;

import com.crescendo.auth.domain_event.OAuthProviderLinkedEvent;
import com.crescendo.auth.domain_event.UserLoggedInEvent;
import com.crescendo.auth.domain_event.UserPasswordChangedEvent;
import com.crescendo.auth.domain_event.UserPasswordResetEvent;
import com.crescendo.user.domain_event.*;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import com.crescendo.user.user_command.user_identity.UserIdentityRepository;
import com.crescendo.user.user_query.User_query;
import com.crescendo.user.user_query.User_queryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listens for user and auth domain events.
 *
 * Responsibilities:
 *   - Evict stale cached data when user state changes
 *   - Log security-relevant events for audit trails
 *   - Sync query-side User_query projections
 *
 * Handlers that modify the query-side User_query projection open their own
 * transaction because @TransactionalEventListener defaults to AFTER_COMMIT
 * (the originating transaction has already committed).
 */
@Component
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);

    private final User_queryRepository userQueryRepo;
    private final User_commandRepository userCommandRepo;
    private final UserCredentialRepository credentialRepo;
    private final UserIdentityRepository identityRepo;

    public UserEventListener(User_queryRepository userQueryRepo,
                             User_commandRepository userCommandRepo,
                             UserCredentialRepository credentialRepo,
                             UserIdentityRepository identityRepo) {
        this.userQueryRepo = userQueryRepo;
        this.userCommandRepo = userCommandRepo;
        this.credentialRepo = credentialRepo;
        this.identityRepo = identityRepo;
    }

    @Transactional
    @TransactionalEventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        logger.info("User registered: userId={}, email={}", event.aggregateId(), event.getEmail());

        UUID userId = event.aggregateId();
        User_command user = userCommandRepo.findById(userId).orElse(null);
        if (user != null) {
            boolean hasLocal = credentialRepo.findByUser_Id(userId).isPresent();
            List<String> providers = identityRepo.findAllByUser_Id(userId)
                    .stream()
                    .map(id -> id.getProvider().name())
                    .toList();

            User_query query = new User_query(
                    userId, user.getEmailId(), user.getUserName(), user.getRole(),
                    providers, hasLocal, user.isEmailVerified());
            userQueryRepo.save(query);
        }
    }

    @Transactional
    @TransactionalEventListener
    @CacheEvict(value = "users", key = "#event.aggregateId()")
    public void onEmailVerified(UserEmailVerifiedEvent event) {
        logger.info("Email verified: userId={}, email={}", event.aggregateId(), event.getEmail());

        userQueryRepo.findById(event.aggregateId()).ifPresent(q -> {
            q.setEmailVerified(true);
            userQueryRepo.save(q);
        });
    }

    @Transactional
    @TransactionalEventListener
    @CacheEvict(value = "users", key = "#event.aggregateId()")
    public void onProfileUpdated(UserProfileUpdatedEvent event) {
        logger.info("Profile updated: userId={}, newUsername={}", event.aggregateId(), event.getNewUsername());

        userQueryRepo.findById(event.aggregateId()).ifPresent(q -> {
            if (event.getNewUsername() != null) {
                q.setUserName(event.getNewUsername());
            }
            userQueryRepo.save(q);
        });
    }

    @Transactional
    @TransactionalEventListener
    @CacheEvict(value = "users", key = "#event.aggregateId()")
    public void onAccountDeleted(UserAccountDeletedEvent event) {
        logger.info("Account deleted: userId={}", event.aggregateId());

        userQueryRepo.deleteById(event.aggregateId());
    }

    @TransactionalEventListener
    public void onSessionRevoked(UserSessionRevokedEvent event) {
        logger.info("Sessions revoked: userId={}, allSessions={}", event.aggregateId(), event.isAllSessions());
    }

    @TransactionalEventListener
    @CacheEvict(value = "users", key = "#event.aggregateId()")
    public void onMFAEnabled(MFAEnabledEvent event) {
        logger.info("MFA enabled: userId={}", event.aggregateId());
    }

    @TransactionalEventListener
    @CacheEvict(value = "users", key = "#event.aggregateId()")
    public void onMFADisabled(MFADisabledEvent event) {
        logger.info("MFA disabled: userId={}", event.aggregateId());
    }

    @Transactional
    @TransactionalEventListener
    @CacheEvict(value = "users", key = "#event.aggregateId()")
    public void onProviderUnlinked(OAuthProviderUnlinkedEvent event) {
        logger.info("OAuth provider unlinked: userId={}, provider={}", event.aggregateId(), event.getProvider());

        syncProviders(event.aggregateId());
    }

    @TransactionalEventListener
    public void onUserLoggedIn(UserLoggedInEvent event) {
        logger.info("User logged in: userId={}, provider={}", event.aggregateId(), event.getProvider());
    }

    @Transactional
    @TransactionalEventListener
    @CacheEvict(value = "users", key = "#event.aggregateId()")
    public void onProviderLinked(OAuthProviderLinkedEvent event) {
        logger.info("OAuth provider linked: userId={}, provider={}", event.aggregateId(), event.getProvider());

        syncProviders(event.aggregateId());
    }

    @Transactional
    @TransactionalEventListener
    public void onPasswordReset(UserPasswordResetEvent event) {
        logger.info("Password reset completed: userId={}", event.aggregateId());

        userQueryRepo.findById(event.aggregateId()).ifPresent(q -> {
            q.setHasLocalCredential(true);
            userQueryRepo.save(q);
        });
    }

    @TransactionalEventListener
    public void onPasswordChanged(UserPasswordChangedEvent event) {
        logger.info("Password changed: userId={}", event.aggregateId());
    }

    /**
     * Re-sync the providers list and hasLocalCredential flag from command-side data.
     */
    private void syncProviders(UUID userId) {
        userQueryRepo.findById(userId).ifPresent(q -> {
            List<String> providers = identityRepo.findAllByUser_Id(userId)
                    .stream()
                    .map(id -> id.getProvider().name())
                    .toList();
            q.setProviders(providers);
            q.setHasLocalCredential(credentialRepo.findByUser_Id(userId).isPresent());
            userQueryRepo.save(q);
        });
    }
}
