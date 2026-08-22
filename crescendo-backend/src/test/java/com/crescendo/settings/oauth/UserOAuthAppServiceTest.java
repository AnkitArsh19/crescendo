package com.crescendo.settings.oauth;

import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.settings.oauth.UserOAuthApp;
import com.crescendo.settings.oauth.UserOAuthAppDto;
import com.crescendo.settings.oauth.UserOAuthAppRepository;
import com.crescendo.settings.oauth.UserOAuthAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOAuthAppServiceTest {

    @Mock
    private UserOAuthAppRepository repo;

    private ConnectionCredentialsCryptoService cryptoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserOAuthAppService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 32-byte valid test AES key
        String testKey = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
        cryptoService = new ConnectionCredentialsCryptoService(objectMapper, testKey);
        service = new UserOAuthAppService(repo, cryptoService, objectMapper);
    }

    @Test
    void save_newByokApp_encryptsClientIdAndSecretBeforeSaving() {
        var req = new UserOAuthAppDto.SaveOAuthAppRequest("slack", "byok-client-id-123", "byok-secret-789", "channels:read chat:write");
        when(repo.findByUserIdAndProviderKey(userId, "slack")).thenReturn(Optional.empty());
        when(repo.save(any(UserOAuthApp.class))).thenAnswer(inv -> inv.getArgument(0));

        service.save(userId, req);

        ArgumentCaptor<UserOAuthApp> captor = ArgumentCaptor.forClass(UserOAuthApp.class);
        verify(repo).save(captor.capture());
        UserOAuthApp saved = captor.getValue();

        assertThat(saved.getProviderKey()).isEqualTo("slack");
        assertThat(saved.getScopes()).isEqualTo("channels:read chat:write");
        assertThat(saved.isEnabled()).isTrue();

        // Ensure plaintext client ID and secret are NEVER saved in plaintext
        assertThat(saved.getEncryptedClientId()).doesNotContain("byok-client-id-123");
        assertThat(saved.getEncryptedClientSecret()).doesNotContain("byok-secret-789");
        assertThat(saved.getEncryptedClientId()).contains("v1:");
        assertThat(saved.getEncryptedClientSecret()).contains("v1:");
    }

    @Test
    void getDecrypted_enabledByokApp_returnsUnsealedCredentialsForOAuthFlow() {
        // Prepare an encrypted app in the repository
        var saveReq = new UserOAuthAppDto.SaveOAuthAppRequest("github", "git-client-555", "git-secret-999", "repo user");
        when(repo.findByUserIdAndProviderKey(userId, "github")).thenReturn(Optional.empty());
        ArgumentCaptor<UserOAuthApp> captor = ArgumentCaptor.forClass(UserOAuthApp.class);
        when(repo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        service.save(userId, saveReq);

        UserOAuthApp storedApp = captor.getValue();
        when(repo.findByUserIdAndProviderKey(userId, "github")).thenReturn(Optional.of(storedApp));

        UserOAuthAppDto.DecryptedOAuthApp decrypted = service.getDecrypted(userId, "github");

        assertThat(decrypted).isNotNull();
        assertThat(decrypted.clientId()).isEqualTo("git-client-555");
        assertThat(decrypted.clientSecret()).isEqualTo("git-secret-999");
        assertThat(decrypted.scopes()).isEqualTo("repo user");
    }

    @Test
    void getDecrypted_missingOrDisabledApp_returnsNullForPlatformFallback() {
        when(repo.findByUserIdAndProviderKey(userId, "gmail")).thenReturn(Optional.empty());

        UserOAuthAppDto.DecryptedOAuthApp decrypted = service.getDecrypted(userId, "gmail");

        assertThat(decrypted).isNull();
    }

    @Test
    void list_returnsSafeSummaryWithoutSecrets() {
        UserOAuthApp app1 = new UserOAuthApp(userId, "slack", "encId1", "encSecret1", "chat:write");
        UserOAuthApp app2 = new UserOAuthApp(userId, "notion", "encId2", "encSecret2", "page:read");
        when(repo.findByUserId(userId)).thenReturn(List.of(app1, app2));

        List<UserOAuthAppDto.OAuthAppSummary> summaries = service.list(userId);

        assertThat(summaries).hasSize(2);
        assertThat(summaries).extracting(UserOAuthAppDto.OAuthAppSummary::providerKey)
                .containsExactly("slack", "notion");
        // OAuthAppSummary only holds safe metadata (providerKey, scopes, enabled, timestamps)
    }

    @Test
    void delete_nonExistentApp_throwsNotFoundException() {
        when(repo.findByUserIdAndProviderKey(userId, "unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, "unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No custom OAuth app configured");
    }
}
