package com.crescendo.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Shared utility for extracting the authenticated user's ID from Spring Security.
 *
 * Eliminates the duplicated {@code private UUID userId(Authentication auth)} pattern
 * across 17+ controllers. Use via static import:
 *
 * <pre>
 *   import static com.crescendo.security.AuthenticatedUser.userId;
 *
 *   userId(auth)  // returns UUID
 * </pre>
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
        // utility class
    }

    /**
     * Extracts the user's UUID from the security context.
     *
     * @param auth the Spring Security Authentication object
     * @return the authenticated user's UUID
     * @throws ResponseStatusException 401 if not authenticated or principal is unexpected type
     */
    public static UUID userId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (auth.getPrincipal() instanceof AppUserDetails details) {
            return details.getId();
        }
        if (auth.getPrincipal() instanceof PublicApiPrincipal principal) {
            return principal.getId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }
}
