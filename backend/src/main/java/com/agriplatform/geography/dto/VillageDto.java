package com.agriplatform.geography.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only DTO for a Village.
 *
 * <p>Includes {@code mandalId} for hierarchy navigation and optional
 * {@code latitude}/{@code longitude} coordinates.
 * Parent mandal is NOT nested to avoid circular JSON and eager-loading issues.
 *
 * @param id        Supabase UUID primary key
 * @param mandalId  Parent mandal UUID
 * @param name      Village name
 * @param code      Village code (nullable)
 * @param latitude  Geographic latitude (nullable)
 * @param longitude Geographic longitude (nullable)
 * @param isActive  Whether this village is active
 */
public record VillageDto(
        UUID id,
        UUID mandalId,
        String name,
        String code,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isActive
) {}
