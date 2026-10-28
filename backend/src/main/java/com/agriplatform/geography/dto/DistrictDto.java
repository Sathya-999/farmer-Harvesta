package com.agriplatform.geography.dto;

import java.util.UUID;

/**
 * Read-only DTO for a District.
 *
 * <p>Includes {@code stateId} so the client can navigate the hierarchy.
 * The parent state object is NOT nested to avoid circular serialization
 * and to keep responses flat and efficient.
 *
 * @param id       Supabase UUID primary key
 * @param stateId  Parent state UUID
 * @param name     District name
 * @param code     District code (nullable)
 * @param isActive Whether this district is active
 */
public record DistrictDto(
        UUID id,
        UUID stateId,
        String name,
        String code,
        boolean isActive
) {}
