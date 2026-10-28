package com.agriplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for farmer authentication / login.
 */
public record LoginRequestDto(
    @NotBlank(message = "Identifier (email or phone) is required")
    String identifier,

    @NotBlank(message = "Password is required")
    String password
) {}
