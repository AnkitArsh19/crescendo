package com.crescendo.emailservice.provider;

import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.emailservice.domain.Domain;
import com.crescendo.enums.CredentialSource;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Resolves the EmailProvider to use based on the Domain's credential source.
 */
@Service
public class EmailProviderResolver {
    private static final Logger log = LoggerFactory.getLogger(EmailProviderResolver.class);

    private final BrevoEmailProvider platformProvider;
    private final Connections_commandRepository connectionsRepository;
    private final ConnectionCredentialsCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public EmailProviderResolver(
            BrevoEmailProvider platformProvider,
            Connections_commandRepository connectionsRepository,
            ConnectionCredentialsCryptoService cryptoService,
            ObjectMapper objectMapper) {
        this.platformProvider = platformProvider;
        this.connectionsRepository = connectionsRepository;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    public EmailProvider resolve(Domain domain) {
        if (domain != null && domain.getCredentialSource() == CredentialSource.PERSONAL && domain.getEmailProviderConnectionId() != null) {
            try {
                Connections_command connection = connectionsRepository.findById(domain.getEmailProviderConnectionId()).orElse(null);
                if (connection != null && "sendgrid".equalsIgnoreCase(connection.getAppKey())) {
                    Map<String, Object> decrypted = cryptoService.open(connection.getCredentials());
                    String apiKey = (String) decrypted.get("apiKey"); // Assuming the frontend stores SendGrid API key under 'apiKey'
                    if (apiKey != null && !apiKey.isBlank()) {
                        return new SendGridEmailProvider(apiKey, objectMapper);
                    } else {
                        log.warn("[Domain {}] Personal provider (sendgrid) missing 'apiKey'. Falling back to platform sending.", domain.getDomainName());
                    }
                } else if (connection == null) {
                    log.warn("[Domain {}] Personal provider connection {} not found. Falling back to platform sending.", domain.getDomainName(), domain.getEmailProviderConnectionId());
                } else {
                    log.warn("[Domain {}] Unsupported personal provider '{}'. Falling back to platform sending.", domain.getDomainName(), connection.getAppKey());
                }
            } catch (Exception e) {
                log.error("[Domain {}] Failed to resolve personal provider credentials. Falling back to platform sending.", domain.getDomainName(), e);
            }
        }
        return platformProvider;
    }
}
