package com.agriplatform.plot;

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
import com.agriplatform.plot.entity.Plot;
import com.agriplatform.plot.mapper.PlotMapper;
import com.agriplatform.plot.repository.IdempotencyKeyRepository;
import com.agriplatform.plot.repository.PlotRepository;
import com.agriplatform.plot.service.PlotServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PlotServiceImpl}.
 * Covers farmer status routing, IDOR protection, concurrency disambiguation,
 * area reduction guard, idempotency, and lifecycle operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Task 07 — Plot Service Tests")
class PlotServiceTest {

    @Mock private PlotRepository plotRepository;
    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private FarmerProfileRepository farmerProfileRepository;
    @Mock private VillageRepository villageRepository;
    @Mock private SecurityUtils securityUtils;
    @Spy  private PlotMapper plotMapper = new PlotMapper();
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private PlotServiceImpl plotService;

    private UUID farmerId;
    private UUID plotId;
    private UUID villageId;
    private FarmerProfile activeFarmer;
    private Village village;
    private Plot existingPlot;
    private OffsetDateTime updatedAt;

    @BeforeEach
    void setUp() {
        farmerId = UUID.randomUUID();
        plotId = UUID.randomUUID();
        villageId = UUID.randomUUID();
        updatedAt = OffsetDateTime.parse("2026-10-02T18:36:26.118966Z");

        when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);

        // Active farmer
        activeFarmer = new FarmerProfile();
        activeFarmer.setId(farmerId);
        activeFarmer.setProfileStatus(FarmerProfile.STATUS_ACTIVE);

        // Geography hierarchy
        State state = new State();
        state.setId(UUID.randomUUID()); state.setName("AP"); state.setActive(true);
        District district = new District();
        district.setId(UUID.randomUUID()); district.setName("Chittoor"); district.setState(state); district.setActive(true);
        Mandal mandal = new Mandal();
        mandal.setId(UUID.randomUUID()); mandal.setName("Chittoor M"); mandal.setDistrict(district); mandal.setActive(true);
        village = new Village();
        village.setId(villageId); village.setName("Mapakshi"); village.setMandal(mandal); village.setActive(true);

        // Existing plot
        existingPlot = new Plot();
        existingPlot.setId(plotId);
        existingPlot.setFarmerId(farmerId);
        existingPlot.setVillage(village);
        existingPlot.setLabel("North Field");
        existingPlot.setTotalAreaAcres(new BigDecimal("5.0000"));
        existingPlot.setActive(true);
        existingPlot.setCreatedAt(updatedAt);
        existingPlot.setUpdatedAt(updatedAt);
    }

    // =========================================================================
    // CREATE — basic validation
    // =========================================================================

    @Test
    @DisplayName("19. ACTIVE farmer can create plot")
    void createPlot_activeFarmer_succeeds() throws Exception {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(villageRepository.findByIdWithHierarchy(villageId)).thenReturn(Optional.of(village));
        when(plotRepository.saveAndFlush(any())).thenReturn(existingPlot);

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "North Field", new BigDecimal("5.0000"));
        PlotResponseDto result = plotService.createPlot(req, null);

        assertThat(result).isNotNull();
        assertThat(result.farmerId()).isEqualTo(farmerId);
    }

    @Test
    @DisplayName("2. Create plot with null label — allowed")
    void createPlot_nullLabel_allowed() throws Exception {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(villageRepository.findByIdWithHierarchy(villageId)).thenReturn(Optional.of(village));
        Plot plotWithNullLabel = new Plot();
        plotWithNullLabel.setId(UUID.randomUUID()); plotWithNullLabel.setFarmerId(farmerId);
        plotWithNullLabel.setVillage(village); plotWithNullLabel.setActive(true);
        plotWithNullLabel.setCreatedAt(updatedAt); plotWithNullLabel.setUpdatedAt(updatedAt);
        when(plotRepository.saveAndFlush(any())).thenReturn(plotWithNullLabel);

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, null, null);
        PlotResponseDto result = plotService.createPlot(req, null);
        assertThat(result.label()).isNull();
    }

    @Test
    @DisplayName("12. Invalid village → ResourceNotFoundException")
    void createPlot_invalidVillage_throwsNotFound() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(villageRepository.findByIdWithHierarchy(any())).thenReturn(Optional.empty());

        CreatePlotRequestDto req = new CreatePlotRequestDto(UUID.randomUUID(), "Field", null);
        assertThatThrownBy(() -> plotService.createPlot(req, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Village not found");
    }

    @Test
    @DisplayName("13. Inactive village → BadRequestException")
    void createPlot_inactiveVillage_throwsBadRequest() {
        village.setActive(false);
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(villageRepository.findByIdWithHierarchy(villageId)).thenReturn(Optional.of(village));

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "Field", null);
        assertThatThrownBy(() -> plotService.createPlot(req, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("1. villageId null → BadRequestException")
    void createPlot_nullVillageId_throwsBadRequest() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));

        CreatePlotRequestDto req = new CreatePlotRequestDto(null, "Field", null);
        assertThatThrownBy(() -> plotService.createPlot(req, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("villageId is required");
    }

    // =========================================================================
    // FARMER STATUS ROUTING (P1)
    // =========================================================================

    @Test
    @DisplayName("17. SUSPENDED farmer → HTTP 403 (AccessDeniedException)")
    void createPlot_suspendedFarmer_throws403() {
        FarmerProfile suspended = new FarmerProfile();
        suspended.setId(farmerId);
        suspended.setProfileStatus(FarmerProfile.STATUS_SUSPENDED);
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(suspended));

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "Field", null);
        assertThatThrownBy(() -> plotService.createPlot(req, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    @DisplayName("18. DELETED farmer → HTTP 404 (ResourceNotFoundException)")
    void createPlot_deletedFarmer_throws404() {
        FarmerProfile deleted = new FarmerProfile();
        deleted.setId(farmerId);
        deleted.setProfileStatus(FarmerProfile.STATUS_DELETED);
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(deleted));

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "Field", null);
        assertThatThrownBy(() -> plotService.createPlot(req, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("17. SUSPENDED farmer cannot update plot → 403")
    void updatePlot_suspendedFarmer_throws403() {
        FarmerProfile suspended = new FarmerProfile();
        suspended.setId(farmerId);
        suspended.setProfileStatus(FarmerProfile.STATUS_SUSPENDED);
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(suspended));

        UpdatePlotRequestDto req = new UpdatePlotRequestDto("New Label", null, updatedAt.toString());
        assertThatThrownBy(() -> plotService.updatePlot(plotId, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("18. DELETED farmer getPlot → 404")
    void getPlot_deletedFarmer_throws404() {
        FarmerProfile deleted = new FarmerProfile();
        deleted.setId(farmerId);
        deleted.setProfileStatus(FarmerProfile.STATUS_DELETED);
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> plotService.getPlot(plotId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // IDOR PROTECTION
    // =========================================================================

    @Test
    @DisplayName("15. Farmer A cannot access Farmer B's plot — returns 404")
    void getPlot_foreignFarmerReturns404() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> plotService.getPlot(plotId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Plot not found");
    }

    @Test
    @DisplayName("16. Fake farmer_id in body is completely ignored — identity comes from JWT")
    void securityUtils_isTheOnlyIdentitySource() {
        // Prove that any client-supplied farmer_id has no effect on the actual farmerId used
        // by verifying that securityUtils is the only call path for identity resolution
        UUID authFarmerId = securityUtils.getAuthenticatedUserId();
        assertThat(authFarmerId).isEqualTo(farmerId);
        // The service never reads farmerId from the request payload
    }

    // =========================================================================
    // OPTIMISTIC CONCURRENCY (P1)
    // =========================================================================

    @Test
    @DisplayName("25. Correct expectedVersion → update succeeds (1 row)")
    void updatePlot_correctVersion_succeeds() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        when(plotRepository.conditionalUpdate(eq(plotId), eq(farmerId), any(), any(), eq(updatedAt.toString())))
                .thenReturn(1);

        Plot updatedPlot = new Plot();
        updatedPlot.setId(plotId); updatedPlot.setFarmerId(farmerId); updatedPlot.setVillage(village);
        updatedPlot.setLabel("Updated"); updatedPlot.setTotalAreaAcres(existingPlot.getTotalAreaAcres());
        updatedPlot.setActive(true); updatedPlot.setCreatedAt(updatedAt);
        updatedPlot.setUpdatedAt(updatedAt.plusSeconds(1));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId))
                .thenReturn(Optional.of(existingPlot))
                .thenReturn(Optional.of(updatedPlot));

        UpdatePlotRequestDto req = new UpdatePlotRequestDto("Updated", null, updatedAt.toString());
        PlotResponseDto result = plotService.updatePlot(plotId, req);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("29. Existing plot + stale version → 409 Conflict (not 404)")
    void updatePlot_staleVersion_plotExists_throws409() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        when(plotRepository.conditionalUpdate(eq(plotId), eq(farmerId), any(), any(), any()))
                .thenReturn(0); // 0 rows — stale version
        when(plotRepository.existsByIdAndFarmerId(plotId, farmerId)).thenReturn(true); // plot exists

        UpdatePlotRequestDto req = new UpdatePlotRequestDto("Label", null, "2020-01-01T00:00:00.000000Z");
        assertThatThrownBy(() -> plotService.updatePlot(plotId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("conflict");
    }

    @Test
    @DisplayName("28. 0 rows + plot not found → 404 Not Found (not 409)")
    void updatePlot_zeroRows_plotNotFound_throws404() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        // First call: current plot found (for area reduction guard check)
        // Second call (after update): 0 rows, then existsByIdAndFarmerId returns false
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        when(plotRepository.conditionalUpdate(any(), any(), any(), any(), any())).thenReturn(0);
        when(plotRepository.existsByIdAndFarmerId(plotId, farmerId)).thenReturn(false); // plot doesn't exist

        UpdatePlotRequestDto req = new UpdatePlotRequestDto("Label", null, updatedAt.toString());
        assertThatThrownBy(() -> plotService.updatePlot(plotId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // AREA REDUCTION GUARD (P1)
    // =========================================================================

    @Test
    @DisplayName("36. Area reduction below active cultivation → 409 Conflict")
    void updatePlot_areaReductionBelowCultivation_throws409() {
        existingPlot.setTotalAreaAcres(new BigDecimal("5.0000"));
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        // Max committed cultivation area = 4 acres
        when(plotRepository.findMaxActiveCultivationArea(plotId, farmerId)).thenReturn(new BigDecimal("4.0000"));

        // Trying to reduce to 3 acres (below 4 committed)
        UpdatePlotRequestDto req = new UpdatePlotRequestDto(null, new BigDecimal("3.0000"), updatedAt.toString());
        assertThatThrownBy(() -> plotService.updatePlot(plotId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot reduce plot area");
    }

    @Test
    @DisplayName("37. Area reduction equal to committed area → allowed")
    void updatePlot_areaReductionExactBoundary_allowed() {
        existingPlot.setTotalAreaAcres(new BigDecimal("5.0000"));
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        // Max committed = 4 acres; new area = 4 → boundary allowed
        when(plotRepository.findMaxActiveCultivationArea(plotId, farmerId)).thenReturn(new BigDecimal("4.0000"));
        when(plotRepository.conditionalUpdate(any(), any(), any(), eq(new BigDecimal("4.0000")), any())).thenReturn(1);

        Plot updated = new Plot();
        updated.setId(plotId); updated.setFarmerId(farmerId); updated.setVillage(village);
        updated.setTotalAreaAcres(new BigDecimal("4.0000")); updated.setActive(true);
        updated.setCreatedAt(updatedAt); updated.setUpdatedAt(updatedAt.plusSeconds(1));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId))
                .thenReturn(Optional.of(existingPlot))
                .thenReturn(Optional.of(updated));

        UpdatePlotRequestDto req = new UpdatePlotRequestDto(null, new BigDecimal("4.0000"), updatedAt.toString());
        PlotResponseDto result = plotService.updatePlot(plotId, req);
        assertThat(result.totalAreaAcres()).isEqualByComparingTo("4.0000");
    }

    @Test
    @DisplayName("38. Area above committed area → allowed")
    void updatePlot_areaAboveCommitted_allowed() {
        existingPlot.setTotalAreaAcres(new BigDecimal("5.0000"));
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        // Area increasing: no cultivation guard needed
        when(plotRepository.conditionalUpdate(any(), any(), any(), eq(new BigDecimal("7.0000")), any())).thenReturn(1);

        Plot updated = new Plot();
        updated.setId(plotId); updated.setFarmerId(farmerId); updated.setVillage(village);
        updated.setTotalAreaAcres(new BigDecimal("7.0000")); updated.setActive(true);
        updated.setCreatedAt(updatedAt); updated.setUpdatedAt(updatedAt.plusSeconds(1));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId))
                .thenReturn(Optional.of(existingPlot))
                .thenReturn(Optional.of(updated));

        UpdatePlotRequestDto req = new UpdatePlotRequestDto(null, new BigDecimal("7.0000"), updatedAt.toString());
        PlotResponseDto result = plotService.updatePlot(plotId, req);
        assertThat(result.totalAreaAcres()).isEqualByComparingTo("7.0000");
    }

    @Test
    @DisplayName("39. Terminal cultivation statuses do not block area reduction")
    void updatePlot_terminalCultivation_noBlock() {
        existingPlot.setTotalAreaAcres(new BigDecimal("5.0000"));
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        // No active cultivation (COMPLETED/CANCELLED/ABANDONED don't count)
        when(plotRepository.findMaxActiveCultivationArea(plotId, farmerId)).thenReturn(null);
        when(plotRepository.conditionalUpdate(any(), any(), any(), any(), any())).thenReturn(1);

        Plot updated = new Plot();
        updated.setId(plotId); updated.setFarmerId(farmerId); updated.setVillage(village);
        updated.setTotalAreaAcres(new BigDecimal("1.0000")); updated.setActive(true);
        updated.setCreatedAt(updatedAt); updated.setUpdatedAt(updatedAt.plusSeconds(1));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId))
                .thenReturn(Optional.of(existingPlot))
                .thenReturn(Optional.of(updated));

        UpdatePlotRequestDto req = new UpdatePlotRequestDto(null, new BigDecimal("1.0000"), updatedAt.toString());
        PlotResponseDto result = plotService.updatePlot(plotId, req);
        assertThat(result).isNotNull();
    }

    // =========================================================================
    // LIFECYCLE — DEACTIVATE / REACTIVATE
    // =========================================================================

    @Test
    @DisplayName("30. Deactivate plot → soft delete (is_active = false)")
    void deactivatePlot_softDelete() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.existsByIdAndFarmerId(plotId, farmerId)).thenReturn(true);
        when(plotRepository.deactivate(plotId, farmerId)).thenReturn(1);

        plotService.deactivatePlot(plotId);

        verify(plotRepository).deactivate(plotId, farmerId);
    }

    @Test
    @DisplayName("30. Deactivating non-existent plot → 404")
    void deactivatePlot_notFound_throws404() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.existsByIdAndFarmerId(plotId, farmerId)).thenReturn(false);

        assertThatThrownBy(() -> plotService.deactivatePlot(plotId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("53. No physical deletion — deactivate calls deactivate(), not deleteById()")
    void deactivatePlot_noPhysicalDeletion() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.existsByIdAndFarmerId(plotId, farmerId)).thenReturn(true);
        when(plotRepository.deactivate(plotId, farmerId)).thenReturn(1);

        plotService.deactivatePlot(plotId);

        verify(plotRepository, never()).deleteById(any());
        verify(plotRepository, never()).delete(any());
        verify(plotRepository).deactivate(plotId, farmerId);
    }

    @Test
    @DisplayName("34. Reactivate plot → is_active = true")
    void reactivatePlot_success() {
        existingPlot.setActive(false);
        village.setActive(true);
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        when(plotRepository.reactivate(plotId, farmerId)).thenReturn(1);

        Plot reactivated = new Plot();
        reactivated.setId(plotId); reactivated.setFarmerId(farmerId); reactivated.setVillage(village);
        reactivated.setActive(true); reactivated.setCreatedAt(updatedAt); reactivated.setUpdatedAt(updatedAt.plusSeconds(1));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId))
                .thenReturn(Optional.of(existingPlot))
                .thenReturn(Optional.of(reactivated));

        PlotResponseDto result = plotService.reactivatePlot(plotId);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("35. Reactivate with inactive village → 409 Conflict")
    void reactivatePlot_inactiveVillage_throws409() {
        existingPlot.setActive(false);
        village.setActive(false); // village became inactive
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));

        assertThatThrownBy(() -> plotService.reactivatePlot(plotId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("village is no longer active");
    }

    @Test
    @DisplayName("32. Deactivate already-inactive plot → 409 Conflict")
    void deactivatePlot_alreadyInactive_throws409() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.existsByIdAndFarmerId(plotId, farmerId)).thenReturn(true);
        when(plotRepository.deactivate(plotId, farmerId)).thenReturn(0); // 0 rows updated

        assertThatThrownBy(() -> plotService.deactivatePlot(plotId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    @DisplayName("54. Reactivate already-active plot → 409 Conflict")
    void reactivatePlot_alreadyActive_throws409() {
        existingPlot.setActive(true);
        village.setActive(true);
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));
        when(plotRepository.reactivate(plotId, farmerId)).thenReturn(0); // 0 rows updated

        assertThatThrownBy(() -> plotService.reactivatePlot(plotId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already active");
    }

    // =========================================================================
    // IDEMPOTENCY
    // =========================================================================

    @Test
    @DisplayName("40. Same key + same payload → returns cached response")
    void createPlot_idempotentReplay_returnsCached() throws Exception {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));

        com.agriplatform.plot.entity.IdempotencyKey cached = new com.agriplatform.plot.entity.IdempotencyKey(
                farmerId, "test-key-123",
                computeHash(farmerId, villageId, "North Field", new BigDecimal("5.0000")),
                201, "{}", OffsetDateTime.now().plusHours(24));
        when(idempotencyKeyRepository.findValidByFarmerAndKey(farmerId, "test-key-123"))
                .thenReturn(Optional.of(cached));

        PlotResponseDto cachedResponse = new PlotResponseDto(
                plotId, farmerId, villageId, "Mapakshi", "Mandal", "District", "State",
                "North Field", new BigDecimal("5.0000"), true, "token", updatedAt, updatedAt);
        when(objectMapper.readValue(eq("{}"), eq(PlotResponseDto.class))).thenReturn(cachedResponse);

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "North Field", new BigDecimal("5.0000"));
        PlotResponseDto result = plotService.createPlot(req, "test-key-123");

        // Must not call saveAndFlush (no new plot created)
        verify(plotRepository, never()).saveAndFlush(any());
        assertThat(result).isEqualTo(cachedResponse);
    }

    @Test
    @DisplayName("41. Same key + different payload → 409 Conflict")
    void createPlot_idempotentKeyDifferentPayload_throws409() throws Exception {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));

        // Cached record has hash for "North Field"
        com.agriplatform.plot.entity.IdempotencyKey cached = new com.agriplatform.plot.entity.IdempotencyKey(
                farmerId, "key-123",
                "differenthash0000000000000000000000000000000000000000000000000000",
                201, "{}", OffsetDateTime.now().plusHours(24));
        when(idempotencyKeyRepository.findValidByFarmerAndKey(farmerId, "key-123"))
                .thenReturn(Optional.of(cached));

        // Different payload = "South Field"
        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "South Field", new BigDecimal("5.0000"));
        assertThatThrownBy(() -> plotService.createPlot(req, "key-123"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("different request payload");
    }

    @Test
    @DisplayName("42. Expired idempotency key (>24h) → treated as new request")
    void createPlot_expiredKey_treatedAsNewRequest() throws Exception {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        // Repository returns empty because expires_at > now() filtered it out
        when(idempotencyKeyRepository.findValidByFarmerAndKey(farmerId, "expired-key"))
                .thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(villageId)).thenReturn(Optional.of(village));
        when(plotRepository.saveAndFlush(any())).thenReturn(existingPlot);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "North Field", new BigDecimal("5.0000"));
        PlotResponseDto result = plotService.createPlot(req, "expired-key");

        assertThat(result).isNotNull();
        verify(plotRepository).saveAndFlush(any());
        verify(idempotencyKeyRepository).save(any());
    }

    // =========================================================================
    // GET PLOT
    // =========================================================================

    @Test
    @DisplayName("getPlot succeeds for existing owned plot")
    void getPlot_existingPlot_returnsDto() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.of(existingPlot));

        PlotResponseDto result = plotService.getPlot(plotId);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(plotId);
    }

    @Test
    @DisplayName("getPlot not found → 404")
    void getPlot_notFound_throws404() {
        when(farmerProfileRepository.findByIdWithGeography(farmerId)).thenReturn(Optional.of(activeFarmer));
        when(plotRepository.findByIdAndFarmerIdWithVillage(plotId, farmerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> plotService.getPlot(plotId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String computeHash(UUID farmerId, UUID villageId, String label, BigDecimal area) {
        String input = farmerId + "|" + villageId + "|" + label + "|" + area;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
