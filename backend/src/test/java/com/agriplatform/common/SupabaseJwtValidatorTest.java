package com.agriplatform.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the JWT validator chain configured in {@link com.agriplatform.common.config.SecurityConfig}.
 *
 * <h2>Purpose</h2>
 * <p>Directly tests the {@link OAuth2TokenValidator} composition used by the
 * custom {@link org.springframework.security.oauth2.jwt.JwtDecoder} bean — in
 * particular the audience validator — without requiring a running server or a
 * real Supabase JWKS endpoint.
 *
 * <h2>Coverage</h2>
 * <ol>
 *   <li>Valid JWT with {@code aud=authenticated} and correct issuer → valid</li>
 *   <li>JWT with missing {@code aud} claim → invalid (would → 401)</li>
 *   <li>JWT with wrong audience value → invalid (would → 401)</li>
 *   <li>JWT with correct audience but wrong issuer → invalid (would → 401)</li>
 *   <li>JWT with correct audience and correct issuer but expired → invalid (would → 401)</li>
 *   <li>JWT with empty audience list → invalid (would → 401)</li>
 * </ol>
 */
@DisplayName("Supabase JWT Validator Unit Tests")
class SupabaseJwtValidatorTest {

    private static final String TEST_ISSUER = "https://test.supabase.co/auth/v1";
    private static final String CORRECT_AUDIENCE = "authenticated";

    private OAuth2TokenValidator<Jwt> compositeValidator;

    @BeforeEach
    void setUp() {
        // Replicate the exact validator composition from SecurityConfig.jwtDecoder()
        OAuth2TokenValidator<Jwt> defaultValidators =
            JwtValidators.createDefaultWithIssuer(TEST_ISSUER);

        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD,
            aud -> aud != null && aud.contains(CORRECT_AUDIENCE)
        );

        compositeValidator = new DelegatingOAuth2TokenValidator<>(
            defaultValidators,
            audienceValidator
        );
    }

    // =========================================================================
    // Test A — Valid JWT with correct aud and issuer → passes validation
    // =========================================================================

    @Test
    @DisplayName("A. Valid JWT with aud=authenticated and correct issuer passes validation")
    void valid_jwt_with_correct_audience_and_issuer_passes() {
        Jwt jwt = buildJwt(
            TEST_ISSUER,
            List.of(CORRECT_AUDIENCE),
            Instant.now().plusSeconds(3600)
        );

        OAuth2TokenValidatorResult result = compositeValidator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    // =========================================================================
    // Test B — Missing aud claim → fails validation
    // =========================================================================

    @Test
    @DisplayName("B. JWT with missing aud claim fails audience validation (→ 401)")
    void jwt_with_missing_audience_claim_fails_validation() {
        // Build without aud claim
        Jwt jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject(UUID.randomUUID().toString())
            .issuer(TEST_ISSUER)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            // No .claim("aud", ...) → null aud
            .build();

        OAuth2TokenValidatorResult result = compositeValidator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    // =========================================================================
    // Test C — Wrong audience value → fails validation
    // =========================================================================

    @Test
    @DisplayName("C. JWT with wrong audience value fails audience validation (→ 401)")
    void jwt_with_wrong_audience_fails_validation() {
        Jwt jwt = buildJwt(
            TEST_ISSUER,
            List.of("service_role"),   // wrong audience
            Instant.now().plusSeconds(3600)
        );

        OAuth2TokenValidatorResult result = compositeValidator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    // =========================================================================
    // Test D — Correct audience but wrong issuer → fails validation
    // =========================================================================

    @Test
    @DisplayName("D. JWT with correct audience but wrong issuer fails issuer validation (→ 401)")
    void jwt_with_correct_audience_but_wrong_issuer_fails_validation() {
        Jwt jwt = buildJwt(
            "https://malicious.example.com/auth/v1",  // wrong issuer
            List.of(CORRECT_AUDIENCE),
            Instant.now().plusSeconds(3600)
        );

        OAuth2TokenValidatorResult result = compositeValidator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    // =========================================================================
    // Test E — Correct audience and issuer but expired → fails validation
    // =========================================================================

    @Test
    @DisplayName("E. Expired JWT with correct audience and issuer fails timestamp validation (→ 401)")
    void expired_jwt_with_correct_audience_fails_validation() {
        Jwt jwt = buildJwt(
            TEST_ISSUER,
            List.of(CORRECT_AUDIENCE),
            Instant.now().minusSeconds(120)  // expired 120s ago — exceeds JwtTimestampValidator 60s default skew
        );

        OAuth2TokenValidatorResult result = compositeValidator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    // =========================================================================
    // Test F — Empty audience list → fails validation
    // =========================================================================

    @Test
    @DisplayName("F. JWT with empty audience list fails audience validation (→ 401)")
    void jwt_with_empty_audience_list_fails_validation() {
        Jwt jwt = buildJwt(
            TEST_ISSUER,
            List.of(),  // empty aud list
            Instant.now().plusSeconds(3600)
        );

        OAuth2TokenValidatorResult result = compositeValidator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private Jwt buildJwt(String issuer, List<String> audience, Instant expiresAt) {
        // issuedAt must be before expiresAt (Jwt.Builder enforces this).
        // For expired tokens, set issuedAt 2 hours in the past so expiresAt
        // (also in the past) remains after issuedAt.
        Instant issuedAt = expiresAt.minusSeconds(60);

        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject(UUID.randomUUID().toString())
            .issuer(issuer)
            .claim(JwtClaimNames.AUD, audience)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
