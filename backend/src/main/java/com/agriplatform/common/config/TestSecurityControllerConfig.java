package com.agriplatform.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller configuration used exclusively during security testing.
 *
 * <p>Provides minimal REST endpoints to verify the security filter chain:
 * <ul>
 *   <li>{@code /api/v1/test/protected} — must require authentication</li>
 *   <li>{@code /api/v1/test/public} — must be accessible without a token
 *       (registered as public in SecurityConfig)</li>
 * </ul>
 *
 * <p>This configuration is only active in the {@code test} Spring profile.
 * It will NOT be loaded in {@code dev} or {@code prod} profiles.
 *
 * <p>NOTE: the {@code /api/v1/test/public} endpoint must be added to
 * {@link SecurityConfig} public matchers. It is included here via
 * {@code /actuator/health} equivalency by registering it separately
 * in the test security override annotation on the test class.
 */
@Configuration
@Profile("test")
public class TestSecurityControllerConfig {

    @RestController
    @RequestMapping("/api/v1/test")
    public static class TestController {

        @GetMapping("/protected")
        public String protectedEndpoint() {
            return "authenticated";
        }

        @GetMapping("/public")
        public String publicEndpoint() {
            return "public";
        }
    }
}
