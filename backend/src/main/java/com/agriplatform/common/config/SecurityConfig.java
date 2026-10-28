package com.agriplatform.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for the Agricultural Intelligence Platform.
 *
 * <h2>Architecture</h2>
 * <p>This backend is a stateless OAuth2 Resource Server. It issues and validates
 * HMAC-SHA256 signed JWT access tokens. Spring Boot is the single authentication authority.
 *
 * <h2>Token Validation</h2>
 * <ul>
 *   <li>JWT signature — verified against configured HMAC secret key</li>
 *   <li>Expiration (exp) — validated by {@link JwtTimestampValidator}</li>
 *   <li>Issuer (iss) — validated against configured issuer URI</li>
 *   <li>Audience (aud) — validated: must contain {@code "authenticated"}</li>
 * </ul>
 *
 * <h2>Audience Validation</h2>
 * <p>JWTs carry {@code aud: "authenticated"} for successfully
 * authenticated users. This claim is validated explicitly via
 * {@link JwtClaimValidator} to prevent tokens issued for other purposes
 * from authenticating Farmer requests. An invalid or missing audience results in HTTP 401.
 *
 * <h2>Session Policy</h2>
 * <p>STATELESS — no server-side sessions. Every request must carry a
 * valid Bearer token. CSRF protection is disabled (appropriate for
 * stateless bearer-token APIs).
 *
 * <h2>Endpoint Rules</h2>
 * <ul>
 *   <li>PUBLIC: {@code /actuator/health}, {@code GET /api/v1/geography/**}, {@code /api/v1/auth/**}</li>
 *   <li>PROTECTED: everything else requires a valid JWT</li>
 * </ul>
 *
 * <h2>Servlet Context Path</h2>
 * <p>No {@code server.servlet.context-path} is configured. All matchers
 * in this class use full request paths (e.g. {@code /api/v1/geography/**}).
 * Actuator is exposed at the default {@code /actuator} path (root context).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * JWT issuer URL.
     * Injected from configuration.
     * Default: {@code https://api.harvesta.in/auth}
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String jwtIssuerUri;

    /**
     * JWKS endpoint URI (optional, for future Supabase migration).
     * Injected from {@code SUPABASE_JWT_JWKS_URI} environment variable.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Expected JWT audience claim value for authenticated users.
     */
    private static final String AUTHENTICATED_AUDIENCE = "authenticated";

    /**
     * Comma-separated allowed CORS origins.
     * Injected from {@code CORS_ALLOWED_ORIGINS} environment variable.
     */
    @Value("${app.security.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    /**
     * Secret key for signing and verifying backend-issued JWT tokens.
     */
    @Value("${app.security.jwt.secret:harvest-a-agri-platform-jwt-secret-key-must-be-at-least-256-bits-long-2026}")
    private String jwtSecret;

    /**
     * Password encoder for BCrypt password hashing.
     */
    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    /**
     * Primary security filter chain.
     *
     * <p>Configures CORS, CSRF (disabled), stateless sessions,
     * public/protected endpoint rules, and the OAuth2 Resource Server
     * JWT mode backed by the custom {@link #jwtDecoder()} bean.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ------------------------------------------------------------------
            // CORS — uses the CorsConfigurationSource bean defined below
            // ------------------------------------------------------------------
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ------------------------------------------------------------------
            // CSRF — disabled: stateless bearer-token API, no browser sessions
            // ------------------------------------------------------------------
            .csrf(AbstractHttpConfigurer::disable)

            // ------------------------------------------------------------------
            // SESSION — stateless: each request authenticated by JWT bearer
            // ------------------------------------------------------------------
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ------------------------------------------------------------------
            // ENDPOINT AUTHORIZATION RULES
            // ------------------------------------------------------------------
            .authorizeHttpRequests(auth -> auth

                // Public: Actuator health check (no authentication required)
                // No server.servlet.context-path is set; Actuator is at /actuator
                .requestMatchers("/actuator/health").permitAll()

                // Public: Geography / reference-data endpoints (GET only)
                // Full request path — no context-path prefix is configured.
                .requestMatchers(HttpMethod.GET, "/api/v1/geography/**").permitAll()

                // Public: Authentication endpoints (register and login)
                .requestMatchers("/api/v1/auth/**").permitAll()

                // DEFAULT: every other request requires a valid JWT
                .anyRequest().authenticated()
            )

            // ------------------------------------------------------------------
            // OAUTH2 RESOURCE SERVER — JWT bearer token validation
            // Uses the custom jwtDecoder() bean below, which adds:
            //   - signature verification (HMAC / JWKS)
            //   - expiry validation
            //   - issuer validation
            //   - audience validation (aud = "authenticated")
            // ------------------------------------------------------------------
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * Custom {@link JwtDecoder} that applies the full validation chain:
     * <ol>
     *   <li>Signature verification (HMAC secret for backend-issued tokens, JWKS for Supabase)</li>
     *   <li>Timestamp / expiry validation ({@link JwtTimestampValidator})</li>
     *   <li>Issuer validation ({@link JwtValidators#createDefaultWithIssuer})</li>
     *   <li>Audience validation — {@code aud} must contain {@code "authenticated"}</li>
     * </ol>
     *
     * <p>An invalid or missing audience results in HTTP 401.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // --- Validators ---

        // 1. Default validators: timestamp (exp/nbf) + issuer (iss)
        OAuth2TokenValidator<Jwt> defaultValidators =
            JwtValidators.createDefaultWithIssuer(jwtIssuerUri);

        // 2. Audience validator: JWT aud claim must contain "authenticated"
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD,
            aud -> aud != null && aud.contains(AUTHENTICATED_AUDIENCE)
        );

        // 3. Compose all validators
        OAuth2TokenValidator<Jwt> compositeValidator = new DelegatingOAuth2TokenValidator<>(
            defaultValidators,
            audienceValidator
        );

        // Build HMAC decoder for backend-issued tokens
        javax.crypto.SecretKey hmacKey = new javax.crypto.spec.SecretKeySpec(
            jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        NimbusJwtDecoder hmacDecoder = NimbusJwtDecoder.withSecretKey(hmacKey).build();
        hmacDecoder.setJwtValidator(compositeValidator);

        // Optionally configure JWKS decoder for Supabase-issued tokens
        NimbusJwtDecoder jwksDecoder = null;
        if (jwkSetUri != null && !jwkSetUri.isBlank() && !jwkSetUri.contains("localhost")) {
            try {
                NimbusJwtDecoder jwks = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
                jwks.setJwtValidator(compositeValidator);
                jwksDecoder = jwks;
            } catch (Exception ignored) {
            }
        }

        final NimbusJwtDecoder fallbackJwks = jwksDecoder;
        return token -> {
            try {
                return hmacDecoder.decode(token);
            } catch (Exception ex) {
                if (fallbackJwks != null) {
                    return fallbackJwks.decode(token);
                }
                throw ex;
            }
        };
    }

    /**
     * JWT Authentication Converter.
     *
     * <p>Maps Supabase JWT claims to Spring Security authorities.
     * Supabase tokens carry {@code role: "authenticated"} for standard users.
     * Feature-level authorization uses the {@code sub} claim (user UUID) via
     * {@link com.agriplatform.common.util.SecurityUtils}.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        // Supabase JWTs use 'role' not 'scope'
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtConverter;
    }

    /**
     * CORS configuration source.
     *
     * <p>Reads allowed origins from the {@code CORS_ALLOWED_ORIGINS} environment
     * variable (via {@code app.security.cors.allowed-origins}).
     * Do NOT use {@code *} for authenticated Farmer APIs.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
            config.setAllowedOrigins(origins);
        } else {
            config.setAllowedOrigins(List.of());
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
