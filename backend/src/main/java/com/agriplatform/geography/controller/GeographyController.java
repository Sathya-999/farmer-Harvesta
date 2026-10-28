package com.agriplatform.geography.controller;

import com.agriplatform.common.response.ApiResponse;
import com.agriplatform.geography.dto.*;
import com.agriplatform.geography.service.GeographyLibraryIntegrationService;
import com.agriplatform.geography.service.GeographyLocationService;
import com.agriplatform.geography.service.GeographyService;
import com.agriplatform.geography.service.OnDemandGeographySyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Geography module.
 *
 * <h2>Base path</h2>
 * {@code /api/v1/geography}
 *
 * <h2>Public access</h2>
 * <p>GET endpoints in this controller are public (no JWT required) per {@link com.agriplatform.common.config.SecurityConfig}:
 * <pre>
 *   .requestMatchers(HttpMethod.GET, "/api/v1/geography/**").permitAll()
 * </pre>
 *
 * <h2>Authenticated access</h2>
 * <p>POST endpoints (such as on-demand state sync request) require a valid authenticated user JWT.
 */
@RestController
@RequestMapping("/api/v1/geography")
public class GeographyController {

    private final GeographyService geographyService;
    private final GeographyLocationService locationService;
    private final GeographyLibraryIntegrationService libraryService;
    private final OnDemandGeographySyncService syncService;

    public GeographyController(
            GeographyService geographyService,
            GeographyLocationService locationService,
            GeographyLibraryIntegrationService libraryService,
            OnDemandGeographySyncService syncService) {
        this.geographyService = geographyService;
        this.locationService = locationService;
        this.libraryService = libraryService;
        this.syncService = syncService;
    }

    // =========================================================================
    // States
    // =========================================================================

    @GetMapping("/states")
    public ResponseEntity<ApiResponse<List<StateDto>>> getAllStates() {
        List<StateDto> states = geographyService.getAllActiveStates();
        return ResponseEntity.ok(ApiResponse.success(states));
    }

    // =========================================================================
    // Districts
    // =========================================================================

    @GetMapping("/states/{stateId}/districts")
    public ResponseEntity<ApiResponse<List<DistrictDto>>> getDistrictsByState(
            @PathVariable UUID stateId) {
        List<DistrictDto> districts = geographyService.getActiveDistrictsByState(stateId);
        return ResponseEntity.ok(ApiResponse.success(districts));
    }

    // =========================================================================
    // Mandals
    // =========================================================================

    @GetMapping("/districts/{districtId}/mandals")
    public ResponseEntity<ApiResponse<List<MandalDto>>> getMandalsByDistrict(
            @PathVariable UUID districtId) {
        List<MandalDto> mandals = geographyService.getActiveMandalsByDistrict(districtId);
        return ResponseEntity.ok(ApiResponse.success(mandals));
    }

    // =========================================================================
    // Villages
    // =========================================================================

    @GetMapping("/mandals/{mandalId}/villages")
    public ResponseEntity<ApiResponse<List<VillageDto>>> getVillagesByMandal(
            @PathVariable UUID mandalId) {
        List<VillageDto> villages = geographyService.getActiveVillagesByMandal(mandalId);
        return ResponseEntity.ok(ApiResponse.success(villages));
    }

    // =========================================================================
    // GPS Location Resolution (Advisory Suggestion with Farmer Confirmation)
    // =========================================================================

    /**
     * Resolves GPS coordinates to the closest candidate administrative location.
     * Always returns advisory suggestion with manual confirmation required.
     */
    @GetMapping("/resolve-location")
    public ResponseEntity<ApiResponse<LocationResolutionDto>> resolveLocation(
            @RequestParam(value = "lat", required = false) BigDecimal lat,
            @RequestParam(value = "lon", required = false) BigDecimal lon) {
        LocationResolutionDto resolution = locationService.resolveCandidateLocation(lat, lon);
        return ResponseEntity.ok(ApiResponse.success(resolution));
    }

    // =========================================================================
    // State Coverage Detection
    // =========================================================================

    /**
     * Checks current database coverage for an approved state against official source targets.
     */
    @GetMapping("/states/{stateCode}/coverage")
    public ResponseEntity<ApiResponse<StateCoverageDto>> checkStateCoverage(
            @PathVariable String stateCode) {
        StateCoverageDto coverage = libraryService.checkStateCoverage(stateCode);
        return ResponseEntity.ok(ApiResponse.success(coverage));
    }

    // =========================================================================
    // Safe On-Demand State Synchronization (Farmer-Accessible)
    // =========================================================================

    /**
     * Requests on-demand background synchronization for a supported state if data is missing.
     * Protected: requires authenticated JWT.
     */
    @PostMapping("/states/{stateCode}/sync")
    public ResponseEntity<ApiResponse<StateSyncStatusDto>> requestStateSync(
            @PathVariable String stateCode) {
        StateSyncStatusDto status = syncService.requestStateSynchronization(stateCode);
        return ResponseEntity.accepted().body(ApiResponse.success(status));
    }

    /**
     * Polls the status of an on-demand state synchronization job.
     */
    @GetMapping("/states/{stateCode}/sync-status")
    public ResponseEntity<ApiResponse<StateSyncStatusDto>> getSyncStatus(
            @PathVariable String stateCode) {
        StateSyncStatusDto status = syncService.getSyncStatus(stateCode);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
