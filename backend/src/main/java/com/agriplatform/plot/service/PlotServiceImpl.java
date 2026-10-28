package com.agriplatform.plot.service;

import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.repository.VillageRepository;
import com.agriplatform.plot.dto.CreatePlotRequestDto;
import com.agriplatform.plot.dto.PlotResponseDto;
import com.agriplatform.plot.dto.UpdatePlotRequestDto;
import com.agriplatform.plot.entity.IdempotencyKey;
import com.agriplatform.plot.entity.Plot;
import com.agriplatform.plot.mapper.PlotMapper;
import com.agriplatform.plot.repository.IdempotencyKeyRepository;
import com.agriplatform.plot.repository.PlotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link PlotService}.
 *
 * <h2>Key Invariants Enforced</h2>
 * <ul>
 *   <li>Farmer identity always from JWT {@code sub} — never from client input.</li>
 *   <li>Farmer must be ACTIVE to create/update/deactivate/reactivate plots.
 *       SUSPENDED → 403; DELETED → 404.</li>
 *   <li>Village validated with full active hierarchy on create and reactivate.</li>
 *   <li>Optimistic concurrency: atomic conditional UPDATE with microsecond ISO version.</li>
 *   <li>Area reduction guard queries {@code cultivation_intentions} before allowing reduction.</li>
 *   <li>Idempotency: database-backed, within same transaction as plot creation.</li>
 *   <li>IDOR: all plot queries scoped to (plotId, farmerId).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PlotServiceImpl implements PlotService {

    private static final Logger log = LoggerFactory.getLogger(PlotServiceImpl.class);

    /** Idempotency record TTL: 24 hours. */
    private static final long IDEMPOTENCY_TTL_HOURS = 24;

    private final PlotRepository plotRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final VillageRepository villageRepository;
    private final PlotMapper plotMapper;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public PlotServiceImpl(
            PlotRepository plotRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            FarmerProfileRepository farmerProfileRepository,
            VillageRepository villageRepository,
            PlotMapper plotMapper,
            SecurityUtils securityUtils,
            ObjectMapper objectMapper) {
        this.plotRepository = plotRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.villageRepository = villageRepository;
        this.plotMapper = plotMapper;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @Override
    @Transactional
    public PlotResponseDto createPlot(CreatePlotRequestDto request, String idempotencyKey) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        log.info("Creating plot for farmer: {}", farmerId);

        // 1. Validate farmer status
        FarmerProfile farmer = requireActiveFarmer(farmerId);

        // 2. Idempotency check (before external/database lookups)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            validateIdempotencyKeyLength(idempotencyKey);
            String requestHash = computeRequestHash(farmerId, request);
            Optional<IdempotencyKey> existing =
                    idempotencyKeyRepository.findValidByFarmerAndKey(farmerId, idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyKey record = existing.get();
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new ConflictException(
                            "Idempotency-Key '" + idempotencyKey +
                            "' was already used with a different request payload");
                }
                log.info("Idempotent replay for farmer: {}, key: {}", farmerId, idempotencyKey);
                return deserializePlotResponse(record.getResponseBody());
            }
        }

        // 3. Validate village (active + hierarchy check)
        if (request.villageId() == null) {
            throw new BadRequestException("villageId is required");
        }
        Village village = validateAndGetActiveVillage(request.villageId());

        // 4. Normalize label
        String label = normalizeLabel(request.label());

        // 5. Build and save plot
        Plot plot = new Plot();
        plot.setFarmerId(farmerId);
        plot.setVillage(village);
        plot.setLabel(label);
        plot.setTotalAreaAcres(request.totalAreaAcres());
        plot.setActive(true);
        OffsetDateTime now = OffsetDateTime.now();
        plot.setCreatedAt(now);
        plot.setUpdatedAt(now);

        Plot saved = plotRepository.saveAndFlush(plot);
        PlotResponseDto responseDto = plotMapper.toDto(saved);

        // 6. Store idempotency record in same transaction
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String requestHash = computeRequestHash(farmerId, request);
            String serialized = serializePlotResponse(responseDto);
            IdempotencyKey record = new IdempotencyKey(
                    farmerId, idempotencyKey, requestHash,
                    201, serialized,
                    OffsetDateTime.now().plusHours(IDEMPOTENCY_TTL_HOURS)
            );
            idempotencyKeyRepository.save(record);
        }

        log.info("Plot created: {} for farmer: {}", saved.getId(), farmerId);
        return responseDto;
    }

    // =========================================================================
    // GET
    // =========================================================================

    @Override
    public PlotResponseDto getPlot(UUID plotId) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        log.debug("Fetching plot: {} for farmer: {}", plotId, farmerId);

        requireFarmerExists(farmerId); // checks DELETED status → 404

        Plot plot = plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found with id: " + plotId));

        return plotMapper.toDto(plot);
    }

    // =========================================================================
    // LIST
    // =========================================================================

    @Override
    public Page<PlotResponseDto> listPlots(Pageable pageable, String status) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        log.debug("Listing plots for farmer: {}, status={}", farmerId, status);

        requireFarmerExists(farmerId);

        // Enforce size limits
        int size = Math.min(pageable.getPageSize(), 100);
        Pageable bounded = PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());

        String statusFilter = (status != null) ? status.toUpperCase() : "ACTIVE";
        return switch (statusFilter) {
            case "INACTIVE" -> plotRepository.findInactiveByFarmerId(farmerId, bounded)
                    .map(plotMapper::toDto);
            case "ALL" -> plotRepository.findAllByFarmerId(farmerId, bounded)
                    .map(plotMapper::toDto);
            default -> plotRepository.findActiveByFarmerId(farmerId, bounded)
                    .map(plotMapper::toDto);
        };
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    @Transactional
    public PlotResponseDto updatePlot(UUID plotId, UpdatePlotRequestDto request) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        log.info("Updating plot: {} for farmer: {}", plotId, farmerId);

        // 1. Validate expectedVersion
        if (request.expectedVersion() == null || request.expectedVersion().isBlank()) {
            throw new BadRequestException("expectedVersion is required");
        }
        try {
            OffsetDateTime.parse(request.expectedVersion());
        } catch (Exception e) {
            throw new BadRequestException("expectedVersion must be a valid ISO-8601 timestamp");
        }

        // 2. Farmer must be ACTIVE
        requireActiveFarmer(farmerId);

        // 2. Fetch current plot (for area reduction guard)
        Plot current = plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found with id: " + plotId));

        // 3. Area reduction guard
        if (request.totalAreaAcres() != null) {
            validateAreaReduction(plotId, farmerId, current.getTotalAreaAcres(), request.totalAreaAcres());
        }

        // 4. Normalize new label
        String newLabel = (request.label() != null) ? normalizeLabel(request.label()) : current.getLabel();
        BigDecimal newArea = (request.totalAreaAcres() != null) ? request.totalAreaAcres() : current.getTotalAreaAcres();

        // 5. Atomic conditional UPDATE
        int rows = plotRepository.conditionalUpdate(
                plotId, farmerId, newLabel, newArea, request.expectedVersion());

        if (rows == 0) {
            // Distinguish 409 (version conflict) from 404 (not found)
            if (plotRepository.existsByIdAndFarmerId(plotId, farmerId)) {
                throw new ConflictException(
                        "Plot update conflict: the plot was modified concurrently. " +
                        "Fetch the latest version and retry.");
            } else {
                throw new ResourceNotFoundException("Plot not found with id: " + plotId);
            }
        }

        // 6. Reload to get the new updated_at set by DB trigger
        Plot updated = plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found after update: " + plotId));

        log.info("Plot updated: {} for farmer: {}", plotId, farmerId);
        return plotMapper.toDto(updated);
    }

    // =========================================================================
    // DEACTIVATE
    // =========================================================================

    @Override
    @Transactional
    public void deactivatePlot(UUID plotId) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        log.info("Deactivating plot: {} for farmer: {}", plotId, farmerId);

        requireActiveFarmer(farmerId);

        // Verify plot ownership first
        if (!plotRepository.existsByIdAndFarmerId(plotId, farmerId)) {
            throw new ResourceNotFoundException("Plot not found with id: " + plotId);
        }

        // Attempt soft delete — DB trigger will raise exception if active cultivation exists
        int rows = plotRepository.deactivate(plotId, farmerId);
        if (rows == 0) {
            // Plot exists (ownership check passed) but is already inactive
            throw new ConflictException("Plot is already inactive: " + plotId);
        }

        log.info("Plot deactivated: {} for farmer: {}", plotId, farmerId);
    }

    // =========================================================================
    // REACTIVATE
    // =========================================================================

    @Override
    @Transactional
    public PlotResponseDto reactivatePlot(UUID plotId) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        log.info("Reactivating plot: {} for farmer: {}", plotId, farmerId);

        requireActiveFarmer(farmerId);

        // Load current state to validate village is still active
        Plot current = plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found with id: " + plotId));

        // Village must still be active for reactivation
        if (!current.getVillage().isActive()) {
            throw new ConflictException(
                    "Cannot reactivate plot: the plot's village is no longer active: " +
                    current.getVillage().getId());
        }

        int rows = plotRepository.reactivate(plotId, farmerId);
        if (rows == 0) {
            throw new ConflictException("Plot is already active: " + plotId);
        }

        Plot updated = plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found after reactivation: " + plotId));

        log.info("Plot reactivated: {} for farmer: {}", plotId, farmerId);
        return plotMapper.toDto(updated);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Loads and validates the farmer profile.
     * <ul>
     *   <li>DELETED status → {@link ResourceNotFoundException} (HTTP 404)</li>
     *   <li>SUSPENDED status → {@link AccessDeniedException} (HTTP 403)</li>
     *   <li>Not found → {@link ResourceNotFoundException} (HTTP 404)</li>
     * </ul>
     */
    private FarmerProfile requireActiveFarmer(UUID farmerId) {
        FarmerProfile farmer = farmerProfileRepository.findByIdWithGeography(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer profile not found for user: " + farmerId));

        if (FarmerProfile.STATUS_DELETED.equals(farmer.getProfileStatus())) {
            throw new ResourceNotFoundException(
                    "Farmer profile not found for user: " + farmerId);
        }
        if (FarmerProfile.STATUS_SUSPENDED.equals(farmer.getProfileStatus())) {
            throw new AccessDeniedException(
                    "Account is suspended. Plot operations are not permitted.");
        }
        return farmer;
    }

    /**
     * Checks whether a farmer profile exists and is not DELETED.
     * Does not require ACTIVE (allows SUSPENDED to read).
     */
    private void requireFarmerExists(UUID farmerId) {
        FarmerProfile farmer = farmerProfileRepository.findByIdWithGeography(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer profile not found for user: " + farmerId));
        if (FarmerProfile.STATUS_DELETED.equals(farmer.getProfileStatus())) {
            throw new ResourceNotFoundException(
                    "Farmer profile not found for user: " + farmerId);
        }
    }

    /**
     * Validates village: exists, active, and full hierarchy (mandal, district, state) active.
     */
    private Village validateAndGetActiveVillage(UUID villageId) {
        Village village = villageRepository.findByIdWithHierarchy(villageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Village not found with ID: " + villageId));

        if (!village.isActive()) {
            throw new BadRequestException("Village is inactive: " + villageId);
        }

        Mandal mandal = village.getMandal();
        if (mandal == null || !mandal.isActive()) {
            throw new BadRequestException(
                    "Village belongs to an inactive mandal: " +
                    (mandal != null ? mandal.getId() : "null"));
        }

        District district = mandal.getDistrict();
        if (district == null || !district.isActive()) {
            throw new BadRequestException(
                    "Village belongs to an inactive district: " +
                    (district != null ? district.getId() : "null"));
        }

        State state = district.getState();
        if (state == null || !state.isActive()) {
            throw new BadRequestException(
                    "Village belongs to an inactive state: " +
                    (state != null ? state.getId() : "null"));
        }

        return village;
    }

    /**
     * Enforces the area reduction guard.
     *
     * <p>New area must not be less than the maximum cultivation allocation
     * in any active (PLANNED or STARTED) season for this plot.
     */
    private void validateAreaReduction(UUID plotId, UUID farmerId,
                                       BigDecimal currentArea, BigDecimal newArea) {
        if (currentArea == null) {
            return; // currently null area — any new area is permitted
        }
        if (newArea.compareTo(currentArea) >= 0) {
            return; // area is increasing or unchanged — no guard needed
        }

        BigDecimal maxCommitted = plotRepository.findMaxActiveCultivationArea(plotId, farmerId);
        if (maxCommitted == null) {
            return; // no active cultivation — reduction permitted
        }

        if (newArea.compareTo(maxCommitted) < 0) {
            throw new ConflictException(
                    "Cannot reduce plot area to " + newArea +
                    " acres because active cultivation commitments require " +
                    maxCommitted + " acres in current/planned seasons.");
        }
    }

    /**
     * Trims a label, converts blank/empty to null.
     * Blank labels are rejected at the DB level; treating empty-after-trim as null
     * is the approved normalization.
     */
    private String normalizeLabel(String label) {
        if (label == null) return null;
        String trimmed = label.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Validates that an idempotency key does not exceed the maximum allowed length.
     */
    private void validateIdempotencyKeyLength(String key) {
        if (key.length() > 128) {
            throw new BadRequestException(
                    "Idempotency-Key must not exceed 128 characters");
        }
    }

    /**
     * Computes a SHA-256 hash of the logical request identity to detect
     * payload changes on duplicate idempotency key submissions.
     */
    private String computeRequestHash(UUID farmerId, CreatePlotRequestDto request) {
        String input = farmerId + "|" + request.villageId() + "|" +
                request.label() + "|" + request.totalAreaAcres();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String serializePlotResponse(PlotResponseDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize plot response for idempotency record", e);
        }
    }

    private PlotResponseDto deserializePlotResponse(String json) {
        try {
            return objectMapper.readValue(json, PlotResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached idempotency response", e);
        }
    }
}
