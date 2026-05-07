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
    private final boolean emailVerified;
    private final Collection<? extends GrantedAuthority> authorities;

    /// Private constructor — forces all creation through the factory method below.
    /// This ensures AppUserDetails can only be correctly assembled from a real User_command.
    private AppUserDetails(UUID id,
                           String email,
                           String username,
                           UserRole role,
                           String passwordHash,
                           boolean hasLocalCredential,
                           boolean emailVerified) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.role = role;
        this.passwordHash = passwordHash;
        this.hasLocalCredential = hasLocalCredential;
        this.emailVerified = emailVerified;
        /// Spring Security expects roles prefixed with "ROLE_" for hasRole() checks.
        /// e.g. UserRole.ADMIN becomes "ROLE_ADMIN"
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /// Static factory method — the only public way to build an AppUserDetails.
    /// credentialOpt is Optional because OAuth-only users have no UserCredential row.
    public static AppUserDetails from(User_command user, Optional<UserCredential> credentialOpt) {
        return new AppUserDetails(
                user.getId(),
                user.getEmailId(),
                user.getUserName(),
                user.getRole(),
                credentialOpt.map(UserCredential::getPasswordHash).orElse(null),
                credentialOpt.isPresent(),
                user.isEmailVerified()
        );
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDomainUsername() { return username; }
    public UserRole getRole() { return role; }
    public boolean hasLocalCredential() { return hasLocalCredential; }
    public boolean isEmailVerified() { return emailVerified; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    /// Spring Security calls getPassword() to compare against the submitted password during login.
    /// Returning "" for OAuth-only users prevents a NullPointerException inside BCryptPasswordEncoder,
    /// while still failing authentication since "" never matches a real BCrypt hash.
    @Override
    public String getPassword() { return passwordHash == null ? "" : passwordHash; }

    /// Spring Security uses getUsername() as the principal identifier in the SecurityContext.
    /// We use email as the unique identifier (not the display username) to avoid case ambiguity.
    @Override
    public String getUsername() { return email; }

    /// The four account status methods — all return true meaning no lockout or expiry logic is
    /// handled here. If you add account suspension, ban, or email verification gates in the
    /// future, these are the methods to override.
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
