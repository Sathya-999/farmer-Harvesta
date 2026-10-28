package com.agriplatform.farmer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for a Farmer Profile.
 *
 * <p>Includes resolved geography hierarchy details (Village, Mandal, District, State)
 * and the optimistic concurrency token ({@code version}).
 *
 * @param id                  Supabase user UUID
 * @param fullName            Farmer's display name
 * @param homeVillageId       Home village UUID (nullable if onboarding not completed)
 * @param villageName         Home village name (nullable)
 * @param mandalId            Parent mandal UUID (nullable)
 * @param mandalName          Parent mandal name (nullable)
 * @param districtId          Parent district UUID (nullable)
 * @param districtName        Parent district name (nullable)
 * @param stateId             Parent state UUID (nullable)
 * @param stateName           Parent state name (nullable)
 * @param profileStatus       Status: ACTIVE, SUSPENDED, DELETED
 * @param onboardingCompleted Whether the farmer has completed onboarding
 * @param version             Optimistic locking version token (updated_at epoch millis)
 * @param createdAt           Profile creation timestamp
 * @param updatedAt           Profile last updated timestamp
 */
public record FarmerProfileResponseDto(
        UUID id,
        String fullName,
        UUID homeVillageId,
        String villageName,
        UUID mandalId,
        String mandalName,
        UUID districtId,
        String districtName,
        UUID stateId,
        String stateName,
        String profileStatus,
        boolean onboardingCompleted,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
