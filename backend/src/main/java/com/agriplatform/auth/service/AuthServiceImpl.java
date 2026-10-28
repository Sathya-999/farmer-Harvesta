package com.agriplatform.auth.service;

import com.agriplatform.auth.dto.AuthResponseDto;
import com.agriplatform.auth.dto.AuthUserDto;
import com.agriplatform.auth.dto.LoginRequestDto;
import com.agriplatform.auth.dto.RegisterRequestDto;
import com.agriplatform.common.exception.ConflictException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Production implementation of {@link AuthService}.
 * Interacts directly with auth.users and public.farmer_profiles in PostgreSQL.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public AuthServiceImpl(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String email = request.email().trim().toLowerCase();
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        String phone = request.phone() != null && !request.phone().isBlank()
                ? request.phone().trim()
                : null;

        log.info("Processing farmer registration for email: {}", email);

        // 1. Enforce email uniqueness
        Integer emailCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM auth.users WHERE LOWER(email) = ?",
                Integer.class,
                email
        );
        if (emailCount != null && emailCount > 0) {
            throw new ConflictException("An account with this email address already exists. Please sign in instead.");
        }

        // 2. Enforce phone uniqueness if provided
        if (phone != null) {
            Integer phoneCount = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM auth.users WHERE phone = ?",
                    Integer.class,
                    phone
            );
            if (phoneCount != null && phoneCount > 0) {
                throw new ConflictException("An account with this phone number already exists. Please sign in instead.");
            }
        }

        // 3. BCrypt hash password — never store plaintext
        String hashedPassword = passwordEncoder.encode(request.password());

        // 4. Create auth.users record
        UUID userId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());

        String rawUserMetadata;
        try {
            rawUserMetadata = objectMapper.writeValueAsString(Map.of(
                    "first_name", firstName,
                    "last_name", lastName,
                    "full_name", fullName
            ));
        } catch (Exception e) {
            rawUserMetadata = "{\"first_name\":\"" + firstName + "\",\"last_name\":\"" + lastName + "\",\"full_name\":\"" + fullName + "\"}";
        }

        String rawAppMetadata = "{\"provider\":\"email\",\"providers\":[\"email\"]}";

        String insertUserSql = """
            INSERT INTO auth.users (
                id, aud, role, email, encrypted_password, email_confirmed_at,
                raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
                phone, phone_confirmed_at, confirmed_at, instance_id
            ) VALUES (
                ?, 'authenticated', 'authenticated', ?, ?, ?,
                ?::jsonb, ?::jsonb, ?, ?,
                ?, ?, ?, '00000000-0000-0000-0000-000000000000'::uuid
            )
        """;

        jdbcTemplate.update(
                insertUserSql,
                userId,
                email,
                hashedPassword,
                now,
                rawAppMetadata,
                rawUserMetadata,
                now,
                now,
                phone,
                phone != null ? now : null,
                now
        );

        // 5. Create associated farmer profile with onboarding completed
        // Profile is created explicitly via upsert to ensure data consistency
        String upsertProfileSql = """
            INSERT INTO public.farmer_profiles (
                id, full_name, profile_status, onboarding_completed, created_at, updated_at
            ) VALUES (
                ?, ?, 'ACTIVE', true, now(), now()
            )
            ON CONFLICT (id) DO UPDATE SET
                full_name = EXCLUDED.full_name,
                onboarding_completed = true,
                updated_at = now()
        """;

        jdbcTemplate.update(upsertProfileSql, userId, fullName);

        log.info("Farmer account and profile successfully persisted for user: {}", userId);

        AuthUserDto userDto = new AuthUserDto(
                userId,
                firstName,
                lastName,
                fullName,
                email,
                phone,
                "FARMER",
                true
        );

        String token = jwtTokenService.generateToken(userDto);
        return new AuthResponseDto(userDto, token, "Registration successful");
    }

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        String identifier = request.identifier().trim();
        log.info("Processing farmer login for identifier: {}", identifier);

        // Find user by email or phone
        String findUserSql = """
            SELECT id, email, phone, encrypted_password, raw_user_meta_data
            FROM auth.users
            WHERE LOWER(email) = LOWER(?) OR (phone IS NOT NULL AND phone = ?)
            LIMIT 1
        """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(findUserSql, identifier, identifier);
        if (rows.isEmpty()) {
            throw new BadCredentialsException("The email/phone or password you entered is incorrect.");
        }

        Map<String, Object> userRow = rows.get(0);
        UUID userId = (UUID) userRow.get("id");
        String storedPasswordHash = (String) userRow.get("encrypted_password");
        String email = (String) userRow.get("email");
        String phone = (String) userRow.get("phone");
        String rawMetadata = userRow.get("raw_user_meta_data") != null ? userRow.get("raw_user_meta_data").toString() : null;

        // Verify password with BCrypt
        if (storedPasswordHash == null || !passwordEncoder.matches(request.password(), storedPasswordHash)) {
            throw new BadCredentialsException("The email/phone or password you entered is incorrect.");
        }

        // Extract names from metadata
        String firstName = "Farmer";
        String lastName = "";
        String fullName = "Farmer";

        if (rawMetadata != null) {
            try {
                JsonNode metaNode = objectMapper.readTree(rawMetadata);
                if (metaNode.has("first_name") && !metaNode.get("first_name").isNull()) {
                    firstName = metaNode.get("first_name").asText();
                }
                if (metaNode.has("last_name") && !metaNode.get("last_name").isNull()) {
                    lastName = metaNode.get("last_name").asText();
                }
                if (metaNode.has("full_name") && !metaNode.get("full_name").isNull()) {
                    fullName = metaNode.get("full_name").asText();
                } else {
                    fullName = (firstName + " " + lastName).trim();
                }
            } catch (Exception e) {
                log.warn("Failed to parse raw_user_meta_data for user: {}", userId);
            }
        }

        // Retrieve existing farmer profile (never create duplicate)
        String findProfileSql = """
            SELECT full_name, onboarding_completed, profile_status
            FROM public.farmer_profiles
            WHERE id = ?
        """;

        List<Map<String, Object>> profileRows = jdbcTemplate.queryForList(findProfileSql, userId);
        boolean onboardingCompleted = true;
        if (profileRows.isEmpty()) {
            // Profile missing - this indicates incomplete registration
            // Block login and require user to contact support
            log.error("CRITICAL: Profile missing for user {} — registration may have failed partially", userId);
            throw new IllegalStateException("User profile not found. Please contact support to complete your account setup.");
        } else {
            Map<String, Object> profileRow = profileRows.get(0);
            if (profileRow.get("full_name") != null) {
                fullName = (String) profileRow.get("full_name");
            }
            if (profileRow.get("onboarding_completed") != null) {
                onboardingCompleted = Boolean.TRUE.equals(profileRow.get("onboarding_completed"));
            }
        }

        AuthUserDto userDto = new AuthUserDto(
                userId,
                firstName,
                lastName,
                fullName,
                email,
                phone,
                "FARMER",
                onboardingCompleted
        );

        String token = jwtTokenService.generateToken(userDto);
        log.info("Farmer login successful for user: {}", userId);
        return new AuthResponseDto(userDto, token, "Login successful");
    }
}
