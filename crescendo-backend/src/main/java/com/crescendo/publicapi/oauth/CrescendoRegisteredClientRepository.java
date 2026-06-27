package com.crescendo.publicapi.oauth;

import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Prevents disabled developer applications from participating in protocol flows.
 */
public class CrescendoRegisteredClientRepository implements RegisteredClientRepository {
    private final JdbcRegisteredClientRepository delegate;
    private final DeveloperApplicationRepository applications;

    public CrescendoRegisteredClientRepository(
            JdbcRegisteredClientRepository delegate,
            DeveloperApplicationRepository applications) {
        this.delegate = delegate;
        this.applications = applications;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        delegate.save(registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        return applications.findByIdAndActiveTrue(id).isPresent() ? delegate.findById(id) : null;
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return applications.findByClientIdAndActiveTrue(clientId).isPresent()
                ? delegate.findByClientId(clientId)
                : null;
    }

    public RegisteredClient findStoredById(String id) {
        return delegate.findById(id);
    }
}
