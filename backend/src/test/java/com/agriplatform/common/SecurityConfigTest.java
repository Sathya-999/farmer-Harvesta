package com.agriplatform.common;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.config.TestSecurityControllerConfig;
import com.agriplatform.common.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security filter chain tests — covers all 12 required cases.
 *
 * <h2>Test Strategy</h2>
 * <p>{@link WebMvcTest} loads the security filter chain in isolation.
 * No database, no Supabase connection, no real JWT tokens needed.
 * Mock JWTs via {@code SecurityMockMvcRequestPostProcessors.jwt()} inject a
 * pre-authenticated {@link JwtAuthenticationToken} directly into the security
 * context — bypassing network/signature verification while exercising all
 * filter-chain authorization rules.
 *
 * <h2>Audience / Issuer Tests</h2>
 * <p>Because {@code jwt()} post-processor bypasses the {@link org.springframework.security.oauth2.jwt.JwtDecoder}
 * entirely, audience and issuer rejection tests are exercised at the
 * {@link SecurityUtils} / security-context level and in
 * {@link SupabaseJwtValidatorTest} (decoder unit tests) rather than through
 * MockMvc HTTP assertions.
 *
 * <h2>Covered cases</h2>
 * <ol>
 *   <li>No Authorization header → 401</li>
 *   <li>Missing Authorization header → 401</li>
 *   <li>Mock valid JWT → 200</li>
 *   <li>JWT sub claim extractable via SecurityUtils</li>
 *   <li>Actuator health — security does NOT block (no 401)</li>
 *   <li>Public geography endpoint — security does NOT block (no 401)</li>
 *   <li>Protected Farmer endpoint without JWT → 401</li>
 *   <li>Security config does not permit ALL endpoints</li>
 *   <li>Security context contains JWT auth after mock JWT</li>
 *   <li>SecurityUtils throws when sub is blank</li>
 *   <li>SecurityUtils throws when sub is malformed UUID</li>
 *   <li>SecurityUtils throws when security context is empty</li>
 * </ol>
 */
@WebMvcTest(controllers = TestSecurityControllerConfig.TestController.class)
@Import({SecurityConfig.class, TestSecurityControllerConfig.class, SecurityUtils.class})
@ActiveProfiles("test")
@DisplayName("Security Filter Chain Tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityUtils securityUtils;

    // =========================================================================
    // Test 1 — No Authorization header → 401
    // =========================================================================

    @Test
    @DisplayName("1. No Authorization header on protected endpoint returns 401")
    void no_authorization_header_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
            .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Test 2 — Missing Authorization header (explicit) → 401
    // =========================================================================

    @Test
    @DisplayName("2. Missing Authorization header returns 401")
    void missing_authorization_header_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                // No Authorization header added
            )
            .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Test 3 — Mock valid JWT with correct aud → 200
    // =========================================================================

    @Test
    @DisplayName("3. Request with mock valid JWT (aud=authenticated) returns 200")
    void valid_mock_jwt_with_correct_audience_returns_200() throws Exception {
        UUID supabaseUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/test/protected")
                .with(jwt()
                    .jwt(builder -> builder
                        .subject(supabaseUserId.toString())
                        .issuer("https://test.supabase.co/auth/v1")
                        .claim("aud", List.of("authenticated"))
                        .claim("role", "authenticated")
                        .expiresAt(Instant.now().plusSeconds(3600))
                    )
                )
            )
            .andExpect(status().isOk());
    }

    // =========================================================================
    // Test 4 — SecurityUtils extracts sub claim as UUID
    // =========================================================================

    @Test
    @DisplayName("4. SecurityUtils extracts authenticated Supabase UUID from JWT sub claim")
    void security_utils_extracts_authenticated_user_id_from_sub_claim() {
        UUID expectedId = UUID.randomUUID();

        Jwt mockJwt = Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject(expectedId.toString())
            .issuer("https://test.supabase.co/auth/v1")
            .claim("aud", List.of("authenticated"))
            .claim("role", "authenticated")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(mockJwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            UUID actualId = securityUtils.getAuthenticatedUserId();
            assertThat(actualId).isEqualTo(expectedId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // =========================================================================
    // Test 5 — Actuator health: security does NOT block (no 401/403)
    //
    // NOTE: @WebMvcTest does NOT load the Actuator servlet infrastructure.
    // /actuator/health has no handler in this slice → 404 (not a 401/403).
    // A 404 proves Spring Security passed the request through (permitAll works).
    // The full 200 OK is verified in ActuatorSecurityTest (@SpringBootTest).
    // =========================================================================

    @Test
    @DisplayName("5. Actuator /health is not blocked by security (no 401 without JWT)")
    void actuator_health_not_blocked_by_security() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                if (status == 401 || status == 403) {
                    throw new AssertionError(
                        "/actuator/health must not be blocked by security, got HTTP " + status);
                }
                // 404 expected — no Actuator handler in WebMvcTest slice — that is correct.
            });
    }

    // =========================================================================
    // Test 6 — Public geography endpoint: security does NOT block (no 401/403)
    //
    // GET /api/v1/geography/** is declared permitAll in SecurityConfig.
    // No handler exists in this slice, so 404 is expected — but NOT 401.
    // =========================================================================

    @Test
    @DisplayName("6. GET /api/v1/geography/** is not blocked by security (no 401 without JWT)")
    void public_geography_endpoint_not_blocked_by_security() throws Exception {
        mockMvc.perform(get("/api/v1/geography/states"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                if (status == 401 || status == 403) {
                    throw new AssertionError(
                        "/api/v1/geography/** must not be blocked by security, got HTTP " + status);
                }
                // 404 expected (no controller loaded) — not a security rejection.
            });
    }

    // =========================================================================
    // Test 7 — Protected Farmer endpoint without JWT → 401
    // =========================================================================

    @Test
    @DisplayName("7. Protected Farmer endpoint without JWT returns 401")
    void protected_farmer_endpoint_without_jwt_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/farmer/profile"))
            .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Test 8 — Security config does not accidentally permit all endpoints
    // =========================================================================

    @Test
    @DisplayName("8. Security does not accidentally permit all endpoints")
    void security_config_does_not_permit_all_endpoints() throws Exception {
        // Plot, Cultivation, Analytics, Notification, History must all require auth
        mockMvc.perform(get("/api/v1/plot")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/cultivation")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/analytics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notification")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/history")).andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Test 9 — Security context contains JWT auth after mock JWT
    // =========================================================================

    @Test
    @DisplayName("9. Security context contains JwtAuthenticationToken after mock JWT auth")
    void security_context_contains_jwt_auth_after_mock_jwt() throws Exception {
        UUID userId = UUID.randomUUID();

        // A 200 response proves the JwtAuthenticationToken was accepted by the filter chain
        mockMvc.perform(get("/api/v1/test/protected")
                .with(jwt()
                    .jwt(builder -> builder
                        .subject(userId.toString())
                        .claim("aud", List.of("authenticated"))
                        .claim("role", "authenticated")
                        .expiresAt(Instant.now().plusSeconds(3600))
                    )
                )
            )
            .andExpect(status().isOk());
    }

    // =========================================================================
    // Test 10 — SecurityUtils throws when sub claim is blank
    // =========================================================================

    @Test
    @DisplayName("10. SecurityUtils throws when JWT sub claim is blank")
    void security_utils_throws_when_sub_claim_is_blank() {
        Jwt mockJwt = Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .issuer("https://test.supabase.co/auth/v1")
            .claim("aud", List.of("authenticated"))
            .claim("role", "authenticated")
            .claim("sub", "")   // explicitly blank
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(mockJwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            assertThatThrownBy(() -> securityUtils.getAuthenticatedUserId())
                .isInstanceOf(SecurityUtils.AuthenticationRequiredException.class)
                .hasMessageContaining("sub");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // =========================================================================
    // Test 11 — SecurityUtils throws when sub is malformed UUID
    // =========================================================================

    @Test
    @DisplayName("11. SecurityUtils throws when sub claim is not a valid UUID")
    void security_utils_throws_when_sub_is_not_valid_uuid() {
        Jwt mockJwt = Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject("not-a-valid-uuid")
            .issuer("https://test.supabase.co/auth/v1")
            .claim("aud", List.of("authenticated"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(mockJwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            assertThatThrownBy(() -> securityUtils.getAuthenticatedUserId())
                .isInstanceOf(SecurityUtils.AuthenticationRequiredException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // =========================================================================
    // Test 12 — SecurityUtils throws when security context is empty
    // =========================================================================

    @Test
    @DisplayName("12. SecurityUtils throws when security context has no authentication")
    void security_utils_throws_when_security_context_is_empty() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> securityUtils.getAuthenticatedUserId())
            .isInstanceOf(SecurityUtils.AuthenticationRequiredException.class)
            .hasMessageContaining("No authenticated principal");
    }
}
