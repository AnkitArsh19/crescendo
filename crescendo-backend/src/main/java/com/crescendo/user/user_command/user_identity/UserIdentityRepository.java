package com.crescendo.user.user_command.user_identity;

import com.crescendo.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    /// Used during OAuth login: looks up an existing OAuth identity record by provider + their user ID.
    /// If found, the linked User_command is the canonical account for this OAuth identity.
    Optional<UserIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    /// Used to build the 'providers' list in LoginResponse — shows which providers a user has linked.
    List<UserIdentity> findAllByUser_Id(java.util.UUID userId);
}
