package com.agriplatform.farmer.service;

import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.farmer.dto.CreateFarmerProfileRequestDto;
import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.dto.UpdateFarmerProfileRequestDto;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.mapper.FarmerProfileMapper;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.repository.VillageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link FarmerProfileService}.
 *
 * <h2>Key Guarantees</h2>
 * <ul>
 *   <li><strong>Identity Integrity:</strong> Always obtains the user's UUID from the JWT
 *       via {@link SecurityUtils}; client cannot pass or spoof identity.</li>
 *   <li><strong>Geography Validation:</strong> Validates village existence, active status,
 *       and hierarchical consistency (Village → Mandal → District → State).</li>
 *   <li><strong>Optimistic Concurrency:</strong> Enforces version checking on updates to prevent
 *       lost updates.</li>
 *   <li><strong>Duplicate Prevention:</strong> Prevents duplicate profile initialization.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class FarmerProfileServiceImpl implements FarmerProfileService {

    private static final Logger log = LoggerFactory.getLogger(FarmerProfileServiceImpl.class);

    private final FarmerProfileRepository farmerProfileRepository;
    private final VillageRepository villageRepository;
    private final FarmerProfileMapper farmerProfileMapper;
    private final SecurityUtils securityUtils;

    public FarmerProfileServiceImpl(
            FarmerProfileRepository farmerProfileRepository,
            VillageRepository villageRepository,
            FarmerProfileMapper farmerProfileMapper,
            SecurityUtils securityUtils
    ) {
        this.farmerProfileRepository = farmerProfileRepository;
        this.villageRepository = villageRepository;
        this.farmerProfileMapper = farmerProfileMapper;
        this.securityUtils = securityUtils;
    }

    @Override
    public FarmerProfileResponseDto getProfile() {
        UUID userId = securityUtils.getAuthenticatedUserId();
        log.debug("Fetching farmer profile for authenticated user: {}", userId);

        FarmerProfile profile = farmerProfileRepository.findByIdWithGeography(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for user: " + userId));

        return farmerProfileMapper.toDto(profile);
    }

    @Override
    @Transactional
    public FarmerProfileResponseDto initProfile(CreateFarmerProfileRequestDto request) {
        UUID userId = securityUtils.getAuthenticatedUserId();
        log.info("Initializing farmer profile for user: {}", userId);

        Optional<FarmerProfile> existingOpt = farmerProfileRepository.findByIdWithGeography(userId);
        if (existingOpt.isPresent() && existingOpt.get().isOnboardingCompleted()) {
            throw new ConflictException("Farmer profile has already been initialized and completed onboarding");
        }

        // Validate geography reference & hierarchy
        Village village = validateAndGetVillage(
                request.homeVillageId(),
                request.mandalId(),
                request.districtId(),
                request.stateId()
        );

        String trimmedName = request.fullName() != null ? request.fullName().trim() : null;
        if (trimmedName == null || trimmedName.isEmpty()) {
            throw new BadRequestException("Full name cannot be blank");
        }

        FarmerProfile profile;
        if (existingOpt.isPresent()) {
            profile = existingOpt.get();
            profile.setFullName(trimmedName);
            profile.setHomeVillage(village);
            profile.setOnboardingCompleted(true);
            profile.setProfileStatus(FarmerProfile.STATUS_ACTIVE);
            profile.setUpdatedAt(OffsetDateTime.now());
        } else {
            profile = new FarmerProfile();
            profile.setId(userId);
            profile.setFullName(trimmedName);
            profile.setHomeVillage(village);
            profile.setOnboardingCompleted(true);
            profile.setProfileStatus(FarmerProfile.STATUS_ACTIVE);
            OffsetDateTime now = OffsetDateTime.now();
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);
        }

        FarmerProfile saved = farmerProfileRepository.saveAndFlush(profile);
        return farmerProfileMapper.toDto(saved);
    }

    @Override
    @Transactional
    public FarmerProfileResponseDto updateProfile(UpdateFarmerProfileRequestDto request) {
        UUID userId = securityUtils.getAuthenticatedUserId();
        log.info("Updating farmer profile for user: {}", userId);

        FarmerProfile profile = farmerProfileRepository.findByIdWithGeography(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for user: " + userId));

        if (!FarmerProfile.STATUS_ACTIVE.equals(profile.getProfileStatus())) {
            throw new BadRequestException("Profile cannot be updated because status is " + profile.getProfileStatus());
        }

        // Optimistic concurrency check
        Long currentVersion = profile.getVersion();
        if (request.expectedVersion() == null || !request.expectedVersion().equals(currentVersion)) {
            throw new ConflictException(
                    "Optimistic lock conflict: expected version " + request.expectedVersion()
                            + " but current version is " + currentVersion
            );
        }

        // Update full name if supplied
        if (request.fullName() != null) {
            String trimmedName = request.fullName().trim();
            if (trimmedName.isEmpty()) {
                throw new BadRequestException("Full name cannot be blank");
            }
            profile.setFullName(trimmedName);
        }

        // Update home village if supplied
        if (request.homeVillageId() != null) {
            Village village = validateAndGetVillage(
                    request.homeVillageId(),
                    request.mandalId(),
                    request.districtId(),
                    request.stateId()
            );
            profile.setHomeVillage(village);
        } else if (profile.getHomeVillage() != null &&
                (request.mandalId() != null || request.districtId() != null || request.stateId() != null)) {
            // Verify hierarchy consistency against current village if partial hierarchy params were passed
            validateAndGetVillage(
                    profile.getHomeVillage().getId(),
                    request.mandalId(),
                    request.districtId(),
                    request.stateId()
            );
        }

        if (request.onboardingCompleted() != null) {
            profile.setOnboardingCompleted(request.onboardingCompleted());
        }

        profile.setUpdatedAt(OffsetDateTime.now());
        FarmerProfile saved = farmerProfileRepository.saveAndFlush(profile);
        return farmerProfileMapper.toDto(saved);
    }

    /**
     * Validates that the village exists, is active, its ancestor chain is active,
     * and that any client-supplied hierarchy IDs match the canonical hierarchy.
     */
    private Village validateAndGetVillage(UUID villageId, UUID mandalId, UUID districtId, UUID stateId) {
        if (villageId == null) {
            throw new BadRequestException("Village ID must not be null");
        }

        Village village = villageRepository.findByIdWithHierarchy(villageId)
                .orElseThrow(() -> new ResourceNotFoundException("Village not found with ID: " + villageId));

        if (!village.isActive()) {
            throw new BadRequestException("Village is inactive: " + villageId);
        }

        Mandal mandal = village.getMandal();
        if (mandal == null || !mandal.isActive()) {
            throw new BadRequestException("Village belongs to an inactive mandal: " + (mandal != null ? mandal.getId() : "null"));
        }

        District district = mandal.getDistrict();
        if (district == null || !district.isActive()) {
            throw new BadRequestException("Village belongs to an inactive district: " + (district != null ? district.getId() : "null"));
        }

        State state = district.getState();
        if (state == null || !state.isActive()) {
            throw new BadRequestException("Village belongs to an inactive state: " + (state != null ? state.getId() : "null"));
        }

        if (mandalId != null && !mandalId.equals(mandal.getId())) {
            throw new BadRequestException("Provided mandalId (" + mandalId + ") does not match village mandal (" + mandal.getId() + ")");
        }

        if (districtId != null && !districtId.equals(district.getId())) {
            throw new BadRequestException("Provided districtId (" + districtId + ") does not match village district (" + district.getId() + ")");
        }

        if (stateId != null && !stateId.equals(state.getId())) {
            throw new BadRequestException("Provided stateId (" + stateId + ") does not match village state (" + state.getId() + ")");
        }

        return village;
    }
}
