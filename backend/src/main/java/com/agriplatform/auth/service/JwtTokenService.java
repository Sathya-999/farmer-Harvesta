package com.agriplatform.auth.service;

import com.agriplatform.auth.dto.AuthUserDto;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Service for issuing signed JWT tokens for authenticated farmers.
 * Matches Spring Security OAuth2 Resource Server validation requirements:
 * - HMAC-SHA256 signature
 * - Issuer (iss) matching configured jwtIssuerUri
 * - Audience (aud) containing "authenticated"
 * - Role claim "authenticated" (mapped to ROLE_authenticated)
 * - Subject (sub) containing the farmer user UUID
 */
@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${app.security.jwt.secret:harvest-a-agri-platform-jwt-secret-key-must-be-at-least-256-bits-long-2026}")
    private String jwtSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://api.harvesta.in/auth}")
    private String jwtIssuerUri;

    /**
     * Generates a signed JWT Bearer token valid for 7 days.
     */
    public String generateToken(AuthUserDto user) {
        try {
            JWSSigner signer = new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Date now = new Date();
            // 7 days token validity
            Date expiry = new Date(now.getTime() + 7L * 24 * 3600 * 1000);

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(user.id().toString())
                    .issuer(jwtIssuerUri)
                    .audience(List.of("authenticated"))
                    .claim("role", "authenticated")
                    .issueTime(now)
                    .expirationTime(expiry);

            if (user.email() != null) {
                claimsBuilder.claim("email", user.email());
            }
            if (user.phone() != null) {
                claimsBuilder.claim("phone", user.phone());
            }
            if (user.fullName() != null) {
                claimsBuilder.claim("full_name", user.fullName());
            }

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Failed to generate JWT token for user: {}", user.id(), e);
            throw new RuntimeException("Failed to generate authentication token", e);
        }
    }
}
