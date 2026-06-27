package com.crescendo.security;

import com.crescendo.publicapi.PublicApiScopes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Authentication-mechanism-neutral identity used by the public API.
 */
public final class PublicApiPrincipal implements UserDetails {

    public enum AuthenticationMethod {
        USER_SESSION,
        API_KEY,
        OAUTH2
    }

    private final AppUserDetails user;
    private final AuthenticationMethod authenticationMethod;
    private final String credentialId;
    private final String clientId;
    private final Set<String> scopes;
    private final Collection<? extends GrantedAuthority> authorities;

    private PublicApiPrincipal(
            AppUserDetails user,
            AuthenticationMethod authenticationMethod,
            String credentialId,
            String clientId,
            Collection<String> scopes) {
        this.user = user;
        this.authenticationMethod = authenticationMethod;
        this.credentialId = credentialId;
        this.clientId = clientId;
        this.scopes = Set.copyOf(new LinkedHashSet<>(scopes));

        var resolvedAuthorities = new ArrayList<GrantedAuthority>(user.getAuthorities());
        resolvedAuthorities.add(new SimpleGrantedAuthority("AUTH_" + authenticationMethod.name()));
        this.scopes.forEach(scope ->
                resolvedAuthorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
        this.authorities = List.copyOf(resolvedAuthorities);
    }

    public static PublicApiPrincipal userSession(AppUserDetails user) {
        return new PublicApiPrincipal(
                user,
                AuthenticationMethod.USER_SESSION,
                null,
                null,
                PublicApiScopes.ALL
        );
    }

    public static PublicApiPrincipal apiKey(
            AppUserDetails user, UUID apiKeyId, Collection<String> scopes) {
        return new PublicApiPrincipal(
                user,
                AuthenticationMethod.API_KEY,
                apiKeyId.toString(),
                null,
                scopes
        );
    }

    public static PublicApiPrincipal oauth2(
            AppUserDetails user, String authorizationId, String clientId, Collection<String> scopes) {
        return new PublicApiPrincipal(
                user,
                AuthenticationMethod.OAUTH2,
                authorizationId,
                clientId,
                scopes
        );
    }

    public UUID getId() {
        return user.getId();
    }

    public AppUserDetails user() {
        return user;
    }

    public AuthenticationMethod authenticationMethod() {
        return authenticationMethod;
    }

    public String credentialId() {
        return credentialId;
    }

    public String clientId() {
        return clientId;
    }

    public Set<String> scopes() {
        return scopes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return user.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
