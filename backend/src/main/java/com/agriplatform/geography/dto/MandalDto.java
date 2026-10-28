package com.agriplatform.geography.dto;

import java.util.UUID;

/**
 * Read-only DTO for a Mandal.
 *
 * <p>Includes {@code districtId} for hierarchy navigation.
 * Parent district is NOT nested to avoid circular JSON and eager-loading issues.
 *
 * @param id         Supabase UUID primary key
 * @param districtId Parent district UUID
 * @param name       Mandal name
 * @param code       Mandal code (nullable)
 * @param isActive   Whether this mandal is active
 */
public record MandalDto(
        UUID id,
        UUID districtId,
        String name,
        String code,
        boolean isActive
) {}
