package com.agriplatform.farmer.service;

import com.agriplatform.farmer.dto.CreateFarmerProfileRequestDto;
import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.dto.UpdateFarmerProfileRequestDto;

/**
 * Service handling Farmer Profile operations.
 *
 * <p>Identity is derived strictly from the authenticated security context.
 */
public interface FarmerProfileService {

    /**
     * Retrieves the profile of the currently authenticated farmer.
     *
     * @return the farmer profile response DTO
     */
    FarmerProfileResponseDto getProfile();

    /**
     * Initializes or completes onboarding for the authenticated farmer's profile.
     *
     * @param request profile creation details
     * @return initialized profile response DTO
     */
    FarmerProfileResponseDto initProfile(CreateFarmerProfileRequestDto request);

    /**
     * Updates profile fields for the authenticated farmer, enforcing optimistic locking.
     *
     * @param request update payload containing expected version
     * @return updated profile response DTO
     */
    FarmerProfileResponseDto updateProfile(UpdateFarmerProfileRequestDto request);
}
