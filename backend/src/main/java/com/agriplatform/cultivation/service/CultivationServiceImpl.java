package com.agriplatform.cultivation.service;

import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.cultivation.dto.*;
import com.agriplatform.cultivation.entity.*;
import com.agriplatform.cultivation.mapper.CultivationMapper;
import com.agriplatform.cultivation.repository.*;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.plot.entity.IdempotencyKey;
import com.agriplatform.plot.entity.Plot;
import com.agriplatform.plot.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CultivationServiceImpl implements CultivationService {

    private static final Logger log = LoggerFactory.getLogger(CultivationServiceImpl.class);
    private static final long IDEMPOTENCY_TTL_HOURS = 24L;

    private final EntityManager entityManager;
    private final CultivationIntentionRepository cultivationRepository;
    private final CultivationStatusEventRepository statusEventRepository;
    private final CultivationHistoryRepository historyRepository;
    private final SeasonRepository seasonRepository;
    private final CropRepository cropRepository;
    private final CropVarietyRepository cropVarietyRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final CultivationMapper cultivationMapper;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public CultivationServiceImpl(
            EntityManager entityManager,
            CultivationIntentionRepository cultivationRepository,
            CultivationStatusEventRepository statusEventRepository,
            CultivationHistoryRepository historyRepository,
            SeasonRepository seasonRepository,
            CropRepository cropRepository,
            CropVarietyRepository cropVarietyRepository,
            FarmerProfileRepository farmerProfileRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            CultivationMapper cultivationMapper,
            SecurityUtils securityUtils,
            ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.cultivationRepository = cultivationRepository;
        this.statusEventRepository = statusEventRepository;
        this.historyRepository = historyRepository;
        this.seasonRepository = seasonRepository;
        this.cropRepository = cropRepository;
        this.cropVarietyRepository = cropVarietyRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.cultivationMapper = cultivationMapper;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @Override
    @Transactional
    public CultivationResponseDto createCultivation(CreateCultivationRequestDto request, String idempotencyKey) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        log.info("Creating cultivation for farmer: {}, plot: {}, season: {}, year: {}",
                farmerId, request.plotId(), request.seasonId(), request.cultivationYear());

        // 1. Farmer lifecycle validation (DELETED -> 404, SUSPENDED -> 403, ACTIVE -> OK)
        requireActiveFarmer(farmerId);

        // 2. Set database session actor context for DB triggers
        setActorContext(farmerId);

        // 3. Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            validateIdempotencyKeyLength(idempotencyKey);
            Optional<IdempotencyKey> existing = idempotencyKeyRepository.findValidByFarmerAndKey(farmerId, idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyKey record = existing.get();
                String requestHash = computeRequestHash(farmerId, request);
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new ConflictException(
                            "Idempotency-Key '" + idempotencyKey +
                            "' was already used with a different request payload");
                }
                log.info("Idempotent replay for farmer: {}, key: {}", farmerId, idempotencyKey);
                return deserializeCultivationResponse(record.getResponseBody());
            }
        }

        // 4. Validate and lock plot (Pessimistic Write Lock eliminates concurrent area allocation races)
        Plot plot = getPlotAndVerifyOwnerWithLock(request.plotId(), farmerId);
        if (!plot.isActive()) {
            throw new BadRequestException("Cannot create cultivation on an inactive plot: " + request.plotId());
        }

        // 5. Validate Crop
        Crop crop = cropRepository.findById(request.cropId())
                .orElseThrow(() -> new BadRequestException("Crop not found with id: " + request.cropId()));
        if (!crop.isActive()) {
            throw new BadRequestException("Selected crop is inactive: " + request.cropId());
        }

        // 6. Validate Crop Variety (optional, but if provided must belong to crop and be active)
        CropVariety variety = null;
        if (request.cropVarietyId() != null) {
            variety = cropVarietyRepository.findById(request.cropVarietyId())
                    .orElseThrow(() -> new BadRequestException("Crop variety not found with id: " + request.cropVarietyId()));
            if (!variety.isActive()) {
                throw new BadRequestException("Selected crop variety is inactive: " + request.cropVarietyId());
            }
            if (!variety.getCrop().getId().equals(crop.getId())) {
                throw new BadRequestException("Crop variety '" + variety.getName() +
                        "' does not belong to crop '" + crop.getName() + "'");
            }
        }

        // 7. Validate Season
        Season season = seasonRepository.findById(request.seasonId())
                .orElseThrow(() -> new BadRequestException("Season not found with id: " + request.seasonId()));
        if (!season.isActive()) {
            throw new BadRequestException("Selected season is inactive: " + request.seasonId());
        }

        // 8. Validate Cultivation Year
        if (request.cultivationYear() < 2000 || request.cultivationYear() > 2100) {
            throw new BadRequestException("Cultivation year must be between 2000 and 2100");
        }

        // 9. Lineage uniqueness check: ONE PLOT + ONE SEASON + ONE YEAR = ONE ACTIVE LINEAGE
        if (cultivationRepository.existsActiveCycle(plot.getId(), season.getId(), request.cultivationYear())) {
            throw new ConflictException("An active cultivation cycle already exists for this plot, season, and year.");
        }

        // 10. Capacity guard: active allocations must not exceed plot's total area
        if (plot.getTotalAreaAcres() != null) {
            if (request.cultivatedAreaAcres().compareTo(plot.getTotalAreaAcres()) > 0) {
                throw new ConflictException(
                        "Cultivated area (" + request.cultivatedAreaAcres() +
                        " acres) exceeds plot total area (" + plot.getTotalAreaAcres() + " acres).");
            }
            BigDecimal currentAllocated = cultivationRepository.sumActiveCultivatedArea(
                    plot.getId(), season.getId(), request.cultivationYear());
            BigDecimal totalAllocated = currentAllocated.add(request.cultivatedAreaAcres());
            if (totalAllocated.compareTo(plot.getTotalAreaAcres()) > 0) {
                throw new ConflictException(
                        "Total active cultivation allocation (" + totalAllocated +
                        " acres) would exceed plot capacity (" + plot.getTotalAreaAcres() +
                        " acres) for this season/year.");
            }
        }

        // 11. Build and save CultivationIntention
        CultivationIntention intention = new CultivationIntention();
        intention.setPlot(plot);
        intention.setFarmerId(farmerId);
        intention.setCrop(crop);
        intention.setCropVariety(variety);
        intention.setSeason(season);
        intention.setCultivationYear(request.cultivationYear());
        intention.setCultivatedAreaAcres(request.cultivatedAreaAcres());
        intention.setPlannedPlantingDate(request.plannedPlantingDate());
        intention.setStatus(CultivationIntention.STATUS_PLANNED);
        intention.setVersion(1);
        intention.setSource(CultivationIntention.SOURCE_FARMER_APP);
        OffsetDateTime now = OffsetDateTime.now();
        intention.setCreatedAt(now);
        intention.setUpdatedAt(now);

        CultivationIntention saved = cultivationRepository.saveAndFlush(intention);
        CultivationResponseDto responseDto = cultivationMapper.toDto(saved);

        // 12. Save idempotency record if key was provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String requestHash = computeRequestHash(farmerId, request);
            String serialized = serializeCultivationResponse(responseDto);
            IdempotencyKey record = new IdempotencyKey(
                    farmerId, idempotencyKey, requestHash,
                    201, serialized,
                    OffsetDateTime.now().plusHours(IDEMPOTENCY_TTL_HOURS)
            );
            idempotencyKeyRepository.save(record);
        }

        log.info("Cultivation intention created: {} for farmer: {}", saved.getId(), farmerId);
        return responseDto;
    }

    // =========================================================================
    // GET
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public CultivationResponseDto getCultivation(UUID id) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        requireFarmerExists(farmerId);

        CultivationIntention intention = cultivationRepository.findByIdAndFarmerIdWithDetails(id, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cultivation intention not found with id: " + id));

        return cultivationMapper.toDto(intention);
    }

    // =========================================================================
    // LIST
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CultivationResponseDto> listCultivations(
            Pageable pageable,
            UUID plotId,
            UUID seasonId,
            Short cultivationYear,
            UUID cropId,
            String status) {

        UUID farmerId = securityUtils.getAuthenticatedUserId();
        requireFarmerExists(farmerId);

        Specification<CultivationIntention> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("farmerId"), farmerId));

            if (plotId != null) {
                predicates.add(cb.equal(root.get("plot").get("id"), plotId));
            }
            if (seasonId != null) {
                predicates.add(cb.equal(root.get("season").get("id"), seasonId));
            }
            if (cultivationYear != null) {
                predicates.add(cb.equal(root.get("cultivationYear"), cultivationYear));
            }
            if (cropId != null) {
                predicates.add(cb.equal(root.get("crop").get("id"), cropId));
            }
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CultivationIntention> page = cultivationRepository.findAll(spec, pageable);
        return page.map(cultivationMapper::toDto);
    }

    // =========================================================================
    // UPDATE (MUTABLE FIELDS IN PLANNED STATUS)
    // =========================================================================

    @Override
    @Transactional
    public CultivationResponseDto updateCultivation(UUID id, UpdateCultivationRequestDto request) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        requireActiveFarmer(farmerId);
        setActorContext(farmerId);

        CultivationIntention intention = cultivationRepository.findByIdAndFarmerIdWithDetails(id, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cultivation intention not found with id: " + id));

        if (intention.isTerminal()) {
            throw new ConflictException("Terminal cultivation cycle cannot be edited.");
        }

        if (!CultivationIntention.STATUS_PLANNED.equals(intention.getStatus())) {
            throw new BadRequestException("Cultivation is in " + intention.getStatus() +
                    " status. Field edits are only permitted in PLANNED status. Use status update endpoint.");
        }

        // Optimistic concurrency control
        if (request.expectedVersion() != null && !request.expectedVersion().equals(intention.getVersion())) {
            throw new ConflictException("Version conflict: expected version " + request.expectedVersion() +
                    " but current version is " + intention.getVersion());
        }

        // Update crop if specified
        if (request.cropId() != null && !request.cropId().equals(intention.getCrop().getId())) {
            Crop crop = cropRepository.findById(request.cropId())
                    .orElseThrow(() -> new BadRequestException("Crop not found with id: " + request.cropId()));
            if (!crop.isActive()) {
                throw new BadRequestException("Selected crop is inactive: " + request.cropId());
            }
            intention.setCrop(crop);
            intention.setCropVariety(null); // reset variety when crop changes
        }

        // Update variety if specified
        if (request.cropVarietyId() != null) {
            CropVariety variety = cropVarietyRepository.findById(request.cropVarietyId())
                    .orElseThrow(() -> new BadRequestException("Crop variety not found with id: " + request.cropVarietyId()));
            if (!variety.isActive()) {
                throw new BadRequestException("Selected crop variety is inactive: " + request.cropVarietyId());
            }
            if (!variety.getCrop().getId().equals(intention.getCrop().getId())) {
                throw new BadRequestException("Crop variety '" + variety.getName() +
                        "' does not belong to crop '" + intention.getCrop().getName() + "'");
            }
            intention.setCropVariety(variety);
        }

        // Update area if specified
        if (request.cultivatedAreaAcres() != null &&
            request.cultivatedAreaAcres().compareTo(intention.getCultivatedAreaAcres()) != 0) {

            Plot plot = getPlotAndVerifyOwnerWithLock(intention.getPlot().getId(), farmerId);
            if (plot.getTotalAreaAcres() != null) {
                if (request.cultivatedAreaAcres().compareTo(plot.getTotalAreaAcres()) > 0) {
                    throw new ConflictException(
                            "Cultivated area (" + request.cultivatedAreaAcres() +
                            " acres) exceeds plot total area (" + plot.getTotalAreaAcres() + " acres).");
                }
                BigDecimal otherAllocated = cultivationRepository.sumActiveCultivatedAreaExcluding(
                        plot.getId(), intention.getSeason().getId(), intention.getCultivationYear(), intention.getId());
                BigDecimal newTotal = otherAllocated.add(request.cultivatedAreaAcres());
                if (newTotal.compareTo(plot.getTotalAreaAcres()) > 0) {
                    throw new ConflictException(
                            "Total active cultivation allocation (" + newTotal +
                            " acres) would exceed plot capacity (" + plot.getTotalAreaAcres() +
                            " acres) for this season/year.");
                }
            }
            intention.setCultivatedAreaAcres(request.cultivatedAreaAcres());
        }

        // Update planned planting date if specified
        if (request.plannedPlantingDate() != null) {
            intention.setPlannedPlantingDate(request.plannedPlantingDate());
        }

        CultivationIntention saved = cultivationRepository.saveAndFlush(intention);
        log.info("Cultivation intention updated: {} for farmer: {}", saved.getId(), farmerId);
        return cultivationMapper.toDto(saved);
    }

    // =========================================================================
    // STATUS TRANSITIONS (LIFECYCLE STATE MACHINE)
    // =========================================================================

    @Override
    @Transactional
    public CultivationResponseDto updateCultivationStatus(UUID id, CultivationStatusUpdateRequestDto request) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        requireActiveFarmer(farmerId);
        setActorContext(farmerId);

        CultivationIntention intention = cultivationRepository.findByIdAndFarmerIdWithDetails(id, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cultivation intention not found with id: " + id));

        if (intention.isTerminal()) {
            throw new ConflictException("Terminal cultivation cycle cannot be edited.");
        }

        // Optimistic concurrency control
        if (request.expectedVersion() != null && !request.expectedVersion().equals(intention.getVersion())) {
            throw new ConflictException("Version conflict: expected version " + request.expectedVersion() +
                    " but current version is " + intention.getVersion());
        }

        String currentStatus = intention.getStatus();
        String targetStatus = request.targetStatus().toUpperCase();

        // Enforce state machine rules matching DB trigger
        if (CultivationIntention.STATUS_PLANNED.equals(currentStatus)) {
            if (CultivationIntention.STATUS_STARTED.equals(targetStatus)) {
                if (request.actualPlantingDate() == null) {
                    throw new BadRequestException("actualPlantingDate is required when transitioning to STARTED");
                }
                intention.setActualPlantingDate(request.actualPlantingDate());
                if (request.actualAreaAcres() != null) {
                    if (request.actualAreaAcres().compareTo(intention.getCultivatedAreaAcres()) > 0) {
                        throw new BadRequestException("actualAreaAcres (" + request.actualAreaAcres() +
                                ") cannot exceed cultivated allocation (" + intention.getCultivatedAreaAcres() + ")");
                    }
                    intention.setActualAreaAcres(request.actualAreaAcres());
                }
                if (request.harvestDate() != null) {
                    throw new BadRequestException("harvestDate is not permitted when transitioning to STARTED");
                }
            } else if (CultivationIntention.STATUS_CANCELLED.equals(targetStatus)) {
                if (request.actualPlantingDate() != null || request.actualAreaAcres() != null || request.harvestDate() != null) {
                    throw new BadRequestException("Actual planting fields are not permitted when cancelling an intention");
                }
            } else {
                throw new ConflictException("Invalid status transition from " + currentStatus + " to " + targetStatus);
            }
        } else if (CultivationIntention.STATUS_STARTED.equals(currentStatus)) {
            if (CultivationIntention.STATUS_COMPLETED.equals(targetStatus)) {
                LocalDate actualPlanting = request.actualPlantingDate() != null
                        ? request.actualPlantingDate()
                        : intention.getActualPlantingDate();
                if (actualPlanting == null) {
                    throw new BadRequestException("actualPlantingDate is required when completing cultivation");
                }
                intention.setActualPlantingDate(actualPlanting);

                if (request.harvestDate() == null) {
                    throw new BadRequestException("harvestDate is required when completing cultivation");
                }
                if (request.harvestDate().isBefore(actualPlanting)) {
                    throw new BadRequestException("harvestDate (" + request.harvestDate() +
                            ") must be on or after actual planting date (" + actualPlanting + ")");
                }
                intention.setHarvestDate(request.harvestDate());

                if (request.actualAreaAcres() != null) {
                    if (request.actualAreaAcres().compareTo(intention.getCultivatedAreaAcres()) > 0) {
                        throw new BadRequestException("actualAreaAcres (" + request.actualAreaAcres() +
                                ") cannot exceed cultivated allocation (" + intention.getCultivatedAreaAcres() + ")");
                    }
                    intention.setActualAreaAcres(request.actualAreaAcres());
                }
            } else if (CultivationIntention.STATUS_ABANDONED.equals(targetStatus)) {
                if (request.actualPlantingDate() != null) {
                    intention.setActualPlantingDate(request.actualPlantingDate());
                }
                if (request.actualAreaAcres() != null) {
                    intention.setActualAreaAcres(request.actualAreaAcres());
                }
            } else {
                throw new ConflictException("Invalid status transition from " + currentStatus + " to " + targetStatus);
            }
        } else {
            throw new ConflictException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        intention.setStatus(targetStatus);
        intention.setStatusReason(request.statusReason());

        CultivationIntention saved = cultivationRepository.saveAndFlush(intention);
        log.info("Cultivation intention status changed to {} for intention: {}", targetStatus, saved.getId());
        return cultivationMapper.toDto(saved);
    }

    // =========================================================================
    // EVENTS & HISTORY AUDIT (OWNER-SCOPED READS)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CultivationStatusEventDto> getCultivationEvents(UUID id) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        requireFarmerExists(farmerId);

        // IDOR check: verify intention exists and belongs to caller
        if (!cultivationRepository.existsByIdAndFarmerId(id, farmerId)) {
            throw new ResourceNotFoundException("Cultivation intention not found with id: " + id);
        }

        List<CultivationStatusEvent> events = statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(id);
        return events.stream().map(cultivationMapper::toEventDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CultivationHistoryDto> getCultivationHistory(UUID id) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        requireFarmerExists(farmerId);

        // IDOR check: verify intention exists and belongs to caller
        if (!cultivationRepository.existsByIdAndFarmerId(id, farmerId)) {
            throw new ResourceNotFoundException("Cultivation intention not found with id: " + id);
        }

        List<CultivationIntentionHistory> history = historyRepository.findAllByIntentionIdOrderByVersionDesc(id);
        return history.stream().map(cultivationMapper::toHistoryDto).toList();
    }

    // =========================================================================
    // REFERENCE DATA
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CropResponseDto> listActiveCrops() {
        return cropRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream().map(cultivationMapper::toCropDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CropVarietyResponseDto> listActiveVarietiesForCrop(UUID cropId) {
        return cropVarietyRepository.findAllByCropIdAndIsActiveTrueOrderByNameAsc(cropId)
                .stream().map(cultivationMapper::toVarietyDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeasonResponseDto> listActiveSeasons() {
        return seasonRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream().map(cultivationMapper::toSeasonDto).toList();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void setActorContext(UUID farmerId) {
        entityManager.createNativeQuery(
                "SELECT set_config('app.actor_id', :actorId, true), " +
                "       set_config('app.actor_type', 'FARMER', true)")
                .setParameter("actorId", farmerId.toString())
                .getSingleResult();
    }

    private Plot getPlotAndVerifyOwnerWithLock(UUID plotId, UUID farmerId) {
        TypedQuery<Plot> q = entityManager.createQuery(
                "SELECT p FROM Plot p WHERE p.id = :plotId AND p.farmerId = :farmerId", Plot.class);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        q.setParameter("plotId", plotId);
        q.setParameter("farmerId", farmerId);

        List<Plot> results = q.getResultList();
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Plot not found with id: " + plotId);
        }
        return results.get(0);
    }

    private void requireActiveFarmer(UUID farmerId) {
        FarmerProfile farmer = farmerProfileRepository.findByIdWithGeography(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer profile not found for user: " + farmerId));

        if (FarmerProfile.STATUS_DELETED.equals(farmer.getProfileStatus())) {
            throw new ResourceNotFoundException("Farmer profile not found for user: " + farmerId);
        }
        if (FarmerProfile.STATUS_SUSPENDED.equals(farmer.getProfileStatus())) {
            throw new AccessDeniedException("Account is suspended. Cultivation operations are not permitted.");
        }
    }

    private void requireFarmerExists(UUID farmerId) {
        FarmerProfile farmer = farmerProfileRepository.findByIdWithGeography(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer profile not found for user: " + farmerId));
        if (FarmerProfile.STATUS_DELETED.equals(farmer.getProfileStatus())) {
            throw new ResourceNotFoundException("Farmer profile not found for user: " + farmerId);
        }
    }

    private void validateIdempotencyKeyLength(String key) {
        if (key.length() > 128) {
            throw new BadRequestException("Idempotency-Key must not exceed 128 characters");
        }
    }

    private String computeRequestHash(UUID farmerId, CreateCultivationRequestDto request) {
        String input = farmerId + "|" + request.plotId() + "|" +
                request.cropId() + "|" + request.cropVarietyId() + "|" +
                request.seasonId() + "|" + request.cultivationYear() + "|" +
                request.cultivatedAreaAcres() + "|" + request.plannedPlantingDate();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String serializeCultivationResponse(CultivationResponseDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cultivation response for idempotency record", e);
        }
    }

    private CultivationResponseDto deserializeCultivationResponse(String json) {
        try {
            return objectMapper.readValue(json, CultivationResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached cultivation response", e);
        }
    }
}
