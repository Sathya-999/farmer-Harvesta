package com.agriplatform.farmer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for initializing or completing farmer onboarding profile (POST /api/v1/farmers/me).
 *
 * <p>Geography hierarchy IDs ({@code mandalId}, {@code districtId}, {@code stateId}) are optional.
 * If provided, they must strictly match the true parentage of {@code homeVillageId}.
 *
 * @param fullName      Farmer's display name (mandatory, cannot be blank)
 * @param homeVillageId Primary home village UUID (mandatory, must be active)
 * @param mandalId      Optional parent mandal UUID for hierarchy cross-validation
 * @param districtId    Optional parent district UUID for hierarchy cross-validation
 * @param stateId       Optional parent state UUID for hierarchy cross-validation
 */
public record CreateFarmerProfileRequestDto(
        @NotBlank(message = "fullName is required and cannot be blank")
        String fullName,

        @NotNull(message = "homeVillageId is required")
        UUID homeVillageId,

        UUID mandalId,
        UUID districtId,
        UUID stateId
) {}
