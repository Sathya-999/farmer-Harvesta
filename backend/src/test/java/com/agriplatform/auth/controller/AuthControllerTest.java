package com.agriplatform.auth.controller;

import com.agriplatform.auth.dto.AuthResponseDto;
import com.agriplatform.auth.dto.AuthUserDto;
import com.agriplatform.auth.dto.LoginRequestDto;
import com.agriplatform.auth.dto.RegisterRequestDto;
import com.agriplatform.auth.service.AuthService;
import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("Register with valid request returns 201 Created")
    void register_with_valid_request_returns_201() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthUserDto user = new AuthUserDto(
                userId, "Ramesh", "Patel", "Ramesh Patel",
                "ramesh@example.com", "+919876543210", "FARMER", true
        );
        AuthResponseDto response = new AuthResponseDto(user, "valid-jwt-token", "Registration successful");

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

        RegisterRequestDto request = new RegisterRequestDto(
                "Ramesh", "Patel", "ramesh@example.com", "Password@123", "+919876543210"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("ramesh@example.com"))
                .andExpect(jsonPath("$.data.token").value("valid-jwt-token"));
    }

    @Test
    @DisplayName("Register with duplicate email returns 409 Conflict")
    void register_with_duplicate_email_returns_409() throws Exception {
        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new ConflictException("An account with this email address already exists. Please sign in instead."));

        RegisterRequestDto request = new RegisterRequestDto(
                "Ramesh", "Patel", "existing@example.com", "Password@123", null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("An account with this email address already exists. Please sign in instead."));
    }

    @Test
    @DisplayName("Register with blank fields returns 400 Bad Request")
    void register_with_blank_fields_returns_400() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "", "", "invalid-email", "123", null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Login with valid credentials returns 200 OK")
    void login_with_valid_credentials_returns_200() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthUserDto user = new AuthUserDto(
                userId, "Ramesh", "Patel", "Ramesh Patel",
                "ramesh@example.com", null, "FARMER", true
        );
        AuthResponseDto response = new AuthResponseDto(user, "valid-jwt-token", "Login successful");

        when(authService.login(any(LoginRequestDto.class))).thenReturn(response);

        LoginRequestDto request = new LoginRequestDto("ramesh@example.com", "Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("valid-jwt-token"))
                .andExpect(jsonPath("$.data.user.fullName").value("Ramesh Patel"));
    }

    @Test
    @DisplayName("Login with invalid credentials returns 401 Unauthorized")
    void login_with_invalid_credentials_returns_401() throws Exception {
        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new BadCredentialsException("The email/phone or password you entered is incorrect."));

        LoginRequestDto request = new LoginRequestDto("ramesh@example.com", "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("The email/phone or password you entered is incorrect."));
    }
}
