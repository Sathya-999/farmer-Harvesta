package com.agriplatform.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Security utility for the Agricultural Intelligence Platform.
 *
 * <h2>Purpose</h2>
 * <p>Provides a single, controlled access point for application code to
 * obtain the authenticated Supabase user's UUID from the JWT security context.
 *
 * <h2>Identity Source</h2>
 * <p>The farmer identity is <strong>always</strong> sourced from the validated
 * JWT {@code sub} claim (Supabase user UUID). It is <strong>never</strong>
 * read from:
 * <ul>
 *   <li>request body {@code farmer_id}</li>
 *   <li>query parameter {@code farmer_id}</li>
 *   <li>path variable {@code farmer_id}</li>
 *   <li>any Flutter-supplied arbitrary identity field</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   UUID farmerId = securityUtils.getAuthenticatedUserId();
 * }</pre>
 *
 * <h2>Design Decision — No Custom Principal</h2>
 * <p>Spring Security's {@link JwtAuthenticationToken} / {@link Jwt} objects
 * already contain all required claims. A custom {@code FarmerPrincipal} class
 * would add unnecessary abstraction at this stage. This utility provides the
 * thin adapter layer that application services need.
 */
@Component
public class SecurityUtils {

    /**
     * Returns the authenticated Supabase user's UUID extracted from the JWT
     * {@code sub} claim in the current Security Context.
     *
     * @return the authenticated farmer's Supabase UUID
     * @throws AuthenticationRequiredException if there is no authenticated JWT
     *         principal in the current security context
     * @throws AuthenticationRequiredException if the {@code sub} claim is
     *         absent or cannot be parsed as a UUID
     */
    public UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationRequiredException("No authenticated principal in security context");
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            throw new AuthenticationRequiredException(
                "Authentication is not a JWT token. Expected JwtAuthenticationToken but found: "
                    + authentication.getClass().getSimpleName()
            );
        }

        Jwt jwt = jwtToken.getToken();
        String sub = jwt.getSubject();

        if (sub == null || sub.isBlank()) {
            throw new AuthenticationRequiredException("JWT 'sub' claim is absent or blank");
        }

        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationRequiredException(
                "JWT 'sub' claim is not a valid UUID: value was malformed"
            );
        }
    }

    /**
     * Returns the raw {@link Jwt} from the current authenticated security context.
     *
     * <p>Prefer {@link #getAuthenticatedUserId()} for obtaining the farmer UUID.
     * Use this method only when direct access to JWT claims is needed.
     *
     * @return the validated {@link Jwt} from the current request
     * @throws AuthenticationRequiredException if there is no authenticated JWT principal
     */
    public Jwt getAuthenticatedJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationRequiredException("No authenticated principal in security context");
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            throw new AuthenticationRequiredException(
                "Authentication is not a JWT token. Expected JwtAuthenticationToken but found: "
                    + authentication.getClass().getSimpleName()
            );
        }

        return jwtToken.getToken();
    }

    // -------------------------------------------------------------------------
    // Inner exception class
    // -------------------------------------------------------------------------

    /**
     * Thrown when a security operation cannot proceed because there is no valid
     * authenticated JWT in the current security context.
     *
     * <p>This exception will later be mapped to HTTP 401 by the global exception
     * handler (implemented in a future task). Spring Security's resource server
     * filter will already handle 401 for unauthenticated HTTP requests before
     * they reach application code; this exception covers edge cases where code
     * runs in a context where authentication is expected but unavailable.
     */
    public static class AuthenticationRequiredException extends RuntimeException {
        public AuthenticationRequiredException(String message) {
            super(message);
        }
    }
}
