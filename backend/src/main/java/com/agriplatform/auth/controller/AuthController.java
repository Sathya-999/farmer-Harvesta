package com.agriplatform.auth.controller;

import com.agriplatform.auth.dto.AuthResponseDto;
import com.agriplatform.auth.dto.LoginRequestDto;
import com.agriplatform.auth.dto.RegisterRequestDto;
import com.agriplatform.auth.service.AuthService;
import com.agriplatform.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for public authentication endpoints.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST /api/v1/auth/register} — Register a new farmer and create account</li>
 *   <li>{@code POST /api/v1/auth/login} — Authenticate farmer and return JWT Bearer token</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new farmer account and persists credentials in auth.users and profile in farmer_profiles.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Authenticates farmer credentials and returns a signed JWT Bearer token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
