package com.crescendo.security;

import com.crescendo.enums.UserRole;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.user_credential.UserCredential;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * Bridges our CQRS model to Spring Security. We treat the command-side user as authoritative
 * for authentication (email, username, role). Password lives separately in UserCredential so
 * that OAuth-only users do not have a stored (blank) password.
 */
public class AppUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String username;
    private final UserRole role;
    private final String passwordHash;
    private final boolean hasLocalCredential;
    private final Collection<? extends GrantedAuthority> authorities;

    private AppUserDetails(UUID id,
                           String email,
                           String username,
                           UserRole role,
                           String passwordHash,
                           boolean hasLocalCredential) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.role = role;
        this.passwordHash = passwordHash;
        this.hasLocalCredential = hasLocalCredential;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static AppUserDetails from(User_command user, Optional<UserCredential> credentialOpt) {
        return new AppUserDetails(
                user.getId(),
                user.getEmailId(),
                user.getUserName(),
                user.getRole(),
                credentialOpt.map(UserCredential::getPasswordHash).orElse(null),
                credentialOpt.isPresent()
        );
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDomainUsername() { return username; }
    public UserRole getRole() { return role; }
    public boolean hasLocalCredential() { return hasLocalCredential; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash == null ? "" : passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
