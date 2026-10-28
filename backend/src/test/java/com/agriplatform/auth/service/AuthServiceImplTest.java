package com.agriplatform.auth.service;

import com.agriplatform.auth.dto.AuthResponseDto;
import com.agriplatform.auth.dto.LoginRequestDto;
import com.agriplatform.auth.dto.RegisterRequestDto;
import com.agriplatform.common.exception.ConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private ObjectMapper objectMapper;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authService = new AuthServiceImpl(jdbcTemplate, passwordEncoder, jwtTokenService, objectMapper);
    }

    @Test
    @DisplayName("Register succeeds and returns valid response when email is unique")
    void register_succeeds_when_email_unique() {
        when(jdbcTemplate.queryForObject(contains("SELECT count(*) FROM auth.users WHERE LOWER(email)"), eq(Integer.class), eq("farmer@test.com")))
                .thenReturn(0);
        when(passwordEncoder.encode("SecretPass123")).thenReturn("$2a$10$hashedPassword");
        when(jwtTokenService.generateToken(any())).thenReturn("signed-test-token");

        RegisterRequestDto request = new RegisterRequestDto(
                "Suresh", "Patil", "farmer@test.com", "SecretPass123", null
        );

        AuthResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.user().email()).isEqualTo("farmer@test.com");
        assertThat(response.user().fullName()).isEqualTo("Suresh Patil");
        assertThat(response.token()).isEqualTo("signed-test-token");

        // Verify insertion into auth.users and public.farmer_profiles
        verify(jdbcTemplate, times(1)).update(
                contains("INSERT INTO auth.users"),
                eq(response.user().id()),
                eq("farmer@test.com"),
                eq("$2a$10$hashedPassword"),
                any(),
                anyString(),
                anyString(),
                any(),
                any(),
                isNull(),
                isNull(),
                any()
        );
        verify(jdbcTemplate, times(1)).update(
                contains("INSERT INTO public.farmer_profiles"),
                eq(response.user().id()),
                eq("Suresh Patil")
        );
    }

    @Test
    @DisplayName("Register throws ConflictException when email already exists")
    void register_throws_conflict_when_email_exists() {
        when(jdbcTemplate.queryForObject(contains("SELECT count(*) FROM auth.users WHERE LOWER(email)"), eq(Integer.class), eq("existing@test.com")))
                .thenReturn(1);

        RegisterRequestDto request = new RegisterRequestDto(
                "Suresh", "Patil", "existing@test.com", "SecretPass123", null
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(jdbcTemplate, never()).update(
                contains("INSERT INTO auth.users"),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("Register throws ConflictException when phone already exists")
    void register_throws_conflict_when_phone_exists() {
        when(jdbcTemplate.queryForObject(contains("SELECT count(*) FROM auth.users WHERE LOWER(email)"), eq(Integer.class), eq("new@test.com")))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("SELECT count(*) FROM auth.users WHERE phone"), eq(Integer.class), eq("+919999999999")))
                .thenReturn(1);

        RegisterRequestDto request = new RegisterRequestDto(
                "Suresh", "Patil", "new@test.com", "SecretPass123", "+919999999999"
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("phone number already exists");
    }

    @Test
    @DisplayName("Login succeeds with valid password and retrieves existing profile")
    void login_succeeds_with_valid_password() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> userRow = Map.of(
                "id", userId,
                "email", "farmer@test.com",
                "phone", "+919876543210",
                "encrypted_password", "$2a$10$encoded",
                "raw_user_meta_data", "{\"first_name\":\"Kavita\",\"last_name\":\"Devi\",\"full_name\":\"Kavita Devi\"}"
        );

        when(jdbcTemplate.queryForList(contains("SELECT id, email, phone, encrypted_password"), eq("farmer@test.com"), eq("farmer@test.com")))
                .thenReturn(List.of(userRow));
        when(passwordEncoder.matches("CorrectPass", "$2a$10$encoded")).thenReturn(true);
        when(jdbcTemplate.queryForList(contains("SELECT full_name, onboarding_completed, profile_status"), eq(userId)))
                .thenReturn(List.of(Map.of("full_name", "Kavita Devi", "onboarding_completed", true, "profile_status", "ACTIVE")));
        when(jwtTokenService.generateToken(any())).thenReturn("login-jwt-token");

        LoginRequestDto request = new LoginRequestDto("farmer@test.com", "CorrectPass");
        AuthResponseDto response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.user().id()).isEqualTo(userId);
        assertThat(response.user().fullName()).isEqualTo("Kavita Devi");
        assertThat(response.token()).isEqualTo("login-jwt-token");
    }

    @Test
    @DisplayName("Login throws BadCredentialsException when user not found")
    void login_throws_when_user_not_found() {
        when(jdbcTemplate.queryForList(anyString(), eq("unknown@test.com"), eq("unknown@test.com")))
                .thenReturn(Collections.emptyList());

        LoginRequestDto request = new LoginRequestDto("unknown@test.com", "Pass123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    @DisplayName("Login throws BadCredentialsException when password does not match")
    void login_throws_when_password_incorrect() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> userRow = Map.of(
                "id", userId,
                "email", "farmer@test.com",
                "encrypted_password", "$2a$10$encoded"
        );

        when(jdbcTemplate.queryForList(anyString(), eq("farmer@test.com"), eq("farmer@test.com")))
                .thenReturn(List.of(userRow));
        when(passwordEncoder.matches("WrongPass", "$2a$10$encoded")).thenReturn(false);

        LoginRequestDto request = new LoginRequestDto("farmer@test.com", "WrongPass");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("incorrect");
    }
}
