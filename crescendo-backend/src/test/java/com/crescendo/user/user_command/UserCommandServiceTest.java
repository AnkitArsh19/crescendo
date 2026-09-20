package com.crescendo.user.user_command;

import com.crescendo.connections.connections_command.Connections_commandService;
import com.crescendo.emailservice.apikey.key_command.ApiKey_commandRepository;
import com.crescendo.emailservice.audience.ContactRepository;
import com.crescendo.emailservice.broadcast.BroadcastRepository;
import com.crescendo.emailservice.domain.DomainRepository;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.security.crypto.CryptoShreddingService;
import com.crescendo.security.mfa.UserMFABackupCodeRepository;
import com.crescendo.security.mfa.UserMFASetting;
import com.crescendo.security.mfa.UserMFASettingRepository;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.storage.FileStorageService;
import com.crescendo.storage.storage_command.ConsumptionModel;
import com.crescendo.storage.storage_command.UploadedFile_command;
import com.crescendo.storage.storage_command.UploadedFile_commandRepository;
import com.crescendo.user.UserDto;
import com.crescendo.user.domain_event.UserAccountDeletedEvent;
import com.crescendo.user.domain_event.UserProfileUpdatedEvent;
import com.crescendo.user.user_command.user_credential.UserCredential;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import com.crescendo.user.user_command.user_identity.UserIdentityRepository;
import com.crescendo.user.user_command.user_session.UserSession;
import com.crescendo.user.user_command.user_session.UserSessionRepository;
import com.crescendo.user.user_command.webauthn.PasskeyCredential_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_commandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock private User_commandRepository userRepo;
    @Mock private UserCredentialRepository credentialRepo;
    @Mock private UserIdentityRepository identityRepo;
    @Mock private UserSessionRepository sessionRepo;
    @Mock private UserMFASettingRepository mfaSettingRepo;
    @Mock private UserMFABackupCodeRepository mfaBackupRepo;
    @Mock private Workflow_commandService workflowCommandService;
    @Mock private Connections_commandService connectionsCommandService;
    @Mock private UploadedFile_commandRepository uploadedFileRepo;
    @Mock private FileStorageService fileStorageService;
    @Mock private PasskeyCredential_commandRepository passkeyRepo;
    @Mock private DomainRepository domainRepo;
    @Mock private ApiKey_commandRepository apiKeyRepo;
    @Mock private EmailTemplate_commandRepository emailTemplateRepo;
    @Mock private ContactRepository contactRepo;
    @Mock private BroadcastRepository broadcastRepo;
    @Mock private CryptoShreddingService cryptoShreddingService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private DomainEventPublisher eventPublisher;

    private User_commandService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new User_commandService(
                userRepo, credentialRepo, identityRepo, sessionRepo,
                mfaSettingRepo, mfaBackupRepo,
                workflowCommandService, connectionsCommandService,
                uploadedFileRepo, fileStorageService,
                passkeyRepo, domainRepo,
                apiKeyRepo, emailTemplateRepo,
                contactRepo, broadcastRepo,
                cryptoShreddingService,
                passwordEncoder, eventPublisher
        );
    }

    @Test
    void updateUsername_updatesEntityAndPublishesEvent() {
        User_command user = new User_command();
        user.setId(userId);
        user.setUserName("OldName");
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        service.updateUsername(userId, new UserDto.UpdateProfileRequest("NewName"));

        assertThat(user.getUserName()).isEqualTo("NewName");
        ArgumentCaptor<UserProfileUpdatedEvent> captor = ArgumentCaptor.forClass(UserProfileUpdatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().aggregateId()).isEqualTo(userId);
        assertThat(captor.getValue().getNewUsername()).isEqualTo("NewName");
    }

    @Test
    void deleteAccount_performsGdprHardDeleteCascadeAndPublishesEvent() {
        // Mock active session, MFA, file, etc. to prove complete cascade eradication
        User_command user = new User_command();
        user.setId(userId);
        UserSession session = new UserSession(UUID.randomUUID(), user, "token-hash-xyz", Instant.now().plusSeconds(3600));

        UploadedFile_command file = new UploadedFile_command(
                "files/user-123/test.pdf", userId, "test.pdf", "application/pdf", 1024L, "abc", ConsumptionModel.RELAY
        );

        when(uploadedFileRepo.findAllByUserId(userId)).thenReturn(List.of(file));
        when(mfaSettingRepo.findByUser_Id(userId)).thenReturn(Optional.of(new UserMFASetting()));
        when(sessionRepo.findAllActiveByUserId(any(UUID.class), any(Instant.class))).thenReturn(List.of(session));
        when(sessionRepo.findAll()).thenReturn(List.of(session));
        when(credentialRepo.findByUser_Id(userId)).thenReturn(Optional.of(new UserCredential()));
        when(identityRepo.findAllByUser_Id(userId)).thenReturn(List.of());

        service.deleteAccount(userId);

        // Verify comprehensive cascade order:
        // 1. Workflows
        verify(workflowCommandService).purgeAllWorkflowsForUser(userId);
        // 2. Connections
        verify(connectionsCommandService).purgeAllConnectionsForUser(userId);
        // 3. Storage files
        verify(fileStorageService).delete(file.getStorageKey());
        verify(uploadedFileRepo).deleteAllByUserId(userId);
        // 4. Email resources & domains
        verify(apiKeyRepo).deleteAllByUserId(userId);
        verify(emailTemplateRepo).deleteAllByUserId(userId);
        verify(broadcastRepo).deleteAllByUserId(userId);
        verify(contactRepo).deleteAllByUserId(userId);
        verify(domainRepo).deleteAllByUser_Id(userId);
        // 5. Passkeys
        verify(passkeyRepo).deleteAllByUserId(userId);
        // 6. Crypto-shredding DEK
        verify(cryptoShreddingService).shredUserKey(userId);
        // 7. MFA
        verify(mfaBackupRepo).deleteAllByUserId(userId);
        verify(mfaSettingRepo).delete(any());
        // 8. Sessions, credentials, identities
        verify(sessionRepo).deleteAll(any());
        verify(credentialRepo).delete(any());
        // 9. User row & event
        verify(userRepo).deleteById(userId);

        ArgumentCaptor<UserAccountDeletedEvent> eventCaptor = ArgumentCaptor.forClass(UserAccountDeletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().aggregateId()).isEqualTo(userId);
    }

    @Test
    void dismissPasskeyNudge_permanentOptOut_setsFlagToTrue() {
        User_command user = new User_command();
        user.setId(userId);
        user.setPasskeyNudgeOptedOut(false);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        service.dismissPasskeyNudge(userId, true);

        assertThat(user.isPasskeyNudgeOptedOut()).isTrue();
    }
}
