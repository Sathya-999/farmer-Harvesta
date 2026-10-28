package com.agriplatform.farmer.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for updating an existing farmer profile (PUT /api/v1/farmers/me).
 *
 * <p>Requires {@code expectedVersion} for optimistic concurrency control.
 *
 * @param fullName            Optional updated full name (cannot be blank if provided)
 * @param homeVillageId       Optional updated home village UUID (must be active)
 * @param mandalId            Optional parent mandal UUID for hierarchy cross-validation
 * @param districtId          Optional parent district UUID for hierarchy cross-validation
 * @param stateId             Optional parent state UUID for hierarchy cross-validation
 * @param onboardingCompleted Optional onboarding completion flag
 * @param expectedVersion     Expected version token for optimistic locking (mandatory)
 */
public record UpdateFarmerProfileRequestDto(
        String fullName,
        UUID homeVillageId,
        UUID mandalId,
        UUID districtId,
        UUID stateId,
        Boolean onboardingCompleted,

        @NotNull(message = "expectedVersion is required for optimistic locking")
        Long expectedVersion
) {}
