package com.agriplatform.cultivation;

import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.cultivation.dto.*;
import com.agriplatform.cultivation.entity.*;
import com.agriplatform.cultivation.mapper.CultivationMapper;
import com.agriplatform.cultivation.repository.*;
import com.agriplatform.cultivation.service.CultivationServiceImpl;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.plot.entity.IdempotencyKey;
import com.agriplatform.plot.entity.Plot;
import com.agriplatform.plot.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 08 — Cultivation Service Unit Tests")
class CultivationServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private CultivationIntentionRepository cultivationRepository;

    @Mock
    private CultivationStatusEventRepository statusEventRepository;

    @Mock
    private CultivationHistoryRepository historyRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private CropRepository cropRepository;

    @Mock
    private CropVarietyRepository cropVarietyRepository;

    @Mock
    private FarmerProfileRepository farmerProfileRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private TypedQuery<Plot> plotQuery;

    @Mock
    private Query nativeQuery;

    private final CultivationMapper cultivationMapper = new CultivationMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private CultivationServiceImpl service;

    private UUID farmerId;
    private FarmerProfile activeFarmer;
    private Plot activePlot;
    private Crop activeCrop;
    private CropVariety activeVariety;
    private Season activeSeason;

    @BeforeEach
    void setUp() {
        service = new CultivationServiceImpl(
                entityManager,
                cultivationRepository,
                statusEventRepository,
                historyRepository,
                seasonRepository,
                cropRepository,
                cropVarietyRepository,
                farmerProfileRepository,
                idempotencyKeyRepository,
                cultivationMapper,
                securityUtils,
                objectMapper
        );

        farmerId = UUID.randomUUID();
        lenient().when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);

        activeFarmer = new FarmerProfile();
        activeFarmer.setId(farmerId);
        activeFarmer.setProfileStatus(FarmerProfile.STATUS_ACTIVE);
        lenient().when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));

        activePlot = new Plot();
        activePlot.setId(UUID.randomUUID());
        activePlot.setFarmerId(farmerId);
        activePlot.setTotalAreaAcres(new BigDecimal("10.00"));
        activePlot.setActive(true);

        activeCrop = new Crop(UUID.randomUUID(), "Paddy", "Cereal", true);
        activeVariety = new CropVariety(UUID.randomUUID(), activeCrop, "Basmati", true);
        activeSeason = new Season(UUID.randomUUID(), "Kharif", (short) 6, (short) 1, "Monsoon", true);

        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.getSingleResult()).thenReturn("ok");

        lenient().when(entityManager.createQuery(anyString(), eq(Plot.class))).thenReturn(plotQuery);
        lenient().when(plotQuery.setLockMode(any())).thenReturn(plotQuery);
        lenient().when(plotQuery.setParameter(anyString(), any())).thenReturn(plotQuery);
        lenient().when(plotQuery.getResultList()).thenReturn(List.of(activePlot));
    }

    // =========================================================================
    // CREATE TESTS (Test Cases 1–20)
    // =========================================================================

    @Nested
    @DisplayName("Create Cultivation Tests")
    class CreateTests {

        @Test
        @DisplayName("1. Valid cultivation creation")
        void testValidCultivationCreation() {
            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(),
                    activeCrop.getId(),
                    activeVariety.getId(),
                    activeSeason.getId(),
                    (short) 2026,
                    new BigDecimal("5.00"),
                    LocalDate.of(2026, 6, 15)
            );

            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(cropVarietyRepository.findById(activeVariety.getId())).thenReturn(Optional.of(activeVariety));
            when(seasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));
            when(cultivationRepository.existsActiveCycle(activePlot.getId(), activeSeason.getId(), (short) 2026)).thenReturn(false);
            when(cultivationRepository.sumActiveCultivatedArea(activePlot.getId(), activeSeason.getId(), (short) 2026))
                    .thenReturn(BigDecimal.ZERO);

            when(cultivationRepository.saveAndFlush(any(CultivationIntention.class))).thenAnswer(inv -> {
                CultivationIntention ci = inv.getArgument(0);
                ci.setId(UUID.randomUUID());
                return ci;
            });

            CultivationResponseDto response = service.createCultivation(request, null);

            assertThat(response).isNotNull();
            assertThat(response.plotId()).isEqualTo(activePlot.getId());
            assertThat(response.farmerId()).isEqualTo(farmerId);
            assertThat(response.cropId()).isEqualTo(activeCrop.getId());
            assertThat(response.cropVarietyId()).isEqualTo(activeVariety.getId());
            assertThat(response.seasonId()).isEqualTo(activeSeason.getId());
            assertThat(response.cultivationYear()).isEqualTo((short) 2026);
            assertThat(response.cultivatedAreaAcres()).isEqualTo(new BigDecimal("5.00"));
            assertThat(response.status()).isEqualTo("PLANNED");
            assertThat(response.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("2. Farmer profile not found -> 404")
        void testFarmerNotFound() {
            when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.empty());

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Farmer profile not found");
        }

        @Test
        @DisplayName("3. Suspended farmer cannot create cultivation -> 403")
        void testSuspendedFarmer() {
            activeFarmer.setProfileStatus(FarmerProfile.STATUS_SUSPENDED);

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Account is suspended");
        }

        @Test
        @DisplayName("4. Deleted farmer treated as non-existent -> 404")
        void testDeletedFarmer() {
            activeFarmer.setProfileStatus(FarmerProfile.STATUS_DELETED);

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Farmer profile not found");
        }

        @Test
        @DisplayName("5 & 6. Foreign or non-existent plot -> 404 (IDOR guard)")
        void testForeignPlot() {
            when(plotQuery.getResultList()).thenReturn(Collections.emptyList());

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    UUID.randomUUID(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plot not found");
        }

        @Test
        @DisplayName("7. Inactive plot rejected -> 400")
        void testInactivePlot() {
            activePlot.setActive(false);

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot create cultivation on an inactive plot");
        }

        @Test
        @DisplayName("8. Inactive crop rejected -> 400")
        void testInactiveCrop() {
            activeCrop.setActive(false);
            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Selected crop is inactive");
        }

        @Test
        @DisplayName("9. Inactive variety rejected -> 400")
        void testInactiveVariety() {
            activeVariety.setActive(false);
            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(cropVarietyRepository.findById(activeVariety.getId())).thenReturn(Optional.of(activeVariety));

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), activeVariety.getId(), activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Selected crop variety is inactive");
        }

        @Test
        @DisplayName("10. Variety does not belong to specified crop -> 400")
        void testVarietyCropMismatch() {
            Crop otherCrop = new Crop(UUID.randomUUID(), "Cotton", "Fiber", true);
            CropVariety mismatchVariety = new CropVariety(UUID.randomUUID(), otherCrop, "Bt Cotton", true);

            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(cropVarietyRepository.findById(mismatchVariety.getId())).thenReturn(Optional.of(mismatchVariety));

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), mismatchVariety.getId(), activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong to crop");
        }

        @Test
        @DisplayName("11 & 12. Inactive season rejected -> 400")
        void testInactiveSeason() {
            activeSeason.setActive(false);
            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(seasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Selected season is inactive");
        }

        @Test
        @DisplayName("13. Invalid year (< 2000 or > 2100) -> 400")
        void testInvalidYear() {
            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(seasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 1999, new BigDecimal("2.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cultivation year must be between 2000 and 2100");
        }

        @Test
        @DisplayName("17. Cultivated area greater than plot total area -> 409")
        void testAreaGreaterThanPlot() {
            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(seasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));

            // Plot area is 10.00, request is 15.00
            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("15.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("exceeds plot total area");
        }

        @Test
        @DisplayName("18. Duplicate cultivation lineage on same plot + season + year -> 409")
        void testDuplicateLineage() {
            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(seasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));
            when(cultivationRepository.existsActiveCycle(activePlot.getId(), activeSeason.getId(), (short) 2026)).thenReturn(true);

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("5.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("An active cultivation cycle already exists for this plot, season, and year.");
        }

        @Test
        @DisplayName("19 & 20. Same plot can cultivate in different season or different year")
        void testSamePlotDifferentSeasonOrYear() {
            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(seasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));
            when(cultivationRepository.existsActiveCycle(eq(activePlot.getId()), any(), any())).thenReturn(false);
            when(cultivationRepository.sumActiveCultivatedArea(eq(activePlot.getId()), any(), any())).thenReturn(BigDecimal.ZERO);

            when(cultivationRepository.saveAndFlush(any(CultivationIntention.class))).thenAnswer(inv -> {
                CultivationIntention ci = inv.getArgument(0);
                ci.setId(UUID.randomUUID());
                return ci;
            });

            // Season 1, Year 2026
            CreateCultivationRequestDto request1 = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("10.00"), LocalDate.of(2026, 6, 15)
            );
            CultivationResponseDto res1 = service.createCultivation(request1, null);
            assertThat(res1).isNotNull();

            // Same plot, Season 1, Year 2027
            CreateCultivationRequestDto request2 = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2027, new BigDecimal("10.00"), LocalDate.of(2027, 6, 15)
            );
            CultivationResponseDto res2 = service.createCultivation(request2, null);
            assertThat(res2).isNotNull();
        }
    }

    // =========================================================================
    // LIFECYCLE & STATE TRANSITION TESTS (Test Cases 21–30)
    // =========================================================================

    @Nested
    @DisplayName("Lifecycle State Machine Tests")
    class LifecycleTests {

        private CultivationIntention intention;

        @BeforeEach
        void initIntention() {
            intention = new CultivationIntention();
            intention.setId(UUID.randomUUID());
            intention.setPlot(activePlot);
            intention.setFarmerId(farmerId);
            intention.setCrop(activeCrop);
            intention.setSeason(activeSeason);
            intention.setCultivationYear((short) 2026);
            intention.setCultivatedAreaAcres(new BigDecimal("5.00"));
            intention.setPlannedPlantingDate(LocalDate.of(2026, 6, 15));
            intention.setStatus(CultivationIntention.STATUS_PLANNED);
            intention.setVersion(1);

            when(cultivationRepository.findByIdAndFarmerIdWithDetails(intention.getId(), farmerId))
                    .thenReturn(Optional.of(intention));
        }

        @Test
        @DisplayName("22. PLANNED -> STARTED requires actualPlantingDate")
        void testPlannedToStartedSuccess() {
            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "STARTED", "Sowing commenced", LocalDate.of(2026, 6, 18), new BigDecimal("4.80"), null, 1
            );

            when(cultivationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            CultivationResponseDto response = service.updateCultivationStatus(intention.getId(), request);
            assertThat(response.status()).isEqualTo("STARTED");
            assertThat(response.actualPlantingDate()).isEqualTo(LocalDate.of(2026, 6, 18));
            assertThat(response.actualAreaAcres()).isEqualTo(new BigDecimal("4.80"));
        }

        @Test
        @DisplayName("PLANNED -> STARTED without actualPlantingDate -> 400")
        void testPlannedToStartedMissingDate() {
            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "STARTED", null, null, null, null, null
            );

            assertThatThrownBy(() -> service.updateCultivationStatus(intention.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("actualPlantingDate is required");
        }

        @Test
        @DisplayName("PLANNED -> STARTED with actualArea exceeding cultivated allocation -> 400")
        void testPlannedToStartedExcessActualArea() {
            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "STARTED", null, LocalDate.of(2026, 6, 18), new BigDecimal("6.00"), null, null
            );

            assertThatThrownBy(() -> service.updateCultivationStatus(intention.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot exceed cultivated allocation");
        }

        @Test
        @DisplayName("23. PLANNED -> CANCELLED success")
        void testPlannedToCancelled() {
            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "CANCELLED", "Water shortage", null, null, null, 1
            );

            when(cultivationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            CultivationResponseDto response = service.updateCultivationStatus(intention.getId(), request);
            assertThat(response.status()).isEqualTo("CANCELLED");
            assertThat(response.statusReason()).isEqualTo("Water shortage");
        }

        @Test
        @DisplayName("24. PLANNED -> ABANDONED is illegal -> 409")
        void testPlannedToAbandonedIllegal() {
            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "ABANDONED", "Gave up early", null, null, null, null
            );

            assertThatThrownBy(() -> service.updateCultivationStatus(intention.getId(), request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Invalid status transition from PLANNED to ABANDONED");
        }

        @Test
        @DisplayName("25. STARTED -> COMPLETED requires harvestDate and actualPlantingDate")
        void testStartedToCompletedSuccess() {
            intention.setStatus(CultivationIntention.STATUS_STARTED);
            intention.setActualPlantingDate(LocalDate.of(2026, 6, 20));

            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "COMPLETED", "Good yield", null, null, LocalDate.of(2026, 11, 15), 1
            );

            when(cultivationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            CultivationResponseDto response = service.updateCultivationStatus(intention.getId(), request);
            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(response.harvestDate()).isEqualTo(LocalDate.of(2026, 11, 15));
        }

        @Test
        @DisplayName("STARTED -> COMPLETED with harvestDate before plantingDate -> 400")
        void testHarvestBeforePlanting() {
            intention.setStatus(CultivationIntention.STATUS_STARTED);
            intention.setActualPlantingDate(LocalDate.of(2026, 6, 20));

            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "COMPLETED", null, null, null, LocalDate.of(2026, 6, 10), null
            );

            assertThatThrownBy(() -> service.updateCultivationStatus(intention.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("must be on or after actual planting date");
        }

        @Test
        @DisplayName("26. STARTED -> CANCELLED is illegal -> 409")
        void testStartedToCancelledIllegal() {
            intention.setStatus(CultivationIntention.STATUS_STARTED);

            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "CANCELLED", "Too late", null, null, null, null
            );

            assertThatThrownBy(() -> service.updateCultivationStatus(intention.getId(), request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Invalid status transition from STARTED to CANCELLED");
        }

        @Test
        @DisplayName("27. STARTED -> ABANDONED success")
        void testStartedToAbandonedSuccess() {
            intention.setStatus(CultivationIntention.STATUS_STARTED);

            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "ABANDONED", "Pest damage destroyed crop", null, null, null, null
            );

            when(cultivationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            CultivationResponseDto response = service.updateCultivationStatus(intention.getId(), request);
            assertThat(response.status()).isEqualTo("ABANDONED");
            assertThat(response.statusReason()).isEqualTo("Pest damage destroyed crop");
        }

        @Test
        @DisplayName("28 & 30. Terminal state immutability -> 409")
        void testTerminalStateImmutability() {
            intention.setStatus(CultivationIntention.STATUS_COMPLETED);

            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "STARTED", "Reopening", null, null, null, null
            );

            assertThatThrownBy(() -> service.updateCultivationStatus(intention.getId(), request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Terminal cultivation cycle cannot be edited.");
        }
    }

    // =========================================================================
    // AREA CAPACITY TESTS (Test Cases 31–38)
    // =========================================================================

    @Nested
    @DisplayName("Area Capacity Tests")
    class AreaTests {

        @Test
        @DisplayName("31 & 32. Allocation up to plot area is accepted")
        void testAllocationUpToPlotArea() {
            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("10.00"), LocalDate.of(2026, 6, 15)
            );

            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(seasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));
            when(cultivationRepository.sumActiveCultivatedArea(any(), any(), any())).thenReturn(BigDecimal.ZERO);

            when(cultivationRepository.saveAndFlush(any())).thenAnswer(inv -> {
                CultivationIntention ci = inv.getArgument(0);
                ci.setId(UUID.randomUUID());
                return ci;
            });

            CultivationResponseDto res = service.createCultivation(request, null);
            assertThat(res.cultivatedAreaAcres()).isEqualTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("34 & 35. Multiple allocations: sum exceeding capacity rejected -> 409")
        void testMultipleAllocationsExceedCapacity() {
            // Existing allocation = 6.00, Plot = 10.00, new request = 5.00 -> total 11.00 > 10.00
            when(cropRepository.findById(activeCrop.getId())).thenReturn(Optional.of(activeCrop));
            when(seasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));
            when(cultivationRepository.sumActiveCultivatedArea(any(), any(), any())).thenReturn(new BigDecimal("6.00"));

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("5.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("would exceed plot capacity");
        }
    }

    // =========================================================================
    // UPDATE TESTS (Test Cases 39–46)
    // =========================================================================

    @Nested
    @DisplayName("Update Mutable Fields Tests")
    class UpdateTests {

        private CultivationIntention intention;

        @BeforeEach
        void initIntention() {
            intention = new CultivationIntention();
            intention.setId(UUID.randomUUID());
            intention.setPlot(activePlot);
            intention.setFarmerId(farmerId);
            intention.setCrop(activeCrop);
            intention.setSeason(activeSeason);
            intention.setCultivationYear((short) 2026);
            intention.setCultivatedAreaAcres(new BigDecimal("5.00"));
            intention.setPlannedPlantingDate(LocalDate.of(2026, 6, 15));
            intention.setStatus(CultivationIntention.STATUS_PLANNED);
            intention.setVersion(1);

            when(cultivationRepository.findByIdAndFarmerIdWithDetails(intention.getId(), farmerId))
                    .thenReturn(Optional.of(intention));
        }

        @Test
        @DisplayName("39. Valid PLANNED update: crop, area, date")
        void testValidPlannedUpdate() {
            Crop newCrop = new Crop(UUID.randomUUID(), "Cotton", "Fiber", true);
            when(cropRepository.findById(newCrop.getId())).thenReturn(Optional.of(newCrop));
            when(cultivationRepository.sumActiveCultivatedAreaExcluding(any(), any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            UpdateCultivationRequestDto request = new UpdateCultivationRequestDto(
                    newCrop.getId(), null, new BigDecimal("6.00"), LocalDate.of(2026, 6, 25), 1
            );

            when(cultivationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            CultivationResponseDto res = service.updateCultivation(intention.getId(), request);
            assertThat(res.cropId()).isEqualTo(newCrop.getId());
            assertThat(res.cultivatedAreaAcres()).isEqualTo(new BigDecimal("6.00"));
            assertThat(res.plannedPlantingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        }

        @Test
        @DisplayName("40. Field updates on STARTED status rejected -> 400")
        void testUpdateOnStartedRejected() {
            intention.setStatus(CultivationIntention.STATUS_STARTED);

            UpdateCultivationRequestDto request = new UpdateCultivationRequestDto(
                    null, null, new BigDecimal("6.00"), null, null
            );

            assertThatThrownBy(() -> service.updateCultivation(intention.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Field edits are only permitted in PLANNED status");
        }

        @Test
        @DisplayName("41. Field updates on terminal status rejected -> 409")
        void testUpdateOnTerminalRejected() {
            intention.setStatus(CultivationIntention.STATUS_CANCELLED);

            UpdateCultivationRequestDto request = new UpdateCultivationRequestDto(
                    null, null, new BigDecimal("6.00"), null, null
            );

            assertThatThrownBy(() -> service.updateCultivation(intention.getId(), request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Terminal cultivation cycle cannot be edited.");
        }

        @Test
        @DisplayName("Optimistic version conflict -> 409")
        void testVersionConflict() {
            UpdateCultivationRequestDto request = new UpdateCultivationRequestDto(
                    null, null, new BigDecimal("4.00"), null, 99
            );

            assertThatThrownBy(() -> service.updateCultivation(intention.getId(), request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Version conflict");
        }
    }

    // =========================================================================
    // SECURITY & IDOR TESTS (Test Cases 47–52)
    // =========================================================================

    @Nested
    @DisplayName("Security & IDOR Isolation Tests")
    class SecurityTests {

        @Test
        @DisplayName("47. Farmer A reads own cultivation -> 200")
        void testReadOwnCultivation() {
            CultivationIntention intention = new CultivationIntention();
            intention.setId(UUID.randomUUID());
            intention.setFarmerId(farmerId);
            intention.setPlot(activePlot);
            intention.setCrop(activeCrop);
            intention.setSeason(activeSeason);
            intention.setCultivationYear((short) 2026);
            intention.setCultivatedAreaAcres(new BigDecimal("3.00"));
            intention.setPlannedPlantingDate(LocalDate.of(2026, 6, 15));

            when(cultivationRepository.findByIdAndFarmerIdWithDetails(intention.getId(), farmerId))
                    .thenReturn(Optional.of(intention));

            CultivationResponseDto dto = service.getCultivation(intention.getId());
            assertThat(dto).isNotNull();
            assertThat(dto.farmerId()).isEqualTo(farmerId);
        }

        @Test
        @DisplayName("48. Farmer A reading Farmer B's cultivation -> 404 (Anti-Enumeration)")
        void testCrossFarmerRead404() {
            UUID foreignId = UUID.randomUUID();
            when(cultivationRepository.findByIdAndFarmerIdWithDetails(foreignId, farmerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCultivation(foreignId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cultivation intention not found");
        }

        @Test
        @DisplayName("49. Farmer A updating Farmer B's cultivation -> 404")
        void testCrossFarmerUpdate404() {
            UUID foreignId = UUID.randomUUID();
            when(cultivationRepository.findByIdAndFarmerIdWithDetails(foreignId, farmerId))
                    .thenReturn(Optional.empty());

            UpdateCultivationRequestDto req = new UpdateCultivationRequestDto(null, null, new BigDecimal("4.00"), null, null);
            assertThatThrownBy(() -> service.updateCultivation(foreignId, req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cultivation intention not found");
        }

        @Test
        @DisplayName("50. Farmer A changing Farmer B's status -> 404")
        void testCrossFarmerStatusChange404() {
            UUID foreignId = UUID.randomUUID();
            when(cultivationRepository.findByIdAndFarmerIdWithDetails(foreignId, farmerId))
                    .thenReturn(Optional.empty());

            CultivationStatusUpdateRequestDto req = new CultivationStatusUpdateRequestDto("STARTED", null, LocalDate.now(), null, null, null);
            assertThatThrownBy(() -> service.updateCultivationStatus(foreignId, req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cultivation intention not found");
        }

        @Test
        @DisplayName("51. Farmer A reading Farmer B's history / events -> 404")
        void testCrossFarmerAudit404() {
            UUID foreignId = UUID.randomUUID();
            when(cultivationRepository.existsByIdAndFarmerId(foreignId, farmerId)).thenReturn(false);

            assertThatThrownBy(() -> service.getCultivationEvents(foreignId))
                    .isInstanceOf(ResourceNotFoundException.class);

            assertThatThrownBy(() -> service.getCultivationHistory(foreignId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // IDEMPOTENCY TESTS (Test Cases 53–58)
    // =========================================================================

    @Nested
    @DisplayName("Idempotency Tests")
    class IdempotencyTests {

        @Test
        @DisplayName("53. Same key + same payload -> cached response replay")
        void testSameKeySamePayloadReplay() throws Exception {
            String key = "idem-key-100";
            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("3.00"), LocalDate.of(2026, 6, 15)
            );

            // Mock pre-existing cached idempotency record
            CultivationResponseDto cachedDto = new CultivationResponseDto(
                    UUID.randomUUID(), activePlot.getId(), "Plot 1", farmerId,
                    activeCrop.getId(), "Paddy", "Cereals", null, null,
                    activeSeason.getId(), "Kharif", (short) 2026, new BigDecimal("3.00"),
                    LocalDate.of(2026, 6, 15), null, null, null, "PLANNED", null,
                    1, "FARMER_APP", OffsetDateTime.now(), OffsetDateTime.now()
            );
            String serialized = objectMapper.writeValueAsString(cachedDto);

            // Compute hash for request
            String input = farmerId + "|" + request.plotId() + "|" +
                    request.cropId() + "|" + request.cropVarietyId() + "|" +
                    request.seasonId() + "|" + request.cultivationYear() + "|" +
                    request.cultivatedAreaAcres() + "|" + request.plannedPlantingDate();
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

            IdempotencyKey record = new IdempotencyKey(farmerId, key, hash, 201, serialized, OffsetDateTime.now().plusHours(24));
            when(idempotencyKeyRepository.findValidByFarmerAndKey(farmerId, key)).thenReturn(Optional.of(record));

            CultivationResponseDto result = service.createCultivation(request, key);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(cachedDto.id());
            // Verify no new intention was saved
            verify(cultivationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("54. Same key + different payload -> 409 Conflict")
        void testSameKeyDifferentPayloadConflict() {
            String key = "idem-key-100";
            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("3.00"), LocalDate.of(2026, 6, 15)
            );

            IdempotencyKey record = new IdempotencyKey(
                    farmerId, key, "different-payload-hash-xyz", 201, "{}", OffsetDateTime.now().plusHours(24)
            );
            when(idempotencyKeyRepository.findValidByFarmerAndKey(farmerId, key)).thenReturn(Optional.of(record));

            assertThatThrownBy(() -> service.createCultivation(request, key))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("was already used with a different request payload");
        }

        @Test
        @DisplayName("Key length > 128 characters -> 400 Bad Request")
        void testKeyLengthExceeded() {
            String longKey = "A".repeat(129);
            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    activePlot.getId(), activeCrop.getId(), null, activeSeason.getId(),
                    (short) 2026, new BigDecimal("3.00"), LocalDate.of(2026, 6, 15)
            );

            assertThatThrownBy(() -> service.createCultivation(request, longKey))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("must not exceed 128 characters");
        }
    }

    // =========================================================================
    // LIST & PAGINATION TESTS (Test Cases 70–74)
    // =========================================================================

    @Nested
    @DisplayName("List & Pagination Tests")
    class ListTests {

        @Test
        @DisplayName("List cultivations with status filter")
        void testListWithFilters() {
            CultivationIntention ci = new CultivationIntention();
            ci.setId(UUID.randomUUID());
            ci.setPlot(activePlot);
            ci.setFarmerId(farmerId);
            ci.setCrop(activeCrop);
            ci.setSeason(activeSeason);
            ci.setCultivationYear((short) 2026);
            ci.setCultivatedAreaAcres(new BigDecimal("3.00"));
            ci.setPlannedPlantingDate(LocalDate.of(2026, 6, 15));
            ci.setStatus("PLANNED");

            Page<CultivationIntention> page = new PageImpl<>(List.of(ci));
            when(cultivationRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(page);

            Page<CultivationResponseDto> result = service.listCultivations(
                    PageRequest.of(0, 20), activePlot.getId(), null, null, null, "PLANNED"
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo("PLANNED");
        }
    }
}
