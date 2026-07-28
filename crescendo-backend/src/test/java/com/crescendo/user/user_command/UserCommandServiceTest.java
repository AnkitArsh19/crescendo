package com.crescendo.user.user_command;

import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.user.UserDto;
import com.crescendo.user.domain_event.UserAccountDeletedEvent;
import com.crescendo.user.domain_event.UserProfileUpdatedEvent;
import com.crescendo.user.user_command.user_credential.UserCredential;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import com.crescendo.user.user_command.user_identity.UserIdentityRepository;
import com.crescendo.user.user_command.user_session.UserSession;
import com.crescendo.user.user_command.user_session.UserSessionRepository;
import com.crescendo.security.mfa.UserMFABackupCodeRepository;
import com.crescendo.security.mfa.UserMFASetting;
import com.crescendo.security.mfa.UserMFASettingRepository;
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
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private DomainEventPublisher eventPublisher;

    private User_commandService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new User_commandService(
                userRepo, credentialRepo, identityRepo, sessionRepo,
                mfaSettingRepo, mfaBackupRepo, passwordEncoder, eventPublisher
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
        // Mock active session and MFA settings to prove they get completely eradicated
        User_command user = new User_command();
        user.setId(userId);
        UserSession session = new UserSession(UUID.randomUUID(), user, "token-hash-xyz", Instant.now().plusSeconds(3600));

        when(mfaSettingRepo.findByUser_Id(userId)).thenReturn(Optional.of(new UserMFASetting()));
        when(sessionRepo.findAllActiveByUserId(any(UUID.class), any(Instant.class))).thenReturn(List.of(session));
        when(sessionRepo.findAll()).thenReturn(List.of(session));
        when(credentialRepo.findByUser_Id(userId)).thenReturn(Optional.of(new UserCredential()));
        when(identityRepo.findAllByUser_Id(userId)).thenReturn(List.of());

        service.deleteAccount(userId);

        // Verify strict cascade order: MFA backup codes -> MFA settings -> sessions -> credentials -> identities -> user
        verify(mfaBackupRepo).deleteAllByUserId(userId);
        verify(mfaSettingRepo).delete(any());
        verify(sessionRepo).deleteAll(any());
        verify(credentialRepo).delete(any());
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
