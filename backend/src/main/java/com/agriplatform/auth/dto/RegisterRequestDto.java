package com.agriplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for farmer registration.
 */
public record RegisterRequestDto(
    @NotBlank(message = "First name is required and cannot be blank")
    String firstName,

    @NotBlank(message = "Last name is required and cannot be blank")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password,

    String phone
) {}
