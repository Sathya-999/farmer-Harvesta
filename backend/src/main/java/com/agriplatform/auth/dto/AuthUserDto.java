package com.agriplatform.auth.dto;

import java.util.UUID;

/**
 * Authenticated user details DTO.
 */
public record AuthUserDto(
    UUID id,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phone,
    String role,
    boolean onboardingCompleted
) {}
