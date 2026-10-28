package com.agriplatform.auth.service;

import com.agriplatform.auth.dto.AuthResponseDto;
import com.agriplatform.auth.dto.LoginRequestDto;
import com.agriplatform.auth.dto.RegisterRequestDto;

/**
 * Service contract for farmer authentication and registration.
 */
public interface AuthService {

    /**
     * Registers a new farmer account, creates entry in auth.users, associates farmer profile
     * in public.farmer_profiles, and generates a valid JWT Bearer token.
     */
    AuthResponseDto register(RegisterRequestDto request);

    /**
     * Authenticates a farmer with email/phone and password, verifies credentials against
     * auth.users, retrieves profile from public.farmer_profiles, and generates a valid JWT Bearer token.
     */
    AuthResponseDto login(LoginRequestDto request);
}
