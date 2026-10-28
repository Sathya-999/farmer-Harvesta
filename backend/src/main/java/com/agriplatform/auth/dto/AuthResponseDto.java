package com.agriplatform.auth.dto;

/**
 * Authentication response DTO containing token and user profile.
 */
public record AuthResponseDto(
    AuthUserDto user,
    String token,
    String message
) {}
