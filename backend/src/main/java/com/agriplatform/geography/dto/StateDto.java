package com.agriplatform.geography.dto;

import java.util.UUID;

/**
 * Read-only DTO for a State.
 *
 * <p>Exposes only the fields relevant to API consumers.
 * Internal fields (created_at, updated_at) are intentionally excluded.
 *
 * @param id       Supabase UUID primary key
 * @param name     State name (e.g. "Andhra Pradesh")
 * @param code     State code (e.g. "AP")
 * @param isActive Whether this state is active in the reference data
 */
public record StateDto(
        UUID id,
        String name,
        String code,
        boolean isActive
) {}
