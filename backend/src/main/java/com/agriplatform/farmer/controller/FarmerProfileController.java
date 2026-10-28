package com.agriplatform.farmer.controller;

import com.agriplatform.common.response.ApiResponse;
import com.agriplatform.farmer.dto.CreateFarmerProfileRequestDto;
import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.dto.UpdateFarmerProfileRequestDto;
import com.agriplatform.farmer.service.FarmerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authenticated Farmer Profile operations.
 *
 * <h2>Base path</h2>
 * {@code /api/v1/farmers/me}
 *
 * <h2>Security & Identity</h2>
 * <p>All endpoints require a valid Supabase JWT Bearer token with {@code aud: "authenticated"}.
 * The farmer's identity is strictly derived from the JWT {@code sub} claim. No client-supplied
 * {@code farmer_id} is accepted or trusted, preventing Insecure Direct Object Reference (IDOR) attacks.
 */
@RestController
@RequestMapping("/api/v1/farmers/me")
public class FarmerProfileController {

    private final FarmerProfileService farmerProfileService;

    public FarmerProfileController(FarmerProfileService farmerProfileService) {
        this.farmerProfileService = farmerProfileService;
    }

    /**
     * Retrieves the profile of the currently authenticated farmer.
     *
     * @return 200 OK with farmer profile details
     */
    @GetMapping
    public ResponseEntity<ApiResponse<FarmerProfileResponseDto>> getProfile() {
        FarmerProfileResponseDto profile = farmerProfileService.getProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Initializes or completes onboarding for the authenticated farmer's profile.
     *
     * @param request profile creation details
     * @return 201 Created with initialized profile details
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FarmerProfileResponseDto>> initProfile(
            @Valid @RequestBody CreateFarmerProfileRequestDto request
    ) {
        FarmerProfileResponseDto profile = farmerProfileService.initProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(profile));
    }

    /**
     * Updates profile fields for the authenticated farmer, enforcing optimistic locking.
     *
     * @param request profile update details with expected version
     * @return 200 OK with updated profile details
     */
    @PutMapping
    public ResponseEntity<ApiResponse<FarmerProfileResponseDto>> updateProfile(
            @Valid @RequestBody UpdateFarmerProfileRequestDto request
    ) {
        FarmerProfileResponseDto profile = farmerProfileService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
